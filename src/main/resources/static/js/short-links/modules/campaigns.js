/* ============================================================
   modules/campaigns.js — Campaign management
   ============================================================ */

import { apiCampaignsUrl, apiFetch } from '../api.js';
import { fmtDate, escHtml, copyToClipboard, buildCopyRow } from '../utils.js';
import { modal, deleteModalMessage, showToast } from '../ui.js';
import { state, setDeleteContext } from '../state.js';

export async function refreshCampaignCache() {
    try {
        state.cachedCampaigns = await apiFetch(apiCampaignsUrl);
    } catch (err) {
        console.error("refreshCampaignCache:", err);
    }
}

function renderCampaignResult(campaign) {
    const div = document.getElementById("campaignResult");
    div.innerHTML = "";
    div.className = "campaign-result";
    const h = document.createElement("h3");
    h.textContent = `Campaign "${campaign.name}" created`;
    div.appendChild(h);
    if (campaign.trackingPixelUrl) div.appendChild(buildCopyRow("Tracking pixel URL", campaign.trackingPixelUrl));
}

export function initCampaigns() {
    // Toggle provider fields based on campaign type radio
    document.querySelectorAll('input[name="campaignType"]').forEach(radio => {
        radio.addEventListener('change', (e) => {
            const isWhatsApp = e.target.value === 'WHATSAPP';
            const isVoice    = e.target.value === 'VOICE';
            document.getElementById('emailProviderGroup').hidden    = isWhatsApp || isVoice;
            document.getElementById('whatsappSenderGroup').hidden   = !isWhatsApp;
            const voiceGrp = document.getElementById('voiceSenderGroup');
            if (voiceGrp) voiceGrp.hidden = !isVoice;
        });
    });

    // Campaign type card visual selector
    document.querySelectorAll('.campaign-type-card').forEach(card => {
        card.addEventListener('click', () => {
            document.querySelectorAll('.campaign-type-card').forEach(c => c.classList.remove('selected'));
            card.classList.add('selected');
            const radio = card.querySelector('input[type="radio"]');
            if (radio) { radio.checked = true; radio.dispatchEvent(new Event('change', { bubbles: true })); }
        });
    });

    // Campaign form submit
    const campaignForm = document.getElementById("campaignForm");
    if (campaignForm) {
        campaignForm.addEventListener("submit", async e => {
            e.preventDefault();
            const campaignType = document.querySelector('input[name="campaignType"]:checked').value;
            const payload = {
                name:                 document.getElementById("campaignName").value.trim(),
                campaignCode:         document.getElementById("campaignCode").value.trim() || null,
                description:          document.getElementById("campaignDescription").value.trim() || null,
                campaignType,
                emailProviderType:    campaignType === 'EMAIL'    ? (document.getElementById("campaignEmailProvider").value  || null) : null,
                whatsappSenderNumber: campaignType === 'WHATSAPP' ? (document.getElementById("campaignWhatsappSender").value || null) : null,
                infobipSenderNumber:  campaignType === 'VOICE'    ? (document.getElementById("campaignVoiceSender").value    || null) : null
            };
            if (!payload.name) return;
            try {
                const campaign = await apiFetch(apiCampaignsUrl, { method: "POST", body: JSON.stringify(payload) });
                renderCampaignResult(campaign);
                e.target.reset();
                document.querySelectorAll('.campaign-type-card').forEach(c => c.classList.remove('selected'));
                const emailCard = document.querySelector('.campaign-type-card[data-value="EMAIL"]');
                if (emailCard) emailCard.classList.add('selected');
                document.getElementById('emailProviderGroup').hidden  = false;
                document.getElementById('whatsappSenderGroup').hidden = true;
                const vg = document.getElementById('voiceSenderGroup');
                if (vg) vg.hidden = true;
                await refreshCampaignCache();
                await loadCampaignTable();
                showToast("Campaign created!", "success");
            } catch (err) {
                document.getElementById("campaignResult").innerHTML = `<p class="error">${err.message}</p>`;
            }
        });
    }

    // Meta OAuth button
    const metaOAuthBtn = document.getElementById("metaOAuthBtn");
    if (metaOAuthBtn) {
        metaOAuthBtn.addEventListener("click", () => {
            const campaignId = document.getElementById("metaTokenCampaignId").value;
            const clientId   = window.META_CLIENT_ID;
            const redirectUri = window.location.origin + window.location.pathname;
            const authUrl = `https://www.facebook.com/v25.0/dialog/oauth?client_id=${clientId}&redirect_uri=${encodeURIComponent(redirectUri)}&response_type=code&scope=whatsapp_business_management,whatsapp_business_messaging,public_profile&state=meta_${campaignId}`;
            window.location.href = authUrl;
        });
    }

    // Meta token modal
    const metaTokenModal   = document.getElementById("metaTokenModal");
    const cancelMetaToken  = document.getElementById("cancelMetaToken");
    const confirmMetaToken = document.getElementById("confirmMetaToken");

    if (cancelMetaToken)  cancelMetaToken.addEventListener("click",  () => { metaTokenModal.style.display = "none"; });
    if (confirmMetaToken) confirmMetaToken.addEventListener("click", async () => {
        const campaignId    = document.getElementById("metaTokenCampaignId").value;
        const accessToken   = document.getElementById("metaAccessTokenInput").value.trim();
        const phoneNumberId = document.getElementById("metaPhoneNumberIdInput").value.trim();
        const wabaId        = document.getElementById("metaWabaIdInput").value.trim();
        if (!accessToken)   { showToast("Access token is required",   "error"); return; }
        try {
            await apiFetch(`${apiCampaignsUrl}/${campaignId}/meta-token`, {
                method: "POST",
                body: JSON.stringify({ access_token: accessToken, phone_number_id: phoneNumberId, waba_id: wabaId })
            });
            showToast("Meta token saved successfully!", "success");
            metaTokenModal.style.display = "none";
            loadCampaignTable();
        } catch (err) {
            showToast("Failed to save Meta token: " + err.message, "error");
        }
    });
}

export async function loadCampaignTable() {
    const tbody = document.getElementById("campaignTableBody");
    const table = document.getElementById("campaignTable");
    const empty = document.getElementById("campaignEmpty");
    const metaTokenModal = document.getElementById("metaTokenModal");
    if (!tbody || !table || !empty) return;
    try {
        const campaigns = await apiFetch(apiCampaignsUrl);
        state.cachedCampaigns = campaigns;
        tbody.innerHTML = "";
        if (campaigns.length === 0) {
            table.hidden = true;
            empty.hidden = false;
            return;
        }
        empty.hidden = true;
        table.hidden = false;
        campaigns.forEach(c => {
            let providerSenderHtml = '<span class="muted">—</span>';
            if (c.campaignType === 'WHATSAPP' && c.whatsappSenderNumber) {
                providerSenderHtml = `<span class="badge badge-default">Sender: ${escHtml(c.whatsappSenderNumber)}</span>`;
            } else if (c.campaignType === 'VOICE' && c.infobipSenderNumber) {
                providerSenderHtml = `<span class="badge badge-default">Sender: ${escHtml(c.infobipSenderNumber)}</span>`;
            } else if (c.emailProviderType) {
                providerSenderHtml = `<span class="badge badge-default">${escHtml(c.emailProviderType)}</span>`;
            }
            const typeMap   = { EMAIL: 'badge-email', WHATSAPP: 'badge-whatsapp', VOICE: 'badge-voice' };
            const typeCls   = typeMap[c.campaignType] || 'badge-default';
            const typeLabel = { EMAIL: '✉️ Email', WHATSAPP: '💬 WhatsApp', VOICE: '🎙️ Voice' }[c.campaignType] || (c.campaignType || 'EMAIL');
            const typeBadge = `<span class="badge ${typeCls}">${typeLabel}</span>`;

            const tr = document.createElement("tr");
            tr.innerHTML = `
                <td><strong>${escHtml(c.name)}</strong></td>
                <td>${typeBadge}</td>
                <td class="truncate" style="max-width:180px">${escHtml(c.description || "—")}</td>
                <td>${providerSenderHtml}</td>
                <td>${fmtDate(c.createdAt)}</td>
                <td class="table-actions">
                    ${c.emailProviderType === 'GMAIL' ? `<button class="primary-btn btn-xs" data-auth-gmail="${c.id}">Sign in with Gmail</button>` : ''}
                    ${c.campaignType === 'WHATSAPP' ? `<button class="primary-btn btn-xs" style="background:linear-gradient(135deg,#25d366,#128c7e);" data-auth-meta="${c.id}">🔑 Sign in with Meta</button>` : ''}
                    ${c.trackingPixelUrl && c.campaignType === 'EMAIL' ? `<button class="secondary-btn btn-xs" data-copy-pixel="${escHtml(c.trackingPixelUrl)}">Copy Pixel</button>` : ''}
                    <button class="danger-btn" data-delete-campaign="${c.id}" data-delete-name="${escHtml(c.name)}">Delete</button>
                </td>`;
            tbody.appendChild(tr);
        });

        // Gmail Auth
        tbody.querySelectorAll("[data-auth-gmail]").forEach(btn => {
            btn.addEventListener("click", () => {
                const campaignId  = btn.dataset.authGmail;
                const clientId    = window.GOOGLE_CLIENT_ID || "YOUR_GOOGLE_CLIENT_ID";
                const redirectUri = window.location.origin + window.location.pathname;
                const authUrl = `https://accounts.google.com/o/oauth2/v2/auth?client_id=${clientId}&redirect_uri=${redirectUri}&response_type=code&scope=https://www.googleapis.com/auth/gmail.send&access_type=offline&prompt=consent&state=${campaignId}`;
                window.location.href = authUrl;
            });
        });

        // Sign in with Meta (WhatsApp)
        tbody.querySelectorAll("[data-auth-meta]").forEach(btn => {
            btn.addEventListener("click", () => {
                const campaignId = btn.dataset.authMeta;
                document.getElementById("metaTokenCampaignId").value   = campaignId;
                document.getElementById("metaAccessTokenInput").value   = "";
                document.getElementById("metaPhoneNumberIdInput").value = "";
                document.getElementById("metaWabaIdInput").value        = "";
                const oauthSection = document.getElementById("metaOAuthSection");
                if (oauthSection) oauthSection.hidden = !window.META_CLIENT_ID;
                metaTokenModal.style.display = "flex";
            });
        });

        // Copy pixel
        tbody.querySelectorAll("[data-copy-pixel]").forEach(btn => {
            btn.addEventListener("click", () => copyToClipboard(btn.dataset.copyPixel, btn));
        });

        // Delete
        tbody.querySelectorAll("[data-delete-campaign]").forEach(btn => {
            btn.addEventListener("click", () => {
                setDeleteContext({ type: "campaign", id: btn.dataset.deleteCampaign });
                deleteModalMessage.textContent = `Delete campaign "${btn.dataset.deleteName}"?`;
                modal.style.display = "flex";
            });
        });
    } catch (err) { console.error("loadCampaignTable:", err); }
}
