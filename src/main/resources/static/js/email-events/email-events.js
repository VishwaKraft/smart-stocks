/* ============================================================
   Email Events — Vanilla JS
   /js/email-events/email-events.js
   ============================================================ */

(function () {
  'use strict';

  /* ── State ─────────────────────────────────────────────────── */
  let eeEvents       = [];
  let eeTriggerEvent = null;   // the event currently being triggered
  let eeFormOpen     = false;

  /* ── Helpers ───────────────────────────────────────────────── */
  function eeApiBase()  { return window.API_EMAIL_EVENTS_URL || '/api/email-events'; }
  function eeTriggerUrl(name) { return `${location.origin}${eeApiBase()}/trigger/${name}`; }

  function eeSlugify(str) {
    return str.trim().toLowerCase()
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/^-+|-+$/g, '');
  }

  function eeShowToast(msg, type) {
    // reuse the existing page toast if available
    if (typeof showToast === 'function') { showToast(msg, type); return; }
    const t = document.getElementById('toast');
    if (!t) return;
    t.textContent = msg;
    t.className   = 'toast ' + (type === 'error' ? 'toast-error' : type === 'success' ? 'toast-success' : '');
    t.style.display = 'block';
    setTimeout(() => { t.style.display = 'none'; }, 3500);
  }

  function eeFmt(n) { return typeof n === 'number' ? n.toLocaleString() : '0'; }

  function eeSuccessRate(ev) {
    if (!ev.triggerCount) return '—';
    return Math.round((ev.successCount / ev.triggerCount) * 100) + '%';
  }

  /* ── Load Events ───────────────────────────────────────────── */
  window.eeLoadEvents = function () {
    const includeInactive = document.getElementById('eeShowInactive')?.checked || false;
    const url = `${eeApiBase()}?includeInactive=${includeInactive}`;

    document.getElementById('eeLoading').style.display = 'block';
    document.getElementById('eeEmpty').style.display   = 'none';
    document.getElementById('eeGrid').innerHTML        = '';

    fetch(url)
      .then(r => r.json())
      .then(data => {
        eeEvents = data;
        document.getElementById('eeLoading').style.display = 'none';
        document.getElementById('eeCount').textContent     = data.length;

        if (!data.length) {
          document.getElementById('eeEmpty').style.display = 'block';
          return;
        }
        eeRenderGrid(data);
      })
      .catch(() => {
        document.getElementById('eeLoading').style.display = 'none';
        eeShowToast('Failed to load email events', 'error');
      });
  };

  function eeRenderGrid(events) {
    const grid = document.getElementById('eeGrid');
    grid.innerHTML = events.map(ev => eeCardHtml(ev)).join('');
  }

  function eeCardHtml(ev) {
    const inactive  = !ev.isActive;
    const triggerUrl = eeTriggerUrl(ev.eventName);
    const provider  = ev.emailProviderType
      ? `<span class="ee-badge ee-badge--provider">${ev.emailProviderType}</span>` : '';

    return `
      <div class="ee-event-card${inactive ? ' ee-event-card--inactive' : ''}" id="eeCard-${ev.id}">
        <div>
          <div class="ee-event-title-row">
            <span class="ee-status-dot${ev.isActive ? ' ee-status-dot--active' : ''}"></span>
            <h4 class="ee-event-title">${htmlEsc(ev.displayName)}</h4>
          </div>
          <div class="ee-event-slug">${htmlEsc(ev.eventName)}</div>
        </div>

        <div class="ee-badges">
          <span class="ee-badge ee-badge--campaign">📣 ${htmlEsc(ev.campaignName)}</span>
          <span class="ee-badge ee-badge--template">📄 ${htmlEsc(ev.templateName)}</span>
          ${provider}
        </div>

        ${ev.description ? `<p style="font-size:12px;color:#64748b;margin:0;line-height:1.5;">${htmlEsc(ev.description)}</p>` : ''}

        <div class="ee-stats-row">
          <div class="ee-stat-cell">
            <span class="ee-stat-val">${eeFmt(ev.triggerCount)}</span>
            <span class="ee-stat-lbl">Triggers</span>
          </div>
          <div class="ee-stat-cell">
            <span class="ee-stat-val ee-stat-val--ok">${eeFmt(ev.successCount)}</span>
            <span class="ee-stat-lbl">Success</span>
          </div>
          <div class="ee-stat-cell">
            <span class="ee-stat-val">${eeSuccessRate(ev)}</span>
            <span class="ee-stat-lbl">Rate</span>
          </div>
        </div>

        <div class="ee-api-row">
          <span class="ee-method-badge">POST</span>
          <span class="ee-url-text" title="${triggerUrl}">${triggerUrl}</span>
          <button class="ee-copy-btn" id="eeCopyBtn-${ev.id}" onclick="eeCopyUrl('${triggerUrl}', ${ev.id})">
            Copy
          </button>
        </div>

        <div class="ee-card-actions">
          <button class="ee-trigger-btn"
            ${!ev.isActive ? 'disabled' : ''}
            onclick="eeOpenTriggerModal(${ev.id})">
            ⚡ Trigger
          </button>
          ${ev.isActive ? `<button class="ee-deactivate-btn" onclick="eeDeactivate(${ev.id})">Deactivate</button>` : '<span style="font-size:11px;color:#9ca3af;">Inactive</span>'}
        </div>
      </div>`;
  }

  function htmlEsc(str) {
    return String(str || '')
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
  }

  /* ── Copy URL ──────────────────────────────────────────────── */
  window.eeCopyUrl = function (url, id) {
    navigator.clipboard.writeText(url).then(() => {
      const btn = document.getElementById(`eeCopyBtn-${id}`);
      if (btn) {
        btn.textContent = '✓ Copied';
        btn.classList.add('ee-copy-btn--copied');
        setTimeout(() => { btn.textContent = 'Copy'; btn.classList.remove('ee-copy-btn--copied'); }, 2000);
      }
    }).catch(() => eeShowToast('Copy failed', 'error'));
  };

  /* ── Create Form Toggle ────────────────────────────────────── */
  window.eeToggleForm = function (open) {
    eeFormOpen = (typeof open === 'boolean') ? open : !eeFormOpen;
    const form  = document.getElementById('eeCreateForm');
    const arrow = document.getElementById('eeCreateArrow');
    if (!form) return;
    form.style.display  = eeFormOpen ? 'block' : 'none';
    arrow.textContent   = eeFormOpen ? '▲ Collapse' : '▼ Expand';

    if (eeFormOpen) {
      eeLoadDropdowns();
    }
  };

  document.addEventListener('DOMContentLoaded', () => {
    const toggle = document.getElementById('eeCreateToggle');
    if (toggle) {
      toggle.addEventListener('click', () => eeToggleForm());
    }

    // Auto-slug from display name
    const dispName = document.getElementById('eeDisplayName');
    const slugName = document.getElementById('eeEventName');
    if (dispName && slugName) {
      dispName.addEventListener('input', () => {
        slugName.value = eeSlugify(dispName.value);
        eeUpdatePreview();
      });
      slugName.addEventListener('input', eeUpdatePreview);
    }
  });

  function eeUpdatePreview() {
    const slug    = document.getElementById('eeEventName')?.value || '';
    const preview = document.getElementById('eeApiPreview');
    const urlEl   = document.getElementById('eePreviewUrl');
    const bodyEl  = document.getElementById('eePreviewBody');
    if (!preview) return;

    if (slug.length >= 3) {
      preview.style.display = 'block';
      urlEl.textContent = `${location.origin}${eeApiBase()}/trigger/${slug}`;
      bodyEl.textContent = `{
  "recipients": ["user@example.com"],
  "variables": { "name": "User Name" }
}`;
    } else {
      preview.style.display = 'none';
    }
  }

  /* ── Load Dropdowns ────────────────────────────────────────── */
  function eeLoadDropdowns() {
    // Campaigns
    fetch(window.API_CAMPAIGNS_URL || '/api/campaigns')
      .then(r => r.json())
      .then(campaigns => {
        const sel = document.getElementById('eeCampaignSelect');
        if (!sel) return;
        sel.innerHTML = '<option value="">— Select campaign —</option>' +
          campaigns
            .filter(c => c.emailProviderType || c.campaignType === 'EMAIL')
            .map(c => `<option value="${c.id}">${htmlEsc(c.name)} (${c.emailProviderType || 'no provider'})</option>`)
            .join('');
      })
      .catch(() => eeShowToast('Failed to load campaigns', 'error'));

    // Templates
    fetch(window.API_TEMPLATES_URL || '/api/templates')
      .then(r => r.json())
      .then(templates => {
        const sel = document.getElementById('eeTemplateSelect');
        if (!sel) return;
        sel.innerHTML = '<option value="">— Select template —</option>' +
          templates.map(t => `<option value="${t.id}">${htmlEsc(t.name)}</option>`).join('');
      })
      .catch(() => eeShowToast('Failed to load templates', 'error'));
  }

  /* ── Create Event ──────────────────────────────────────────── */
  window.eeCreateEvent = function () {
    const displayName = document.getElementById('eeDisplayName')?.value?.trim();
    const eventName   = document.getElementById('eeEventName')?.value?.trim();
    const campaignId  = document.getElementById('eeCampaignSelect')?.value;
    const templateId  = document.getElementById('eeTemplateSelect')?.value;
    const description = document.getElementById('eeDescription')?.value?.trim();

    if (!displayName)  { eeShowToast('Display name is required', 'error'); return; }
    if (!eventName || eventName.length < 3) { eeShowToast('Event slug must be at least 3 characters', 'error'); return; }
    if (!/^[a-z0-9][a-z0-9\-]*[a-z0-9]$/.test(eventName)) { eeShowToast('Slug must be lowercase alphanumeric with hyphens', 'error'); return; }
    if (!campaignId)   { eeShowToast('Please select a campaign', 'error'); return; }
    if (!templateId)   { eeShowToast('Please select a template', 'error'); return; }

    const btn = document.getElementById('eeCreateBtn');
    if (btn) { btn.disabled = true; btn.textContent = 'Creating…'; }

    fetch(eeApiBase(), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ displayName, eventName, description, campaignId: +campaignId, templateId: +templateId })
    })
      .then(async r => {
        const body = await r.json().catch(() => null);
        if (!r.ok) throw new Error(body || r.statusText);
        return body;
      })
      .then(ev => {
        eeShowToast(`Event "${ev.displayName}" created!`, 'success');
        eeToggleForm(false);
        eeResetCreateForm();
        eeLoadEvents();
      })
      .catch(err => {
        eeShowToast(err.message || 'Failed to create event', 'error');
      })
      .finally(() => {
        if (btn) { btn.disabled = false; btn.textContent = '⚡ Create Event'; }
      });
  };

  function eeResetCreateForm() {
    ['eeDisplayName', 'eeEventName', 'eeDescription'].forEach(id => {
      const el = document.getElementById(id);
      if (el) el.value = '';
    });
    const selC = document.getElementById('eeCampaignSelect');
    const selT = document.getElementById('eeTemplateSelect');
    if (selC) selC.selectedIndex = 0;
    if (selT) selT.selectedIndex = 0;
    const preview = document.getElementById('eeApiPreview');
    if (preview) preview.style.display = 'none';
  }

  /* ── Deactivate ────────────────────────────────────────────── */
  window.eeDeactivate = function (id) {
    const ev = eeEvents.find(e => e.id === id);
    if (!ev) return;
    if (!confirm(`Deactivate event "${ev.displayName}"?\n\nIt will no longer accept trigger calls.`)) return;

    fetch(`${eeApiBase()}/${id}`, { method: 'DELETE' })
      .then(r => {
        if (!r.ok) throw new Error('Delete failed');
        eeShowToast('Event deactivated', 'success');
        eeLoadEvents();
      })
      .catch(() => eeShowToast('Failed to deactivate event', 'error'));
  };

  /* ── Trigger Modal ─────────────────────────────────────────── */
  window.eeOpenTriggerModal = function (id) {
    eeTriggerEvent = eeEvents.find(e => e.id === id);
    if (!eeTriggerEvent) return;

    const modal    = document.getElementById('eeTriggerModal');
    const subtitle = document.getElementById('eeTriggerModalSubtitle');
    const apiBox   = document.getElementById('eeTriggerApiUrl');

    if (subtitle) subtitle.textContent = eeTriggerEvent.displayName;
    if (apiBox)   apiBox.innerHTML = `<span class="ee-method-badge">POST</span> ${eeTriggerUrl(eeTriggerEvent.eventName)}`;

    const recip = document.getElementById('eeTriggerRecipients');
    const vars  = document.getElementById('eeTriggerVariables');
    if (recip) recip.value = '';
    if (vars)  vars.value  = '{}';

    if (modal) modal.style.display = 'flex';
  };

  window.eeCloseTriggerModal = function () {
    const modal = document.getElementById('eeTriggerModal');
    if (modal) modal.style.display = 'none';
    eeTriggerEvent = null;
  };

  // Close on backdrop click
  document.addEventListener('DOMContentLoaded', () => {
    const modal = document.getElementById('eeTriggerModal');
    if (modal) {
      modal.addEventListener('click', e => {
        if (e.target === modal) eeCloseTriggerModal();
      });
    }
  });

  /* ── Do Trigger ────────────────────────────────────────────── */
  window.eeDoTrigger = function () {
    if (!eeTriggerEvent) return;

    const recipientsRaw = document.getElementById('eeTriggerRecipients')?.value || '';
    const variablesRaw  = document.getElementById('eeTriggerVariables')?.value  || '{}';

    const recipients = recipientsRaw
      .split(/[\n,]+/)
      .map(r => r.trim())
      .filter(r => r.length > 0);

    if (!recipients.length) {
      eeShowToast('Please enter at least one recipient', 'error');
      return;
    }

    let variables = {};
    try {
      variables = JSON.parse(variablesRaw);
    } catch (e) {
      eeShowToast('Variables must be valid JSON', 'error');
      return;
    }

    const btn = document.getElementById('eeTriggerSendBtn');
    if (btn) { btn.disabled = true; btn.textContent = 'Sending…'; }

    fetch(`${eeApiBase()}/trigger/${eeTriggerEvent.eventName}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ recipients, variables })
    })
      .then(async r => {
        const body = await r.json().catch(() => null);
        if (r.status === 422 || (body && !body.success)) {
          // provider returned failure — show response but treat as soft error
          eeCloseTriggerModal();
          eeShowToast('Send failed: ' + (body?.errorMessage || 'unknown error'), 'error');
          eeLoadEvents();
          return;
        }
        if (!r.ok) throw new Error(body || r.statusText);
        eeCloseTriggerModal();
        eeShowToast(`✓ Email sent to ${body.recipientCount} recipient(s) — Log #${body.triggerLogId}`, 'success');
        eeLoadEvents();
      })
      .catch(err => {
        eeShowToast(err.message || 'Trigger failed', 'error');
      })
      .finally(() => {
        if (btn) { btn.disabled = false; btn.textContent = '📤 Send Email'; }
      });
  };

  /* ── Section switch hook ───────────────────────────────────── */
  // When the user clicks the "Email Events" sidebar nav button, load events
  document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('.section-nav-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        if (btn.dataset.section === 'email-events') {
          eeLoadEvents();
        }
      });
    });
  });

})();
