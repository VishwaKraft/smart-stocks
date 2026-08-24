/* ============================================================
   app.js — Main entry point (ES6 module)
   ============================================================ */

import { apiLinksUrl, apiCampaignsUrl, apiTemplatesUrl, apiActivitiesUrl, apiSegmentsUrl, apiFetch } from './api.js';
import { showToast, switchSection, sidebar, mobileMenuBtn, modal, confirmDeleteBtn, cancelDeleteBtn, testFireModal, cloneModal } from './ui.js';
import { state, setDeleteContext } from './state.js';

import { loadShortLinksTable, initShortLinks }         from './modules/short-links.js';
import { refreshCampaignCache, loadCampaignTable, initCampaigns } from './modules/campaigns.js';
import { initTemplates, loadTemplateTable }            from './modules/templates.js';
import { initWhatsappTemplates, loadWhatsappTemplates, loadWaCampaignDropdown } from './modules/whatsapp-templates.js';
import { initVoiceTemplates, loadVoiceTemplates, loadVoiceTemplateCampaignDropdown } from './modules/voice-templates.js';
import { initActivities, loadActivityTable, loadTemplateDropdowns } from './modules/activities.js';
import { initSegments, loadSegmentTable }              from './modules/segments.js';
import { loadAnalyticsDashboard }                      from './modules/analytics.js';

document.addEventListener("DOMContentLoaded", () => {

    // ── Section navigation ───────────────────────────────────
    const loadCallbacks = {
        shortener:            () => loadShortLinksTable(),
        campaigns:            () => loadCampaignTable(),
        templates:            () => loadTemplateTable(),
        "whatsapp-templates": () => { loadWaCampaignDropdown(); loadWhatsappTemplates(); },
        "voice-templates":    () => { loadVoiceTemplateCampaignDropdown(); loadVoiceTemplates(); },
        activities:           () => { loadTemplateDropdowns(); loadActivityTable(); },
        segments:             () => loadSegmentTable(),
        analytics:            () => loadAnalyticsDashboard()
    };

    document.querySelectorAll(".section-nav-btn").forEach(btn =>
        btn.addEventListener("click", () => switchSection(btn.dataset.section, loadCallbacks)));

    // ── Mobile sidebar ───────────────────────────────────────
    if (mobileMenuBtn) {
        mobileMenuBtn.addEventListener("click", () => { sidebar.classList.toggle("open"); });
    }

    // ── Domain module initializations ───────────────────────
    initShortLinks();
    initCampaigns();
    initTemplates();
    initWhatsappTemplates();
    initVoiceTemplates();
    initActivities();
    initSegments();

    // ── Global delete modal handler ──────────────────────────
    confirmDeleteBtn.addEventListener("click", async () => {
        const ctx = state.deleteContext;
        if (!ctx) return;
        const { type, id, name, wabaId } = ctx;
        try {
            if (type === "whatsapp-template") {
                const query = ctx.queryParams || new URLSearchParams({ wabaId }).toString();
                await fetch(`/api/whatsapp/templates?${query}&name=${encodeURIComponent(name)}`, { method: "DELETE" });
                loadWhatsappTemplates();
            } else if (type === "segment") {
                await fetch(`${apiSegmentsUrl}/${id}`, { method: "DELETE" });
                loadSegmentTable();
            } else {
                const urlMap = {
                    template:       `${apiTemplatesUrl}/${id}`,
                    activity:       `${apiActivitiesUrl}/${id}`,
                    link:           `${apiLinksUrl}/${id}`,
                    campaign:       `${apiCampaignsUrl}/${id}`,
                    "voice-template": `${(window.API_VOICE_TEMPLATES_URL || "/api/voice/templates")}/${id}`
                };
                const url = urlMap[type];
                if (url) {
                    await fetch(url, { method: "DELETE" });
                    if (type === "template")       loadTemplateTable();
                    else if (type === "activity")  loadActivityTable();
                    else if (type === "link")      loadShortLinksTable();
                    else if (type === "campaign")  loadCampaignTable();
                    else if (type === "voice-template") loadVoiceTemplates();
                }
            }
            showToast("Deleted successfully", "success");
        } catch (err) {
            showToast("Error: " + err.message, "error");
        } finally {
            modal.style.display = "none";
            setDeleteContext(null);
        }
    });

    cancelDeleteBtn.addEventListener("click", () => { modal.style.display = "none"; setDeleteContext(null); });
    window.addEventListener("click", ev => {
        if (ev.target === modal)          { modal.style.display = "none"; setDeleteContext(null); }
        if (ev.target === testFireModal)  { testFireModal.style.display = "none"; }
        if (ev.target === cloneModal)     { cloneModal.style.display = "none"; }
    });

    // ── OAuth callback handling (Meta / Gmail) ───────────────
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.has('code') && urlParams.has('state')) {
        const code        = urlParams.get('code');
        const oauthState  = urlParams.get('state');
        const redirectUri = window.location.origin + window.location.pathname;
        window.history.replaceState({}, document.title, window.location.origin + window.location.pathname);

        if (oauthState.startsWith('meta_')) {
            const campaignId = oauthState.replace('meta_', '');
            apiFetch(`${apiCampaignsUrl}/${campaignId}/meta-code`, {
                method: "POST",
                body: JSON.stringify({ code, redirect_uri: redirectUri })
            }).then(() => {
                showToast("Meta authorized successfully!", "success");
                switchSection("campaigns", loadCallbacks);
            }).catch(err => showToast("Failed to save Meta token: " + err.message, "error"));
        } else {
            const campaignId = oauthState;
            apiFetch(`${apiCampaignsUrl}/${campaignId}/google-code`, {
                method: "POST",
                body: JSON.stringify({ code, redirect_uri: redirectUri })
            }).then(() => {
                showToast("Gmail authorized successfully!", "success");
                switchSection("campaigns", loadCallbacks);
            }).catch(err => showToast("Failed to save Gmail token: " + err.message, "error"));
        }
    }

    // ── Bootstrap ────────────────────────────────────────────
    loadTemplateDropdowns();
    refreshCampaignCache();
    loadShortLinksTable();

    if (!urlParams.has('code')) {
        switchSection("analytics", loadCallbacks);
    }
});
