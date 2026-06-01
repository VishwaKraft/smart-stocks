document.addEventListener("DOMContentLoaded", () => {
    const modal = document.getElementById("deleteModal");
    const deleteModalMessage = document.getElementById("deleteModalMessage");
    const confirmDeleteBtn = document.getElementById("confirmDelete");
    const cancelDeleteBtn = document.getElementById("cancelDelete");
    const shortenForm = document.getElementById("shortenForm");
    const campaignForm = document.getElementById("campaignForm");
    const resultDiv = document.getElementById("shortUrlResult");
    const campaignResultDiv = document.getElementById("campaignResult");
    const linksContainer = document.getElementById("linksContainer");
    const campaignsContainer = document.getElementById("campaignsContainer");
    const shortenerPanel = document.getElementById("shortenerPanel");
    const campaignPanel = document.getElementById("campaignPanel");
    const editorPanel = document.getElementById("editorPanel");
    const sidebarLists = document.getElementById("sidebarLists");
    const editorSidebar = document.getElementById("editorSidebar");
    const sidebarListTitle = document.getElementById("sidebarListTitle");
    const sectionNavButtons = document.querySelectorAll(".section-nav-btn");
    const htmlSource = document.getElementById("htmlSource");
    const htmlPreview = document.getElementById("htmlPreview");
    const editorCampaignSelect = document.getElementById("editorCampaignSelect");
    const editorLinkSelect = document.getElementById("editorLinkSelect");

    const HTML_DRAFT_KEY = "smartstocks-email-html-draft";
    const DEFAULT_EMAIL_TEMPLATE = `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Email</title>
</head>
<body style="margin:0;padding:0;font-family:Arial,sans-serif;background:#f4f4f4;">
  <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:#f4f4f4;padding:24px 0;">
    <tr>
      <td align="center">
        <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:8px;padding:32px;">
          <tr>
            <td>
              <h1 style="margin:0 0 16px;font-size:24px;color:#333;">Hello</h1>
              <p style="margin:0 0 16px;font-size:16px;line-height:1.5;color:#555;">Your email content goes here.</p>
              <p style="margin:0;font-size:16px;line-height:1.5;color:#555;">
                <a href="#" style="color:#8e24aa;">Call to action</a>
              </p>
            </td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
</body>
</html>`;

    let deleteContext = null;
    let cachedCampaigns = [];
    let cachedLinks = [];
    let previewDebounceTimer = null;
    let editorInitialized = false;

    const shortLinkBaseUrl = normalizeTrailingSlash(
        window.SHORT_LINK_BASE_URL || "/s/"
    );
    const apiLinksUrl = (window.API_LINKS_URL || "/api/links").replace(/\/$/, "");
    const apiCampaignsUrl = (window.API_CAMPAIGNS_URL || "/api/campaigns").replace(/\/$/, "");

    function normalizeTrailingSlash(url) {
        return url.endsWith("/") ? url : url + "/";
    }

    function shortUrlFor(shortId) {
        return shortLinkBaseUrl + shortId;
    }

    function switchSection(section) {
        const isShortener = section === "shortener";
        const isCampaigns = section === "campaigns";
        const isEditor = section === "editor";

        sectionNavButtons.forEach(btn => {
            btn.classList.toggle("active", btn.dataset.section === section);
        });

        document.body.classList.toggle("editor-active", isEditor);

        shortenerPanel.hidden = !isShortener;
        campaignPanel.hidden = !isCampaigns;
        editorPanel.hidden = !isEditor;

        sidebarLists.hidden = isEditor;
        editorSidebar.hidden = !isEditor;

        if (!isEditor) {
            htmlPreview.removeAttribute("srcdoc");
        }

        linksContainer.hidden = !isShortener;
        campaignsContainer.hidden = !isCampaigns;

        if (isShortener) {
            sidebarListTitle.textContent = "My shortened URLs";
            loadLinks();
        } else if (isCampaigns) {
            sidebarListTitle.textContent = "My campaigns";
            loadCampaigns();
        } else if (isEditor) {
            initEditor();
        }
    }

    sectionNavButtons.forEach(btn => {
        btn.addEventListener("click", () => switchSection(btn.dataset.section));
    });

    async function loadLinks() {
        try {
            const response = await fetch(apiLinksUrl);
            if (!response.ok) throw new Error("Failed to fetch links");

            const links = await response.json();
            linksContainer.innerHTML = "";

            links.forEach(link => {
                const shortId = link.shortId || link.shortLink.split("/").filter(Boolean).pop();
                const div = document.createElement("div");
                div.className = "sidebar-item";

                const a = document.createElement("a");
                a.href = shortUrlFor(shortId);
                a.textContent = link.shortLink || shortUrlFor(shortId);

                const deleteSpan = document.createElement("span");
                deleteSpan.className = "delete";
                deleteSpan.setAttribute("data-type", "link");
                deleteSpan.setAttribute("data-id", shortId);
                deleteSpan.textContent = "🗑️";

                const dropdownSpan = document.createElement("span");
                dropdownSpan.className = "dropdown";
                dropdownSpan.textContent = "▼";
                dropdownSpan.style.cursor = "pointer";
                dropdownSpan.style.marginLeft = "5px";

                const detailsDiv = document.createElement("div");
                detailsDiv.className = "sidebar-details";
                detailsDiv.style.display = "none";

                const clickCountP = document.createElement("p");
                clickCountP.textContent = `This link has been clicked ${link.clickCount} times.`;

                const originalUrlP = document.createElement("p");
                originalUrlP.textContent = `Original URL: ${link.originalUrl}`;
                originalUrlP.className = "muted truncate";

                const createdAtP = document.createElement("p");
                createdAtP.textContent = `Created at: ${new Date(link.createdAt).toLocaleString()}`;

                const qrDiv = document.createElement("div");
                qrDiv.className = "dropdown";

                const qrButton = document.createElement("button");
                qrButton.textContent = "Show QR Code";
                qrButton.type = "button";
                qrButton.className = "dropdown-toggle";

                const qrImg = document.createElement("img");
                qrImg.style.display = "none";
                qrImg.style.marginTop = "5px";
                qrImg.style.width = "120px";

                qrButton.addEventListener("click", async () => {
                    if (qrImg.style.display === "none") {
                        try {
                            const qrResponse = await fetch(`${apiLinksUrl}/qr/${shortId}`);
                            if (qrResponse.ok) {
                                const blob = await qrResponse.blob();
                                qrImg.src = URL.createObjectURL(blob);
                                qrImg.style.display = "block";
                                qrButton.textContent = "Hide QR Code";
                            }
                        } catch (err) {
                            console.error("Error fetching QR:", err);
                        }
                    } else {
                        qrImg.style.display = "none";
                        qrButton.textContent = "Show QR Code";
                    }
                });

                qrDiv.appendChild(qrButton);
                qrDiv.appendChild(qrImg);

                detailsDiv.appendChild(clickCountP);
                detailsDiv.appendChild(originalUrlP);
                detailsDiv.appendChild(createdAtP);
                detailsDiv.appendChild(qrDiv);

                dropdownSpan.addEventListener("click", () => {
                    detailsDiv.style.display =
                        detailsDiv.style.display === "none" ? "block" : "none";
                });

                div.appendChild(a);
                div.appendChild(deleteSpan);
                div.appendChild(dropdownSpan);
                div.appendChild(detailsDiv);

                linksContainer.appendChild(div);
            });

            attachDeleteEvents();
            if (!editorPanel.hidden) {
                cachedLinks = links;
                populateEditorLinkSelect();
            }
        } catch (err) {
            console.error("Error loading links:", err);
        }
    }

    async function loadCampaigns() {
        try {
            const response = await fetch(apiCampaignsUrl);
            if (!response.ok) throw new Error("Failed to fetch campaigns");

            const campaigns = await response.json();
            campaignsContainer.innerHTML = "";

            campaigns.forEach(campaign => {
                const div = document.createElement("div");
                div.className = "sidebar-item";

                const title = document.createElement("span");
                title.className = "campaign-title";
                title.textContent = campaign.name;

                const deleteSpan = document.createElement("span");
                deleteSpan.className = "delete";
                deleteSpan.setAttribute("data-type", "campaign");
                deleteSpan.setAttribute("data-id", campaign.id);
                deleteSpan.textContent = "🗑️";

                const dropdownSpan = document.createElement("span");
                dropdownSpan.className = "dropdown";
                dropdownSpan.textContent = "▼";
                dropdownSpan.style.cursor = "pointer";
                dropdownSpan.style.marginLeft = "5px";

                const detailsDiv = document.createElement("div");
                detailsDiv.className = "sidebar-details";
                detailsDiv.style.display = "none";

                const codeP = document.createElement("p");
                codeP.textContent = `Code: ${campaign.campaignCode}`;

                const opensP = document.createElement("p");
                opensP.textContent = `Email opens: ${campaign.openCount}`;

                const createdAtP = document.createElement("p");
                createdAtP.textContent = `Created at: ${new Date(campaign.createdAt).toLocaleString()}`;

                const pixelBlock = buildCopyableUrlBlock(
                    "Tracking pixel URL",
                    campaign.trackingPixelUrl
                );

                const htmlBlock = buildCopyableUrlBlock(
                    "Email HTML",
                    buildEmailImgTag(campaign.trackingPixelUrl)
                );

                detailsDiv.appendChild(codeP);
                detailsDiv.appendChild(opensP);
                if (campaign.description) {
                    const descP = document.createElement("p");
                    descP.textContent = campaign.description;
                    descP.className = "muted";
                    detailsDiv.appendChild(descP);
                }
                detailsDiv.appendChild(createdAtP);
                detailsDiv.appendChild(pixelBlock);
                detailsDiv.appendChild(htmlBlock);

                dropdownSpan.addEventListener("click", () => {
                    detailsDiv.style.display =
                        detailsDiv.style.display === "none" ? "block" : "none";
                });

                div.appendChild(title);
                div.appendChild(deleteSpan);
                div.appendChild(dropdownSpan);
                div.appendChild(detailsDiv);

                campaignsContainer.appendChild(div);
            });

            attachDeleteEvents();
            if (!editorPanel.hidden) {
                cachedCampaigns = campaigns;
                populateEditorCampaignSelect();
            }
        } catch (err) {
            console.error("Error loading campaigns:", err);
        }
    }

    function buildEmailImgTag(pixelUrl) {
        return `<img src="${pixelUrl}" width="1" height="1" alt="" style="display:none;" />`;
    }

    function initEditor() {
        if (!editorInitialized) {
            const savedDraft = localStorage.getItem(HTML_DRAFT_KEY);
            htmlSource.value = savedDraft || DEFAULT_EMAIL_TEMPLATE;

            htmlSource.addEventListener("input", () => {
                persistHtmlDraft();
                schedulePreviewUpdate();
            });

            document.getElementById("editorFormatBtn").addEventListener("click", formatHtmlSource);
            document.getElementById("editorCopyBtn").addEventListener("click", copyEditorHtml);
            document.getElementById("editorDownloadBtn").addEventListener("click", downloadEditorHtml);
            document.getElementById("editorClearBtn").addEventListener("click", resetEditorTemplate);
            document.getElementById("insertPixelBtn").addEventListener("click", insertSelectedPixel);
            document.getElementById("insertLinkBtn").addEventListener("click", insertSelectedLink);

            editorInitialized = true;
        }

        updatePreview();
        loadEditorResources();
    }

    function persistHtmlDraft() {
        localStorage.setItem(HTML_DRAFT_KEY, htmlSource.value);
    }

    function schedulePreviewUpdate() {
        clearTimeout(previewDebounceTimer);
        previewDebounceTimer = setTimeout(updatePreview, 250);
    }

    function updatePreview() {
        htmlPreview.srcdoc = htmlSource.value;
    }

    async function loadEditorResources() {
        try {
            const [campaignsRes, linksRes] = await Promise.all([
                fetch(apiCampaignsUrl),
                fetch(apiLinksUrl),
            ]);

            if (campaignsRes.ok) {
                cachedCampaigns = await campaignsRes.json();
                populateEditorCampaignSelect();
            }
            if (linksRes.ok) {
                cachedLinks = await linksRes.json();
                populateEditorLinkSelect();
            }
        } catch (err) {
            console.error("Error loading editor resources:", err);
        }
    }

    function populateEditorCampaignSelect() {
        editorCampaignSelect.innerHTML = '<option value="">Select a campaign…</option>';
        cachedCampaigns.forEach(campaign => {
            const option = document.createElement("option");
            option.value = campaign.id;
            option.textContent = campaign.name;
            option.dataset.pixelUrl = campaign.trackingPixelUrl;
            editorCampaignSelect.appendChild(option);
        });
    }

    function populateEditorLinkSelect() {
        editorLinkSelect.innerHTML = '<option value="">Select a short link…</option>';
        cachedLinks.forEach(link => {
            const shortId = link.shortId || link.shortLink.split("/").filter(Boolean).pop();
            const shortUrl = link.shortLink || shortUrlFor(shortId);
            const option = document.createElement("option");
            option.value = shortId;
            option.textContent = shortUrl;
            option.dataset.shortUrl = shortUrl;
            option.dataset.originalUrl = link.originalUrl;
            editorLinkSelect.appendChild(option);
        });
    }

    function insertAtCursor(textarea, text) {
        const start = textarea.selectionStart;
        const end = textarea.selectionEnd;
        const before = textarea.value.substring(0, start);
        const after = textarea.value.substring(end);
        textarea.value = before + text + after;
        const cursor = start + text.length;
        textarea.selectionStart = cursor;
        textarea.selectionEnd = cursor;
        textarea.focus();
        persistHtmlDraft();
        schedulePreviewUpdate();
    }

    function insertBeforeClosingBody(html) {
        const closingBody = htmlSource.value.lastIndexOf("</body>");
        if (closingBody !== -1) {
            const before = htmlSource.value.substring(0, closingBody);
            const after = htmlSource.value.substring(closingBody);
            htmlSource.value = before + html + "\n" + after;
        } else {
            htmlSource.value += "\n" + html;
        }
        persistHtmlDraft();
        schedulePreviewUpdate();
        htmlSource.focus();
    }

    function insertSelectedPixel() {
        const selected = editorCampaignSelect.selectedOptions[0];
        if (!selected || !selected.dataset.pixelUrl) {
            alert("Select a campaign first");
            return;
        }
        insertBeforeClosingBody(buildEmailImgTag(selected.dataset.pixelUrl));
    }

    function insertSelectedLink() {
        const selected = editorLinkSelect.selectedOptions[0];
        if (!selected || !selected.dataset.shortUrl) {
            alert("Select a short link first");
            return;
        }
        const label = selected.dataset.originalUrl || "Link";
        const anchor = `<a href="${selected.dataset.shortUrl}" style="color:#8e24aa;">${label}</a>`;
        insertAtCursor(htmlSource, anchor);
    }

    function formatHtmlSource() {
        htmlSource.value = formatHtml(htmlSource.value);
        persistHtmlDraft();
        schedulePreviewUpdate();
    }

    function formatHtml(html) {
        const lines = html.replace(/>\s+</g, "><").split(/>\s*</);
        if (lines.length <= 1) {
            return html.trim();
        }

        let formatted = "";
        let indent = 0;
        const voidTags = /^(area|base|br|col|embed|hr|img|input|link|meta|source|track|!DOCTYPE)/i;

        lines.forEach((chunk, index) => {
            let line = chunk;
            if (index > 0) line = "<" + line;
            if (index < lines.length - 1) line += ">";

            const isClosing = /^<\//.test(line.trim());
            const isOpening = /^<[^/!][^>]*[^/]>$/.test(line.trim()) && !voidTags.test(line.trim());
            const isSelfClosing = /\/>$/.test(line.trim()) || voidTags.test(line.trim());

            if (isClosing) indent = Math.max(indent - 1, 0);
            formatted += "  ".repeat(indent) + line.trim() + "\n";
            if (isOpening && !isSelfClosing) indent += 1;
        });

        return formatted.trim();
    }

    async function copyEditorHtml() {
        const btn = document.getElementById("editorCopyBtn");
        await copyToClipboard(htmlSource.value, btn);
    }

    function downloadEditorHtml() {
        const blob = new Blob([htmlSource.value], { type: "text/html;charset=utf-8" });
        const url = URL.createObjectURL(blob);
        const anchor = document.createElement("a");
        anchor.href = url;
        anchor.download = "email.html";
        anchor.click();
        URL.revokeObjectURL(url);
    }

    function resetEditorTemplate() {
        if (!confirm("Reset the editor to the default template? Your current draft will be replaced.")) {
            return;
        }
        htmlSource.value = DEFAULT_EMAIL_TEMPLATE;
        persistHtmlDraft();
        schedulePreviewUpdate();
    }

    function buildCopyableUrlBlock(label, value) {
        const wrapper = document.createElement("div");
        wrapper.className = "copy-block";

        const labelEl = document.createElement("label");
        labelEl.textContent = label;

        const row = document.createElement("div");
        row.className = "copy-row";

        const input = document.createElement("input");
        input.type = "text";
        input.readOnly = true;
        input.value = value;

        const copyBtn = document.createElement("button");
        copyBtn.type = "button";
        copyBtn.textContent = "Copy";
        copyBtn.addEventListener("click", () => copyToClipboard(value, copyBtn));

        row.appendChild(input);
        row.appendChild(copyBtn);
        wrapper.appendChild(labelEl);
        wrapper.appendChild(row);
        return wrapper;
    }

    function renderCampaignResult(campaign) {
        campaignResultDiv.innerHTML = "";
        campaignResultDiv.className = "campaign-result";

        const heading = document.createElement("h3");
        heading.textContent = `Campaign "${campaign.name}" created`;

        const codeP = document.createElement("p");
        codeP.textContent = `Campaign code: ${campaign.campaignCode}`;

        campaignResultDiv.appendChild(heading);
        campaignResultDiv.appendChild(codeP);
        campaignResultDiv.appendChild(
            buildCopyableUrlBlock("Tracking pixel URL", campaign.trackingPixelUrl)
        );
        campaignResultDiv.appendChild(
            buildCopyableUrlBlock("Email HTML snippet", buildEmailImgTag(campaign.trackingPixelUrl))
        );
    }

    async function copyToClipboard(text, button) {
        try {
            await navigator.clipboard.writeText(text);
            const original = button.textContent;
            button.textContent = "Copied!";
            setTimeout(() => {
                button.textContent = original;
            }, 1500);
        } catch (err) {
            console.error("Copy failed:", err);
            alert("Could not copy to clipboard");
        }
    }

    function attachDeleteEvents() {
        document.querySelectorAll(".delete").forEach(button => {
            button.onclick = () => {
                deleteContext = {
                    type: button.getAttribute("data-type"),
                    id: button.getAttribute("data-id"),
                };
                deleteModalMessage.textContent =
                    deleteContext.type === "campaign"
                        ? "Are you sure you want to delete this campaign?"
                        : "Are you sure you want to delete this link?";
                modal.style.display = "flex";
            };
        });
    }

    confirmDeleteBtn.addEventListener("click", async () => {
        if (!deleteContext) return;

        const url =
            deleteContext.type === "campaign"
                ? `${apiCampaignsUrl}/${deleteContext.id}`
                : `${apiLinksUrl}/${deleteContext.id}`;

        try {
            const response = await fetch(url, { method: "DELETE" });
            if (response.ok) {
                if (deleteContext.type === "campaign") {
                    await loadCampaigns();
                } else {
                    await loadLinks();
                }
            } else {
                console.error("Failed to delete item");
            }
        } catch (err) {
            console.error("Error:", err);
        } finally {
            modal.style.display = "none";
            deleteContext = null;
        }
    });

    cancelDeleteBtn.addEventListener("click", () => {
        modal.style.display = "none";
        deleteContext = null;
    });

    window.addEventListener("click", event => {
        if (event.target === modal) {
            modal.style.display = "none";
            deleteContext = null;
        }
    });

    shortenForm.addEventListener("submit", async e => {
        e.preventDefault();

        const originalUrl = document.getElementById("originalUrl").value.trim();
        if (!originalUrl) {
            alert("Please enter a URL");
            return;
        }

        try {
            const params = new URLSearchParams();
            params.append("originalUrl", originalUrl);

            const response = await fetch(`${apiLinksUrl}/shorten`, {
                method: "POST",
                headers: { "Content-Type": "application/x-www-form-urlencoded" },
                body: params.toString(),
            });

            if (response.ok) {
                const shortUrl = await response.text();
                const shortId = shortUrl.split("/").filter(Boolean).pop();
                resultDiv.innerHTML = `<p>Shortened URL: <a href="${shortUrlFor(shortId)}" target="_blank">${shortUrlFor(shortId)}</a></p>`;
                shortenForm.reset();
                await loadLinks();
            } else {
                resultDiv.innerHTML = `<p class="error">Error shortening URL</p>`;
            }
        } catch (err) {
            console.error("Error:", err);
            resultDiv.innerHTML = `<p class="error">Error: ${err.message}</p>`;
        }
    });

    campaignForm.addEventListener("submit", async e => {
        e.preventDefault();

        const name = document.getElementById("campaignName").value.trim();
        const campaignCode = document.getElementById("campaignCode").value.trim();
        const description = document.getElementById("campaignDescription").value.trim();

        if (!name) {
            alert("Please enter a campaign name");
            return;
        }

        const payload = { name };
        if (campaignCode) payload.campaignCode = campaignCode;
        if (description) payload.description = description;

        try {
            const response = await fetch(apiCampaignsUrl, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(payload),
            });

            if (response.ok) {
                const campaign = await response.json();
                renderCampaignResult(campaign);
                campaignForm.reset();
                await loadCampaigns();
            } else {
                const message = await response.text();
                campaignResultDiv.innerHTML = `<p class="error">${message || "Error creating campaign"}</p>`;
            }
        } catch (err) {
            console.error("Error:", err);
            campaignResultDiv.innerHTML = `<p class="error">Error: ${err.message}</p>`;
        }
    });

    switchSection("shortener");
});
