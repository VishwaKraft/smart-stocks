/* ============================================================
   modules/analytics.js — Analytics dashboard
   ============================================================ */

import { apiFetch } from '../api.js';
import { animateValue } from '../utils.js';
import { showToast } from '../ui.js';

export async function loadAnalyticsDashboard() {
    try {
        const data = await apiFetch("/api/analytics/email-metrics");
        animateValue("statSends",  0, data.totalSends,  1000);
        animateValue("statOpens",  0, data.totalOpens,  1000);
        animateValue("statClicks", 0, data.totalClicks, 1000);
    } catch (error) {
        console.error("Failed to load analytics data", error);
        showToast("Failed to load analytics data", "error");
    }
}
