/* ============================================================
   modules/short-links.js — URL Shortener section
   ============================================================ */

import { apiLinksUrl, shortLinkBaseUrl, apiFetch } from '../api.js';
import { fmtDate, escHtml, copyToClipboard } from '../utils.js';
import { modal, deleteModalMessage } from '../ui.js';
import { setDeleteContext } from '../state.js';

export function initShortLinks() {
    const shortenForm = document.getElementById("shortenForm");
    if (!shortenForm) return;

    shortenForm.addEventListener("submit", async e => {
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
        } catch (err) {
            document.getElementById("shortUrlResult").innerHTML = `<p class="error">${err.message}</p>`;
        }
    });
}

export async function loadShortLinksTable() {
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

        tbody.querySelectorAll("[data-copy-link]").forEach(btn => {
            btn.addEventListener("click", () => copyToClipboard(btn.dataset.copyLink, btn));
        });

        tbody.querySelectorAll("[data-delete-link]").forEach(btn => {
            btn.addEventListener("click", () => {
                setDeleteContext({ type: "link", id: btn.dataset.deleteLink });
                deleteModalMessage.textContent = `Delete short link "${btn.dataset.deleteName}"?`;
                modal.style.display = "flex";
            });
        });
    } catch (err) { console.error("loadShortLinksTable:", err); }
}
