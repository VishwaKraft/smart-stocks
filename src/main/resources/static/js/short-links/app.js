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
    const whatsappTemplatesPanel = document.getElementById("whatsappTemplatesPanel");
    const voiceTemplatesPanel   = document.getElementById("voiceTemplatesPanel");
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
    let campaignsById         = {};

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
            "whatsapp-templates": whatsappTemplatesPanel,
            "voice-templates": voiceTemplatesPanel,
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
        } else if (section === "whatsapp-templates") {
            loadWaCampaignDropdown();
            loadWhatsappTemplates();
        } else if (section === "voice-templates") {
            loadVoiceTemplates();
            loadVoiceTemplateCampaignDropdown();
        } else if (section === "activities") {
            loadCampaignDropdowns().then(() => loadActivityTable());
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
        const method = (opts.method || "GET").toUpperCase();
        console.log(`[apiFetch] → ${method} ${url}`);
        const res = await fetch(url, {
            headers: { "Content-Type": "application/json" },
            ...opts
        });
        if (!res.ok) {
            const msg = await res.text().catch(() => "Unknown error");
            console.error(`[apiFetch] ✗ ${method} ${url} — HTTP ${res.status}:`, msg);
            throw new Error(msg || `HTTP ${res.status}`);
        }
        const ct = res.headers.get("content-type") || "";
        const data = ct.includes("application/json") ? await res.json() : await res.text();
        console.log(`[apiFetch] ✓ ${method} ${url} — HTTP ${res.status}`);
        return data;
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

    // Toggle fields based on campaign type
    document.querySelectorAll('input[name="campaignType"]').forEach(radio => {
        radio.addEventListener('change', (e) => {
            const isWhatsApp = e.target.value === 'WHATSAPP';
            const isVoice = e.target.value === 'VOICE';
            document.getElementById('emailProviderGroup').hidden = isWhatsApp || isVoice;
            document.getElementById('whatsappSenderGroup').hidden = !isWhatsApp;
            if (document.getElementById('voiceSenderGroup')) document.getElementById('voiceSenderGroup').hidden = !isVoice;
        });
    });

    // Campaign type card selector — visual selection handler
    document.querySelectorAll('.campaign-type-card').forEach(card => {
        card.addEventListener('click', () => {
            document.querySelectorAll('.campaign-type-card').forEach(c => c.classList.remove('selected'));
            card.classList.add('selected');
            // Trigger the hidden radio change
            const radio = card.querySelector('input[type="radio"]');
            if (radio) { radio.checked = true; radio.dispatchEvent(new Event('change', { bubbles: true })); }
        });
    });

    document.getElementById("campaignForm").addEventListener("submit", async e => {
        e.preventDefault();
        
        const campaignType = document.querySelector('input[name="campaignType"]:checked').value;
        const payload = {
            name:              document.getElementById("campaignName").value.trim(),
            campaignCode:      document.getElementById("campaignCode").value.trim() || null,
            description:       document.getElementById("campaignDescription").value.trim() || null,
            campaignType:      campaignType,
            emailProviderType: campaignType === 'EMAIL' ? (document.getElementById("campaignEmailProvider").value || null) : null,
            whatsappSenderNumber: campaignType === 'WHATSAPP' ? (document.getElementById("campaignWhatsappSender").value || null) : null,
            infobipSenderNumber: campaignType === 'VOICE' ? (document.getElementById("campaignVoiceSender").value || null) : null
        };
        if (!payload.name) return;
        try {
            const campaign = await apiFetch(apiCampaignsUrl, { method: "POST", body: JSON.stringify(payload) });
            renderCampaignResult(campaign);
            e.target.reset();
            // Reset card selection to EMAIL
            document.querySelectorAll('.campaign-type-card').forEach(c => c.classList.remove('selected'));
            const emailCard = document.querySelector('.campaign-type-card[data-value="EMAIL"]');
            if (emailCard) emailCard.classList.add('selected');
            document.getElementById('emailProviderGroup').hidden = false;
            document.getElementById('whatsappSenderGroup').hidden = true;
            if (document.getElementById('voiceSenderGroup')) document.getElementById('voiceSenderGroup').hidden = true;
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
                let providerSenderHtml = '<span class="muted">—</span>';
                if (c.campaignType === 'WHATSAPP' && c.whatsappSenderNumber) {
                    providerSenderHtml = `<span class="badge badge-default">Sender: ${escHtml(c.whatsappSenderNumber)}</span>`;
                } else if (c.campaignType === 'VOICE' && c.infobipSenderNumber) {
                    providerSenderHtml = `<span class="badge badge-default">Sender: ${escHtml(c.infobipSenderNumber)}</span>`;
                } else if (c.emailProviderType) {
                    providerSenderHtml = `<span class="badge badge-default">${escHtml(c.emailProviderType)}</span>`;
                }
                const typeMap = { EMAIL: 'badge-email', WHATSAPP: 'badge-whatsapp', VOICE: 'badge-voice' };
                const typeCls = typeMap[c.campaignType] || 'badge-default';
                const typeLabel = { EMAIL: '✉️ Email', WHATSAPP: '💬 WhatsApp', VOICE: '🎙️ Voice' }[c.campaignType] || (c.campaignType || 'EMAIL');
                const typeBadge = `<span class="badge ${typeCls}">${typeLabel}</span>`;
                
                const tr = document.createElement("tr");
                tr.innerHTML = `
                    <td><strong>${escHtml(c.name)}</strong></td>
                    <td>${typeBadge}</td>
                    <td class="truncate" style="max-width:180px">${escHtml(c.description || "—")}</td>
                    <td>${providerSenderHtml}</td>
                    <td>${fmtDate(c.createdAt)}</td>
                    <td class="table-actions">
                        ${c.emailProviderType === 'GMAIL' ? `<button class="primary-btn btn-xs" data-auth-gmail="${c.id}">Sign in with Gmail</button>` : ''}
                        ${c.campaignType === 'WHATSAPP' ? `<button class="primary-btn btn-xs" style="background: linear-gradient(135deg, #25d366, #128c7e);" data-auth-meta="${c.id}">🔑 Sign in with Meta</button>` : ''}
                        ${c.trackingPixelUrl && c.campaignType === 'EMAIL' ? `<button class="secondary-btn btn-xs" data-copy-pixel="${escHtml(c.trackingPixelUrl)}">Copy Pixel</button>` : ''}
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

            // Sign in with Meta (WhatsApp)
            tbody.querySelectorAll("[data-auth-meta]").forEach(btn => {
                btn.addEventListener("click", () => {
                    const campaignId = btn.dataset.authMeta;
                    document.getElementById("metaTokenCampaignId").value = campaignId;
                    document.getElementById("metaAccessTokenInput").value = "";
                    document.getElementById("metaPhoneNumberIdInput").value = "";
                    
                    const oauthSection = document.getElementById("metaOAuthSection");
                    if (oauthSection) {
                        oauthSection.hidden = !window.META_CLIENT_ID;
                    }
                    
                    document.getElementById("metaTokenModal").style.display = "flex";
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
        document.getElementById("chatHistory").innerHTML = ""; // Clear chat history
        templateFormWrapper.hidden = false;
        scheduleTplPreview();
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

    const sourceCodeModal = document.getElementById("sourceCodeModal");
    const tplPreviewToggle = document.getElementById("tplPreviewToggle");

    tplPreviewToggle.addEventListener("click", () => {
        sourceCodeModal.style.display = "flex";
    });

    document.getElementById("closeSourceCodeModal").addEventListener("click", () => {
        sourceCodeModal.style.display = "none";
        scheduleTplPreview(); // refresh preview in case manual edits were made
    });

    // Chatbot logic
    const aiSubmitBtn = document.getElementById("aiSubmitBtn");
    const aiPromptInput = document.getElementById("aiPromptInput");
    const aiLoadingSpinner = document.getElementById("aiLoadingSpinner");

    const chatHistory = document.getElementById("chatHistory");

    function appendChatMessage(text, sender) {
        const msgDiv = document.createElement("div");
        msgDiv.className = `chat-message ${sender}`;
        msgDiv.textContent = text;
        chatHistory.appendChild(msgDiv);
        chatHistory.scrollTop = chatHistory.scrollHeight;
    }

    aiSubmitBtn.addEventListener("click", async () => {
        const prompt = aiPromptInput.value.trim();
        if (!prompt) return;

        appendChatMessage(prompt, "user");
        aiPromptInput.value = "";
        aiSubmitBtn.disabled = true;
        aiLoadingSpinner.hidden = false;

        try {
            const res = await fetch(`${apiTemplatesUrl}/chat`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ prompt, currentHtml: tplHtmlBody.value })
            });

            if (!res.ok) {
                const msg = await res.text();
                throw new Error(msg || "Failed to get AI response");
            }

            const newHtml = await res.text();
            tplHtmlBody.value = newHtml;
            scheduleTplPreview();
            appendChatMessage("I've updated the template based on your request!", "ai");
            showToast("Template updated by AI!", "success");
        } catch (err) {
            appendChatMessage("Error: " + err.message, "ai");
            showToast("AI Error: " + err.message, "error");
        } finally {
            aiSubmitBtn.disabled = false;
            aiLoadingSpinner.hidden = true;
        }
    });

    tplHtmlBody.addEventListener("input", scheduleTplPreview);

    function scheduleTplPreview() {
        clearTimeout(tplPreviewTimer);
        tplPreviewTimer = setTimeout(() => {
            if (tplHtmlPreview) {
                tplHtmlPreview.srcdoc = tplHtmlBody.value;
                tplHtmlPreview.onload = () => {
                    const doc = tplHtmlPreview.contentWindow.document;
                    doc.designMode = "on";
                    doc.body.style.outline = "none";
                    doc.addEventListener("input", () => {
                        const hasHtmlTag = /<html/i.test(tplHtmlBody.value);
                        if (hasHtmlTag) {
                            tplHtmlBody.value = "<!DOCTYPE html>\n" + doc.documentElement.outerHTML;
                        } else {
                            tplHtmlBody.value = doc.body.innerHTML;
                        }
                    });
                };
            }
        }, 300);
    }

    // Modal Copy and Download Buttons
    document.getElementById("tplCopyBtn").addEventListener("click", () => {
        navigator.clipboard.writeText(tplHtmlBody.value).then(() => {
            showToast("HTML copied to clipboard!", "success");
        }).catch(() => {
            showToast("Failed to copy HTML", "error");
        });
    });

    document.getElementById("tplDownloadBtn").addEventListener("click", () => {
        const blob = new Blob([tplHtmlBody.value], { type: "text/html" });
        const url = URL.createObjectURL(blob);
        const a = document.createElement("a");
        a.href = url;
        a.download = "template.html";
        a.click();
        URL.revokeObjectURL(url);
    });

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
                    document.getElementById("chatHistory").innerHTML = ""; // Clear chat history
                    templateFormWrapper.hidden = false;
                    scheduleTplPreview();
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
        const emailGrp = document.getElementById("actEmailTemplateGroup");
        const waGrp = document.getElementById("actWaTemplateGroup");
        const waLangGrp = document.getElementById("actWaLanguageGroup");
        if (emailGrp) emailGrp.hidden = true;
        if (waGrp) waGrp.hidden = true;
        if (waLangGrp) waLangGrp.hidden = true;
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
            campaignsById = {};
            cachedCampaigns.forEach(c => {
                campaignsById[c.id] = c;
                const opt = document.createElement("option");
                opt.value = c.id;
                opt.textContent = c.name;
                sel.appendChild(opt);
            });
        } catch (e) { console.error(e); }
    }

    document.getElementById("actCampaign").addEventListener("change", async (e) => {
        const campaignId = e.target.value;
        const c = campaignsById[campaignId];
        const emailGrp = document.getElementById("actEmailTemplateGroup");
        const waGrp = document.getElementById("actWaTemplateGroup");
        const waLangGrp = document.getElementById("actWaLanguageGroup");
        const voiceGrp = document.getElementById("actVoiceTemplateGroup");
        
        if (c && c.campaignType === "WHATSAPP") {
            if (emailGrp) emailGrp.hidden = true;
            if (waGrp) waGrp.hidden = false;
            if (waLangGrp) waLangGrp.hidden = false;
            if (voiceGrp) voiceGrp.hidden = true;
            await loadWhatsappTemplatesForActivity("1726866808739698", c.id);
        } else if (c && c.campaignType === "VOICE") {
            if (emailGrp) emailGrp.hidden = true;
            if (waGrp) waGrp.hidden = true;
            if (waLangGrp) waLangGrp.hidden = true;
            if (voiceGrp) voiceGrp.hidden = false;
            await loadVoiceTemplatesForActivity(c.id);
        } else if (c && c.campaignType === "EMAIL") {
            if (emailGrp) emailGrp.hidden = false;
            if (waGrp) waGrp.hidden = true;
            if (waLangGrp) waLangGrp.hidden = true;
            if (voiceGrp) voiceGrp.hidden = true;
            await loadTemplateDropdowns();
        } else {
            if (emailGrp) emailGrp.hidden = true;
            if (waGrp) waGrp.hidden = true;
            if (waLangGrp) waLangGrp.hidden = true;
            if (voiceGrp) voiceGrp.hidden = true;
        }
    });

    async function loadWhatsappTemplatesForActivity(wabaId, campaignId) {
        const sel = document.getElementById("actWaTemplate");
        if (!sel) return;
        sel.innerHTML = '<option value="">— loading WhatsApp templates... —</option>';
        try {
            const params = new URLSearchParams();
            if (wabaId) params.append("wabaId", wabaId);
            if (campaignId) params.append("campaignId", campaignId);
            const res = await apiFetch(`/api/whatsapp/templates?${params}`);
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
            const res = await apiFetch(`/api/voice/templates?${params}`);
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

        const actCampaignId = Number(document.getElementById("actCampaign").value) || null;
        const c = campaignsById[actCampaignId];
        const isWa = c && c.campaignType === "WHATSAPP";

        const payload = {
            campaignId:    actCampaignId,
            templateId:    c && c.campaignType === "EMAIL" ? (Number(document.getElementById("actTemplate").value) || null) : null,
            voiceTemplateId: c && c.campaignType === "VOICE" ? (Number(document.getElementById("actVoiceTemplate").value) || null) : null,
            whatsappTemplateName: isWa ? document.getElementById("actWaTemplate").value : null,
            whatsappLanguage: isWa ? document.getElementById("actWaLanguage").value : null,
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
                // Look up campaign type for this activity
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

            // Edit
            activityTableBody.querySelectorAll("[data-edit-act]").forEach(btn => {
                btn.addEventListener("click", async () => {
                    try {
                        const a = await apiFetch(`${apiActivitiesUrl}/${btn.dataset.editAct}`);
                        activityEditId.value = a.id;
                        activityFormTitle.textContent = "Edit Activity";
                        document.getElementById("actCampaign").value   = a.campaignId;
                        
                        const c = campaignsById[a.campaignId];
                        const emailGrp = document.getElementById("actEmailTemplateGroup");
                        const waGrp = document.getElementById("actWaTemplateGroup");
                        const waLangGrp = document.getElementById("actWaLanguageGroup");
                        const voiceGrp = document.getElementById("actVoiceTemplateGroup");
                        if (c && c.campaignType === "WHATSAPP") {
                            if (emailGrp) emailGrp.hidden = true;
                            if (waGrp) waGrp.hidden = false;
                            if (waLangGrp) waLangGrp.hidden = false;
                            if (voiceGrp) voiceGrp.hidden = true;
                            await loadWhatsappTemplatesForActivity("1726866808739698", c.id);
                            document.getElementById("actWaTemplate").value = a.whatsappTemplateName || "";
                            document.getElementById("actWaLanguage").value = a.whatsappLanguage || "en";
                        } else if (c && c.campaignType === "VOICE") {
                            if (emailGrp) emailGrp.hidden = true;
                            if (waGrp) waGrp.hidden = true;
                            if (waLangGrp) waLangGrp.hidden = true;
                            if (voiceGrp) voiceGrp.hidden = false;
                            await loadVoiceTemplatesForActivity(c.id);
                            document.getElementById("actVoiceTemplate").value = a.voiceTemplateId || "";
                        } else if (c && c.campaignType === "EMAIL") {
                            if (emailGrp) emailGrp.hidden = false;
                            if (waGrp) waGrp.hidden = true;
                            if (waLangGrp) waLangGrp.hidden = true;
                            if (voiceGrp) voiceGrp.hidden = true;
                            document.getElementById("actTemplate").value   = a.templateId;
                        } else {
                            if (emailGrp) emailGrp.hidden = true;
                            if (waGrp) waGrp.hidden = true;
                            if (waLangGrp) waLangGrp.hidden = true;
                            if (voiceGrp) voiceGrp.hidden = true;
                        }
                        
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
                        document.getElementById("actTimezone").value = a.timezone || "Asia/Kolkata";
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

            // Generate
            activityTableBody.querySelectorAll("[data-generate-act]").forEach(btn => {
                btn.addEventListener("click", async () => {
                    if (!confirm("Are you sure you want to generate recipients for this activity?")) return;
                    try {
                        const actId = btn.dataset.generateAct;
                        btn.disabled = true;
                        btn.textContent = "Generating...";
                        await apiFetch(`${apiActivitiesUrl}/${actId}/generate`, { method: "POST" });
                        showToast("Activity generated successfully!", "success");
                        loadActivityTable();
                    } catch (err) {
                        showToast("Error generating activity: " + err.message, "error");
                        btn.disabled = false;
                        btn.textContent = "Generate";
                    }
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
                    // Determine campaign type for this activity
                    const actRow = btn.closest('tr');
                    const campaignName = actRow ? actRow.querySelector('td:nth-child(2)')?.textContent : '';
                    // Look up campaign type from the activity's campaignId via the row's data
                    const campaignType = btn.dataset.campaignType || 'EMAIL';
                    const isVoiceOrWa = (campaignType === 'VOICE' || campaignType === 'WHATSAPP');

                    document.getElementById("testFireRecipients").value = "";
                    document.getElementById("confirmTestFire").dataset.actId = actId;
                    document.getElementById("confirmTestFire").dataset.campaignType = campaignType;

                    const icon = { EMAIL: '✉️', WHATSAPP: '💬', VOICE: '🎙️' }[campaignType] || '✉️';
                    const title = { EMAIL: 'Test Email Send', WHATSAPP: 'Test WhatsApp Send', VOICE: 'Test Voice Call' }[campaignType] || 'Test Fire Activity';
                    const hint = isVoiceOrWa
                        ? 'Enter a phone number to receive the test (with country code, e.g. +91xxxxxxxxxx).'
                        : 'Enter comma-separated email addresses to receive the test.';
                    const label = isVoiceOrWa ? 'Phone Number *' : 'Email Addresses *';
                    const placeholder = isVoiceOrWa ? '+91xxxxxxxxxx' : 'user1@example.com, user2@example.com';

                    document.getElementById("testFireIcon").textContent = icon;
                    document.getElementById("testFireTitle").textContent = title;
                    document.getElementById("testFireHint").textContent = hint;
                    document.getElementById("testFireLabel").textContent = label;
                    document.getElementById("testFireRecipients").placeholder = placeholder;

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
        const { type, id, name, wabaId } = deleteContext;
        try {
            if (type === "whatsapp-template") {
                const params = getWabaRequestParams();
                const cleanParams = params.replace(/wabaId=[^&]*/, `wabaId=${wabaId}`);
                await fetch(`/api/whatsapp/templates?${cleanParams}&name=${name}`, { method: "DELETE" });
                loadWhatsappTemplates();
            } else {
                let url;
                if (type === "template") url = `${apiTemplatesUrl}/${id}`;
                else if (type === "activity") url = `${apiActivitiesUrl}/${id}`;
                else if (type === "link") url = `${apiLinksUrl}/${id}`;
                else if (type === "campaign") url = `${apiCampaignsUrl}/${id}`;
                else if (type === "voice-template") url = `${window.API_VOICE_TEMPLATES_URL}/${id}`;

                await fetch(url, { method: "DELETE" });

                if (type === "template") loadTemplateTable();
                else if (type === "activity") loadActivityTable();
                else if (type === "link") loadShortLinksTable();
                else if (type === "campaign") { loadCampaignTable(); refreshCampaignCache(); }
                else if (type === "voice-template") loadVoiceTemplates();
            }

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
        const campaignType = e.target.dataset.campaignType || 'EMAIL';
        const recipientsStr = document.getElementById("testFireRecipients").value.trim();
        const recipients = recipientsStr ? recipientsStr.split(",").map(s => s.trim()).filter(s => s) : [];

        if (!recipients.length) {
            showToast(campaignType === 'EMAIL' ? "Please enter at least one email address" : "Please enter a phone number", "error");
            return;
        }

        try {
            e.target.disabled = true;
            e.target.textContent = "Sending...";

            const isVoiceOrWa = (campaignType === 'VOICE' || campaignType === 'WHATSAPP');
            const payload = isVoiceOrWa
                ? { phoneNumbers: recipients }
                : recipients; // email: send plain array (existing backend format)

            await apiFetch(`${apiActivitiesUrl}/${actId}/test-trigger`, {
                method: "POST",
                body: JSON.stringify(payload)
            });

            const successMsg = { EMAIL: "Test email triggered!", WHATSAPP: "Test WhatsApp message triggered!", VOICE: "Test voice call triggered!" }[campaignType] || "Test triggered successfully!";
            showToast(successMsg, "success");
            testFireModal.style.display = "none";
            loadActivityTable();
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
            document.getElementById("s3TabContent").hidden = (tabId !== "s3Tab");
            
            let segmentType = "CSV";
            if (tabId === "sqlTab") segmentType = "SQL";
            else if (tabId === "s3Tab") segmentType = "S3";
            document.getElementById("segmentType").value = segmentType;
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
            } else if (type === "SQL") {
                const sqlQuery = document.getElementById("segSqlQuery").value.trim();
                if (!sqlQuery) throw new Error("Please enter an SQL query");
                
                await apiFetch(`${apiSegmentsUrl}/sql`, {
                    method: "POST",
                    body: JSON.stringify({ name, description, sqlQuery })
                });
            } else if (type === "S3") {
                const s3Path = document.getElementById("segS3Path").value.trim();
                if (!s3Path) throw new Error("Please enter an S3 Path");
                
                await apiFetch(`${apiSegmentsUrl}/s3-path`, {
                    method: "POST",
                    body: JSON.stringify({ name, description, s3Path })
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
                    <td>${s.userCount != null ? s.userCount : "—"}</td>
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
       SIGN IN WITH META MODAL
    ====================================================== */
    const metaTokenModal = document.getElementById("metaTokenModal");

    document.getElementById("cancelMetaToken").addEventListener("click", () => {
        metaTokenModal.style.display = "none";
    });

    document.getElementById("confirmMetaToken").addEventListener("click", async () => {
        const campaignId = document.getElementById("metaTokenCampaignId").value;
        const accessToken = document.getElementById("metaAccessTokenInput").value.trim();
        const phoneNumberId = document.getElementById("metaPhoneNumberIdInput").value.trim();

        if (!accessToken) { showToast("Access token is required", "error"); return; }
        if (!phoneNumberId) { showToast("Phone Number ID is required", "error"); return; }

        try {
            await apiFetch(`${apiCampaignsUrl}/${campaignId}/meta-token`, {
                method: "POST",
                body: JSON.stringify({ access_token: accessToken, phone_number_id: phoneNumberId })
            });
            showToast("Meta token saved successfully!", "success");
            metaTokenModal.style.display = "none";
            loadCampaignTable();
        } catch (err) {
            showToast("Failed to save Meta token: " + err.message, "error");
        }
    });

    const metaOAuthBtn = document.getElementById("metaOAuthBtn");
    if (metaOAuthBtn) {
        metaOAuthBtn.addEventListener("click", () => {
            const campaignId = document.getElementById("metaTokenCampaignId").value;
            const clientId = window.META_CLIENT_ID;
            const redirectUri = window.location.origin + window.location.pathname;
            const authUrl = `https://www.facebook.com/v25.0/dialog/oauth?client_id=${clientId}&redirect_uri=${encodeURIComponent(redirectUri)}&response_type=code&scope=whatsapp_business_management,whatsapp_business_messaging,public_profile&state=meta_${campaignId}`;
            window.location.href = authUrl;
        });
    }

    /* ======================================================
       WHATSAPP TEMPLATES PANEL
    ====================================================== */
    const waTemplateCampaignSelect = document.getElementById("waTemplateCampaignSelect");
    const waManualTokenGroup = document.getElementById("waManualTokenGroup");
    const waManualTokenInput = document.getElementById("waManualTokenInput");
    const waReloadTemplatesBtn = document.getElementById("waReloadTemplatesBtn");
    const waTemplateEmpty = document.getElementById("waTemplateEmpty");
    const waTemplateTable = document.getElementById("waTemplateTable");
    const waTemplateTableBody = document.getElementById("waTemplateTableBody");
    const newWhatsappTemplateBtn = document.getElementById("newWhatsappTemplateBtn");
    const waTemplateFormWrapper = document.getElementById("waTemplateFormWrapper");
    const waTemplateForm = document.getElementById("waTemplateForm");

    const waTplHeaderText = document.getElementById("waTplHeaderText");
    const waTplHeaderExampleGroup = document.getElementById("waTplHeaderExampleGroup");
    const waTplHeaderExample = document.getElementById("waTplHeaderExample");
    const waTplBodyText = document.getElementById("waTplBodyText");
    const waTplBodyExamplesGroup = document.getElementById("waTplBodyExamplesGroup");
    const waTplBodyExamples = document.getElementById("waTplBodyExamples");

    // Toggle fields visibility based on input patterns
    if (waTemplateCampaignSelect) {
        waTemplateCampaignSelect.addEventListener("change", () => {
            waManualTokenGroup.hidden = waTemplateCampaignSelect.value !== "manual";
        });
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
            waTplBodyExamplesGroup.hidden = true;
            waTemplateFormWrapper.hidden = false;
            waTemplateFormWrapper.scrollIntoView({ behavior: "smooth" });
        });
    }

    if (document.getElementById("waTplCancelBtn")) {
        document.getElementById("waTplCancelBtn").addEventListener("click", () => {
            waTemplateFormWrapper.hidden = true;
            waTemplateForm.reset();
        });
    }

    // Load WhatsApp Campaigns to populate the campaign selector
    async function loadWaCampaignDropdown() {
        if (!waTemplateCampaignSelect) return;
        try {
            if (cachedCampaigns.length === 0) cachedCampaigns = await apiFetch(apiCampaignsUrl);
            const currentVal = waTemplateCampaignSelect.value;
            waTemplateCampaignSelect.innerHTML = `
                <option value="">— select campaign —</option>
                <option value="manual">— Use Manual Token —</option>
            `;
            cachedCampaigns.forEach(c => {
                if (c.campaignType === 'WHATSAPP') {
                    const opt = document.createElement("option");
                    opt.value = c.id;
                    opt.textContent = c.name;
                    waTemplateCampaignSelect.appendChild(opt);
                }
            });
            if (currentVal) waTemplateCampaignSelect.value = currentVal;
        } catch (e) {
            console.error("loadWaCampaignDropdown error:", e);
        }
    }

    function getSelectedWabaId() {
        return "1726866808739698";
    }

    function getWabaRequestParams() {
        const wabaId = getSelectedWabaId();
        const campaignId = waTemplateCampaignSelect ? waTemplateCampaignSelect.value : "";
        const manualToken = waManualTokenInput ? waManualTokenInput.value.trim() : "";

        const params = new URLSearchParams();
        params.append("wabaId", wabaId);
        if (campaignId && campaignId !== "manual") {
            params.append("campaignId", campaignId);
        } else if (manualToken) {
            params.append("token", manualToken);
        }
        return params.toString();
    }

    async function loadWhatsappTemplates() {
        if (!waTemplateTableBody) return;

        const params = getWabaRequestParams();
        const wabaId = getSelectedWabaId();
        console.log(`[WA Templates] Loading templates for wabaId=${wabaId}`, Object.fromEntries(new URLSearchParams(params)));

        if (!params.includes("campaignId") && !params.includes("token")) {
            console.warn("[WA Templates] No token source configured — cannot load templates.");
            waTemplateTable.hidden = true;
            waTemplateEmpty.hidden = false;
            waTemplateEmpty.innerHTML = "Please configure/select a <strong>Campaign Token Source</strong> or enter a <strong>Manual Token</strong> to load templates.";
            return;
        }

        try {
            waTemplateEmpty.innerHTML = "🔄 Loading WhatsApp templates...";
            waTemplateEmpty.hidden = false;
            waTemplateTable.hidden = true;

            const res = await apiFetch(`/api/whatsapp/templates?${params}`);
            const templates = res.data || [];
            console.log(`[WA Templates] Received ${templates.length} template(s) from Meta.`);

            waTemplateTableBody.innerHTML = "";
            if (templates.length === 0) {
                waTemplateTable.hidden = true;
                waTemplateEmpty.hidden = false;
                waTemplateEmpty.innerHTML = "No templates found on Meta for the selected configuration.";
                return;
            }

            waTemplateEmpty.hidden = true;
            waTemplateTable.hidden = false;

            templates.forEach(t => {
                let bodyText = "—";
                (t.components || []).forEach(comp => {
                    if (comp.type === "BODY") {
                        bodyText = comp.text || "—";
                    }
                });

                const statusClass = {
                    APPROVED: "badge-active",
                    PENDING: "badge-paused",
                    REJECTED: "badge-cancelled"
                }[t.status] || "badge-default";

                const tr = document.createElement("tr");
                tr.innerHTML = `
                    <td><strong>${escHtml(t.name)}</strong></td>
                    <td><span class="badge badge-default">${escHtml(t.language)}</span></td>
                    <td><span class="badge badge-default">${escHtml(t.category)}</span></td>
                    <td><span class="badge ${statusClass}">${escHtml(t.status)}</span></td>
                    <td class="truncate" style="max-width: 300px;" title="${escHtml(bodyText)}">${escHtml(bodyText)}</td>
                    <td class="table-actions">
                        <button class="danger-btn" data-delete-wa-tpl="${escHtml(t.name)}" data-waba-id="${escHtml(getSelectedWabaId())}" data-delete-wa-tpl-name="${escHtml(t.name)}">Delete</button>
                    </td>
                `;
                waTemplateTableBody.appendChild(tr);
            });

            // Bind delete events
            waTemplateTableBody.querySelectorAll("[data-delete-wa-tpl]").forEach(btn => {
                btn.addEventListener("click", () => {
                    deleteContext = {
                        type: "whatsapp-template",
                        name: btn.dataset.deleteWaTpl,
                        wabaId: btn.dataset.wabaId
                    };
                    console.log(`[WA Templates] Delete requested for template: ${btn.dataset.deleteWaTpl}, wabaId: ${btn.dataset.wabaId}`);
                    deleteModalMessage.textContent = `Delete WhatsApp template "${btn.dataset.deleteWaTplName}"? This action cannot be undone.`;
                    modal.style.display = "flex";
                });
            });

        } catch (e) {
            console.error("[WA Templates] Failed to load templates:", e);
            waTemplateTable.hidden = true;
            waTemplateEmpty.hidden = false;
            waTemplateEmpty.innerHTML = `<span class="error">Error: ${escHtml(e.message)}</span>`;
        }
    }

    if (waReloadTemplatesBtn) {
        waReloadTemplatesBtn.addEventListener("click", () => {
            loadWhatsappTemplates();
        });
    }

    // Submit form to create template
    if (waTemplateForm) {
        waTemplateForm.addEventListener("submit", async (e) => {
            e.preventDefault();

            const name = document.getElementById("waTplName").value.trim().toLowerCase();
            const language = document.getElementById("waTplLanguage").value;
            const category = document.getElementById("waTplCategory").value;

            console.log(`[WA Template Create] Starting creation — name=${name}, language=${language}, category=${category}`);

            if (!/^[a-z0-9_]+$/.test(name)) {
                console.warn(`[WA Template Create] Invalid template name: "${name}"`);
                showToast("Template Name must contain only lowercase letters, numbers, and underscores.", "error");
                return;
            }

            const components = [];

            // Header component
            const headerText = waTplHeaderText.value.trim();
            if (headerText) {
                const headerComp = {
                    type: "HEADER",
                    format: "TEXT",
                    text: headerText
                };
                if (headerText.includes("{{1}}")) {
                    const hExample = waTplHeaderExample.value.trim();
                    if (!hExample) {
                        console.warn("[WA Template Create] Header has {{1}} variable but no example value provided.");
                        showToast("Please provide an example value for the header variable.", "error");
                        return;
                    }
                    headerComp.example = {
                        header_text: [hExample]
                    };
                    console.log(`[WA Template Create] Header variable example: "${hExample}"`);
                }
                components.push(headerComp);
                console.log(`[WA Template Create] Header component added: "${headerText}"`);
            }

            // Body component
            const bodyText = waTplBodyText.value.trim();
            if (!bodyText) {
                console.warn("[WA Template Create] Body text is empty — rejecting submission.");
                showToast("Body text is required.", "error");
                return;
            }
            const bodyComp = {
                type: "BODY",
                text: bodyText
            };

            const matches = bodyText.match(/\{\{\d+\}\}/g) || [];
            if (matches.length > 0) {
                const bExamplesStr = waTplBodyExamples.value.trim();
                if (!bExamplesStr) {
                    console.warn(`[WA Template Create] Body has ${matches.length} variable(s) but no examples provided.`);
                    showToast("Please provide comma-separated example values for body variables.", "error");
                    return;
                }
                const examplesArr = bExamplesStr.split(",").map(x => x.trim()).filter(x => x);
                bodyComp.example = {
                    body_text: [examplesArr]
                };
                console.log(`[WA Template Create] Body variables found: ${matches.join(", ")} — examples: [${examplesArr.join(", ")}]`);
            }
            components.push(bodyComp);

            // Footer component
            const footerText = document.getElementById("waTplFooterText").value.trim();
            if (footerText) {
                components.push({
                    type: "FOOTER",
                    text: footerText
                });
                console.log(`[WA Template Create] Footer component added: "${footerText}"`);
            }

            // Buttons component (Quick Replies)
            const buttonInputs = document.querySelectorAll(".wa-reply-btn-input");
            const quickReplies = [];
            buttonInputs.forEach(inp => {
                const val = inp.value.trim();
                if (val) {
                    quickReplies.push({
                        type: "QUICK_REPLY",
                        text: val
                    });
                }
            });

            if (quickReplies.length > 0) {
                components.push({
                    type: "BUTTONS",
                    buttons: quickReplies
                });
                console.log(`[WA Template Create] Quick reply buttons added: [${quickReplies.map(b => b.text).join(", ")}]`);
            }

            const payload = {
                name: name,
                language: language,
                category: category,
                components: components
            };

            const params = getWabaRequestParams();
            if (!params.includes("campaignId") && !params.includes("token")) {
                console.warn("[WA Template Create] No token source configured — aborting.");
                showToast("Please specify a valid Campaign Token Source or Manual Token.", "error");
                return;
            }

            console.log(`[WA Template Create] Submitting payload to Meta via proxy:`, JSON.stringify(payload, null, 2));

            try {
                const saveBtn = document.getElementById("waTplSaveBtn");
                saveBtn.disabled = true;
                saveBtn.textContent = "Saving...";

                const result = await apiFetch(`/api/whatsapp/templates?${params}`, {
                    method: "POST",
                    body: JSON.stringify(payload)
                });

                console.log("[WA Template Create] ✓ Template created successfully:", result);
                showToast("WhatsApp Template created successfully!", "success");
                waTemplateFormWrapper.hidden = true;
                waTemplateForm.reset();
                loadWhatsappTemplates();
            } catch (err) {
                console.error("[WA Template Create] ✗ Failed to create template:", err.message);
                showToast("Failed to create template: " + err.message, "error");
            } finally {
                const saveBtn = document.getElementById("waTplSaveBtn");
                saveBtn.disabled = false;
                saveBtn.textContent = "💾 Create WhatsApp Template";
            }
        });
    }

    /* ======================================================
       VOICE TEMPLATES PANEL
    ====================================================== */
    const voiceTemplateEmpty = document.getElementById("voiceTemplateEmpty");
    const voiceTemplateTable = document.getElementById("voiceTemplateTable");
    const voiceTemplateTableBody = document.getElementById("voiceTemplateTableBody");
    const newVoiceTemplateBtn = document.getElementById("newVoiceTemplateBtn");
    const voiceTemplateFormWrapper = document.getElementById("voiceTemplateFormWrapper");
    const voiceTemplateForm = document.getElementById("voiceTemplateForm");
    const voiceTemplateFormTitle = document.getElementById("voiceTemplateFormTitle");
    const voiceTplId = document.getElementById("voiceTplId");

    if (newVoiceTemplateBtn) {
        newVoiceTemplateBtn.addEventListener("click", () => {
            if (voiceTemplateForm) voiceTemplateForm.reset();
            if (voiceTplId) voiceTplId.value = "";
            if (voiceTemplateFormTitle) voiceTemplateFormTitle.textContent = "New Voice Template";
            const vName = document.getElementById("voiceTplVoiceName");
            const vGender = document.getElementById("voiceTplVoiceGender");
            if (vName) vName.value = "Joanna";
            if (vGender) vGender.value = "female";
            if (voiceTemplateFormWrapper) voiceTemplateFormWrapper.hidden = false;
            if (voiceTemplateFormWrapper) voiceTemplateFormWrapper.scrollIntoView({ behavior: "smooth" });
            updateVoicePayloadPreview();
        });
    }

    if (document.getElementById("voiceTplCancelBtn")) {
        document.getElementById("voiceTplCancelBtn").addEventListener("click", () => {
            if (voiceTemplateFormWrapper) voiceTemplateFormWrapper.hidden = true;
            if (voiceTemplateForm) voiceTemplateForm.reset();
        });
    }

    if (voiceTemplateForm) {
        voiceTemplateForm.addEventListener("submit", async (e) => {
            e.preventDefault();
            const id = voiceTplId ? voiceTplId.value : "";
            const payload = {
                name: document.getElementById("voiceTplName").value.trim(),
                language: document.getElementById("voiceTplLanguage").value,
                voiceName: document.getElementById("voiceTplVoiceName").value,
                voiceGender: document.getElementById("voiceTplVoiceGender").value,
                messageText: document.getElementById("voiceTplMessageText").value.trim(),
                isActive: true,
                campaignId: document.getElementById("voiceTplCampaign")?.value ? Number(document.getElementById("voiceTplCampaign").value) : null
            };
            try {
                if (id) {
                    await apiFetch(`${window.API_VOICE_TEMPLATES_URL}/${id}`, { method: "PUT", body: JSON.stringify(payload) });
                    showToast("Voice template updated successfully!", "success");
                } else {
                    await apiFetch(window.API_VOICE_TEMPLATES_URL, { method: "POST", body: JSON.stringify(payload) });
                    showToast("Voice template created successfully!", "success");
                }
                if (voiceTemplateFormWrapper) voiceTemplateFormWrapper.hidden = true;
                if (voiceTemplateForm) voiceTemplateForm.reset();
                loadVoiceTemplates();
            } catch (err) {
                showToast("Error saving template: " + err.message, "error");
            }
        });
    }

    // Voice name → auto-fill gender
    const voiceTplVoiceNameSel = document.getElementById("voiceTplVoiceName");
    const voiceTplVoiceGenderSel = document.getElementById("voiceTplVoiceGender");
    const VOICE_GENDER_MAP = {
        "Joanna": "female", "Celine": "female", "Aditi": "female",
        "Raveena": "female", "Conchita": "female",
        "Matthew": "male", "Mathieu": "male", "Enrique": "male"
    };
    if (voiceTplVoiceNameSel) {
        voiceTplVoiceNameSel.addEventListener("change", () => {
            const g = VOICE_GENDER_MAP[voiceTplVoiceNameSel.value];
            if (g && voiceTplVoiceGenderSel) voiceTplVoiceGenderSel.value = g;
            updateVoicePayloadPreview();
        });
    }
    if (voiceTplVoiceGenderSel) voiceTplVoiceGenderSel.addEventListener("change", updateVoicePayloadPreview);
    const voiceTplLangSel = document.getElementById("voiceTplLanguage");
    if (voiceTplLangSel) voiceTplLangSel.addEventListener("change", updateVoicePayloadPreview);

    // Live preview function (also exposed globally for oninput)
    window.updateVoicePayloadPreview = function() {
        const pre = document.getElementById("voicePayloadPreviewCode");
        if (!pre) return;
        const lang = document.getElementById("voiceTplLanguage")?.value || "en";
        const text = document.getElementById("voiceTplMessageText")?.value.trim() || "...";
        const vName = document.getElementById("voiceTplVoiceName")?.value || "Joanna";
        const vGender = document.getElementById("voiceTplVoiceGender")?.value || "female";
        pre.textContent = JSON.stringify({
            language: lang,
            text: text,
            voice: { name: vName, gender: vGender }
        }, null, 2);
    };

    // Populate voice template campaign dropdown
    async function loadVoiceTemplateCampaignDropdown() {
        const sel = document.getElementById("voiceTplCampaign");
        if (!sel) return;
        try {
            if (cachedCampaigns.length === 0) cachedCampaigns = await apiFetch(apiCampaignsUrl);
            sel.innerHTML = '<option value="">— none —</option>';
            cachedCampaigns.filter(c => c.campaignType === 'VOICE').forEach(c => {
                const opt = document.createElement("option");
                opt.value = c.id;
                opt.textContent = c.name;
                sel.appendChild(opt);
            });
        } catch (e) { console.error(e); }
    }

    async function loadVoiceTemplates() {
        if (!voiceTemplateTableBody) return;
        try {
            const templates = await apiFetch(window.API_VOICE_TEMPLATES_URL);
            voiceTemplateTableBody.innerHTML = "";
            if (!templates || templates.length === 0) {
                voiceTemplateTable.hidden = true;
                voiceTemplateEmpty.hidden = false;
                return;
            }
            voiceTemplateTable.hidden = false;
            voiceTemplateEmpty.hidden = true;

            templates.forEach(t => {
                const tr = document.createElement("tr");
                tr.innerHTML = `
                    <td><strong>${escHtml(t.name)}</strong></td>
                    <td>${escHtml(t.language)}</td>
                    <td>${escHtml(t.voiceName)} (${escHtml(t.voiceGender)})</td>
                    <td style="max-width:300px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;" title="${escHtml(t.messageText)}">
                        ${escHtml(t.messageText)}
                    </td>
                    <td class="table-actions">
                        <button class="secondary-btn btn-xs" data-edit-vt="${t.id}">Edit</button>
                        <button class="danger-btn btn-xs" data-del-vt="${t.id}" data-name="${escHtml(t.name)}">Delete</button>
                    </td>
                `;
                voiceTemplateTableBody.appendChild(tr);
            });

            // Edit
            voiceTemplateTableBody.querySelectorAll("[data-edit-vt]").forEach(btn => {
                btn.addEventListener("click", async () => {
                    const id = btn.dataset.editVt;
                    try {
                        const t = await apiFetch(`${window.API_VOICE_TEMPLATES_URL}/${id}`);
                        voiceTplId.value = t.id;
                        document.getElementById("voiceTplName").value = t.name || "";
                        document.getElementById("voiceTplLanguage").value = t.language || "en";
                        document.getElementById("voiceTplVoiceName").value = t.voiceName || "Joanna";
                        document.getElementById("voiceTplVoiceGender").value = t.voiceGender || "female";
                        document.getElementById("voiceTplMessageText").value = t.messageText || "";
                        if (voiceTemplateFormTitle) voiceTemplateFormTitle.textContent = "Edit Voice Template";
                        if (voiceTemplateFormWrapper) voiceTemplateFormWrapper.hidden = false;
                        if (voiceTemplateFormWrapper) voiceTemplateFormWrapper.scrollIntoView({ behavior: "smooth" });
                        updateVoicePayloadPreview();
                    } catch (e) {
                        showToast("Failed to load template", "error");
                    }
                });
            });

            // Delete
            voiceTemplateTableBody.querySelectorAll("[data-del-vt]").forEach(btn => {
                btn.addEventListener("click", () => {
                    deleteContext = { type: "voice-template", id: btn.dataset.delVt };
                    deleteModalMessage.innerHTML = `Are you sure you want to delete Voice template <strong>${btn.dataset.name}</strong>?`;
                    modal.classList.add("show");
                });
            });
        } catch (e) {
            console.error(e);
            showToast("Failed to load Voice templates", "error");
        }
    }

    /* ======================================================
       BOOTSTRAP
    ====================================================== */
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.has('code') && urlParams.has('state')) {
        const code = urlParams.get('code');
        const state = urlParams.get('state');
        const redirectUri = window.location.origin + window.location.pathname;

        const newUrl = window.location.origin + window.location.pathname;
        window.history.replaceState({}, document.title, newUrl);

        if (state.startsWith('meta_')) {
            const campaignId = state.replace('meta_', '');
            apiFetch(`${apiCampaignsUrl}/${campaignId}/meta-code`, {
                method: "POST",
                body: JSON.stringify({ code: code, redirect_uri: redirectUri })
            }).then(() => {
                showToast("Meta authorized successfully!", "success");
                switchSection("campaigns");
            }).catch(err => {
                showToast("Failed to save Meta token: " + err.message, "error");
            });
        } else {
            const campaignId = state;
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
    }


    // Pre-load templates for campaign and activity forms
    loadTemplateDropdowns();
    refreshCampaignCache();

    loadShortLinksTable();
    if (!urlParams.has('code')) {
        switchSection("shortener");
    }
});
