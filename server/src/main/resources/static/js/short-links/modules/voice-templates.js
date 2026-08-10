/* ============================================================
   modules/voice-templates.js — Infobip Voice template management
   ============================================================ */

import { apiCampaignsUrl, apiFetch } from '../api.js';
import { escHtml } from '../utils.js';
import { modal, deleteModalMessage, showToast } from '../ui.js';
import { state, setDeleteContext } from '../state.js';

const VOICE_API_URL = () => window.API_VOICE_TEMPLATES_URL || "/api/voice/templates";

const VOICE_GENDER_MAP = {
    "Joanna": "female", "Celine": "female", "Aditi": "female",
    "Raveena": "female", "Conchita": "female",
    "Matthew": "male",  "Mathieu": "male",  "Enrique": "male"
};

function updateVoicePayloadPreview() {
    const pre = document.getElementById("voicePayloadPreviewCode");
    if (!pre) return;
    const lang    = document.getElementById("voiceTplLanguage")?.value    || "en";
    const text    = document.getElementById("voiceTplMessageText")?.value.trim() || "...";
    const vName   = document.getElementById("voiceTplVoiceName")?.value   || "Joanna";
    const vGender = document.getElementById("voiceTplVoiceGender")?.value || "female";
    pre.textContent = JSON.stringify({ language: lang, text, voice: { name: vName, gender: vGender } }, null, 2);
}
// Expose for the inline oninput on the textarea in HTML
window.updateVoicePayloadPreview = updateVoicePayloadPreview;

export async function loadVoiceTemplateCampaignDropdown() {
    const sel = document.getElementById("voiceTplCampaign");
    if (!sel) return;
    try {
        if (state.cachedCampaigns.length === 0) state.cachedCampaigns = await apiFetch(apiCampaignsUrl);
        sel.innerHTML = '<option value="">— none —</option>';
        state.cachedCampaigns.filter(c => c.campaignType === 'VOICE').forEach(c => {
            const opt = document.createElement("option");
            opt.value = c.id;
            opt.textContent = c.name;
            sel.appendChild(opt);
        });
    } catch (e) { console.error(e); }
}

export async function loadVoiceTemplates() {
    const voiceTemplateTableBody = document.getElementById("voiceTemplateTableBody");
    const voiceTemplateTable     = document.getElementById("voiceTemplateTable");
    const voiceTemplateEmpty     = document.getElementById("voiceTemplateEmpty");
    const voiceTemplateFormWrapper = document.getElementById("voiceTemplateFormWrapper");
    const voiceTemplateFormTitle   = document.getElementById("voiceTemplateFormTitle");
    const voiceTplId               = document.getElementById("voiceTplId");

    if (!voiceTemplateTableBody) return;
    try {
        const templates = await apiFetch(VOICE_API_URL());
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
                    const t = await apiFetch(`${VOICE_API_URL()}/${id}`);
                    voiceTplId.value = t.id;
                    document.getElementById("voiceTplName").value          = t.name || "";
                    document.getElementById("voiceTplLanguage").value      = t.language || "en";
                    document.getElementById("voiceTplVoiceName").value     = t.voiceName || "Joanna";
                    document.getElementById("voiceTplVoiceGender").value   = t.voiceGender || "female";
                    document.getElementById("voiceTplMessageText").value   = t.messageText || "";
                    document.getElementById("voiceTplDataSourceUrl").value = t.dataSourceUrl || "";
                    if (voiceTemplateFormTitle)   voiceTemplateFormTitle.textContent = "Edit Voice Template";
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
                setDeleteContext({ type: "voice-template", id: btn.dataset.delVt });
                deleteModalMessage.innerHTML = `Are you sure you want to delete Voice template <strong>${btn.dataset.name}</strong>?`;
                modal.style.display = "flex";
            });
        });
    } catch (e) {
        console.error(e);
        showToast("Failed to load Voice templates", "error");
    }
}

export function initVoiceTemplates() {
    const voiceTemplateFormWrapper = document.getElementById("voiceTemplateFormWrapper");
    const voiceTemplateForm        = document.getElementById("voiceTemplateForm");
    const voiceTemplateFormTitle   = document.getElementById("voiceTemplateFormTitle");
    const voiceTplId               = document.getElementById("voiceTplId");
    const newVoiceTemplateBtn      = document.getElementById("newVoiceTemplateBtn");

    if (newVoiceTemplateBtn) {
        newVoiceTemplateBtn.addEventListener("click", () => {
            if (voiceTemplateForm)    voiceTemplateForm.reset();
            if (voiceTplId)           voiceTplId.value = "";
            if (voiceTemplateFormTitle) voiceTemplateFormTitle.textContent = "New Voice Template";
            const vName   = document.getElementById("voiceTplVoiceName");
            const vGender = document.getElementById("voiceTplVoiceGender");
            const vData   = document.getElementById("voiceTplDataSourceUrl");
            if (vName)   vName.value   = "Joanna";
            if (vGender) vGender.value = "female";
            if (vData)   vData.value   = "";
            if (voiceTemplateFormWrapper) { voiceTemplateFormWrapper.hidden = false; voiceTemplateFormWrapper.scrollIntoView({ behavior: "smooth" }); }
            updateVoicePayloadPreview();
        });
    }

    const cancelBtn = document.getElementById("voiceTplCancelBtn");
    if (cancelBtn) {
        cancelBtn.addEventListener("click", () => {
            if (voiceTemplateFormWrapper) voiceTemplateFormWrapper.hidden = true;
            if (voiceTemplateForm)        voiceTemplateForm.reset();
        });
    }

    if (voiceTemplateForm) {
        voiceTemplateForm.addEventListener("submit", async (e) => {
            e.preventDefault();
            const id = voiceTplId ? voiceTplId.value : "";
            const payload = {
                name:          document.getElementById("voiceTplName").value.trim(),
                language:      document.getElementById("voiceTplLanguage").value,
                voiceName:     document.getElementById("voiceTplVoiceName").value,
                voiceGender:   document.getElementById("voiceTplVoiceGender").value,
                messageText:   document.getElementById("voiceTplMessageText").value.trim(),
                dataSourceUrl: document.getElementById("voiceTplDataSourceUrl").value.trim() || null,
                isActive:      true,
                campaignId:    document.getElementById("voiceTplCampaign")?.value ? Number(document.getElementById("voiceTplCampaign").value) : null
            };
            try {
                if (id) {
                    await apiFetch(`${VOICE_API_URL()}/${id}`, { method: "PUT", body: JSON.stringify(payload) });
                    showToast("Voice template updated successfully!", "success");
                } else {
                    await apiFetch(VOICE_API_URL(), { method: "POST", body: JSON.stringify(payload) });
                    showToast("Voice template created successfully!", "success");
                }
                if (voiceTemplateFormWrapper) voiceTemplateFormWrapper.hidden = true;
                if (voiceTemplateForm)        voiceTemplateForm.reset();
                loadVoiceTemplates();
            } catch (err) {
                showToast("Error saving template: " + err.message, "error");
            }
        });
    }

    // Voice name → auto-fill gender + payload preview
    const voiceTplVoiceNameSel   = document.getElementById("voiceTplVoiceName");
    const voiceTplVoiceGenderSel = document.getElementById("voiceTplVoiceGender");
    const voiceTplLangSel        = document.getElementById("voiceTplLanguage");

    if (voiceTplVoiceNameSel) {
        voiceTplVoiceNameSel.addEventListener("change", () => {
            const g = VOICE_GENDER_MAP[voiceTplVoiceNameSel.value];
            if (g && voiceTplVoiceGenderSel) voiceTplVoiceGenderSel.value = g;
            updateVoicePayloadPreview();
        });
    }
    if (voiceTplVoiceGenderSel) voiceTplVoiceGenderSel.addEventListener("change", updateVoicePayloadPreview);
    if (voiceTplLangSel)        voiceTplLangSel.addEventListener("change",        updateVoicePayloadPreview);
}
