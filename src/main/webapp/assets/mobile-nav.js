(() => {
  "use strict";

  const MOBILE_BREAKPOINT = 820;

  function setupTopbar(topbar, index) {
    if (!topbar || topbar.dataset.mobileNavReady === "true") return;

    const nav = topbar.querySelector(".nav");
    if (!nav) return;

    const button = document.createElement("button");
    const navId = nav.id || `mobile-nav-${index}`;

    nav.id = navId;
    button.type = "button";
    button.className = "mobile-menu-toggle";
    button.setAttribute("aria-label", "Open navigation");
    button.setAttribute("aria-controls", navId);
    button.setAttribute("aria-expanded", "false");
    button.innerHTML =
      '<span class="mobile-menu-toggle-lines" aria-hidden="true"></span>';

    const brand = topbar.querySelector(".brand");
    if (brand && brand.nextSibling) {
      topbar.insertBefore(button, brand.nextSibling);
    } else {
      topbar.insertBefore(button, nav);
    }

    function setOpen(open) {
      topbar.classList.toggle("mobile-menu-open", open);
      button.setAttribute("aria-expanded", String(open));
      button.setAttribute(
        "aria-label",
        open ? "Close navigation" : "Open navigation"
      );
    }

    button.addEventListener("click", event => {
      event.stopPropagation();
      setOpen(!topbar.classList.contains("mobile-menu-open"));
    });

    nav.addEventListener("click", event => {
      if (event.target.closest("a") && window.innerWidth <= MOBILE_BREAKPOINT) {
        setOpen(false);
      }
    });

    document.addEventListener("click", event => {
      if (
        window.innerWidth <= MOBILE_BREAKPOINT &&
        !topbar.contains(event.target)
      ) {
        setOpen(false);
      }
    });

    document.addEventListener("keydown", event => {
      if (event.key === "Escape") setOpen(false);
    });

    window.addEventListener("resize", () => {
      if (window.innerWidth > MOBILE_BREAKPOINT) setOpen(false);
    });

    topbar.dataset.mobileNavReady = "true";
  }

  function init() {
    document
      .querySelectorAll(".topbar")
      .forEach((topbar, index) => setupTopbar(topbar, index));
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }
})();
