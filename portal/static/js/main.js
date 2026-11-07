/* =============================================================
   Alumni Nexus AI — main.js
   Core UI behavior for the landing page:
     - Navbar scroll state (frosted background after scroll)
     - Mobile menu toggle
     - FAQ accordion
     - Smooth anchor scrolling
     - Contact form (front-end only) validation + toast
     - Top scroll-progress bar
     - Footer year
   ============================================================= */

(() => {
  "use strict";

  /* ---------------------------------------------------------
     NAVBAR: add .scrolled once the page is scrolled a little
     --------------------------------------------------------- */
  const nav = document.querySelector(".nav");
  const onScrollNav = () => {
    if (!nav) return;
    nav.classList.toggle("scrolled", window.scrollY > 24);
  };
  window.addEventListener("scroll", onScrollNav, { passive: true });
  onScrollNav();

  /* ---------------------------------------------------------
     MOBILE MENU toggle
     --------------------------------------------------------- */
  const toggle = document.querySelector(".nav-toggle");
  const mobileMenu = document.querySelector(".mobile-menu");
  if (toggle && mobileMenu) {
    const closeMenu = () => {
      toggle.classList.remove("open");
      mobileMenu.classList.remove("open");
      toggle.setAttribute("aria-expanded", "false");
    };
    toggle.addEventListener("click", () => {
      const isOpen = toggle.classList.toggle("open");
      mobileMenu.classList.toggle("open", isOpen);
      toggle.setAttribute("aria-expanded", String(isOpen));
    });
    // Close after tapping a link
    mobileMenu.querySelectorAll("a").forEach((a) =>
      a.addEventListener("click", closeMenu)
    );
    // Close on escape
    document.addEventListener("keydown", (e) => {
      if (e.key === "Escape") closeMenu();
    });
  }

  /* ---------------------------------------------------------
     FAQ ACCORDION
     Only one item open at a time; animates via max-height.
     --------------------------------------------------------- */
  const faqItems = document.querySelectorAll(".faq-item");
  faqItems.forEach((item) => {
    const q = item.querySelector(".faq-q");
    const a = item.querySelector(".faq-a");
    if (!q || !a) return;

    q.addEventListener("click", () => {
      const isOpen = item.classList.contains("open");

      // Close all
      faqItems.forEach((other) => {
        other.classList.remove("open");
        const oa = other.querySelector(".faq-a");
        if (oa) oa.style.maxHeight = null;
        const oq = other.querySelector(".faq-q");
        if (oq) oq.setAttribute("aria-expanded", "false");
      });

      // Open clicked one (unless it was already open)
      if (!isOpen) {
        item.classList.add("open");
        a.style.maxHeight = a.scrollHeight + "px";
        q.setAttribute("aria-expanded", "true");
      }
    });
  });

  /* ---------------------------------------------------------
     SMOOTH ANCHOR SCROLL
     --------------------------------------------------------- */
  document.querySelectorAll('a[href^="#"]').forEach((link) => {
    link.addEventListener("click", (e) => {
      const id = link.getAttribute("href");
      if (id === "#" || id.length < 2) return;
      const el = document.querySelector(id);
      if (!el) return;
      e.preventDefault();
      el.scrollIntoView({ behavior: "smooth", block: "start" });
    });
  });

  /* ---------------------------------------------------------
     SCROLL PROGRESS BAR
     --------------------------------------------------------- */
  const progress = document.querySelector(".scroll-progress");
  if (progress) {
    const onProgress = () => {
      const h = document.documentElement;
      const scrolled = h.scrollTop;
      const max = h.scrollHeight - h.clientHeight;
      const pct = max > 0 ? (scrolled / max) * 100 : 0;
      progress.style.width = pct + "%";
    };
    window.addEventListener("scroll", onProgress, { passive: true });
    onProgress();
  }

  /* ---------------------------------------------------------
     CONTACT FORM (front-end only)
     Basic validation + a small toast confirmation. No backend.
     --------------------------------------------------------- */
  const form = document.querySelector("#contact-form");
  if (form) {
    form.addEventListener("submit", (e) => {
      e.preventDefault();
      const name = form.querySelector('[name="name"]');
      const email = form.querySelector('[name="email"]');
      const emailOk = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value.trim());

      if (!name.value.trim() || !emailOk) {
        showToast("Please add your name and a valid email.", "warn");
        return;
      }
      showToast("Thanks! Our alumni team will reach out shortly.", "ok");
      form.reset();
    });
  }

  /* Minimal toast helper (creates the node once, reuses it) */
  let toastEl;
  function showToast(message, type = "ok") {
    if (!toastEl) {
      toastEl = document.createElement("div");
      toastEl.className = "toast";
      document.body.appendChild(toastEl);
      // Inline styles keep this self-contained (no extra CSS file needed)
      Object.assign(toastEl.style, {
        position: "fixed",
        left: "50%",
        bottom: "26px",
        transform: "translateX(-50%) translateY(20px)",
        padding: "13px 20px",
        borderRadius: "14px",
        fontSize: "14px",
        fontWeight: "600",
        color: "#fff",
        zIndex: "300",
        opacity: "0",
        transition: "opacity .3s ease, transform .3s ease",
        backdropFilter: "blur(10px)",
        border: "1px solid rgba(255,255,255,.14)",
        boxShadow: "0 18px 50px -20px rgba(0,0,0,.8)"
      });
    }
    toastEl.style.background =
      type === "warn"
        ? "linear-gradient(120deg, #f59e0b, #ef4444)"
        : "linear-gradient(120deg, #6C63FF, #00E5FF)";
    toastEl.textContent = message;

    requestAnimationFrame(() => {
      toastEl.style.opacity = "1";
      toastEl.style.transform = "translateX(-50%) translateY(0)";
    });

    clearTimeout(showToast._t);
    showToast._t = setTimeout(() => {
      toastEl.style.opacity = "0";
      toastEl.style.transform = "translateX(-50%) translateY(20px)";
    }, 3200);
  }

  /* ---------------------------------------------------------
     FOOTER YEAR
     --------------------------------------------------------- */
  const yearEl = document.querySelector("#year");
  if (yearEl) yearEl.textContent = new Date().getFullYear();
})();