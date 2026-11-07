/* Alumni Nexus AI — utils.js : tiny shared helpers (loaded on every page) */
window.NX = {
  csrf() { const m = document.querySelector('meta[name="csrf-token"]'); return m ? m.content : ""; },
  esc(s) { return String(s).replace(/[&<>"']/g, c => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c])); },
  async post(url, data) {
    const r = await fetch(url, { method: "POST", headers: { "Content-Type": "application/json", "X-CSRF-Token": this.csrf() }, body: JSON.stringify(data || {}) });
    if (!r.ok) throw new Error((await r.json().catch(() => ({}))).error || "Request failed");
    return r.json();
  },
};
