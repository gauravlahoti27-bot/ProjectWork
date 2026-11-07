/* Alumni Nexus AI — dashboard.js : behaviour for the logged-in app shell */
(() => {
  "use strict";
  // Mobile sidebar
  const sb = document.querySelector(".sidebar"), btn = document.querySelector(".menu-btn");
  if (sb && btn) {
    btn.addEventListener("click", () => sb.classList.toggle("open"));
    document.addEventListener("click", e => { if (sb.classList.contains("open") && !sb.contains(e.target) && e.target !== btn) sb.classList.remove("open"); });
  }
  // Flash messages auto-dismiss
  document.querySelectorAll(".flash").forEach(el => {
    const close = () => { el.classList.add("out"); setTimeout(() => el.remove(), 300); };
    el.querySelector("button")?.addEventListener("click", close);
    setTimeout(close, 6000);
  });
  // Confirm destructive actions
  document.addEventListener("submit", e => {
    const msg = e.target.dataset.confirm;
    if (msg && !confirm(msg)) e.preventDefault();
  });
  // Animate progress bars
  document.querySelectorAll(".progress > span[data-w]").forEach(el => requestAnimationFrame(() => (el.style.width = el.dataset.w + "%")));
  // Demo credential fill on login page
  document.querySelectorAll("[data-fill]").forEach(b => b.addEventListener("click", () => {
    const [email, pw] = b.dataset.fill.split("|");
    document.querySelector("[name=email]").value = email; document.querySelector("[name=password]").value = pw;
  }));
  // Reveal on scroll for app cards
  const io = "IntersectionObserver" in window && new IntersectionObserver(es => es.forEach(en => { if (en.isIntersecting) { en.target.classList.add("is-visible"); io.unobserve(en.target); } }), { threshold: .08 });
  document.querySelectorAll(".reveal").forEach(el => io ? io.observe(el) : el.classList.add("is-visible"));
})();
