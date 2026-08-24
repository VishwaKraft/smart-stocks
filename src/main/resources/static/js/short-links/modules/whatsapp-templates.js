/* ============================================================
   modules/whatsapp-templates.js — WhatsApp (Meta) template management
   ============================================================ */

import { apiCampaignsUrl, apiFetch } from '../api.js';
import { escHtml } from '../utils.js';
import { modal, deleteModalMessage, showToast } from '../ui.js';
import { state, setDeleteContext } from '../state.js';

// ── Internal DOM refs (populated once on init) ──────────────
let _waTemplateCampaignSelect = null;
let _waManualTokenInput        = null;
let _waWabaIdInput             = null;
let _waAutoDetectWabaBtn       = null;
let _waTemplateEmpty           = null;
let _waTemplateTable           = null;
let _waTemplateTableBody       = null;
let _waTemplateFormWrapper     = null;

function getSelectedWabaId() {
    if (_waWabaIdInput && _waWabaIdInput.value.trim()) {
        const val = _waWabaIdInput.value.trim();
        if (val !== "1726866808739698") {
            return val;
        }
    }
    if (_waTemplateCampaignSelect && _waTemplateCampaignSelect.value && _waTemplateCampaignSelect.value !== "manual") {
        const campaign = state.cachedCampaigns.find(c => String(c.id) === _waTemplateCampaignSelect.value);
        if (campaign && campaign.wabaId && campaign.wabaId !== "1726866808739698") {
            return campaign.wabaId;
        }
    }
    return "";  // let the server resolve using its own discovery
}

function getWabaRequestParams() {
    const wabaId      = getSelectedWabaId();
    const campaignId  = _waTemplateCampaignSelect ? _waTemplateCampaignSelect.value : "";
    const manualToken = _waManualTokenInput        ? _waManualTokenInput.value.trim() : "";

    const params = new URLSearchParams();
    if (wabaId) {
        params.append("wabaId", wabaId);
    }
    if (campaignId && campaignId !== "manual") {
        params.append("campaignId", campaignId);
    } else if (manualToken) {
        params.append("token", manualToken);
    }
    return params.toString();
}

export async function autoDetectWabaId() {
    const campaignId  = _waTemplateCampaignSelect ? _waTemplateCampaignSelect.value : "";
    const manualToken = _waManualTokenInput ? _waManualTokenInput.value.trim() : "";

    if (!campaignId && !manualToken) {
        showToast("Please select a Campaign or enter a Manual Meta Token first.", "error");
        return;
    }

    const params = new URLSearchParams();
    if (campaignId && campaignId !== "manual") {
        params.append("campaignId", campaignId);
    } else if (manualToken) {
        params.append("token", manualToken);
    }

    try {
        if (_waAutoDetectWabaBtn) {
            _waAutoDetectWabaBtn.disabled = true;
            _waAutoDetectWabaBtn.textContent = "⏳ Detecting...";
        }
        const res = await apiFetch(`/api/whatsapp/templates/accounts?${params.toString()}`);
        const accounts = res.data || [];
        if (accounts.length > 0) {
            const primary = accounts[0];
            const wabaId = primary.wabaId || primary.id;
            const wabaName = primary.wabaName || primary.name || "WhatsApp Business Account";
            if (_waWabaIdInput) {
                _waWabaIdInput.value = wabaId;
            }
            showToast(`Auto-detected: ${wabaName} (${wabaId})`, "success");
            console.log("[WA Templates] Auto-detected accounts:", accounts);
        } else {
            showToast("No WhatsApp Business Accounts found for this token. Verify 'whatsapp_business_management' permission in Meta App Dashboard.", "error");
        }
    } catch (err) {
        console.error("[WA Templates] Auto-detect failed:", err);
        showToast("Auto-detect failed: " + err.message, "error");
    } finally {
        if (_waAutoDetectWabaBtn) {
            _waAutoDetectWabaBtn.disabled = false;
            _waAutoDetectWabaBtn.textContent = "🔍 Detect";
        }
    }
}

export async function loadWaCampaignDropdown() {
    if (!_waTemplateCampaignSelect) return;
    try {
        if (state.cachedCampaigns.length === 0) state.cachedCampaigns = await apiFetch(apiCampaignsUrl);
        const currentVal = _waTemplateCampaignSelect.value;
        _waTemplateCampaignSelect.innerHTML = `
            <option value="">— select campaign —</option>
            <option value="manual">— Use Manual Token —</option>
        `;
        state.cachedCampaigns.forEach(c => {
            if (c.campaignType === 'WHATSAPP') {
                const opt = document.createElement("option");
                opt.value = c.id;
                opt.textContent = c.name;
                _waTemplateCampaignSelect.appendChild(opt);
            }
        });
        if (currentVal) _waTemplateCampaignSelect.value = currentVal;
    } catch (e) { console.error("loadWaCampaignDropdown error:", e); }
}

export async function loadWhatsappTemplates() {
    if (!_waTemplateTableBody) return;

    const params = getWabaRequestParams();
    const wabaId = getSelectedWabaId();
    console.log(`[WA Templates] Loading templates for wabaId=${wabaId}`, Object.fromEntries(new URLSearchParams(params)));

    if (!params.includes("campaignId") && !params.includes("token")) {
        console.warn("[WA Templates] No token source configured — cannot load templates.");
        _waTemplateTable.hidden = true;
        _waTemplateEmpty.hidden = false;
        _waTemplateEmpty.innerHTML = "Please configure/select a <strong>Campaign Token Source</strong> or enter a <strong>Manual Token</strong> to load templates.";
        return;
    }

    try {
        _waTemplateEmpty.innerHTML = "🔄 Loading WhatsApp templates...";
        _waTemplateEmpty.hidden = false;
        _waTemplateTable.hidden = true;

        const res       = await apiFetch(`/api/whatsapp/templates?${params}`);
        const templates = res.data || [];
        console.log(`[WA Templates] Received ${templates.length} template(s) from Meta.`);

        _waTemplateTableBody.innerHTML = "";
        if (templates.length === 0) {
            _waTemplateTable.hidden = true;
            _waTemplateEmpty.hidden = false;
            _waTemplateEmpty.innerHTML = `No templates found on Meta for WhatsApp Business Account ID: <code>${escHtml(wabaId || 'auto-detected')}</code>. Click <strong>+ New WhatsApp Template</strong> above to create one.`;
            return;
        }

        _waTemplateEmpty.hidden = true;
        _waTemplateTable.hidden = false;

        templates.forEach(t => {
            let bodyText = "—";
            (t.components || []).forEach(comp => {
                if (comp.type === "BODY") bodyText = comp.text || "—";
            });

            const statusClass = { APPROVED: "badge-active", PENDING: "badge-paused", REJECTED: "badge-cancelled" }[t.status] || "badge-default";
            const tr = document.createElement("tr");
            tr.innerHTML = `
                <td><strong>${escHtml(t.name)}</strong></td>
                <td><span class="badge badge-default">${escHtml(t.language)}</span></td>
                <td><span class="badge badge-default">${escHtml(t.category)}</span></td>
                <td><span class="badge ${statusClass}">${escHtml(t.status)}</span></td>
                <td class="truncate" style="max-width:300px;" title="${escHtml(bodyText)}">${escHtml(bodyText)}</td>
                <td class="table-actions">
                    <button class="danger-btn" data-delete-wa-tpl="${escHtml(t.name)}" data-waba-id="${escHtml(getSelectedWabaId())}" data-delete-wa-tpl-name="${escHtml(t.name)}">Delete</button>
                </td>
            `;
            _waTemplateTableBody.appendChild(tr);
        });

        _waTemplateTableBody.querySelectorAll("[data-delete-wa-tpl]").forEach(btn => {
            btn.addEventListener("click", () => {
                setDeleteContext({
                    type: "whatsapp-template",
                    name: btn.dataset.deleteWaTpl,
                    wabaId: btn.dataset.wabaId,
                    queryParams: getWabaRequestParams()
                });
                console.log(`[WA Templates] Delete requested for template: ${btn.dataset.deleteWaTpl}, wabaId: ${btn.dataset.wabaId}`);
                deleteModalMessage.textContent = `Delete WhatsApp template "${btn.dataset.deleteWaTplName}"? This action cannot be undone.`;
                modal.style.display = "flex";
            });
        });
    } catch (e) {
        console.error("[WA Templates] Failed to load templates:", e);
        _waTemplateTable.hidden = true;
        _waTemplateEmpty.hidden = false;
        _waTemplateEmpty.innerHTML = `
            <div style="padding: 16px; background: #fff5f5; border: 1px solid #feb2b2; border-radius: 8px; color: #9b2c2c;">
                <strong>⚠️ Failed to load WhatsApp Templates:</strong>
                <p style="margin: 6px 0 10px; font-size: 13px;">${escHtml(e.message)}</p>
                <div style="font-size: 12px; color: #742a2a; border-top: 1px dashed #feb2b2; padding-top: 8px;">
                    <strong>💡 Troubleshooting Tips:</strong>
                    <ul style="margin: 4px 0 0 18px; padding: 0;">
                        <li>Click <strong>🔍 Detect</strong> next to the WABA ID field to auto-discover your WhatsApp Business Account ID.</li>
                        <li>Ensure your Meta Access Token has the <code>whatsapp_business_management</code> and <code>whatsapp_business_messaging</code> permissions.</li>
                        <li>Do not use the Meta App ID or Phone Number ID as the WABA ID. WhatsApp Business Account ID can be found in Meta Business Suite → WhatsApp Accounts.</li>
                    </ul>
                </div>
            </div>
        `;
    }
}

export function initWhatsappTemplates() {
    _waTemplateCampaignSelect = document.getElementById("waTemplateCampaignSelect");
    _waManualTokenInput        = document.getElementById("waManualTokenInput");
    _waWabaIdInput             = document.getElementById("waWabaIdInput");
    _waAutoDetectWabaBtn       = document.getElementById("waAutoDetectWabaBtn");
    const waManualTokenGroup   = document.getElementById("waManualTokenGroup");
    const waReloadTemplatesBtn = document.getElementById("waReloadTemplatesBtn");
    _waTemplateEmpty           = document.getElementById("waTemplateEmpty");
    _waTemplateTable           = document.getElementById("waTemplateTable");
    _waTemplateTableBody       = document.getElementById("waTemplateTableBody");
    const newWhatsappTemplateBtn = document.getElementById("newWhatsappTemplateBtn");
    _waTemplateFormWrapper     = document.getElementById("waTemplateFormWrapper");
    const waTemplateForm       = document.getElementById("waTemplateForm");

    const waTplHeaderText         = document.getElementById("waTplHeaderText");
    const waTplHeaderExampleGroup = document.getElementById("waTplHeaderExampleGroup");
    const waTplHeaderExample      = document.getElementById("waTplHeaderExample");
    const waTplBodyText           = document.getElementById("waTplBodyText");
    const waTplBodyExamplesGroup  = document.getElementById("waTplBodyExamplesGroup");
    const waTplBodyExamples       = document.getElementById("waTplBodyExamples");

    if (_waTemplateCampaignSelect) {
        _waTemplateCampaignSelect.addEventListener("change", () => {
            const isManual = _waTemplateCampaignSelect.value === "manual";
            if (waManualTokenGroup) {
                waManualTokenGroup.hidden = !isManual;
            }
            if (_waWabaIdInput) {
                if (!isManual && _waTemplateCampaignSelect.value) {
                    const campaign = state.cachedCampaigns.find(c => String(c.id) === _waTemplateCampaignSelect.value);
                    if (campaign && campaign.wabaId && campaign.wabaId !== "1726866808739698") {
                        _waWabaIdInput.value = campaign.wabaId;
                    } else {
                        _waWabaIdInput.value = "";
                        autoDetectWabaId();
                    }
                } else if (isManual) {
                    _waWabaIdInput.value = "";
                }
            }
        });
    }

    if (_waAutoDetectWabaBtn) {
        _waAutoDetectWabaBtn.addEventListener("click", () => autoDetectWabaId());
    }

    if (waTplHeaderText) {
        waTplHeaderText.addEventListener("input", () => {
            waTplHeaderExampleGroup.hidden = !waTplHeaderText.value.includes("{{1}}");
        });
    }

    if (waTplBodyText) {
        waTplBodyText.addEventListener("input", () => {
            waTplBodyExamplesGroup.hidden = !waTplBodyText.value.includes("{{1}}");
        });
    }

    if (newWhatsappTemplateBtn) {
        newWhatsappTemplateBtn.addEventListener("click", () => {
            waTemplateForm.reset();
            waTplHeaderExampleGroup.hidden = true;
            waTplBodyExamplesGroup.hidden  = true;
            _waTemplateFormWrapper.hidden  = false;
            _waTemplateFormWrapper.scrollIntoView({ behavior: "smooth" });
        });
    }

    const waTplCancelBtn = document.getElementById("waTplCancelBtn");
    if (waTplCancelBtn) {
        waTplCancelBtn.addEventListener("click", () => {
            _waTemplateFormWrapper.hidden = true;
            waTemplateForm.reset();
        });
    }

    if (waReloadTemplatesBtn) {
        waReloadTemplatesBtn.addEventListener("click", () => loadWhatsappTemplates());
    }

    if (waTemplateForm) {
        waTemplateForm.addEventListener("submit", async (e) => {
            e.preventDefault();

            const name     = document.getElementById("waTplName").value.trim().toLowerCase();
            const language = document.getElementById("waTplLanguage").value;
            const category = document.getElementById("waTplCategory").value;

            console.log(`[WA Template Create] Starting creation — name=${name}, language=${language}, category=${category}`);

            if (!/^[a-z0-9_]+$/.test(name)) {
                showToast("Template Name must contain only lowercase letters, numbers, and underscores.", "error");
                return;
            }

            const components = [];

            const headerText = waTplHeaderText.value.trim();
            if (headerText) {
                const headerComp = { type: "HEADER", format: "TEXT", text: headerText };
                if (headerText.includes("{{1}}")) {
                    const hExample = waTplHeaderExample.value.trim();
                    if (!hExample) { showToast("Please provide an example value for the header variable.", "error"); return; }
                    headerComp.example = { header_text: [hExample] };
                }
                components.push(headerComp);
            }

            const bodyText = waTplBodyText.value.trim();
            if (!bodyText) { showToast("Body text is required.", "error"); return; }
            const bodyComp = { type: "BODY", text: bodyText };
            const matches  = bodyText.match(/\{\{\d+\}\}/g) || [];
            if (matches.length > 0) {
                const bExamplesStr = waTplBodyExamples.value.trim();
                if (!bExamplesStr) { showToast("Please provide comma-separated example values for body variables.", "error"); return; }
                bodyComp.example = { body_text: [bExamplesStr.split(",").map(x => x.trim()).filter(x => x)] };
            }
            components.push(bodyComp);

            const footerText = document.getElementById("waTplFooterText").value.trim();
            if (footerText) components.push({ type: "FOOTER", text: footerText });

            const quickReplies = [];
            document.querySelectorAll(".wa-reply-btn-input").forEach(inp => {
                const val = inp.value.trim();
                if (val) quickReplies.push({ type: "QUICK_REPLY", text: val });
            });
            if (quickReplies.length > 0) components.push({ type: "BUTTONS", buttons: quickReplies });

            const payload = { name, language, category, components };
            const params  = getWabaRequestParams();

            if (!params.includes("campaignId") && !params.includes("token")) {
                showToast("Please specify a valid Campaign Token Source or Manual Token.", "error");
                return;
            }

            try {
                const saveBtn = document.getElementById("waTplSaveBtn");
                saveBtn.disabled    = true;
                saveBtn.textContent = "Saving...";
                await apiFetch(`/api/whatsapp/templates?${params}`, { method: "POST", body: JSON.stringify(payload) });
                showToast("WhatsApp Template created successfully!", "success");
                _waTemplateFormWrapper.hidden = true;
                waTemplateForm.reset();
                loadWhatsappTemplates();
            } catch (err) {
                showToast("Failed to create template: " + err.message, "error");
            } finally {
                const saveBtn = document.getElementById("waTplSaveBtn");
                saveBtn.disabled    = false;
                saveBtn.textContent = "💾 Create WhatsApp Template";
            }
        });
    }
}
