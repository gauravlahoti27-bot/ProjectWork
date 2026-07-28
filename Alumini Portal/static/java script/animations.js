/* =============================================================
   Alumni Nexus AI — animations.js
   Visual/motion engine for the landing page:
     1. Nexus constellation (interactive canvas — the signature)
     2. Typing headline effect
     3. Counter (count-up) animation
     4. Scroll reveal (IntersectionObserver)
     5. Light parallax on the hero card
   Pure vanilla JS. Respects prefers-reduced-motion.
   ============================================================= */

(() => {
  "use strict";

  const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;

  /* ===========================================================
     1. NEXUS CONSTELLATION
     A network of alumni "nodes" connected by lines. Nodes drift
     slowly and gently gravitate toward the cursor, so the graph
     feels alive — the visual metaphor for the whole product.
     =========================================================== */
  function initNexus() {
    const canvas = document.getElementById("nexus-canvas");
    if (!canvas) return;
    const ctx = canvas.getContext("2d");

    let width, height, dpr;
    let nodes = [];
    const mouse = { x: null, y: null, active: false };

    // Palette pulled from the brand tokens
    const COLORS = ["#6C63FF", "#8B5CF6", "#00E5FF"];

    function resize() {
      dpr = Math.min(window.devicePixelRatio || 1, 2);
      width = canvas.clientWidth;
      height = canvas.clientHeight;
      canvas.width = width * dpr;
      canvas.height = height * dpr;
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
      buildNodes();
    }

    function buildNodes() {
      // Node count scales with screen area (kept light on mobile)
      const area = width * height;
      const count = Math.min(Math.max(Math.round(area / 16000), 28), 90);
      nodes = [];
      for (let i = 0; i < count; i++) {
        nodes.push({
          x: Math.random() * width,
          y: Math.random() * height,
          vx: (Math.random() - 0.5) * 0.35,
          vy: (Math.random() - 0.5) * 0.35,
          r: Math.random() * 1.8 + 1.2,
          color: COLORS[(Math.random() * COLORS.length) | 0]
        });
      }
    }

    const LINK_DIST = 140;   // max distance to draw a link
    const MOUSE_DIST = 180;  // cursor influence radius

    function tick() {
      ctx.clearRect(0, 0, width, height);

      for (let i = 0; i < nodes.length; i++) {
        const n = nodes[i];

        // Drift
        n.x += n.vx;
        n.y += n.vy;

        // Bounce off edges
        if (n.x < 0 || n.x > width) n.vx *= -1;
        if (n.y < 0 || n.y > height) n.vy *= -1;

        // Cursor gravity (subtle)
        if (mouse.active) {
          const dx = mouse.x - n.x;
          const dy = mouse.y - n.y;
          const dist = Math.hypot(dx, dy);
          if (dist < MOUSE_DIST) {
            const pull = (MOUSE_DIST - dist) / MOUSE_DIST;
            n.x += (dx / dist) * pull * 0.6;
            n.y += (dy / dist) * pull * 0.6;
          }
        }

        // Draw node with soft glow
        ctx.beginPath();
        ctx.arc(n.x, n.y, n.r, 0, Math.PI * 2);
        ctx.fillStyle = n.color;
        ctx.shadowBlur = 12;
        ctx.shadowColor = n.color;
        ctx.fill();
        ctx.shadowBlur = 0;
      }

      // Links between nearby nodes
      for (let i = 0; i < nodes.length; i++) {
        for (let j = i + 1; j < nodes.length; j++) {
          const a = nodes[i], b = nodes[j];
          const dx = a.x - b.x, dy = a.y - b.y;
          const dist = Math.hypot(dx, dy);
          if (dist < LINK_DIST) {
            const alpha = (1 - dist / LINK_DIST) * 0.5;
            ctx.strokeStyle = `rgba(108,99,255,${alpha})`;
            ctx.lineWidth = 1;
            ctx.beginPath();
            ctx.moveTo(a.x, a.y);
            ctx.lineTo(b.x, b.y);
            ctx.stroke();
          }
        }
      }

      // Highlight links from cursor to nearby nodes (cyan)
      if (mouse.active) {
        for (let i = 0; i < nodes.length; i++) {
          const n = nodes[i];
          const dist = Math.hypot(mouse.x - n.x, mouse.y - n.y);
          if (dist < MOUSE_DIST) {
            const alpha = (1 - dist / MOUSE_DIST) * 0.6;
            ctx.strokeStyle = `rgba(0,229,255,${alpha})`;
            ctx.lineWidth = 1;
            ctx.beginPath();
            ctx.moveTo(mouse.x, mouse.y);
            ctx.lineTo(n.x, n.y);
            ctx.stroke();
          }
        }
      }

      requestAnimationFrame(tick);
    }

    // Cursor tracking (relative to canvas)
    window.addEventListener("mousemove", (e) => {
      const rect = canvas.getBoundingClientRect();
      mouse.x = e.clientX - rect.left;
      mouse.y = e.clientY - rect.top;
      mouse.active = mouse.y >= 0 && mouse.y <= rect.height;
    });
    window.addEventListener("mouseleave", () => { mouse.active = false; });

    window.addEventListener("resize", debounce(resize, 200));
    resize();

    // If reduced motion, render one static frame instead of animating
    if (reduceMotion) {
      ctx.clearRect(0, 0, width, height);
      nodes.forEach(n => {
        ctx.beginPath();
        ctx.arc(n.x, n.y, n.r, 0, Math.PI * 2);
        ctx.fillStyle = n.color;
        ctx.fill();
      });
    } else {
      tick();
    }
  }

  /* ===========================================================
     2. TYPING HEADLINE
     Cycles through a set of audience words in the hero H1.
     =========================================================== */
  function initTyping() {
    const target = document.querySelector("[data-typing]");
    if (!target) return;

    const words = JSON.parse(target.getAttribute("data-typing"));
    if (reduceMotion) { target.textContent = words[0]; return; }

    let wordIndex = 0;
    let charIndex = 0;
    let deleting = false;

    function step() {
      const current = words[wordIndex];
      if (!deleting) {
        target.textContent = current.slice(0, ++charIndex);
        if (charIndex === current.length) {
          deleting = true;
          return setTimeout(step, 1600); // pause on full word
        }
      } else {
        target.textContent = current.slice(0, --charIndex);
        if (charIndex === 0) {
          deleting = false;
          wordIndex = (wordIndex + 1) % words.length;
        }
      }
      setTimeout(step, deleting ? 45 : 95);
    }
    step();
  }

  /* ===========================================================
     3. COUNTER ANIMATION
     Counts a number up when it scrolls into view.
     =========================================================== */
  function initCounters() {
    const counters = document.querySelectorAll("[data-count]");
    if (!counters.length) return;

    const run = (el) => {
      const target = parseFloat(el.getAttribute("data-count"));
      const decimals = (el.getAttribute("data-decimals") | 0);
      const duration = 1600;
      const start = performance.now();

      if (reduceMotion) {
        el.textContent = target.toFixed(decimals);
        return;
      }

      function frame(now) {
        const p = Math.min((now - start) / duration, 1);
        // easeOutExpo for a punchy finish
        const eased = p === 1 ? 1 : 1 - Math.pow(2, -10 * p);
        el.textContent = (target * eased).toFixed(decimals);
        if (p < 1) requestAnimationFrame(frame);
        else el.textContent = target.toFixed(decimals);
      }
      requestAnimationFrame(frame);
    };

    const obs = new IntersectionObserver((entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          run(entry.target);
          obs.unobserve(entry.target);
        }
      });
    }, { threshold: 0.6 });

    counters.forEach((c) => obs.observe(c));
  }

  /* ===========================================================
     4. SCROLL REVEAL
     Adds .is-visible to .reveal elements as they enter view.
     =========================================================== */
  function initReveal() {
    const items = document.querySelectorAll(".reveal");
    if (!items.length) return;

    if (reduceMotion) {
      items.forEach((el) => el.classList.add("is-visible"));
      return;
    }

    const obs = new IntersectionObserver((entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add("is-visible");
          obs.unobserve(entry.target);
        }
      });
    }, { threshold: 0.14, rootMargin: "0px 0px -40px 0px" });

    items.forEach((el) => obs.observe(el));
  }

  /* ===========================================================
     5. HERO PARALLAX
     Nudges the floating hero card as the cursor moves.
     =========================================================== */
  function initParallax() {
    if (reduceMotion) return;
    const card = document.querySelector("[data-parallax]");
    if (!card) return;

    window.addEventListener("mousemove", (e) => {
      const rx = (e.clientX / window.innerWidth - 0.5) * 14;
      const ry = (e.clientY / window.innerHeight - 0.5) * 14;
      card.style.transform = `translate(${rx}px, ${ry}px)`;
    });
  }

  /* ---- small debounce helper ---- */
  function debounce(fn, wait) {
    let t;
    return function (...args) {
      clearTimeout(t);
      t = setTimeout(() => fn.apply(this, args), wait);
    };
  }

  /* ---- boot everything on DOM ready ---- */
  document.addEventListener("DOMContentLoaded", () => {
    initNexus();
    initTyping();
    initCounters();
    initReveal();
    initParallax();
  });
})();