/* Alumni Nexus AI — chatbot.js */
(() => {
  "use strict";
  const log = document.getElementById("chat-log"), form = document.getElementById("chat-form"), input = document.getElementById("chat-input");
  if (!log || !form) return;
  const scroll = () => (log.scrollTop = log.scrollHeight);
  const add = (cls, text) => { const d = document.createElement("div"); d.className = "msg " + cls; d.textContent = text; log.appendChild(d); scroll(); return d; };
  const addLinks = links => {
    if (!links || !links.length) return;
    const w = document.createElement("div"); w.className = "msg-links";
    links.forEach(l => { const a = document.createElement("a"); a.className = "btn btn-ghost btn-sm"; a.href = l.url; a.textContent = l.label + " →"; w.appendChild(a); });
    log.appendChild(w); scroll();
  };
  async function send(text) {
    text = text.trim(); if (!text) return;
    add("user", text); input.value = "";
    const t = document.createElement("div"); t.className = "msg bot"; t.innerHTML = '<span class="typing"><i></i><i></i><i></i></span>'; log.appendChild(t); scroll();
    try { const r = await NX.post("/ask", { message: text }); t.textContent = r.reply; addLinks(r.links); }
    catch (e) { t.textContent = "Sorry, something went wrong. Please try again."; }
    scroll();
  }
  form.addEventListener("submit", e => { e.preventDefault(); send(input.value); });
  document.querySelectorAll("[data-ask]").forEach(b => b.addEventListener("click", () => send(b.dataset.ask)));
  document.getElementById("chat-clear")?.addEventListener("click", async () => {
    if (!confirm("Clear this conversation?")) return;
    await NX.post("/chatbot/clear"); log.querySelectorAll(".msg:not(.welcome), .msg-links").forEach(n => n.remove());
  });
  scroll();
})();
