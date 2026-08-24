/* ============================================================
   modules/activities.js — Activity scheduling & management
   ============================================================ */

import { apiActivitiesUrl, apiCampaignsUrl, apiSegmentsUrl, apiTemplatesUrl, apiFetch } from '../api.js';
import { fmtDate, escHtml } from '../utils.js';
import { modal, deleteModalMessage, showToast, testFireModal, cloneModal } from '../ui.js';
import { state, setDeleteContext } from '../state.js';

// ── Internal module state ────────────────────────────────────
const campaignsById = {};

// ── Helper dropdowns ────────────────────────────────────────
async function loadCampaignDropdowns() {
    try {
        if (state.cachedCampaigns.length === 0) state.cachedCampaigns = await apiFetch(apiCampaignsUrl);
        const sel = document.getElementById("actCampaign");
        if (!sel) return;
        const currentVal = sel.value;
        sel.innerHTML = '<option value="">— select —</option>';
        // Clear map
        for (const k in campaignsById) delete campaignsById[k];
        state.cachedCampaigns.forEach(c => {
            campaignsById[c.id] = c;
            const opt = document.createElement("option");
            opt.value = c.id;
            opt.textContent = c.name;
            sel.appendChild(opt);
        });
        if (currentVal) sel.value = currentVal;
    } catch (e) { console.error(e); }
}

async function loadSegmentDropdownForActivity() {
    try {
        const segments = await apiFetch(apiSegmentsUrl);
        const sel = document.getElementById("actSegment");
        if (!sel) return;
        const currentVal = sel.value;
        sel.innerHTML = '<option value="">— select a segment —</option>';
        segments.forEach(s => {
            const opt = document.createElement("option");
            opt.value = s.id;
            opt.textContent = s.name;
            sel.appendChild(opt);
        });
        if (currentVal) sel.value = currentVal;
    } catch (e) { console.error("loadSegmentDropdownForActivity:", e); }
}

async function loadTemplateDropdownsForActivity() {
    try {
        if (state.cachedTemplates.length === 0) state.cachedTemplates = await apiFetch(apiTemplatesUrl);
        const sel = document.getElementById("actTemplate");
        if (!sel) return;
        sel.innerHTML = '<option value="">— select —</option>';
        state.cachedTemplates.forEach(t => {
            const opt = document.createElement("option");
            opt.value = t.id;
            opt.textContent = t.name;
            sel.appendChild(opt);
        });
    } catch (e) { console.error(e); }
}

/** Exported for use by app.js bootstrap */
export async function loadTemplateDropdowns() {
    return loadTemplateDropdownsForActivity();
}

async function loadWhatsappTemplatesForActivity(wabaId, campaignId) {
    const sel = document.getElementById("actWaTemplate");
    if (!sel) return;
    sel.innerHTML = '<option value="">— loading WhatsApp templates... —</option>';
    try {
        const params = new URLSearchParams();
        if (wabaId)    params.append("wabaId",     wabaId);
        if (campaignId) params.append("campaignId", campaignId);
        const res       = await apiFetch(`/api/whatsapp/templates?${params}`);
        const templates = res.data || [];
        sel.innerHTML = '<option value="">— select a WhatsApp template —</option>';
        templates.forEach(t => {
            const opt = document.createElement("option");
            opt.value = t.name;
            opt.textContent = `${t.name} (${t.language})`;
            sel.appendChild(opt);
        });
    } catch (err) {
        console.error(err);
        sel.innerHTML = '<option value="">— error loading templates —</option>';
    }
}

async function loadVoiceTemplatesForActivity(campaignId) {
    const sel = document.getElementById("actVoiceTemplate");
    if (!sel) return;
    sel.innerHTML = '<option value="">— loading Voice templates... —</option>';
    try {
        const params = new URLSearchParams();
        if (campaignId) params.append("campaignId", campaignId);
        const res       = await apiFetch(`/api/voice/templates?${params}`);
        const templates = res || [];
        sel.innerHTML = '<option value="">— select a Voice template —</option>';
        templates.forEach(t => {
            const opt = document.createElement("option");
            opt.value = t.id;
            opt.textContent = t.name;
            sel.appendChild(opt);
        });
    } catch (e) {
        console.error(e);
        sel.innerHTML = '<option value="">— error loading templates —</option>';
    }
}

function updateTemplateGroupVisibility(c) {
    const emailGrp  = document.getElementById("actEmailTemplateGroup");
    const waGrp     = document.getElementById("actWaTemplateGroup");
    const waLangGrp = document.getElementById("actWaLanguageGroup");
    const voiceGrp  = document.getElementById("actVoiceTemplateGroup");
    const isWA    = c && c.campaignType === "WHATSAPP";
    const isVoice = c && c.campaignType === "VOICE";
    const isEmail = c && c.campaignType === "EMAIL";
    if (emailGrp)  emailGrp.hidden  = !isEmail;
    if (waGrp)     waGrp.hidden     = !isWA;
    if (waLangGrp) waLangGrp.hidden = !isWA;
    if (voiceGrp)  voiceGrp.hidden  = !isVoice;
}

// ── Execution logs modal ────────────────────────────────────
async function openLogsModal(activityId) {
    try {
        const logs = await apiFetch(`${apiActivitiesUrl}/${activityId}/executions`);
        const existing = document.getElementById("logsModal");
        if (existing) existing.remove();

        const overlay = document.createElement("div");
        overlay.id = "logsModal";
        overlay.className = "modal";
        overlay.style.display = "flex";

        const content = document.createElement("div");
        content.className = "modal-content";
        content.style.cssText = "width:680px;max-width:95vw;text-align:left;max-height:80vh;overflow-y:auto;";

        const heading = document.createElement("h3");
        heading.textContent = "Execution Logs";
        heading.style.cssText = "margin:0 0 16px;font-size:16px;";
        content.appendChild(heading);

        if (logs.length === 0) {
            content.insertAdjacentHTML("beforeend", "<p style='color:#999;'>No executions yet.</p>");
        } else {
            const table = document.createElement("table");
            table.className = "data-table";
            table.style.marginBottom = "0";
            table.innerHTML = `<thead><tr><th>Started</th><th>Status</th><th>Recipients</th><th>Response</th></tr></thead>`;
            const tbody = document.createElement("tbody");
            logs.forEach(log => {
                const statusBadge = { SUCCESS: "badge-active", FAILED: "badge-cancelled", PARTIAL_SUCCESS: "badge-paused" }[log.status] || "badge-default";
                const tr = document.createElement("tr");
                tr.innerHTML = `
                    <td>${fmtDate(log.startedAt)}</td>
                    <td><span class="badge ${statusBadge}">${log.status}</span></td>
                    <td>${log.recipientCount ?? "—"}</td>
                    <td class="truncate" style="max-width:200px">${escHtml(log.providerResponse || log.errorMessage || "—")}</td>`;
                tbody.appendChild(tr);
            });
            table.appendChild(tbody);
            content.appendChild(table);
        }

        const closeBtn = document.createElement("button");
        closeBtn.className = "secondary-btn";
        closeBtn.textContent = "Close";
        closeBtn.style.marginTop = "16px";
        closeBtn.onclick = () => overlay.remove();
        content.appendChild(closeBtn);

        overlay.appendChild(content);
        overlay.onclick = ev => { if (ev.target === overlay) overlay.remove(); };
        document.body.appendChild(overlay);
    } catch (err) { showToast("Error loading logs: " + err.message, "error"); }
}

// ── Main exported init ───────────────────────────────────────
export async function loadActivityTable() {
    const activityTableBody = document.getElementById("activityTableBody");
    const activityTable     = document.getElementById("activityTable");
    const activityEmpty     = document.getElementById("activityEmpty");
    const activityFormWrapper = document.getElementById("activityFormWrapper");

    const actScheduleType = document.getElementById("actScheduleType");
    const actRecurrence   = document.getElementById("actRecurrence");
    const oneTimeFields   = document.getElementById("oneTimeFields");
    const recurringFields = document.getElementById("recurringFields");
    const weeklyFields    = document.getElementById("weeklyFields");
    const monthlyFields   = document.getElementById("monthlyFields");
    const activityEditId  = document.getElementById("activityEditId");
    const activityFormTitle = document.getElementById("activityFormTitle");

    try {
        // Pre-populate campaignsById if empty
        if (state.cachedCampaigns.length === 0) state.cachedCampaigns = await apiFetch(apiCampaignsUrl);
        for (const k in campaignsById) delete campaignsById[k];
        state.cachedCampaigns.forEach(c => { campaignsById[c.id] = c; });

        const activities = await apiFetch(apiActivitiesUrl);
        activityTableBody.innerHTML = "";
        if (activities.length === 0) {
            activityTable.hidden = true;
            activityEmpty.hidden = false;
            return;
        }
        activityEmpty.hidden = true;
        activityTable.hidden = false;

        activities.forEach(a => {
            const schedLabel = a.scheduleType === "ONE_TIME"
                ? `One-time – ${fmtDate(a.executionDatetime)}`
                : `${a.recurrenceType || ""}${a.executionTime ? " @ " + a.executionTime : ""}`;
            const statusBadge = { ACTIVE: "badge-active", PAUSED: "badge-paused", COMPLETED: "badge-completed", CANCELLED: "badge-cancelled" }[a.status] || "badge-default";
            const isCompleted = a.status === "COMPLETED" || a.status === "CANCELLED";
            const actCampaign = campaignsById[a.campaignId];
            const actCampaignType = (actCampaign && actCampaign.campaignType) || a.campaignType || 'EMAIL';

            const tr = document.createElement("tr");
            tr.innerHTML = `
                <td><strong>${escHtml(a.activityName || "—")}</strong></td>
                <td>${escHtml(a.campaignName)}</td>
                <td>${escHtml(a.templateName)}</td>
                <td>${a.segmentCount != null ? a.segmentCount : "—"}</td>
                <td>${a.recipientCount != null ? a.recipientCount : "—"}</td>
                <td>${escHtml(schedLabel)}</td>
                <td>${fmtDate(a.nextExecutionAt)}</td>
                <td><span class="badge ${statusBadge}">${a.status}</span></td>
                <td class="table-actions">
                    ${isCompleted ? `
                        <button class="secondary-btn btn-xs" data-clone-act="${a.id}" data-clone-name="${escHtml(a.activityName || '')}">Clone</button>
                        <button class="secondary-btn btn-xs" data-logs-act="${a.id}">Logs</button>
                    ` : `
                        ${a.status === 'GENERATING' ? `<button class="primary-btn btn-xs" data-generate-act="${a.id}">Generate</button>` : ''}
                        <button class="secondary-btn btn-xs" data-test-trigger-act="${a.id}" data-campaign-type="${actCampaignType}">Test Trigger</button>
                        <button class="secondary-btn btn-xs" data-edit-act="${a.id}">Edit</button>
                        <button class="secondary-btn btn-xs" data-logs-act="${a.id}">Logs</button>
                        <button class="danger-btn" data-cancel-act="${a.id}">Cancel</button>
                    `}
                </td>`;
            activityTableBody.appendChild(tr);
        });

        // Clone
        activityTableBody.querySelectorAll("[data-clone-act]").forEach(btn => {
            btn.addEventListener("click", () => {
                document.getElementById("cloneActivityName").value    = (btn.dataset.cloneName || "") + " – Copy";
                document.getElementById("confirmClone").dataset.actId = btn.dataset.cloneAct;
                cloneModal.style.display = "flex";
            });
        });

        // Generate
        activityTableBody.querySelectorAll("[data-generate-act]").forEach(btn => {
            btn.addEventListener("click", async () => {
                if (!confirm("Are you sure you want to generate recipients for this activity?")) return;
                try {
                    const actId = btn.dataset.generateAct;
                    btn.disabled = true; btn.textContent = "Generating...";
                    await apiFetch(`${apiActivitiesUrl}/${actId}/generate`, { method: "POST" });
                    showToast("Activity generated successfully!", "success");
                    loadActivityTable();
                } catch (err) {
                    showToast("Error generating activity: " + err.message, "error");
                    btn.disabled = false; btn.textContent = "Generate";
                }
            });
        });

        // Logs
        activityTableBody.querySelectorAll("[data-logs-act]").forEach(btn => {
            btn.addEventListener("click", () => openLogsModal(btn.dataset.logsAct));
        });

        // Cancel
        activityTableBody.querySelectorAll("[data-cancel-act]").forEach(btn => {
            btn.addEventListener("click", () => {
                setDeleteContext({ type: "activity", id: btn.dataset.cancelAct });
                deleteModalMessage.textContent = "Cancel this activity? It will stop running.";
                modal.style.display = "flex";
            });
        });

        // Test trigger
        activityTableBody.querySelectorAll("[data-test-trigger-act]").forEach(btn => {
            btn.addEventListener("click", () => {
                const actId        = btn.dataset.testTriggerAct;
                const campaignType = btn.dataset.campaignType || 'EMAIL';
                const isVoiceOrWa  = campaignType === 'VOICE' || campaignType === 'WHATSAPP';
                const icon    = { EMAIL: '✉️', WHATSAPP: '💬', VOICE: '🎙️' }[campaignType] || '✉️';
                const title   = { EMAIL: 'Test Email Send', WHATSAPP: 'Test WhatsApp Send', VOICE: 'Test Voice Call' }[campaignType] || 'Test Fire Activity';
                const hint    = isVoiceOrWa ? 'Enter a phone number to receive the test (with country code, e.g. +91xxxxxxxxxx).' : 'Enter comma-separated email addresses to receive the test.';
                const label   = isVoiceOrWa ? 'Phone Number *' : 'Email Addresses *';
                const placeholder = isVoiceOrWa ? '+91xxxxxxxxxx' : 'user1@example.com, user2@example.com';

                document.getElementById("testFireRecipients").value   = "";
                document.getElementById("confirmTestFire").dataset.actId = actId;
                document.getElementById("confirmTestFire").dataset.campaignType = campaignType;
                document.getElementById("testFireIcon").textContent   = icon;
                document.getElementById("testFireTitle").textContent  = title;
                document.getElementById("testFireHint").textContent   = hint;
                document.getElementById("testFireLabel").textContent  = label;
                document.getElementById("testFireRecipients").placeholder = placeholder;
                testFireModal.style.display = "flex";
            });
        });

        // Edit
        activityTableBody.querySelectorAll("[data-edit-act]").forEach(btn => {
            btn.addEventListener("click", async () => {
                try {
                    const a = await apiFetch(`${apiActivitiesUrl}/${btn.dataset.editAct}`);
                    activityEditId.value = a.id;
                    activityFormTitle.textContent = "Edit Activity";
                    document.getElementById("actCampaign").value = a.campaignId;

                    const c = campaignsById[a.campaignId];
                    updateTemplateGroupVisibility(c);
                    if (c && c.campaignType === "WHATSAPP") {
                        await loadWhatsappTemplatesForActivity(c.wabaId || "", c.id);
                        document.getElementById("actWaTemplate").value  = a.whatsappTemplateName || "";
                        document.getElementById("actWaLanguage").value  = a.whatsappLanguage || "en";
                    } else if (c && c.campaignType === "VOICE") {
                        await loadVoiceTemplatesForActivity(c.id);
                        document.getElementById("actVoiceTemplate").value = a.voiceTemplateId || "";
                    } else if (c && c.campaignType === "EMAIL") {
                        document.getElementById("actTemplate").value = a.templateId;
                    }

                    await loadSegmentDropdownForActivity();
                    if (a.segmentId) document.getElementById("actSegment").value = a.segmentId;
                    document.getElementById("actName").value = a.activityName || "";

                    actScheduleType.value  = a.scheduleType;
                    oneTimeFields.hidden   = a.scheduleType !== "ONE_TIME";
                    recurringFields.hidden = a.scheduleType !== "RECURRING";
                    document.querySelectorAll("[name=actScheduleTypeRadio]").forEach(r => { r.checked = r.value === a.scheduleType; });

                    if (a.executionDatetime) document.getElementById("actExecDatetime").value = a.executionDatetime.slice(0,16);
                    if (a.recurrenceType) actRecurrence.value = a.recurrenceType;
                    weeklyFields.hidden  = a.recurrenceType !== "WEEKLY";
                    monthlyFields.hidden = a.recurrenceType !== "MONTHLY";
                    if (a.executionTime) document.getElementById("actExecTime").value    = a.executionTime;
                    if (a.dayOfMonth)    document.getElementById("actDayOfMonth").value  = a.dayOfMonth;
                    if (a.startDate)     document.getElementById("actStartDate").value   = a.startDate;
                    if (a.endDate)       document.getElementById("actEndDate").value      = a.endDate;

                    const tzEl    = document.getElementById("actTimezone");
                    const tzElRec = document.getElementById("actTimezoneRecurring");
                    if (tzEl)    tzEl.value    = a.timezone || "Asia/Kolkata";
                    if (tzElRec) tzElRec.value = a.timezone || "Asia/Kolkata";

                    document.querySelectorAll("[name=actStatusRadio]").forEach(r => { r.checked = r.value === (a.status || "ACTIVE"); });
                    document.getElementById("actStatus").value = a.status || "ACTIVE";
                    document.querySelectorAll("#weeklyFields input[type=checkbox]").forEach(cb => {
                        cb.checked = (a.weekdays || []).includes(cb.value);
                    });

                    activityFormWrapper.hidden = false;
                    activityFormWrapper.scrollIntoView({ behavior: "smooth" });
                } catch (err) { showToast("Error loading activity: " + err.message, "error"); }
            });
        });
    } catch (err) { console.error("loadActivityTable:", err); }
}

export function initActivities() {
    const activityFormWrapper = document.getElementById("activityFormWrapper");
    const activityEditId      = document.getElementById("activityEditId");
    const activityFormTitle   = document.getElementById("activityFormTitle");
    const actScheduleType     = document.getElementById("actScheduleType");
    const actRecurrence       = document.getElementById("actRecurrence");
    const oneTimeFields       = document.getElementById("oneTimeFields");
    const recurringFields     = document.getElementById("recurringFields");
    const weeklyFields        = document.getElementById("weeklyFields");
    const monthlyFields       = document.getElementById("monthlyFields");

    // ── Wizard helpers ────────────────────────────────────────
    function wizardGoStep(n) {
        const step1 = document.getElementById("actStep1");
        const step2 = document.getElementById("actStep2");
        const dot1  = document.getElementById("actStepDot1");
        const dot2  = document.getElementById("actStepDot2");
        const conn  = document.getElementById("actStepConnector");
        if (n === 1) {
            step1.hidden = false; step2.hidden = true;
            dot1.className = "act-step-item active"; dot2.className = "act-step-item";
            conn.classList.remove("done");
        } else {
            step1.hidden = true; step2.hidden = false;
            dot1.className = "act-step-item done"; dot2.className = "act-step-item active";
            conn.classList.add("done");
        }
    }

    function wizardReset() {
        if (activityEditId)   activityEditId.value   = "";
        if (activityFormTitle) activityFormTitle.textContent = "New Activity";
        const fields = ["actCampaign","actName","actSegment","actExecDatetime","actExecTime","actStartDate","actEndDate","actDayOfMonth"];
        fields.forEach(id => { const el = document.getElementById(id); if (el) el.value = ""; });
        if (actRecurrence)   actRecurrence.value = "";
        if (actScheduleType) actScheduleType.value = "";
        if (oneTimeFields)   oneTimeFields.hidden = true;
        if (recurringFields) recurringFields.hidden = true;
        if (weeklyFields)    weeklyFields.hidden = true;
        if (monthlyFields)   monthlyFields.hidden = true;
        document.querySelectorAll("[name=actScheduleTypeRadio]").forEach(r => r.checked = false);
        wizardGoStep(1);
    }

    // ── Schedule type card radios ─────────────────────────────
    document.querySelectorAll("[name=actScheduleTypeRadio]").forEach(radio => {
        radio.addEventListener("change", () => {
            actScheduleType.value  = radio.value;
            oneTimeFields.hidden   = radio.value !== "ONE_TIME";
            recurringFields.hidden = radio.value !== "RECURRING";
        });
    });
    document.querySelectorAll(".act-schedule-card").forEach(card => {
        card.addEventListener("click", () => {
            document.querySelectorAll(".act-schedule-card").forEach(c => c.classList.remove("selected"));
            card.classList.add("selected");
            const radio = card.querySelector("input[type='radio']");
            if (radio) { radio.checked = true; radio.dispatchEvent(new Event("change", { bubbles: true })); }
        });
    });

    // ── Recurrence pattern ────────────────────────────────────
    if (actRecurrence) {
        actRecurrence.addEventListener("change", () => {
            weeklyFields.hidden  = actRecurrence.value !== "WEEKLY";
            monthlyFields.hidden = actRecurrence.value !== "MONTHLY";
        });
    }

    // ── Status radio ──────────────────────────────────────────
    document.querySelectorAll("[name=actStatusRadio]").forEach(r => {
        r.addEventListener("change", () => { document.getElementById("actStatus").value = r.value; });
    });

    // ── Wizard navigation ─────────────────────────────────────
    document.getElementById("actNextBtn").addEventListener("click", async () => {
        const campId = document.getElementById("actCampaign").value;
        if (!campId) { showToast("Please select a campaign first", "error"); return; }
        const segId = document.getElementById("actSegment").value;
        if (!segId)  { showToast("Please select an audience segment", "error"); return; }
        await loadCampaignDropdowns();
        wizardGoStep(2);
    });

    document.getElementById("actBackBtn").addEventListener("click", () => wizardGoStep(1));

    ["actCancelBtn", "actCancelBtnStep1"].forEach(id => {
        const el = document.getElementById(id);
        if (el) el.addEventListener("click", () => { activityFormWrapper.hidden = true; wizardReset(); });
    });

    // ── New activity ──────────────────────────────────────────
    const newActivityBtn = document.getElementById("newActivityBtn");
    if (newActivityBtn) {
        newActivityBtn.addEventListener("click", async () => {
            wizardReset();
            activityFormWrapper.hidden = false;
            await loadCampaignDropdowns();
            await loadSegmentDropdownForActivity();
            activityFormWrapper.scrollIntoView({ behavior: "smooth" });
        });
    }

    // ── Campaign change → toggle template fields ───────────────
    const actCampaignSel = document.getElementById("actCampaign");
    if (actCampaignSel) {
        actCampaignSel.addEventListener("change", async (e) => {
            const c = campaignsById[e.target.value];
            updateTemplateGroupVisibility(c);
            if (c && c.campaignType === "WHATSAPP") await loadWhatsappTemplatesForActivity(c.wabaId || "", c.id);
            else if (c && c.campaignType === "VOICE") await loadVoiceTemplatesForActivity(c.id);
            else if (c && c.campaignType === "EMAIL") await loadTemplateDropdownsForActivity();
        });
    }

    // ── Save button ───────────────────────────────────────────
    document.getElementById("actSaveBtn").addEventListener("click", async () => {
        const schedType = actScheduleType.value;
        if (!schedType) { showToast("Please choose a schedule type (One Time or Recurring)", "error"); return; }
        if (schedType === "ONE_TIME" && !document.getElementById("actExecDatetime").value) {
            showToast("Please set an execution date & time", "error"); return;
        }

        const id          = activityEditId.value;
        const actCampaignId = Number(document.getElementById("actCampaign").value) || null;
        const c           = campaignsById[actCampaignId];
        const isWA        = c && c.campaignType === "WHATSAPP";
        const isVoice     = c && c.campaignType === "VOICE";
        const isEmail     = c && c.campaignType === "EMAIL";
        const checkedDays = [...document.querySelectorAll("#weeklyFields input[type=checkbox]:checked")].map(cb => cb.value);
        const tz          = schedType === "ONE_TIME"
            ? (document.getElementById("actTimezone").value || "Asia/Kolkata")
            : (document.getElementById("actTimezoneRecurring").value || "Asia/Kolkata");

        const payload = {
            campaignId:           actCampaignId,
            templateId:           isEmail ? (Number(document.getElementById("actTemplate").value) || null) : null,
            voiceTemplateId:      isVoice ? (Number(document.getElementById("actVoiceTemplate").value) || null) : null,
            whatsappTemplateName: isWA    ? document.getElementById("actWaTemplate").value : null,
            whatsappLanguage:     isWA    ? document.getElementById("actWaLanguage").value : null,
            segmentId:     Number(document.getElementById("actSegment").value) || null,
            activityName:  document.getElementById("actName").value.trim() || null,
            scheduleType:  schedType,
            recurrenceType: actRecurrence.value || null,
            executionDatetime: document.getElementById("actExecDatetime").value || null,
            executionTime:     document.getElementById("actExecTime").value || null,
            weekdays:          checkedDays.length ? checkedDays : null,
            dayOfMonth:        document.getElementById("actDayOfMonth").value ? Number(document.getElementById("actDayOfMonth").value) : null,
            startDate:         document.getElementById("actStartDate").value || null,
            endDate:           document.getElementById("actEndDate").value || null,
            timezone:          tz,
            status:            document.getElementById("actStatus").value || "ACTIVE"
        };

        if (!payload.campaignId || (!payload.templateId && !payload.whatsappTemplateName && !payload.voiceTemplateId) || !payload.scheduleType) {
            showToast("Campaign, Template, and Schedule Type are required", "error");
            return;
        }

        try {
            if (id) {
                await apiFetch(`${apiActivitiesUrl}/${id}`, { method: "PUT", body: JSON.stringify(payload) });
                showToast("Activity updated!", "success");
            } else {
                await apiFetch(apiActivitiesUrl, { method: "POST", body: JSON.stringify(payload) });
                showToast("Activity scheduled!", "success");
            }
            activityFormWrapper.hidden = true;
            wizardReset();
            loadActivityTable();
        } catch (err) {
            showToast("Error: " + err.message, "error");
        }
    });

    // ── Test Fire modal ────────────────────────────────────────
    document.getElementById("cancelTestFire").addEventListener("click", () => { testFireModal.style.display = "none"; });

    document.getElementById("confirmTestFire").addEventListener("click", async () => {
        const btn         = document.getElementById("confirmTestFire");
        const actId       = btn.dataset.actId;
        const campaignType = btn.dataset.campaignType || 'EMAIL';
        const recipients  = (document.getElementById("testFireRecipients").value.trim() || "").split(",").map(s => s.trim()).filter(s => s);

        if (!recipients.length) {
            showToast(campaignType === 'EMAIL' ? "Please enter at least one email address" : "Please enter a phone number", "error");
            return;
        }
        try {
            btn.disabled = true; btn.textContent = "Sending...";
            await apiFetch(`${apiActivitiesUrl}/${actId}/test-trigger`, { method: "POST", body: JSON.stringify(recipients) });
            const successMsg = { EMAIL: "Test email triggered!", WHATSAPP: "Test WhatsApp message triggered!", VOICE: "Test voice call triggered!" }[campaignType] || "Test triggered successfully!";
            showToast(successMsg, "success");
            testFireModal.style.display = "none";
            loadActivityTable();
        } catch (err) {
            showToast("Test failed: " + err.message, "error");
        } finally {
            btn.disabled = false; btn.textContent = "Send Test";
        }
    });

    // ── Clone modal ────────────────────────────────────────────
    document.getElementById("cancelClone").addEventListener("click", () => { cloneModal.style.display = "none"; });

    document.getElementById("confirmClone").addEventListener("click", async (e) => {
        const actId   = e.target.dataset.actId;
        const newName = document.getElementById("cloneActivityName").value.trim();
        if (!newName) { showToast("Please enter a name for the cloned activity", "error"); return; }
        try {
            e.target.disabled = true; e.target.textContent = "Cloning...";
            await apiFetch(`${apiActivitiesUrl}/${actId}/clone?newName=${encodeURIComponent(newName)}`, { method: "POST" });
            showToast("Activity cloned successfully!", "success");
            cloneModal.style.display = "none";
            loadActivityTable();
        } catch (err) {
            showToast("Clone failed: " + err.message, "error");
        } finally {
            e.target.disabled = false; e.target.textContent = "Clone";
        }
    });
}
