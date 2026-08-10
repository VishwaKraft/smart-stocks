/* ============================================================
   state.js — Shared mutable application state
   ============================================================ */

export const state = {
    deleteContext: null,
    cachedCampaigns: [],
    cachedTemplates: [],
    campaignsById: {}
};

export function setDeleteContext(ctx) {
    state.deleteContext = ctx;
}
