(() => {
  "use strict";

  let lang = "en";
  let text;

  const translations = {
    en: {
      loading: "Loading subscription details…",
      error: "Could not load subscription details.",
      retry: "Try again",
      active: "Active",
      inactive: "Ended",
      noCategory: "No category",
      monthly: "Approx. per month",
      yearly: "Yearly estimate",
      totalSpent: "Total found in history",
      payments: "Payments found",
      overview: "Overview",
      status: "Status",
      category: "Category",
      interval: "Interval",
      pricePerPeriod: "Price per period",
      nextCharge: "Next charge",
      added: "Added",
      billingEmail: "Billing email",
      historyStats: "History statistics",
      firstPayment: "First payment",
      lastPayment: "Latest payment",
      averagePayment: "Average payment",
      smallestPayment: "Smallest payment",
      largestPayment: "Largest payment",
      insights: "Insights",
      history: "Payment history",
      noHistory: "No matching imported payments were found yet.",
      close: "Close",
      manage: "Manage subscription",
      cancelProvider: "Open provider",
      daysToday: "today",
      daysTomorrow: "tomorrow",
      daysIn: "in {days} days",
      daysAgo: "{days} days ago",
      monthlyInsight: "This subscription costs about {amount} NOK per month.",
      yearlyInsight: "Keeping it for one year is estimated to cost {amount} NOK.",
      totalInsight: "Approximately {amount} NOK was found in matching imported payments.",
      countInsight: "{count} matching payments were found in your imported transaction history.",
      nextInsight: "The next charge is {when}.",
      priceUp: "The latest matching payment is {amount} NOK higher than the first one ({percent}%).",
      priceDown: "The latest matching payment is {amount} NOK lower than the first one ({percent}%).",
      noHistoryInsight: "Import or sync more transactions to unlock payment history and price-change insights.",
      approximateFx: "NOK history values use the available exchange rate and are approximate.",
      unknown: "—"
    },
    nb: {
      loading: "Laster abonnementsdetaljer…",
      error: "Kunne ikke laste abonnementsdetaljene.",
      retry: "Prøv igjen",
      active: "Aktiv",
      inactive: "Avsluttet",
      noCategory: "Ingen kategori",
      monthly: "Ca. per måned",
      yearly: "Årlig estimat",
      totalSpent: "Totalt funnet i historikken",
      payments: "Betalinger funnet",
      overview: "Oversikt",
      status: "Status",
      category: "Kategori",
      interval: "Intervall",
      pricePerPeriod: "Pris per periode",
      nextCharge: "Neste trekk",
      added: "Lagt til",
      billingEmail: "Faktura-e-post",
      historyStats: "Historikkstatistikk",
      firstPayment: "Første betaling",
      lastPayment: "Siste betaling",
      averagePayment: "Gjennomsnittlig betaling",
      smallestPayment: "Laveste betaling",
      largestPayment: "Høyeste betaling",
      insights: "Innsikter",
      history: "Betalingshistorikk",
      noHistory: "Fant ingen samsvarende importerte betalinger ennå.",
      close: "Lukk",
      manage: "Administrer abonnement",
      cancelProvider: "Åpne leverandør",
      daysToday: "i dag",
      daysTomorrow: "i morgen",
      daysIn: "om {days} dager",
      daysAgo: "for {days} dager siden",
      monthlyInsight: "Dette abonnementet koster omtrent {amount} NOK per måned.",
      yearlyInsight: "Ett år er estimert til å koste {amount} NOK.",
      totalInsight: "Omtrent {amount} NOK ble funnet i samsvarende importerte betalinger.",
      countInsight: "{count} samsvarende betalinger ble funnet i den importerte transaksjonshistorikken.",
      nextInsight: "Neste trekk er {when}.",
      priceUp: "Den nyeste samsvarende betalingen er {amount} NOK høyere enn den første ({percent} %).",
      priceDown: "Den nyeste samsvarende betalingen er {amount} NOK lavere enn den første ({percent} %).",
      noHistoryInsight: "Importer eller synkroniser flere transaksjoner for å få betalingshistorikk og innsikt i prisendringer.",
      approximateFx: "Historikk i NOK bruker tilgjengelig valutakurs og er omtrentlig.",
      unknown: "—"
    }
  };

  function setLanguage(value) {
    lang = String(value || "en").toLowerCase().startsWith("nb")
      ? "nb"
      : "en";
    text = translations[lang];
  }

  setLanguage(document.documentElement.lang);

  const contextPath =
    document.documentElement.dataset.contextPath || "";

  let overlay = null;
  let lastTrigger = null;
  let currentId = null;

  function escapeHtml(value) {
    return String(value ?? "")
      .replaceAll("&", "&amp;")
      .replaceAll("<", "&lt;")
      .replaceAll(">", "&gt;")
      .replaceAll('"', "&quot;")
      .replaceAll("'", "&#039;");
  }

  function money(value) {
    const number = Number(value ?? 0);

    return new Intl.NumberFormat(
      lang === "nb" ? "nb-NO" : "en-GB",
      {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
      }
    ).format(Number.isFinite(number) ? number : 0);
  }

  function date(value) {
    if (!value) return text.unknown;

    const parsed = new Date(`${value}T12:00:00`);
    if (Number.isNaN(parsed.getTime())) return String(value);

    return new Intl.DateTimeFormat(
      lang === "nb" ? "nb-NO" : "en-GB",
      {
        day: "numeric",
        month: "short",
        year: "numeric"
      }
    ).format(parsed);
  }

  function interval(value) {
    const values = {
      en: {
        WEEKLY: "Weekly",
        MONTHLY: "Monthly",
        QUARTERLY: "Quarterly",
        YEARLY: "Yearly"
      },
      nb: {
        WEEKLY: "Ukentlig",
        MONTHLY: "Månedlig",
        QUARTERLY: "Kvartalsvis",
        YEARLY: "Årlig"
      }
    };

    return values[lang][value] || value || text.unknown;
  }

  function relativeDays(value) {
    const days = Number(value);

    if (days === 0) return text.daysToday;
    if (days === 1) return text.daysTomorrow;
    if (days > 1) return text.daysIn.replace("{days}", days);

    return text.daysAgo.replace("{days}", Math.abs(days));
  }

  function createOverlay() {
    if (overlay) return overlay;

    overlay = document.createElement("div");
    overlay.className = "subscription-modal-overlay";
    overlay.setAttribute("aria-hidden", "true");
    overlay.innerHTML = `
      <section class="subscription-modal"
               role="dialog"
               aria-modal="true"
               aria-labelledby="subscription-modal-title">
        <div id="subscription-modal-content"></div>
      </section>
    `;

    overlay.addEventListener("click", event => {
      if (event.target === overlay) close();
    });

    document.body.appendChild(overlay);
    return overlay;
  }

  function contentNode() {
    return document.getElementById("subscription-modal-content");
  }

  function open(id, trigger) {
    if (!id) return;

    currentId = id;
    lastTrigger = trigger || document.activeElement;

    const modalOverlay = createOverlay();
    modalOverlay.setAttribute("aria-hidden", "false");
    modalOverlay.classList.add("open");
    document.body.classList.add("subscription-modal-lock");

    renderLoading();
    fetchDetails(id);
  }

  function close() {
    if (!overlay) return;

    overlay.classList.remove("open");
    overlay.setAttribute("aria-hidden", "true");
    document.body.classList.remove("subscription-modal-lock");

    if (lastTrigger && typeof lastTrigger.focus === "function") {
      lastTrigger.focus();
    }
  }

  function renderLoading() {
    contentNode().innerHTML = `
      <div class="subscription-modal-header">
        <div class="subscription-modal-title-row">
          <div class="subscription-modal-icon">S</div>
          <div>
            <h2 class="subscription-modal-title"
                id="subscription-modal-title">
              ${escapeHtml(text.loading)}
            </h2>
          </div>
        </div>

        <button class="subscription-modal-close"
                type="button"
                data-subscription-modal-close
                aria-label="${escapeHtml(text.close)}">×</button>
      </div>

      <div class="subscription-modal-loading">
        ${escapeHtml(text.loading)}
      </div>
    `;

    bindCloseButtons();
  }

  function renderError() {
    contentNode().innerHTML = `
      <div class="subscription-modal-header">
        <h2 class="subscription-modal-title"
            id="subscription-modal-title">
          ${escapeHtml(text.error)}
        </h2>

        <button class="subscription-modal-close"
                type="button"
                data-subscription-modal-close
                aria-label="${escapeHtml(text.close)}">×</button>
      </div>

      <div class="subscription-modal-error">
        <p>${escapeHtml(text.error)}</p>
        <button class="btn"
                type="button"
                data-subscription-modal-retry>
          ${escapeHtml(text.retry)}
        </button>
      </div>
    `;

    bindCloseButtons();

    const retry = contentNode().querySelector(
      "[data-subscription-modal-retry]"
    );

    if (retry) {
      retry.addEventListener("click", () => {
        renderLoading();
        fetchDetails(currentId);
      });
    }
  }

  async function fetchDetails(id) {
    try {
      const response = await fetch(
        `${contextPath}/app/subscriptions/${encodeURIComponent(id)}/details`,
        {
          headers: {
            "Accept": "application/json"
          },
          credentials: "same-origin"
        }
      );

      if (!response.ok) throw new Error("Request failed");

      const data = await response.json();

      // The language now comes from Spring's SessionLocaleResolver.
      setLanguage(data.language);
      renderDetails(data);
    } catch (error) {
      renderError();
    }
  }

  function insightText(raw) {
    const parts = String(raw || "").split("|");

    switch (parts[0]) {
      case "MONTHLY":
        return text.monthlyInsight.replace(
          "{amount}",
          money(parts[1])
        );

      case "YEARLY":
        return text.yearlyInsight.replace(
          "{amount}",
          money(parts[1])
        );

      case "TOTAL_SPENT":
        return text.totalInsight.replace(
          "{amount}",
          money(parts[1])
        );

      case "PAYMENT_COUNT":
        return text.countInsight.replace(
          "{count}",
          parts[1]
        );

      case "NEXT_PAYMENT":
        return text.nextInsight.replace(
          "{when}",
          relativeDays(parts[1])
        );

      case "PRICE_CHANGE": {
        const amount = Number(parts[1]);
        const percent = Number(parts[2]);
        const template =
          amount >= 0 ? text.priceUp : text.priceDown;

        return template
          .replace("{amount}", money(Math.abs(amount)))
          .replace("{percent}", money(Math.abs(percent)));
      }

      case "NO_HISTORY":
        return text.noHistoryInsight;

      default:
        return raw;
    }
  }

  function kpi(label, value) {
    return `
      <div class="subscription-detail-kpi">
        <div class="subscription-detail-label">
          ${escapeHtml(label)}
        </div>
        <div class="subscription-detail-value">
          ${escapeHtml(value)}
        </div>
      </div>
    `;
  }

  function fact(label, value) {
    return `
      <div class="subscription-detail-fact">
        <span>${escapeHtml(label)}</span>
        <strong>${escapeHtml(value)}</strong>
      </div>
    `;
  }

  function renderDetails(data) {
    const history = Array.isArray(data.history)
      ? data.history
      : [];

    const insights = Array.isArray(data.insights)
      ? data.insights
      : [];

    const initial = String(data.name || "S")
      .trim()
      .charAt(0)
      .toUpperCase();

    const category = data.category || text.noCategory;
    const status = data.active ? text.active : text.inactive;

    const priceChange = data.priceChange
      ? `
        <div class="subscription-price-change">
          <strong>
            ${Number(data.priceChange.changeAmountNok) >= 0 ? "↗" : "↘"}
            ${money(Math.abs(Number(data.priceChange.changeAmountNok)))} NOK
          </strong>

          <div class="muted" style="margin-top:5px;">
            ${money(data.priceChange.firstAmountNok)} NOK →
            ${money(data.priceChange.latestAmountNok)} NOK
            (${money(Math.abs(Number(data.priceChange.changePercent)))} %)
          </div>
        </div>
      `
      : "";

    const historyHtml = history.length
      ? history.map(item => `
          <div class="subscription-history-row">
            <div>${escapeHtml(date(item.date))}</div>

            <div class="subscription-history-description">
              <strong>
                ${escapeHtml(item.description || data.name)}
              </strong>
              <span>
                ${escapeHtml(item.reference || item.currency || "")}
              </span>
            </div>

            <div class="subscription-history-amount">
              ${money(item.amount)} ${escapeHtml(item.currency)}

              ${item.currency !== "NOK"
                ? `<div class="muted">
                     ≈ ${money(item.amountNok)} NOK
                   </div>`
                : ""}
            </div>
          </div>
        `).join("")
      : `<div class="muted">${escapeHtml(text.noHistory)}</div>`;

    const insightHtml = insights.map(item => `
      <div class="subscription-detail-insight">
        💡 ${escapeHtml(insightText(item))}
      </div>
    `).join("");

    contentNode().innerHTML = `
      <div class="subscription-modal-header">
        <div class="subscription-modal-title-row">
          <div class="subscription-modal-icon">
            ${escapeHtml(initial)}
          </div>

          <div>
            <h2 class="subscription-modal-title"
                id="subscription-modal-title">
              ${escapeHtml(data.name)}
            </h2>

            <div class="subscription-modal-subtitle">
              <span class="pill ${data.active ? "ok" : "warn"}">
                ${escapeHtml(status)}
              </span>
              <span class="pill">${escapeHtml(category)}</span>
              <span class="pill">${escapeHtml(interval(data.interval))}</span>
            </div>
          </div>
        </div>

        <button class="subscription-modal-close"
                type="button"
                data-subscription-modal-close
                aria-label="${escapeHtml(text.close)}">×</button>
      </div>

      <div class="subscription-modal-body">
        <div class="subscription-detail-kpis">
          ${kpi(text.monthly, `${money(data.monthlyNok)} NOK`)}
          ${kpi(text.yearly, `${money(data.yearlyNok)} NOK`)}
          ${kpi(text.totalSpent, `${money(data.totalSpentNok)} NOK`)}
          ${kpi(text.payments, String(data.paymentCount ?? 0))}
        </div>

        <div class="subscription-detail-grid">
          <section class="subscription-detail-section">
            <h3>${escapeHtml(text.overview)}</h3>

            <div class="subscription-detail-facts">
              ${fact(text.status, status)}
              ${fact(text.category, category)}
              ${fact(text.interval, interval(data.interval))}
              ${fact(
                text.pricePerPeriod,
                `${money(data.amount)} ${data.currency}`
              )}
              ${fact(text.nextCharge, date(data.nextChargeDate))}
              ${fact(
                text.added,
                data.createdAt
                  ? date(String(data.createdAt).slice(0, 10))
                  : text.unknown
              )}
              ${fact(
                text.billingEmail,
                data.billingEmail || text.unknown
              )}
            </div>
          </section>

          <section class="subscription-detail-section">
            <h3>${escapeHtml(text.historyStats)}</h3>

            <div class="subscription-detail-facts">
              ${fact(text.firstPayment, date(data.firstPaymentDate))}
              ${fact(text.lastPayment, date(data.lastPaymentDate))}
              ${fact(
                text.averagePayment,
                `${money(data.averagePaymentNok)} NOK`
              )}
              ${fact(
                text.smallestPayment,
                `${money(data.smallestPaymentNok)} NOK`
              )}
              ${fact(
                text.largestPayment,
                `${money(data.largestPaymentNok)} NOK`
              )}
            </div>

            ${priceChange}
          </section>

          <section class="subscription-detail-section full">
            <h3>${escapeHtml(text.insights)}</h3>

            <div class="subscription-insight-list">
              ${insightHtml}
            </div>
          </section>

          <section class="subscription-detail-section full">
            <h3>${escapeHtml(text.history)}</h3>

            <div class="subscription-history-list">
              ${historyHtml}
            </div>

            ${history.some(item => item.currency !== "NOK")
              ? `<div class="muted" style="margin-top:12px;">
                   ${escapeHtml(text.approximateFx)}
                 </div>`
              : ""}
          </section>
        </div>

        <div class="subscription-modal-actions">
          <a class="btn btn-primary"
             href="${contextPath}/app/subscriptions">
            ${escapeHtml(text.manage)}
          </a>

          ${data.cancelUrl
            ? `<a class="btn"
                  href="${escapeHtml(data.cancelUrl)}"
                  target="_blank"
                  rel="noopener">
                 ${escapeHtml(text.cancelProvider)}
               </a>`
            : ""}

          <button class="btn"
                  type="button"
                  data-subscription-modal-close>
            ${escapeHtml(text.close)}
          </button>
        </div>
      </div>
    `;

    bindCloseButtons();

    const closeButton = contentNode().querySelector(
      ".subscription-modal-close"
    );

    if (closeButton) closeButton.focus();
  }

  function bindCloseButtons() {
    contentNode()
      .querySelectorAll("[data-subscription-modal-close]")
      .forEach(button =>
        button.addEventListener("click", close)
      );
  }

  document.addEventListener("click", event => {
    const trigger = event.target.closest(
      ".subscription-details-trigger[data-subscription-id]"
    );

    if (!trigger) return;

    if (event.target.closest(
      "a, button, form, input, select, textarea, label"
    )) {
      return;
    }

    event.preventDefault();
    open(trigger.dataset.subscriptionId, trigger);
  });

  document.addEventListener("keydown", event => {
    if (
      event.key === "Escape"
      && overlay?.classList.contains("open")
    ) {
      close();
      return;
    }

    const trigger = event.target.closest?.(
      ".subscription-details-trigger[data-subscription-id]"
    );

    if (
      trigger
      && (event.key === "Enter" || event.key === " ")
    ) {
      event.preventDefault();
      open(trigger.dataset.subscriptionId, trigger);
    }
  });
})();
