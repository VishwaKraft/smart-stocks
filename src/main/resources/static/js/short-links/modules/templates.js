/* ============================================================
   modules/templates.js — Email template management + AI editor
   ============================================================ */

import { apiTemplatesUrl, apiFetch } from '../api.js';
import { fmtDate, escHtml, formatHtml } from '../utils.js';
import { modal, deleteModalMessage, showToast } from '../ui.js';
import { state, setDeleteContext } from '../state.js';

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

        <div style="font-size:22px;color:#e75480;margin-top:35px">
          <img data-emoji="❤️" class="an1" alt="❤️" aria-label="❤️" draggable="false" src="https://fonts.gstatic.com/s/e/notoemoji/17.0/2764_fe0f/72.png" loading="lazy">
        </div>

      </td>
    </tr>
  </tbody></table>

</td></tr>
</tbody></table>`;

export function initTemplates() {
    const templateFormWrapper = document.getElementById("templateFormWrapper");
    const templateForm        = document.getElementById("templateForm");
    const templateEditId      = document.getElementById("templateEditId");
    const templateFormTitle   = document.getElementById("templateFormTitle");
    const tplHtmlBody         = document.getElementById("tplHtmlBody");
    const tplHtmlPreview      = document.getElementById("tplHtmlPreview");
    let   tplPreviewTimer     = null;

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

    // New template button
    const newTemplateBtn = document.getElementById("newTemplateBtn");
    if (newTemplateBtn) {
        newTemplateBtn.addEventListener("click", () => {
            templateEditId.value = "";
            templateFormTitle.textContent = "New Template";
            templateForm.reset();
            tplHtmlBody.value = DEFAULT_EMAIL_TEMPLATE;
            document.getElementById("chatHistory").innerHTML = "";
            templateFormWrapper.hidden = false;
            scheduleTplPreview();
            templateFormWrapper.scrollIntoView({ behavior: "smooth" });
        });
    }

    document.getElementById("tplCancelBtn").addEventListener("click", () => {
        templateFormWrapper.hidden = true;
        templateForm.reset();
    });

    document.getElementById("tplFormatBtn").addEventListener("click", () => {
        tplHtmlBody.value = formatHtml(tplHtmlBody.value);
        scheduleTplPreview();
    });

    const sourceCodeModal  = document.getElementById("sourceCodeModal");
    const tplPreviewToggle = document.getElementById("tplPreviewToggle");
    if (tplPreviewToggle) {
        tplPreviewToggle.addEventListener("click", () => { sourceCodeModal.style.display = "flex"; });
    }

    document.getElementById("closeSourceCodeModal").addEventListener("click", () => {
        sourceCodeModal.style.display = "none";
        scheduleTplPreview();
    });

    // AI Chatbot
    const aiSubmitBtn      = document.getElementById("aiSubmitBtn");
    const aiPromptInput    = document.getElementById("aiPromptInput");
    const aiLoadingSpinner = document.getElementById("aiLoadingSpinner");
    const chatHistory      = document.getElementById("chatHistory");

    function appendChatMessage(text, sender) {
        const msgDiv = document.createElement("div");
        msgDiv.className = `chat-message ${sender}`;
        msgDiv.textContent = text;
        chatHistory.appendChild(msgDiv);
        chatHistory.scrollTop = chatHistory.scrollHeight;
    }

    if (aiSubmitBtn) {
        aiSubmitBtn.addEventListener("click", async () => {
            const prompt = aiPromptInput.value.trim();
            if (!prompt) return;

            appendChatMessage(prompt, "user");
            aiPromptInput.value = "";
            aiSubmitBtn.disabled = true;
            aiLoadingSpinner.hidden = false;

            try {
                const res = await fetch(`${apiTemplatesUrl}/chat/stream`, {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ prompt, currentHtml: tplHtmlBody.value })
                });

                if (!res.ok) {
                    const msg = await res.text();
                    throw new Error(msg || "Failed to get AI response");
                }

                tplHtmlBody.value = "";
                const reader  = res.body.getReader();
                const decoder = new TextDecoder("utf-8");
                let buffer = "";
                while (true) {
                    const { value, done } = await reader.read();
                    if (done) break;
                    buffer += decoder.decode(value, { stream: true });
                    const lines = buffer.split('\n');
                    buffer = lines.pop() || "";
                    for (const line of lines) {
                        if (line.startsWith('data:')) {
                            const dataStr = line.substring(5).trim();
                            if (dataStr) {
                                try {
                                    const parsed = JSON.parse(dataStr);
                                    if (parsed.text) { tplHtmlBody.value += parsed.text; scheduleTplPreview(); }
                                } catch (e) { console.error("Error parsing stream chunk:", e, dataStr); }
                            }
                        }
                    }
                }
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
    }

    if (tplHtmlBody) tplHtmlBody.addEventListener("input", scheduleTplPreview);

    // Copy/Download buttons in source code modal
    const tplCopyBtn     = document.getElementById("tplCopyBtn");
    const tplDownloadBtn = document.getElementById("tplDownloadBtn");

    if (tplCopyBtn) {
        tplCopyBtn.addEventListener("click", () => {
            navigator.clipboard.writeText(tplHtmlBody.value)
                .then(() => showToast("HTML copied to clipboard!", "success"))
                .catch(() => showToast("Failed to copy HTML", "error"));
        });
    }

    if (tplDownloadBtn) {
        tplDownloadBtn.addEventListener("click", () => {
            const blob = new Blob([tplHtmlBody.value], { type: "text/html" });
            const url  = URL.createObjectURL(blob);
            const a    = document.createElement("a");
            a.href = url; a.download = "template.html"; a.click();
            URL.revokeObjectURL(url);
        });
    }

    // Form submit (create / update)
    if (templateForm) {
        templateForm.addEventListener("submit", async e => {
            e.preventDefault();
            const id   = templateEditId.value;
            const body = {
                name:          document.getElementById("tplName").value.trim(),
                subject:       document.getElementById("tplSubject").value.trim(),
                htmlBody:      tplHtmlBody.value,
                dataSourceUrl: document.getElementById("tplDataSourceUrl").value.trim() || null
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
    }

    // Expose scheduleTplPreview via window (used by template table for edit)
    window._scheduleTplPreview = scheduleTplPreview;
}

export async function loadTemplateTable() {
    const templateTableBody = document.getElementById("templateTableBody");
    const templateTable     = document.getElementById("templateTable");
    const templateEmpty     = document.getElementById("templateEmpty");
    const templateFormWrapper = document.getElementById("templateFormWrapper");
    const templateFormTitle   = document.getElementById("templateFormTitle");
    const templateEditId      = document.getElementById("templateEditId");
    const tplHtmlBody         = document.getElementById("tplHtmlBody");

    try {
        const templates = await apiFetch(`${apiTemplatesUrl}?includeInactive=false`);
        state.cachedTemplates = templates;
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
                const tpl = state.cachedTemplates.find(t => t.id == btn.dataset.editTpl);
                if (!tpl) return;
                templateEditId.value = tpl.id;
                templateFormTitle.textContent = "Edit Template";
                document.getElementById("tplName").value          = tpl.name;
                document.getElementById("tplSubject").value       = tpl.subject;
                document.getElementById("tplDataSourceUrl").value = tpl.dataSourceUrl || "";
                tplHtmlBody.value = tpl.htmlBody;
                document.getElementById("chatHistory").innerHTML  = "";
                templateFormWrapper.hidden = false;
                if (window._scheduleTplPreview) window._scheduleTplPreview();
                templateFormWrapper.scrollIntoView({ behavior: "smooth" });
            });
        });

        // Delete
        templateTableBody.querySelectorAll("[data-delete-tpl]").forEach(btn => {
            btn.addEventListener("click", () => {
                setDeleteContext({ type: "template", id: btn.dataset.deleteTpl });
                deleteModalMessage.textContent = `Delete template "${btn.dataset.deleteName}"?`;
                modal.style.display = "flex";
            });
        });
    } catch (err) {
        console.error("loadTemplateTable:", err);
    }
}
