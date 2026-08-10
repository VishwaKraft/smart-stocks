/* ============================================================
   ui.js — Core UI helpers (toast, section switching, DOM refs)
   ============================================================ */

export const toastEl            = document.getElementById("toast");
export const modal              = document.getElementById("deleteModal");
export const deleteModalMessage = document.getElementById("deleteModalMessage");
export const confirmDeleteBtn   = document.getElementById("confirmDelete");
export const cancelDeleteBtn    = document.getElementById("cancelDelete");
export const testFireModal      = document.getElementById("testFireModal");
export const cloneModal         = document.getElementById("cloneModal");
export const sidebar            = document.querySelector(".sidebar");
export const mobileMenuBtn      = document.getElementById("mobileMenuBtn");

export function showToast(msg, type = "default") {
    toastEl.textContent = msg;
    toastEl.className = "toast show" + (type !== "default" ? " toast-" + type : "");
    clearTimeout(toastEl._timer);
    toastEl._timer = setTimeout(() => {
        toastEl.classList.remove("show");
    }, 3000);
}

/**
 * Switch the visible panel.
 * NOTE: Load callbacks are passed in from app.js to avoid circular imports.
 */
export function switchSection(section, loadCallbacks = {}) {
    const panelIds = {
        shortener:            "shortenerPanel",
        campaigns:            "campaignPanel",
        templates:            "templatePanel",
        "whatsapp-templates": "whatsappTemplatesPanel",
        "voice-templates":    "voiceTemplatesPanel",
        activities:           "activityPanel",
        segments:             "segmentPanel",
        analytics:            "analyticsPanel",
        "email-events":       "emailEventsPanel"
    };

    document.querySelectorAll(".section-nav-btn").forEach(btn =>
        btn.classList.toggle("active", btn.dataset.section === section));

    Object.entries(panelIds).forEach(([key, id]) => {
        const el = document.getElementById(id);
        if (el) el.hidden = key !== section;
    });

    // Trigger load callback for the activated section
    if (loadCallbacks[section]) {
        loadCallbacks[section]();
    }

    if (window.innerWidth <= 960) {
        sidebar.classList.remove("open");
    }
}
