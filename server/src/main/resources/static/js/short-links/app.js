document.addEventListener("DOMContentLoaded", () => {
    const modal = document.getElementById("deleteModal");
    const confirmDeleteBtn = document.getElementById("confirmDelete");
    const cancelDeleteBtn = document.getElementById("cancelDelete");
    const shortenForm = document.getElementById("shortenForm");
    const resultDiv = document.getElementById("shortUrlResult");
    const linksContainer = document.getElementById("linksContainer");
    let linkIdToDelete = null;

    const shortLinkBaseUrl = normalizeTrailingSlash(
        window.SHORT_LINK_BASE_URL || "/s/"
    );
    const apiLinksUrl = (window.API_LINKS_URL || "/api/links").replace(/\/$/, "");

    function normalizeTrailingSlash(url) {
        return url.endsWith("/") ? url : url + "/";
    }

    function shortUrlFor(shortId) {
        return shortLinkBaseUrl + shortId;
    }

    async function loadLinks() {
        try {
            const response = await fetch(apiLinksUrl);
            if (!response.ok) throw new Error("Failed to fetch links");

            const links = await response.json();
            linksContainer.innerHTML = "";

            links.forEach(link => {
                const shortId = link.shortId || link.shortLink.split("/").filter(Boolean).pop();
                const div = document.createElement("div");

                const a = document.createElement("a");
                a.href = shortUrlFor(shortId);
                a.textContent = link.shortLink || shortUrlFor(shortId);

                const deleteSpan = document.createElement("span");
                deleteSpan.className = "delete";
                deleteSpan.setAttribute("data-id", shortId);
                deleteSpan.textContent = "🗑️";

                const dropdownSpan = document.createElement("span");
                dropdownSpan.className = "dropdown";
                dropdownSpan.textContent = "▼";
                dropdownSpan.style.cursor = "pointer";
                dropdownSpan.style.marginLeft = "5px";

                const detailsDiv = document.createElement("div");
                detailsDiv.style.display = "none";
                detailsDiv.style.margin = "2px 0 10px 20px";
                detailsDiv.style.fontSize = "14px";
                detailsDiv.style.color = "#9bb7f4";

                const clickCountP = document.createElement("p");
                clickCountP.textContent = `This link has been clicked ${link.clickCount} times.`;
                clickCountP.style.margin = "2px 0";

                const originalUrlP = document.createElement("p");
                originalUrlP.textContent = `Original URL: ${link.originalUrl}`;
                originalUrlP.style.margin = "2px 0";
                originalUrlP.style.fontSize = "14px";
                originalUrlP.style.color = "#9bb7f4";
                originalUrlP.style.overflow = "hidden";
                originalUrlP.style.textOverflow = "ellipsis";
                originalUrlP.style.whiteSpace = "nowrap";
                originalUrlP.style.maxWidth = "300px";

                const createdAtP = document.createElement("p");
                createdAtP.textContent = `Created at: ${new Date(link.createdAt).toLocaleString()}`;
                createdAtP.style.margin = "2px 0";

                const expiresAtP = document.createElement("p");
                expiresAtP.textContent = `Expires at: ${new Date(link.expiresAt).toLocaleString()}`;
                expiresAtP.style.margin = "2px 0";

                const qrDiv = document.createElement("div");
                qrDiv.style.margin = "5px 0";
                qrDiv.className = "dropdown";

                const qrButton = document.createElement("button");
                qrButton.textContent = "Show QR Code";
                qrButton.style.cursor = "pointer";
                qrButton.style.marginTop = "5px";
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
                detailsDiv.appendChild(expiresAtP);
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
        } catch (err) {
            console.error("Error loading links:", err);
        }
    }

    function attachDeleteEvents() {
        document.querySelectorAll(".delete").forEach(button => {
            button.addEventListener("click", () => {
                linkIdToDelete = button.getAttribute("data-id");
                modal.style.display = "flex";
            });
        });
    }

    confirmDeleteBtn.addEventListener("click", async () => {
        if (!linkIdToDelete) return;
        try {
            const response = await fetch(`${apiLinksUrl}/${linkIdToDelete}`, {
                method: "DELETE",
            });
            if (response.ok) {
                loadLinks();
            } else {
                console.error("Failed to delete link");
            }
        } catch (err) {
            console.error("Error:", err);
        } finally {
            modal.style.display = "none";
            linkIdToDelete = null;
        }
    });

    cancelDeleteBtn.addEventListener("click", () => {
        modal.style.display = "none";
        linkIdToDelete = null;
    });

    window.addEventListener("click", event => {
        if (event.target === modal) {
            modal.style.display = "none";
            linkIdToDelete = null;
        }
    });

    shortenForm.addEventListener("submit", async e => {
        e.preventDefault();

        const originalUrl = document.getElementById("originalUrl").value.trim();
        const ttlInMinutes = document.getElementById("ttlInMinutes").value;

        if (!originalUrl) {
            alert("Please enter a URL");
            return;
        }

        try {
            const params = new URLSearchParams();
            params.append("originalUrl", originalUrl);
            if (ttlInMinutes) params.append("ttlInMinutes", ttlInMinutes);

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
                resultDiv.innerHTML = `<p style="color:red;">Error shortening URL</p>`;
            }
        } catch (err) {
            console.error("Error:", err);
            resultDiv.innerHTML = `<p style="color:red;">Error: ${err.message}</p>`;
        }
    });

    loadLinks();
});
