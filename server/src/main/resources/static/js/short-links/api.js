/* ============================================================
   api.js — API URL constants & fetch helpers
   ============================================================ */

export const apiLinksUrl      = (window.API_LINKS_URL      || "/api/links").replace(/\/$/, "");
export const apiCampaignsUrl  = (window.API_CAMPAIGNS_URL  || "/api/campaigns").replace(/\/$/, "");
export const apiTemplatesUrl  = (window.API_TEMPLATES_URL  || "/api/templates").replace(/\/$/, "");
export const apiActivitiesUrl = (window.API_ACTIVITIES_URL || "/api/activities").replace(/\/$/, "");
export const apiSegmentsUrl   = (window.API_SEGMENTS_URL   || "/api/segments").replace(/\/$/, "");

export const shortLinkBaseUrl = (() => {
    const u = window.SHORT_LINK_BASE_URL || "/s/";
    return u.endsWith("/") ? u : u + "/";
})();

export async function apiFetch(url, opts = {}) {
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
