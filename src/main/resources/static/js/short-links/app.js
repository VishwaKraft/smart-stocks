/* ============================================================
   Smart Stocks – Campaign Manager  |  app.js
   ============================================================ */
document.addEventListener("DOMContentLoaded", () => {

    /* ── API URL constants ────────────────────────────────── */
    const apiLinksUrl      = (window.API_LINKS_URL      || "/api/links").replace(/\/$/, "");
    const apiCampaignsUrl  = (window.API_CAMPAIGNS_URL  || "/api/campaigns").replace(/\/$/, "");
    const apiTemplatesUrl  = (window.API_TEMPLATES_URL  || "/api/templates").replace(/\/$/, "");
    const apiActivitiesUrl = (window.API_ACTIVITIES_URL || "/api/activities").replace(/\/$/, "");
    const apiSegmentsUrl   = (window.API_SEGMENTS_URL   || "/api/segments").replace(/\/$/, "");

    const shortLinkBaseUrl = (() => {
        const u = window.SHORT_LINK_BASE_URL || "/s/";
        return u.endsWith("/") ? u : u + "/";
    })();

    /* ── DOM refs ─────────────────────────────────────────── */
    const sectionNavButtons     = document.querySelectorAll(".section-nav-btn");
    const shortenerPanel        = document.getElementById("shortenerPanel");
    const campaignPanel         = document.getElementById("campaignPanel");
    const templatePanel         = document.getElementById("templatePanel");
    const activityPanel         = document.getElementById("activityPanel");
    const segmentPanel          = document.getElementById("segmentPanel");
    const modal                 = document.getElementById("deleteModal");
    const testFireModal         = document.getElementById("testFireModal");
    const deleteModalMessage    = document.getElementById("deleteModalMessage");
    const confirmDeleteBtn      = document.getElementById("confirmDelete");
    const cancelDeleteBtn       = document.getElementById("cancelDelete");
    const toastEl               = document.getElementById("toast");
    const mobileMenuBtn         = document.getElementById("mobileMenuBtn");
    const sidebar               = document.querySelector(".sidebar");
    const cloneModal            = document.getElementById("cloneModal");

    /* ── State ────────────────────────────────────────────── */
    let deleteContext      = null;
    let cachedCampaigns    = [];
    let cachedTemplates    = [];
    let tplPreviewTimer       = null;

    const DEFAULT_EMAIL_TEMPLATE = `<table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="background-color:#ffe6ee;padding:40px 20px">
  <tbody><tr>
    <td align="center"><br><table role="presentation" width="600" cellspacing="0" cellpadding="0" border="0" style="background-color:#ffffff;border-radius:20px">
    <tbody><tr>
      <td align="center" style="padding:50px 40px">

        <div style="font-size:42px;line-height:1"><img data-emoji="🌸" class="an1" alt="🌸" aria-label="🌸" draggable="false" src="https://fonts.gstatic.com/s/e/notoemoji/17.0/1f338/72.png" loading="lazy"></div>

        <div style="font-family:Arial,Helvetica,sans-serif;font-size:28px;color:#e75480;font-weight:bold;margin-top:20px">
          Just a Thought
        </div>

        <div style="font-family:Arial,Helvetica,sans-serif;font-size:18px;line-height:1.9;color:#555555;margin-top:30px">
          Silence can fill a moment,
          <br><br>
          but talking and staying connected
          are how people who care about each other
          keep each other close.
        </div>

        <div style="font-family:Arial,Helvetica,sans-serif;font-size:18px;line-height:1.9;color:#555555;margin-top:25px">
          We don't always need perfect words,
          only a little effort to understand,
          a little patience to listen,
          and a reason to stay.
        </div>

        <div style="font-family:Arial,Helvetica,sans-serif;font-size:18px;line-height:1.9;color:#555555;margin-top:25px">
          Because even the smallest conversation
          can make a distance feel shorter.
        </div>
        <div style="font-family:Arial,Helvetica,sans-serif;font-size:18px;line-height:1.9;color:#555555;margin-top:25px"><br></div>

        <a href="https://smart-stocks-1c68.onrender.com/s/hvVkGQ" style="color:#8e24aa" target="_blank">Dont Click Here</a>

        <div style="font-size:22px;color:#e75480;margin-top:35px">
          <img data-emoji="❤️" class="an1" alt="❤️" aria-label="❤️" draggable="false" src="https://fonts.gstatic.com/s/e/notoemoji/17.0/2764_fe0f/72.png" loading="lazy">
        </div>

      </td>
    </tr>
  </tbody></table>

</td></tr>
</tbody></table>`;

    /* ======================================================
       TOAST
    ====================================================== */
    function showToast(msg, type = "default") {
        toastEl.textContent = msg;
        toastEl.className = "toast show" + (type !== "default" ? " toast-" + type : "");
        clearTimeout(toastEl._timer);
        toastEl._timer = setTimeout(() => {
            toastEl.classList.remove("show");
        }, 3000);
    }

    /* ======================================================
       SECTION SWITCHING
    ====================================================== */
    function switchSection(section) {
        const panels = {
            shortener: shortenerPanel,
            campaigns: campaignPanel,
            templates: templatePanel,
            activities: activityPanel,
            segments: segmentPanel
        };

        sectionNavButtons.forEach(btn =>
            btn.classList.toggle("active", btn.dataset.section === section));

        Object.entries(panels).forEach(([key, el]) => {
            if (el) el.hidden = key !== section;
        });

        if (section === "shortener") {
            loadShortLinksTable();
        } else if (section === "campaigns") {
            loadCampaignTable();
        } else if (section === "templates") {
            loadTemplateTable();
        } else if (section === "activities") {
            loadActivityTable();
            loadCampaignDropdowns();
            loadTemplateDropdowns();
            loadSegmentDropdownForActivity();
        } else if (section === "segments") {
            loadSegmentTable();
        }

        if (window.innerWidth <= 960) {
            sidebar.classList.remove("open");
        }
    }

    sectionNavButtons.forEach(btn => btn.addEventListener("click", () => switchSection(btn.dataset.section)));

    /* ======================================================
       UTILITY: fetch helpers
    ====================================================== */
    async function apiFetch(url, opts = {}) {
        const res = await fetch(url, {
            headers: { "Content-Type": "application/json" },
            ...opts
        });
        if (!res.ok) {
            const msg = await res.text().catch(() => "Unknown error");
            throw new Error(msg || `HTTP ${res.status}`);
        }
        const ct = res.headers.get("content-type") || "";
        return ct.includes("application/json") ? res.json() : res.text();
    }

    function fmtDate(isoStr) {
        if (!isoStr) return "—";
        return new Date(isoStr).toLocaleString([], { dateStyle: "medium", timeStyle: "short" });
    }

    function escHtml(str) {
        if (!str) return "";
        return str.replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;");
    }

    /* ======================================================
       SHORT LINKS
    ====================================================== */
    document.getElementById("shortenForm").addEventListener("submit", async e => {
        e.preventDefault();
        const originalUrl = document.getElementById("originalUrl").value.trim();
        if (!originalUrl) return;
        try {
            const params = new URLSearchParams({ originalUrl });
            const res = await fetch(`${apiLinksUrl}/shorten`, {
                method: "POST",
                headers: { "Content-Type": "application/x-www-form-urlencoded" },
                body: params.toString()
            });
            if (res.ok) {
                const short = await res.text();
                const shortId = short.split("/").filter(Boolean).pop();
                document.getElementById("shortUrlResult").innerHTML =
                    `<p>Shortened: <a href="${shortLinkBaseUrl + shortId}" target="_blank">${shortLinkBaseUrl + shortId}</a></p>`;
                e.target.reset();
                loadShortLinksTable();
            } else {
                document.getElementById("shortUrlResult").innerHTML = `<p class="error">Error shortening URL</p>`;
            }
        } catch (err) { document.getElementById("shortUrlResult").innerHTML = `<p class="error">${err.message}</p>`; }
    });

    /* ── Short links table ────────────────────────────────── */
    async function loadShortLinksTable() {
        const tbody = document.getElementById("linksTableBody");
        const table = document.getElementById("linksTable");
        const empty = document.getElementById("linksEmpty");
        if (!tbody || !table || !empty) return;
        try {
            const links = await apiFetch(apiLinksUrl);
            tbody.innerHTML = "";
            if (links.length === 0) {
                table.hidden = true;
                empty.hidden = false;
                return;
            }
            empty.hidden = true;
            table.hidden = false;
            links.forEach(l => {
                const fullShort = shortLinkBaseUrl + l.shortId;
                const tr = document.createElement("tr");
                tr.innerHTML = `
                    <td class="truncate" style="max-width:260px" title="${escHtml(l.originalUrl)}"><a href="${escHtml(l.originalUrl)}" target="_blank">${escHtml(l.originalUrl)}</a></td>
                    <td><a href="${escHtml(fullShort)}" target="_blank">${escHtml(fullShort)}</a></td>
                    <td>${l.clickCount ?? 0}</td>
                    <td>${fmtDate(l.createdAt)}</td>
                    <td class="table-actions">
                        <button class="secondary-btn btn-xs" data-copy-link="${escHtml(fullShort)}">Copy</button>
                        <button class="danger-btn" data-delete-link="${escHtml(l.shortId)}" data-delete-name="${escHtml(fullShort)}">Delete</button>
                    </td>`;
                tbody.appendChild(tr);
            });

            // Copy
            tbody.querySelectorAll("[data-copy-link]").forEach(btn => {
                btn.addEventListener("click", () => copyToClipboard(btn.dataset.copyLink, btn));
            });

            // Delete
            tbody.querySelectorAll("[data-delete-link]").forEach(btn => {
                btn.addEventListener("click", () => {
                    deleteContext = { type: "link", id: btn.dataset.deleteLink };
                    deleteModalMessage.textContent = `Delete short link "${btn.dataset.deleteName}"?`;
                    modal.style.display = "flex";
                });
            });
        } catch (err) { console.error("loadShortLinksTable:", err); }
    }

    /* ======================================================
       CAMPAIGNS
    ====================================================== */
    async function refreshCampaignCache() {
        try {
            cachedCampaigns = await apiFetch(apiCampaignsUrl);
        } catch (err) {
            console.error("refreshCampaignCache:", err);
        }
    }

    document.getElementById("campaignForm").addEventListener("submit", async e => {
        e.preventDefault();
        const payload = {
            name:              document.getElementById("campaignName").value.trim(),
            campaignCode:      document.getElementById("campaignCode").value.trim() || null,
            description:       document.getElementById("campaignDescription").value.trim() || null,
            emailProviderType: document.getElementById("campaignEmailProvider").value || null
        };
        if (!payload.name) return;
        try {
            const campaign = await apiFetch(apiCampaignsUrl, { method: "POST", body: JSON.stringify(payload) });
            renderCampaignResult(campaign);
            e.target.reset();
            refreshCampaignCache();
            loadCampaignTable();
            showToast("Campaign created!", "success");
        } catch (err) {
            document.getElementById("campaignResult").innerHTML = `<p class="error">${err.message}</p>`;
        }
    });

    /* ── Campaign listing table ───────────────────────────── */
    async function loadCampaignTable() {
        const tbody = document.getElementById("campaignTableBody");
        const table = document.getElementById("campaignTable");
        const empty = document.getElementById("campaignEmpty");
        if (!tbody || !table || !empty) return;
        try {
            const campaigns = await apiFetch(apiCampaignsUrl);
            cachedCampaigns = campaigns;
            tbody.innerHTML = "";
            if (campaigns.length === 0) {
                table.hidden = true;
                empty.hidden = false;
                return;
            }
            empty.hidden = true;
            table.hidden = false;
            campaigns.forEach(c => {
                const providerBadge = c.emailProviderType
                    ? `<span class="badge badge-default">${escHtml(c.emailProviderType)}</span>`
                    : '<span class="muted">—</span>';
                const tr = document.createElement("tr");
                tr.innerHTML = `
                    <td><strong>${escHtml(c.name)}</strong></td>
                    <td class="truncate" style="max-width:180px">${escHtml(c.description || "—")}</td>
                    <td>${providerBadge}</td>
                    <td>${c.openCount ?? 0}</td>
                    <td>${fmtDate(c.createdAt)}</td>
                    <td class="table-actions">
                        ${c.emailProviderType === 'GMAIL' ? `<button class="primary-btn btn-xs" data-auth-gmail="${c.id}">Sign in with Gmail</button>` : ''}
                        ${c.trackingPixelUrl ? `<button class="secondary-btn btn-xs" data-copy-pixel="${escHtml(c.trackingPixelUrl)}">Copy Pixel</button>` : ''}
                        <button class="danger-btn" data-delete-campaign="${c.id}" data-delete-name="${escHtml(c.name)}">Delete</button>
                    </td>`;
                tbody.appendChild(tr);
            });

            // Gmail Auth
            tbody.querySelectorAll("[data-auth-gmail]").forEach(btn => {
                btn.addEventListener("click", () => {
                    const campaignId = btn.dataset.authGmail;
                    const clientId = window.GOOGLE_CLIENT_ID || "YOUR_GOOGLE_CLIENT_ID";
                    const redirectUri = window.location.origin + window.location.pathname;
                    const authUrl = `https://accounts.google.com/o/oauth2/v2/auth?client_id=${clientId}&redirect_uri=${redirectUri}&response_type=code&scope=https://www.googleapis.com/auth/gmail.send&access_type=offline&prompt=consent&state=${campaignId}`;
                    window.location.href = authUrl;
                });
            });

            // Copy pixel
            tbody.querySelectorAll("[data-copy-pixel]").forEach(btn => {
                btn.addEventListener("click", () => copyToClipboard(btn.dataset.copyPixel, btn));
            });

            // Delete
            tbody.querySelectorAll("[data-delete-campaign]").forEach(btn => {
                btn.addEventListener("click", () => {
                    deleteContext = { type: "campaign", id: btn.dataset.deleteCampaign };
                    deleteModalMessage.textContent = `Delete campaign "${btn.dataset.deleteName}"?`;
                    modal.style.display = "flex";
                });
            });
        } catch (err) { console.error("loadCampaignTable:", err); }
    }

    /* ======================================================
       TEMPLATES
    ====================================================== */
    const templateFormWrapper = document.getElementById("templateFormWrapper");
    const templateForm        = document.getElementById("templateForm");
    const templateEditId      = document.getElementById("templateEditId");
    const templateFormTitle   = document.getElementById("templateFormTitle");
    const templateTable       = document.getElementById("templateTable");
    const templateTableBody   = document.getElementById("templateTableBody");
    const templateEmpty       = document.getElementById("templateEmpty");
    const tplHtmlBody         = document.getElementById("tplHtmlBody");
    const tplHtmlPreview      = document.getElementById("tplHtmlPreview");

    document.getElementById("newTemplateBtn").addEventListener("click", () => {
        templateEditId.value = "";
        templateFormTitle.textContent = "New Template";
        templateForm.reset();
        tplHtmlBody.value = DEFAULT_EMAIL_TEMPLATE;
        templateFormWrapper.hidden = false;
        templateFormWrapper.scrollIntoView({ behavior: "smooth" });
    });

    document.getElementById("tplCancelBtn").addEventListener("click", () => {
        templateFormWrapper.hidden = true;
        templateForm.reset();
    });

    document.getElementById("tplFormatBtn").addEventListener("click", () => {
        tplHtmlBody.value = formatHtml(tplHtmlBody.value);
        scheduleTplPreview();
    });

    document.getElementById("tplPreviewToggle").addEventListener("click", () => {
        const previewModal = document.getElementById("previewModal");
        const modalHtmlPreview = document.getElementById("modalHtmlPreview");
        modalHtmlPreview.srcdoc = tplHtmlBody.value;
        previewModal.style.display = "flex";
    });

    document.getElementById("closePreviewModal").addEventListener("click", () => {
        document.getElementById("previewModal").style.display = "none";
    });

    tplHtmlBody.addEventListener("input", scheduleTplPreview);

    function scheduleTplPreview() {
        clearTimeout(tplPreviewTimer);
        tplPreviewTimer = setTimeout(() => {
            const previewModal = document.getElementById("previewModal");
            if (previewModal && previewModal.style.display === "flex") {
                document.getElementById("modalHtmlPreview").srcdoc = tplHtmlBody.value;
            }
        }, 300);
    }

    templateForm.addEventListener("submit", async e => {
        e.preventDefault();
        const id   = templateEditId.value;
        const body = {
            name:     document.getElementById("tplName").value.trim(),
            subject:  document.getElementById("tplSubject").value.trim(),
            htmlBody: tplHtmlBody.value
        };
        if (!body.name || !body.subject || !body.htmlBody) {
            showToast("Please fill all required fields", "error");
            return;
        }
        try {
            if (id) {
                await apiFetch(`${apiTemplatesUrl}/${id}`, { method: "PUT", body: JSON.stringify({ ...body, isActive: true }) });
                showToast("Template updated!", "success");
            } else {
                await apiFetch(apiTemplatesUrl, { method: "POST", body: JSON.stringify(body) });
                showToast("Template created!", "success");
            }
            templateFormWrapper.hidden = true;
            templateForm.reset();
            loadTemplateTable();
        } catch (err) {
            showToast("Error: " + err.message, "error");
        }
    });

    async function loadTemplateTable() {
        try {
            const templates = await apiFetch(`${apiTemplatesUrl}?includeInactive=false`);
            cachedTemplates = templates;
            templateTableBody.innerHTML = "";
            if (templates.length === 0) {
                templateTable.hidden = true;
                templateEmpty.hidden = false;
                return;
            }
            templateEmpty.hidden = true;
            templateTable.hidden = false;
            templates.forEach(t => {
                const tr = document.createElement("tr");
                tr.innerHTML = `
                    <td><strong>${escHtml(t.name)}</strong></td>
                    <td class="truncate">${escHtml(t.subject)}</td>
                    <td>${t.isActive ? '<span class="badge badge-active">Active</span>' : '<span class="badge badge-cancelled">Inactive</span>'}</td>
                    <td>${fmtDate(t.createdAt)}</td>
                    <td class="table-actions">
                        <button class="secondary-btn btn-xs" data-edit-tpl="${t.id}">Edit</button>
                        <button class="danger-btn" data-delete-tpl="${t.id}" data-delete-name="${escHtml(t.name)}">Delete</button>
                    </td>`;
                templateTableBody.appendChild(tr);
            });

            // Edit
            templateTableBody.querySelectorAll("[data-edit-tpl]").forEach(btn => {
                btn.addEventListener("click", async () => {
                    const tpl = cachedTemplates.find(t => t.id == btn.dataset.editTpl);
                    if (!tpl) return;
                    templateEditId.value = tpl.id;
                    templateFormTitle.textContent = "Edit Template";
                    document.getElementById("tplName").value    = tpl.name;
                    document.getElementById("tplSubject").value = tpl.subject;
                    tplHtmlBody.value = tpl.htmlBody;
                    templateFormWrapper.hidden = false;
                    templateFormWrapper.scrollIntoView({ behavior: "smooth" });
                });
            });

            // Soft-delete
            templateTableBody.querySelectorAll("[data-delete-tpl]").forEach(btn => {
                btn.addEventListener("click", () => {
                    deleteContext = { type: "template", id: btn.dataset.deleteTpl };
                    deleteModalMessage.textContent = `Delete template "${btn.dataset.deleteName}"?`;
                    modal.style.display = "flex";
                });
            });
        } catch (err) {
            console.error("loadTemplateTable:", err);
        }
    }

    /* ======================================================
       ACTIVITIES
    ====================================================== */
    const activityFormWrapper = document.getElementById("activityFormWrapper");
    const activityForm        = document.getElementById("activityForm");
    const activityEditId      = document.getElementById("activityEditId");
    const activityFormTitle   = document.getElementById("activityFormTitle");
    const activityTable       = document.getElementById("activityTable");
    const activityTableBody   = document.getElementById("activityTableBody");
    const activityEmpty       = document.getElementById("activityEmpty");
    const actScheduleType     = document.getElementById("actScheduleType");
    const actRecurrence       = document.getElementById("actRecurrence");
    const oneTimeFields       = document.getElementById("oneTimeFields");
    const recurringFields     = document.getElementById("recurringFields");
    const weeklyFields        = document.getElementById("weeklyFields");
    const monthlyFields       = document.getElementById("monthlyFields");

    // Schedule type visibility
    actScheduleType.addEventListener("change", () => {
        oneTimeFields.hidden   = actScheduleType.value !== "ONE_TIME";
        recurringFields.hidden = actScheduleType.value !== "RECURRING";
    });

    actRecurrence.addEventListener("change", () => {
        weeklyFields.hidden  = actRecurrence.value !== "WEEKLY";
        monthlyFields.hidden = actRecurrence.value !== "MONTHLY";
    });

    document.getElementById("newActivityBtn").addEventListener("click", () => {
        activityEditId.value = "";
        activityFormTitle.textContent = "New Activity";
        activityForm.reset();
        oneTimeFields.hidden = true;
        recurringFields.hidden = true;
        weeklyFields.hidden = true;
        monthlyFields.hidden = true;
        activityFormWrapper.hidden = false;
        loadSegmentDropdownForActivity();
        activityFormWrapper.scrollIntoView({ behavior: "smooth" });
    });

    document.getElementById("actCancelBtn").addEventListener("click", () => {
        activityFormWrapper.hidden = true;
        activityForm.reset();
    });

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

    async function loadCampaignDropdowns() {
        try {
            if (cachedCampaigns.length === 0) cachedCampaigns = await apiFetch(apiCampaignsUrl);
            const sel = document.getElementById("actCampaign");
            sel.innerHTML = '<option value="">— select —</option>';
            cachedCampaigns.forEach(c => {
                const opt = document.createElement("option");
                opt.value = c.id;
                opt.textContent = c.name;
                sel.appendChild(opt);
            });
        } catch (e) { console.error(e); }
    }

    async function loadTemplateDropdowns() {
        try {
            if (cachedTemplates.length === 0) cachedTemplates = await apiFetch(apiTemplatesUrl);
            const sel = document.getElementById("actTemplate");
            if (!sel) return;
            sel.innerHTML = '<option value="">— select —</option>';
            cachedTemplates.forEach(t => {
                const opt = document.createElement("option");
                opt.value = t.id;
                opt.textContent = t.name;
                sel.appendChild(opt);
            });
        } catch (e) { console.error(e); }
    }

    activityForm.addEventListener("submit", async e => {
        e.preventDefault();
        const id = activityEditId.value;
        const schedType = actScheduleType.value;

        const checkedDays = [...document.querySelectorAll("#weeklyFields input[type=checkbox]:checked")]
            .map(cb => cb.value);

        const payload = {
            campaignId:    Number(document.getElementById("actCampaign").value) || null,
            templateId:    Number(document.getElementById("actTemplate").value) || null,
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
            timezone:          document.getElementById("actTimezone").value || "UTC",
            status:            document.getElementById("actStatus").value || "ACTIVE"
        };

        if (!payload.campaignId || !payload.templateId || !payload.scheduleType) {
            showToast("Campaign, Template and Schedule Type are required", "error");
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
            activityForm.reset();
            oneTimeFields.hidden = true;
            recurringFields.hidden = true;
            loadActivityTable();
        } catch (err) {
            showToast("Error: " + err.message, "error");
        }
    });

    async function loadActivityTable() {
        try {
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
                const statusBadge = {
                    ACTIVE: "badge-active", PAUSED: "badge-paused",
                    COMPLETED: "badge-completed", CANCELLED: "badge-cancelled"
                }[a.status] || "badge-default";
                const isCompleted = a.status === "COMPLETED" || a.status === "CANCELLED";
                const tr = document.createElement("tr");
                tr.innerHTML = `
                    <td><strong>${escHtml(a.activityName || "—")}</strong></td>
                    <td>${escHtml(a.campaignName)}</td>
                    <td>${escHtml(a.templateName)}</td>
                    <td>${escHtml(schedLabel)}</td>
                    <td>${fmtDate(a.nextExecutionAt)}</td>
                    <td><span class="badge ${statusBadge}">${a.status}</span></td>
                    <td class="table-actions">
                        ${isCompleted ? `
                            <button class="secondary-btn btn-xs" data-clone-act="${a.id}" data-clone-name="${escHtml(a.activityName || '')}">Clone</button>
                            <button class="secondary-btn btn-xs" data-logs-act="${a.id}">Logs</button>
                        ` : `
                            <button class="secondary-btn btn-xs" data-test-trigger-act="${a.id}">Test Trigger</button>
                            <button class="secondary-btn btn-xs" data-edit-act="${a.id}">Edit</button>
                            <button class="secondary-btn btn-xs" data-logs-act="${a.id}">Logs</button>
                            <button class="danger-btn" data-cancel-act="${a.id}">Cancel</button>
                        `}
                    </td>`;
                activityTableBody.appendChild(tr);
            });

            // Edit
            activityTableBody.querySelectorAll("[data-edit-act]").forEach(btn => {
                btn.addEventListener("click", async () => {
                    try {
                        const a = await apiFetch(`${apiActivitiesUrl}/${btn.dataset.editAct}`);
                        activityEditId.value = a.id;
                        activityFormTitle.textContent = "Edit Activity";
                        document.getElementById("actCampaign").value   = a.campaignId;
                        document.getElementById("actTemplate").value   = a.templateId;
                        await loadSegmentDropdownForActivity();
                        if (a.segmentId) document.getElementById("actSegment").value = a.segmentId;
                        document.getElementById("actName").value       = a.activityName || "";
                        actScheduleType.value = a.scheduleType;
                        oneTimeFields.hidden   = a.scheduleType !== "ONE_TIME";
                        recurringFields.hidden = a.scheduleType !== "RECURRING";
                        if (a.executionDatetime) {
                            document.getElementById("actExecDatetime").value = a.executionDatetime.slice(0,16);
                        }
                        if (a.recurrenceType) actRecurrence.value = a.recurrenceType;
                        weeklyFields.hidden  = a.recurrenceType !== "WEEKLY";
                        monthlyFields.hidden = a.recurrenceType !== "MONTHLY";
                        if (a.executionTime) document.getElementById("actExecTime").value = a.executionTime;
                        if (a.dayOfMonth)    document.getElementById("actDayOfMonth").value = a.dayOfMonth;
                        if (a.startDate)     document.getElementById("actStartDate").value = a.startDate;
                        if (a.endDate)       document.getElementById("actEndDate").value   = a.endDate;
                        document.getElementById("actTimezone").value = a.timezone || "UTC";
                        document.getElementById("actStatus").value   = a.status   || "ACTIVE";
                        // Weekdays
                        document.querySelectorAll("#weeklyFields input[type=checkbox]").forEach(cb => {
                            cb.checked = (a.weekdays || []).includes(cb.value);
                        });
                        activityFormWrapper.hidden = false;
                        activityFormWrapper.scrollIntoView({ behavior: "smooth" });
                    } catch (err) { showToast("Error loading activity: " + err.message, "error"); }
                });
            });

            // Clone
            activityTableBody.querySelectorAll("[data-clone-act]").forEach(btn => {
                btn.addEventListener("click", () => {
                    document.getElementById("cloneActivityName").value = (btn.dataset.cloneName || "") + " – Copy";
                    document.getElementById("confirmClone").dataset.actId = btn.dataset.cloneAct;
                    cloneModal.style.display = "flex";
                });
            });

            // Execution logs
            activityTableBody.querySelectorAll("[data-logs-act]").forEach(btn => {
                btn.addEventListener("click", () => openLogsModal(btn.dataset.logsAct));
            });

            // Cancel
            activityTableBody.querySelectorAll("[data-cancel-act]").forEach(btn => {
                btn.addEventListener("click", () => {
                    deleteContext = { type: "activity", id: btn.dataset.cancelAct };
                    deleteModalMessage.textContent = "Cancel this activity? It will stop running.";
                    modal.style.display = "flex";
                });
            });

            // Test trigger
            activityTableBody.querySelectorAll("[data-test-trigger-act]").forEach(btn => {
                btn.addEventListener("click", () => {
                    const actId = btn.dataset.testTriggerAct;
                    document.getElementById("testFireEmails").value = "";
                    document.getElementById("confirmTestFire").dataset.actId = actId;
                    testFireModal.style.display = "flex";
                });
            });
        } catch (err) { console.error("loadActivityTable:", err); }
    }

    /* ── Execution logs inline viewer ─────────────────────── */
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
                table.innerHTML = `<thead><tr>
                    <th>Started</th><th>Status</th><th>Recipients</th><th>Response</th>
                </tr></thead>`;
                const tbody = document.createElement("tbody");
                logs.forEach(log => {
                    const statusBadge = {
                        SUCCESS: "badge-active", FAILED: "badge-cancelled", PARTIAL_SUCCESS: "badge-paused"
                    }[log.status] || "badge-default";
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

    /* ======================================================
       DELETE / CANCEL MODAL
    ====================================================== */
    confirmDeleteBtn.addEventListener("click", async () => {
        if (!deleteContext) return;
        const { type, id } = deleteContext;
        try {
            let url;
            if (type === "template") url = `${apiTemplatesUrl}/${id}`;
            else if (type === "activity") url = `${apiActivitiesUrl}/${id}`;
            else if (type === "link") url = `${apiLinksUrl}/${id}`;
            else if (type === "campaign") url = `${apiCampaignsUrl}/${id}`;

            await fetch(url, { method: "DELETE" });

            if (type === "template") loadTemplateTable();
            else if (type === "activity") loadActivityTable();
            else if (type === "link") loadShortLinksTable();
            else if (type === "campaign") { loadCampaignTable(); refreshCampaignCache(); }

            showToast("Deleted successfully", "success");
        } catch (err) { showToast("Error: " + err.message, "error"); }
        finally { modal.style.display = "none"; deleteContext = null; }
    });

    cancelDeleteBtn.addEventListener("click",  () => { modal.style.display = "none"; deleteContext = null; });
    window.addEventListener("click", ev       => { 
        if (ev.target === modal) { modal.style.display = "none"; deleteContext = null; } 
        if (ev.target === testFireModal) { testFireModal.style.display = "none"; }
        if (ev.target === cloneModal) { cloneModal.style.display = "none"; }
    });

    /* ======================================================
       TEST FIRE MODAL LOGIC
    ====================================================== */
    document.getElementById("cancelTestFire").addEventListener("click", () => {
        testFireModal.style.display = "none";
    });

    document.getElementById("confirmTestFire").addEventListener("click", async (e) => {
        const actId = e.target.dataset.actId;
        const emailsStr = document.getElementById("testFireEmails").value.trim();
        const emailIds = emailsStr ? emailsStr.split(",").map(s => s.trim()).filter(s => s) : [];

        try {
            e.target.disabled = true;
            e.target.textContent = "Sending...";
            
            await apiFetch(`${apiActivitiesUrl}/${actId}/test-trigger`, {
                method: "POST",
                body: JSON.stringify(emailIds)
            });
            
            showToast("Test mail triggered successfully!", "success");
            testFireModal.style.display = "none";
            loadActivityTable(); // To update status badge
        } catch (err) {
            showToast("Test failed: " + err.message, "error");
        } finally {
            e.target.disabled = false;
            e.target.textContent = "Send Test";
        }
    });

    /* ======================================================
       CLONE MODAL LOGIC
    ====================================================== */
    document.getElementById("cancelClone").addEventListener("click", () => {
        cloneModal.style.display = "none";
    });

    document.getElementById("confirmClone").addEventListener("click", async (e) => {
        const actId = e.target.dataset.actId;
        const newName = document.getElementById("cloneActivityName").value.trim();
        if (!newName) {
            showToast("Please enter a name for the cloned activity", "error");
            return;
        }
        try {
            e.target.disabled = true;
            e.target.textContent = "Cloning...";
            await apiFetch(`${apiActivitiesUrl}/${actId}/clone?newName=${encodeURIComponent(newName)}`, { method: "POST" });
            showToast("Activity cloned successfully!", "success");
            cloneModal.style.display = "none";
            loadActivityTable();
        } catch (err) {
            showToast("Clone failed: " + err.message, "error");
        } finally {
            e.target.disabled = false;
            e.target.textContent = "Clone";
        }
    });

    /* ======================================================
       SEGMENTS LOGIC
    ====================================================== */
    const segmentFormWrapper = document.getElementById("segmentFormWrapper");
    const segmentForm = document.getElementById("segmentForm");
    const segmentTableBody = document.getElementById("segmentTableBody");
    const segmentTable = document.getElementById("segmentTable");
    const segmentEmpty = document.getElementById("segmentEmpty");

    // Tabs logic
    document.querySelectorAll(".segment-tabs .tab-btn").forEach(btn => {
        btn.addEventListener("click", (e) => {
            document.querySelectorAll(".segment-tabs .tab-btn").forEach(b => b.classList.remove("active"));
            e.target.classList.add("active");
            
            const tabId = e.target.dataset.tab;
            document.getElementById("csvTabContent").hidden = (tabId !== "csvTab");
            document.getElementById("sqlTabContent").hidden = (tabId !== "sqlTab");
            document.getElementById("segmentType").value = (tabId === "csvTab") ? "CSV" : "SQL";
        });
    });

    document.getElementById("newSegmentBtn").addEventListener("click", () => {
        segmentForm.reset();
        segmentFormWrapper.hidden = false;
        segmentFormWrapper.scrollIntoView({ behavior: "smooth" });
    });

    document.getElementById("segCancelBtn").addEventListener("click", () => {
        segmentFormWrapper.hidden = true;
        segmentForm.reset();
    });

    segmentForm.addEventListener("submit", async (e) => {
        e.preventDefault();
        const type = document.getElementById("segmentType").value;
        const name = document.getElementById("segName").value.trim();
        const description = document.getElementById("segDescription").value.trim();
        
        try {
            document.getElementById("segSaveBtn").disabled = true;
            document.getElementById("segSaveBtn").textContent = "Saving...";

            if (type === "CSV") {
                const fileInput = document.getElementById("segCsvFile");
                if (!fileInput.files.length) throw new Error("Please select a CSV file");
                
                const formData = new FormData();
                formData.append("name", name);
                formData.append("description", description);
                formData.append("file", fileInput.files[0]);
                
                const res = await fetch(`${apiSegmentsUrl}/csv`, {
                    method: "POST",
                    body: formData
                });
                
                if (!res.ok) {
                    const msg = await res.text();
                    throw new Error(msg);
                }
            } else {
                const sqlQuery = document.getElementById("segSqlQuery").value.trim();
                if (!sqlQuery) throw new Error("Please enter an SQL query");
                
                await apiFetch(`${apiSegmentsUrl}/sql`, {
                    method: "POST",
                    body: JSON.stringify({ name, description, sqlQuery })
                });
            }
            
            showToast("Segment created successfully!", "success");
            segmentFormWrapper.hidden = true;
            segmentForm.reset();
            loadSegmentTable();
        } catch (err) {
            showToast("Error: " + err.message, "error");
        } finally {
            document.getElementById("segSaveBtn").disabled = false;
            document.getElementById("segSaveBtn").textContent = "💾 Save Segment";
        }
    });

    async function loadSegmentTable() {
        try {
            const segments = await apiFetch(apiSegmentsUrl);
            segmentTableBody.innerHTML = "";
            if (segments.length === 0) {
                segmentTable.hidden = true;
                segmentEmpty.hidden = false;
                return;
            }
            segmentEmpty.hidden = true;
            segmentTable.hidden = false;
            segments.forEach(s => {
                const typeBadge = s.segmentType === "CSV" ? "badge-default" : "badge-active";
                const tr = document.createElement("tr");
                tr.innerHTML = `
                    <td><strong>${escHtml(s.name)}</strong><br><small class="muted">${escHtml(s.description || "")}</small></td>
                    <td><span class="badge ${typeBadge}">${s.segmentType}</span></td>
                    <td>${fmtDate(s.createdAt)}</td>
                    <td class="table-actions">
                        <button class="danger-btn" data-delete-seg="${s.id}" data-delete-name="${escHtml(s.name)}">Delete</button>
                    </td>`;
                segmentTableBody.appendChild(tr);
            });

            // Delete
            segmentTableBody.querySelectorAll("[data-delete-seg]").forEach(btn => {
                btn.addEventListener("click", () => {
                    deleteContext = { type: "segment", id: btn.dataset.deleteSeg };
                    deleteModalMessage.textContent = `Delete segment "${btn.dataset.deleteName}"?`;
                    modal.style.display = "flex";
                });
            });
        } catch (err) { console.error("loadSegmentTable:", err); }
    }

    // Update delete handler for segments
    const originalConfirmDelete = confirmDeleteBtn.onclick;
    confirmDeleteBtn.addEventListener("click", async () => {
        if (!deleteContext) return;
        const { type, id } = deleteContext;
        if (type === "segment") {
            try {
                await fetch(`${apiSegmentsUrl}/${id}`, { method: "DELETE" });
                loadSegmentTable();
                showToast("Segment deleted successfully", "success");
            } catch (err) { showToast("Error: " + err.message, "error"); }
            finally { modal.style.display = "none"; deleteContext = null; }
            // Prevents original click listener from triggering if we handled it
            // Actually, we replaced it using event listener, so we just add conditions.
        }
    });

    /* ======================================================
       MOBILE MENU LOGIC
    ====================================================== */
    mobileMenuBtn.addEventListener("click", () => {
        sidebar.classList.toggle("open");
    });

    /* ======================================================
       UTILITIES
    ====================================================== */
    function buildCopyRow(label, value) {
        const wrap = document.createElement("div");
        wrap.className = "copy-block";
        const lbl = document.createElement("label");
        lbl.textContent = label;
        const row = document.createElement("div");
        row.className = "copy-row";
        const inp = document.createElement("input");
        inp.type = "text"; inp.readOnly = true; inp.value = value;
        const btn = document.createElement("button");
        btn.type = "button"; btn.textContent = "Copy";
        btn.addEventListener("click", () => copyToClipboard(value, btn));
        row.append(inp, btn);
        wrap.append(lbl, row);
        return wrap;
    }

    function renderCampaignResult(campaign) {
        const div = document.getElementById("campaignResult");
        div.innerHTML = "";
        div.className = "campaign-result";
        const h = document.createElement("h3");
        h.textContent = `Campaign "${campaign.name}" created`;
        div.appendChild(h);
        if (campaign.trackingPixelUrl) div.appendChild(buildCopyRow("Tracking pixel URL", campaign.trackingPixelUrl));
    }

    async function copyToClipboard(text, btn) {
        try {
            await navigator.clipboard.writeText(text);
            const orig = btn.textContent;
            btn.textContent = "Copied!";
            setTimeout(() => { btn.textContent = orig; }, 1500);
        } catch { alert("Could not copy"); }
    }

    function formatHtml(html) {
        const lines = html.replace(/>\s+</g, "><").split(/></).map((c, i, a) => {
            if (i > 0) c = "<" + c;
            if (i < a.length - 1) c += ">";
            return c;
        });
        let out = "", indent = 0;
        const voids = /^(area|base|br|col|embed|hr|img|input|link|meta|source|track|!DOCTYPE)/i;
        lines.forEach(line => {
            const isClose   = /^<\//.test(line.trim());
            const isOpen    = /^<[^/!][^>]*[^/]>$/.test(line.trim()) && !voids.test(line.trim());
            const isSelf    = /\/>$/.test(line.trim()) || voids.test(line.trim());
            if (isClose) indent = Math.max(indent - 1, 0);
            out += "  ".repeat(indent) + line.trim() + "\n";
            if (isOpen && !isSelf) indent++;
        });
        return out.trim();
    }

    /* ======================================================
       BOOTSTRAP
    ====================================================== */
    // Check for Google OAuth code
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.has('code') && urlParams.has('state')) {
        const code = urlParams.get('code');
        const campaignId = urlParams.get('state');
        const redirectUri = window.location.origin + window.location.pathname;

        const newUrl = window.location.origin + window.location.pathname;
        window.history.replaceState({}, document.title, newUrl);

        apiFetch(`${apiCampaignsUrl}/${campaignId}/google-code`, {
            method: "POST",
            body: JSON.stringify({ code: code, redirect_uri: redirectUri })
        }).then(() => {
            showToast("Gmail authorized successfully!", "success");
            switchSection("campaigns");
        }).catch(err => {
            showToast("Failed to save Gmail token: " + err.message, "error");
        });
    }

    // Pre-load templates for campaign and activity forms
    loadTemplateDropdowns();
    refreshCampaignCache();

    loadShortLinksTable();
    if (!urlParams.has('code')) {
        switchSection("shortener");
    }
});
