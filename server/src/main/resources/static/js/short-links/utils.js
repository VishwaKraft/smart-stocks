/* ============================================================
   utils.js — Shared utility functions
   ============================================================ */

export function fmtDate(isoStr) {
    if (!isoStr) return "—";
    return new Date(isoStr).toLocaleString([], { dateStyle: "medium", timeStyle: "short" });
}

export function escHtml(str) {
    if (!str) return "";
    return str.replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;");
}

export async function copyToClipboard(text, btn) {
    try {
        await navigator.clipboard.writeText(text);
        const orig = btn.textContent;
        btn.textContent = "Copied!";
        setTimeout(() => { btn.textContent = orig; }, 1500);
    } catch { alert("Could not copy"); }
}

export function buildCopyRow(label, value) {
    const wrap = document.createElement("div");
    wrap.className = "copy-block";
    const lbl = document.createElement("label");
    lbl.textContent = label;
    const row = document.createElement("div");
    row.className = "copy-row";
    const inp = document.createElement("input");
    inp.type = "text"; inp.readOnly = true; inp.value = value;
    const btn = document.createElement("button");
    btn.type = "button"; btn.textContent = "Copy";
    btn.addEventListener("click", () => copyToClipboard(value, btn));
    row.append(inp, btn);
    wrap.append(lbl, row);
    return wrap;
}

export function formatHtml(html) {
    const lines = html.replace(/>\s+</g, "><").split(/></).map((c, i, a) => {
        if (i > 0) c = "<" + c;
        if (i < a.length - 1) c += ">";
        return c;
    });
    let out = "", indent = 0;
    const voids = /^(area|base|br|col|embed|hr|img|input|link|meta|source|track|!DOCTYPE)/i;
    lines.forEach(line => {
        const isClose = /^<\//.test(line.trim());
        const isOpen  = /^<[^/!][^>]*[^/]>$/.test(line.trim()) && !voids.test(line.trim());
        const isSelf  = /\/>$/.test(line.trim()) || voids.test(line.trim());
        if (isClose) indent = Math.max(indent - 1, 0);
        out += "  ".repeat(indent) + line.trim() + "\n";
        if (isOpen && !isSelf) indent++;
    });
    return out.trim();
}

export function animateValue(id, start, end, duration) {
    if (start === end) {
        document.getElementById(id).innerText = end;
        return;
    }
    let startTimestamp = null;
    const step = (timestamp) => {
        if (!startTimestamp) startTimestamp = timestamp;
        const progress = Math.min((timestamp - startTimestamp) / duration, 1);
        const easeOutProgress = 1 - Math.pow(1 - progress, 3);
        const obj = document.getElementById(id);
        obj.innerText = Math.floor(easeOutProgress * (end - start) + start);
        if (progress < 1) {
            window.requestAnimationFrame(step);
        } else {
            obj.innerText = end;
        }
    };
    window.requestAnimationFrame(step);
}
