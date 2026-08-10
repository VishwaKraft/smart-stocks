/* ============================================================
   modules/segments.js — Audience segments management
   ============================================================ */

import { apiSegmentsUrl, apiFetch } from '../api.js';
import { fmtDate, escHtml } from '../utils.js';
import { modal, deleteModalMessage, showToast } from '../ui.js';
import { setDeleteContext } from '../state.js';

export function initSegments() {
    const segmentFormWrapper = document.getElementById("segmentFormWrapper");
    const segmentForm        = document.getElementById("segmentForm");

    // Tabs logic
    document.querySelectorAll(".segment-tabs .tab-btn").forEach(btn => {
        btn.addEventListener("click", (e) => {
            document.querySelectorAll(".segment-tabs .tab-btn").forEach(b => b.classList.remove("active"));
            e.target.classList.add("active");
            const tabId = e.target.dataset.tab;
            document.getElementById("csvTabContent").hidden = (tabId !== "csvTab");
            document.getElementById("sqlTabContent").hidden = (tabId !== "sqlTab");
            document.getElementById("s3TabContent").hidden  = (tabId !== "s3Tab");
            let segmentType = "CSV";
            if (tabId === "sqlTab") segmentType = "SQL";
            else if (tabId === "s3Tab") segmentType = "S3";
            document.getElementById("segmentType").value = segmentType;
        });
    });

    const newSegmentBtn = document.getElementById("newSegmentBtn");
    if (newSegmentBtn) {
        newSegmentBtn.addEventListener("click", () => {
            segmentForm.reset();
            segmentFormWrapper.hidden = false;
            segmentFormWrapper.scrollIntoView({ behavior: "smooth" });
        });
    }

    const segCancelBtn = document.getElementById("segCancelBtn");
    if (segCancelBtn) {
        segCancelBtn.addEventListener("click", () => {
            segmentFormWrapper.hidden = true;
            segmentForm.reset();
        });
    }

    if (segmentForm) {
        segmentForm.addEventListener("submit", async (e) => {
            e.preventDefault();
            const type        = document.getElementById("segmentType").value;
            const name        = document.getElementById("segName").value.trim();
            const description = document.getElementById("segDescription").value.trim();

            try {
                document.getElementById("segSaveBtn").disabled    = true;
                document.getElementById("segSaveBtn").textContent = "Saving...";

                if (type === "CSV") {
                    const fileInput = document.getElementById("segCsvFile");
                    if (!fileInput.files.length) throw new Error("Please select a CSV file");
                    const formData = new FormData();
                    formData.append("name", name);
                    formData.append("description", description);
                    formData.append("file", fileInput.files[0]);
                    const res = await fetch(`${apiSegmentsUrl}/csv`, { method: "POST", body: formData });
                    if (!res.ok) { const msg = await res.text(); throw new Error(msg); }
                } else if (type === "SQL") {
                    const sqlQuery = document.getElementById("segSqlQuery").value.trim();
                    if (!sqlQuery) throw new Error("Please enter an SQL query");
                    await apiFetch(`${apiSegmentsUrl}/sql`, { method: "POST", body: JSON.stringify({ name, description, sqlQuery }) });
                } else if (type === "S3") {
                    const s3Path = document.getElementById("segS3Path").value.trim();
                    if (!s3Path) throw new Error("Please enter an S3 Path");
                    await apiFetch(`${apiSegmentsUrl}/s3-path`, { method: "POST", body: JSON.stringify({ name, description, s3Path }) });
                }

                showToast("Segment created successfully!", "success");
                segmentFormWrapper.hidden = true;
                segmentForm.reset();
                loadSegmentTable();
            } catch (err) {
                showToast("Error: " + err.message, "error");
            } finally {
                document.getElementById("segSaveBtn").disabled    = false;
                document.getElementById("segSaveBtn").textContent = "💾 Save Segment";
            }
        });
    }

    const closeSegmentPreviewBtn = document.getElementById("closeSegmentPreviewModal");
    if (closeSegmentPreviewBtn) {
        closeSegmentPreviewBtn.addEventListener("click", () => {
            document.getElementById("segmentPreviewModal").style.display = "none";
        });
    }
}

export async function loadSegmentTable() {
    const segmentTableBody = document.getElementById("segmentTableBody");
    const segmentTable     = document.getElementById("segmentTable");
    const segmentEmpty     = document.getElementById("segmentEmpty");

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
                    <button class="primary-btn btn-xs" data-preview-seg="${s.id}">Preview</button>
                    <button class="danger-btn" data-delete-seg="${s.id}" data-delete-name="${escHtml(s.name)}">Delete</button>
                </td>`;
            segmentTableBody.appendChild(tr);
        });

        segmentTableBody.querySelectorAll("[data-delete-seg]").forEach(btn => {
            btn.addEventListener("click", () => {
                setDeleteContext({ type: "segment", id: btn.dataset.deleteSeg });
                deleteModalMessage.textContent = `Delete segment "${btn.dataset.deleteName}"?`;
                modal.style.display = "flex";
            });
        });

        segmentTableBody.querySelectorAll("[data-preview-seg]").forEach(btn => {
            btn.addEventListener("click", async () => {
                const segId = btn.dataset.previewSeg;
                try {
                    const users = await apiFetch(`${apiSegmentsUrl}/${segId}/users`);
                    const top5 = users.slice(0, 5);
                    
                    const head = document.getElementById("segmentPreviewHead");
                    const body = document.getElementById("segmentPreviewBody");
                    head.innerHTML = "";
                    body.innerHTML = "";
                    
                    if (top5.length === 0) {
                        head.innerHTML = "<tr><th>No data found</th></tr>";
                    } else {
                        const keys = new Set(["emailId", "userId", "phoneNumber"]);
                        top5.forEach(u => {
                            if (u.data) Object.keys(u.data).forEach(k => keys.add(k));
                        });
                        const cols = Array.from(keys);
                        
                        const trHead = document.createElement("tr");
                        cols.forEach(c => {
                            const th = document.createElement("th");
                            th.textContent = c;
                            trHead.appendChild(th);
                        });
                        head.appendChild(trHead);
                        
                        top5.forEach(u => {
                            const tr = document.createElement("tr");
                            cols.forEach(c => {
                                const td = document.createElement("td");
                                let val = u[c];
                                if (val === undefined && u.data) val = u.data[c];
                                td.textContent = val !== null && val !== undefined ? val : "—";
                                tr.appendChild(td);
                            });
                            body.appendChild(tr);
                        });
                    }
                    document.getElementById("segmentPreviewModal").style.display = "flex";
                } catch (err) {
                    showToast("Failed to fetch segment data", "error");
                }
            });
        });
    } catch (err) { console.error("loadSegmentTable:", err); }
}
