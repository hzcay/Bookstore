// =================== helpers ===================
function API() {
  return window.API_BASE || "/api/v1";
}
function api(path, opts) {
  return fetch(API() + path, opts);
}
async function jsonOrThrow(r) {
  if (!r.ok) throw new Error(await r.text());
  return r.json();
}
const $ = (s) => document.querySelector(s);
const $$ = (s) => document.querySelectorAll(s);

// optional fallback (nếu bạn đã có ở nơi khác thì ignore)
window.fmt ||= (n) => (n ?? 0).toLocaleString();
window.toast ||= (msg, ok = true) =>
  console[ok ? "log" : "warn"]("[toast]", msg);
window.toDateTime ||= (s) => {
  try {
    return new Date(s).toLocaleString();
  } catch {
    return s || "";
  }
};

// =================== PAGE: Cart (optimized, fixed) ===================
if (location.pathname.startsWith("/cart")) {
  const $ = (s, r = document) => r.querySelector(s);

  const table = $("#cartTable");
  const btnClear = $("#btnClear");
  const btnCheckout = document.querySelector('a[href="/checkout"]');

  // Khớp controller mới
  const CART_API = "/api/v1/cart";

  const toN = (x) => Number(x ?? 0) || 0;
  const money = (n) => toN(n).toLocaleString("vi-VN") + "₫";

  // state
  let items = []; // [{bookId,title,price,qty,thumbnail}]
  let selected = new Set(); // bookId
  const inflightQty = new Map(); // khoá từng item khi update

  loadCart();

  async function loadCart() {
    if (!table) return;
    table.innerHTML = "<div class='muted'>Loading…</div>";
    try {
      const res = await fetch(`${CART_API}`, { credentials: "include" });
      const data = await res.json();
      const raw = Array.isArray(data) ? data : data?.items ?? [];
      items = (Array.isArray(raw) ? raw : Object.values(raw || {})).map(
        (it) => ({
          bookId: it.bookId || it.id || it.code,
          title: it.title || "Unknown",
          price: toN(it.price),
          qty: toN(it.qty || it.quantity || 1),
          thumbnail: it.thumbnail || "/img/placeholder.png",
        })
      );
      renderCart();
    } catch (e) {
      table.innerHTML = `<div class='error'>${
        e?.message || "Load cart failed"
      }</div>`;
    }
  }

  function renderCart() {
    if (!items.length) {
      table.innerHTML = "<div class='muted'>Your cart is empty.</div>";
      btnCheckout?.classList.add("ghost");
      return;
    }

    const html = `
      <div class="table">
        <div class="thead row">
          <div class="sel-col"><input id="selAll" type="checkbox" class="sel"></div>
          <div class="col">Item</div>
          <div class="col center">Qty</div>
          <div class="col right">Price</div>
          <div class="col right">Subtotal</div>
        </div>
        ${items
          .map(
            (it) => `
          <div class="row item" data-id="${it.bookId}">
            <div class="sel-col">
              <input type="checkbox" class="sel" ${
                selected.has(String(it.bookId)) ? "checked" : ""
              }/>
            </div>
            <div class="col">
              <div class="row gap">
                <img src="${
                  it.thumbnail
                }" alt="" style="width:48px;height:48px;object-fit:cover;border-radius:8px">
                <div>
                  <a href="/book/${it.bookId}"><b>${it.title}</b></a>
                  <div class="muted">${it.bookId}</div>
                </div>
              </div>
            </div>
            <div class="col center">
              <div class="qty-ctrl">
                <button class="btn outline btn-dec" type="button">–</button>
                <input class="qty-input" type="number" min="1" step="1" value="${
                  it.qty
                }">
                <button class="btn outline btn-inc" type="button">+</button>
              </div>
            </div>
            <div class="col right price-cell">${money(it.price)}</div>
            <div class="col right subtotal-cell"><b id="sub-${
              it.bookId
            }">${money(it.qty * it.price)}</b></div>
          </div>
        `
          )
          .join("")}
      </div>
      <div id="cartActions" style="display:${
        selected.size ? "flex" : "none"
      }; gap:12px; align-items:center; justify-content:space-between;">
        <button class="btn outline" id="btnRemoveSel">Remove selected</button>
        <div class="row gap" style="align-items:center">
          <span class="muted">Selected total:</span>
          <span id="selTotal">0₫</span>
        </div>
      </div>
    `;
    table.innerHTML = html;

    const selAll = table.querySelector("#selAll");

    const syncSelAll = () => {
      const boxes = [...table.querySelectorAll(".row.item .sel")];
      const checked = boxes.filter((b) => b.checked).length;
      selAll.checked = checked === boxes.length && boxes.length > 0;
      selAll.indeterminate = checked > 0 && checked < boxes.length;
    };

    // select all
    selAll?.addEventListener("change", (e) => {
      if (e.target.checked)
        items.forEach((it) => selected.add(String(it.bookId)));
      else selected.clear();
      table
        .querySelectorAll(".row.item .sel")
        .forEach((c) => (c.checked = e.target.checked));
      recomputeSelected();
      syncSelAll();
    });

    // per row
    table.querySelectorAll(".row.item").forEach((row) => {
      const id = row.getAttribute("data-id");

      // tick
      row.querySelector(".sel")?.addEventListener("change", (e) => {
        e.target.checked
          ? selected.add(String(id))
          : selected.delete(String(id));
        recomputeSelected();
        syncSelAll();
      });

      // minus
      row
        .querySelector(".btn-dec")
        ?.addEventListener("click", () => stepQty(id, -1, row));
      // plus
      row
        .querySelector(".btn-inc")
        ?.addEventListener("click", () => stepQty(id, +1, row));
      // manual
      row.querySelector(".qty-input")?.addEventListener("change", (ev) => {
        const v = Math.max(1, toN(ev.target.value));
        ev.target.value = v;
        changeQty(id, v, row);
      });
    });

    $("#btnRemoveSel")?.addEventListener("click", removeSelected);

    recomputeSelected();
    syncSelAll();
  }

  function recomputeSelected() {
    const total = items
      .filter((it) => selected.has(String(it.bookId)))
      .reduce((s, it) => s + toN(it.qty) * toN(it.price), 0);

    $("#selTotal") && ($("#selTotal").textContent = money(total));

    const actions = $("#cartActions");
    if (actions) actions.style.display = selected.size ? "flex" : "none";

    if (btnCheckout) {
      const disabled = total <= 0;
      btnCheckout.classList.toggle("ghost", disabled);
      btnCheckout.setAttribute("aria-disabled", disabled ? "true" : "false");
      localStorage.setItem(
        "checkout_selected_ids",
        JSON.stringify([...selected])
      );
    }
  }

  function stepQty(bookId, delta, row) {
    const it = items.find((x) => String(x.bookId) === String(bookId));
    if (!it) return;
    const next = Math.max(1, toN(it.qty) + delta);
    changeQty(bookId, next, row);
  }

  async function changeQty(bookId, qty, row) {
    const key = String(bookId);
    if (inflightQty.get(key)) return;

    const it = items.find((x) => String(x.bookId) === key);
    if (!it) return;

    const prev = it.qty;
    it.qty = qty;

    // update UI ngay (optimistic)
    row.querySelector(".qty-input").value = qty;
    const subEl = document.getElementById(`sub-${bookId}`);
    if (subEl) subEl.textContent = money(qty * toN(it.price));
    recomputeSelected();

    // disable controls trong row, tránh spam
    row
      ?.querySelectorAll(".btn, input")
      .forEach((el) => el.setAttribute("disabled", ""));
    inflightQty.set(key, true);

    try {
      await updateQty(bookId, qty);
    } catch (e) {
      // revert nếu fail
      it.qty = prev;
      row.querySelector(".qty-input").value = prev;
      if (subEl) subEl.textContent = money(prev * toN(it.price));
      recomputeSelected();
      toast?.(e?.message || "Cập nhật số lượng thất bại", false);
    } finally {
      inflightQty.delete(key);
      row
        ?.querySelectorAll(".btn, input")
        .forEach((el) => el.removeAttribute("disabled"));
    }
  }

  async function removeSelected() {
    if (!selected.size) return toast?.("Chưa chọn món nào", false);

    const ids = [...selected].map(String);

    // optimistic: gỡ khỏi UI
    items = items.filter((it) => !selected.has(String(it.bookId)));
    selected.clear();
    renderCart();

    try {
      await removeItemsBulk(ids);
      toast?.("Đã xoá mục đã chọn");
    } catch (e) {
      toast?.(e?.message || "Xoá thất bại", false);
      await loadCart(); // đồng bộ lại với server nếu lỗi
    }
  }

  // ===== API helpers (khớp CartController đã gửi) =====
  function updateQty(bookId, qty) {
    // dùng JSON body cho ổn định
    return fetch(`${CART_API}/update`, {
      method: "POST",
      credentials: "include",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ bookId: String(bookId), qty: Number(qty) }),
    }).then(async (r) => {
      if (!r.ok)
        throw new Error((await r.text().catch(() => "")) || r.statusText);
    });
  }

  function removeItem(bookId) {
    return fetch(`${CART_API}/item/${encodeURIComponent(bookId)}`, {
      method: "DELETE",
      credentials: "include",
    }).then(async (r) => {
      if (!r.ok)
        throw new Error((await r.text().catch(() => "")) || r.statusText);
    });
  }

  function removeItemsBulk(ids) {
    return fetch(`${CART_API}/items`, {
      method: "DELETE",
      credentials: "include",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(ids),
    }).then(async (r) => {
      if (!r.ok)
        throw new Error((await r.text().catch(() => "")) || r.statusText);
    });
  }

  // Clear all
  btnClear?.addEventListener("click", () => {
    fetch(`${CART_API}/clear`, { method: "DELETE", credentials: "include" })
      .then(() => {
        toast?.("Cart cleared");
        selected.clear();
        loadCart();
      })
      .catch((e) => toast?.(e?.message || "Clear failed", false));
  });

  // Checkout chỉ các item đã tick
  btnCheckout?.addEventListener("click", (e) => {
    e.preventDefault();
    checkoutSelected().catch((err) =>
      toast?.(err.message || "Checkout fail", false)
    );
  });

  async function checkoutSelected() {
    const selectedIds = [...selected].map(String);
    if (!selectedIds.length) return toast?.("Chưa chọn món nào", false);

    const payload = {
      fullName: $("#fullName")?.value || "",
      email: $("#email")?.value || "",
      phone: $("#phone")?.value || "",
      shippingAddress: $("#shipAddress")?.value || "",
      promoCode: $("#promoCode")?.value || "",
      usePoints: Number($("#usePoints")?.value || 0),
      selectedIds,
    };

    const res = await fetch(`${CART_API}/checkout`, {
      method: "POST",
      credentials: "include",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });
    if (!res.ok) {
      const t = await res.text().catch(() => "");
      throw new Error(t || "Checkout failed");
    }
    const placed = await res.json();
    await loadCart();
    location.href = `/order/success?orderId=${encodeURIComponent(
      placed.orderId
    )}`;
  }
}

// =================== PAGE: Browse books (FULL) ===================
if (
  location.pathname === "/homePage" ||
  location.pathname === "/books" ||
  location.pathname === "/browse"
) {
  const $ = window.$ || ((s, r = document) => r.querySelector(s));

  const grid = $("#bookGrid");
  const form = $("#searchForm");
  const pager = $("#pager");
  const catSel = $("#filterCategory"); // <select name="categoryId" id="filterCategory">
  const pubSel = $("#filterPublisher"); // <select name="publisherId" id="filterPublisher"> (nếu có)
  const qInput = form?.querySelector('[name="q"]');
  let qHints = document.getElementById("qHints"); // <datalist id="qHints"> (tạo nếu chưa có)

  // nếu chưa có <datalist id="qHints"> thì tạo và gắn ngay sau input
  if (qInput && !qHints) {
    qHints = document.createElement("datalist");
    qHints.id = "qHints";
    qInput.setAttribute("list", "qHints");
    qInput.insertAdjacentElement("afterend", qHints);
  }

  const clean = (obj) =>
    Object.fromEntries(
      Object.entries(obj).filter(([, v]) => v != null && v !== "")
    );

  // ---------- Load categories vào dropdown ----------
  async function loadCategories() {
    if (!catSel) return;
    try {
      // BE: /api/v1/categories?status=1 (api() sẽ tự prepend /api/v1)
      const cats = await api(`/categories?status=1`).then(jsonOrThrow);
      const list = Array.isArray(cats) ? cats : cats?.content || [];
      catSel.innerHTML =
        `<option value="">All categories</option>` +
        list
          .map(
            (c) =>
              `<option value="${c.categoryId ?? c.categoryid}">${
                c.name
              }</option>`
          )
          .join("");

      // auto filter khi đổi category
      catSel.addEventListener("change", () => {
        const data = Object.fromEntries(new FormData(form).entries());
        loadBooks({ ...data, page: 0 });
      });
    } catch (e) {
      console.warn("[categories] load fail:", e);
      catSel.innerHTML = `<option value="">All categories</option>`;
    }
  }

  // ---------- (NEW) Load publishers (NXB) ----------
  async function loadPublishers() {
    if (!pubSel) return;
    try {
      const pubs = await api(`/publishers?status=1`).then(jsonOrThrow);
      const list = Array.isArray(pubs) ? pubs : pubs?.content || [];
      pubSel.innerHTML =
        `<option value="">All publishers</option>` +
        list
          .map(
            (p) =>
              `<option value="${p.publisherId ?? p.publisherid}">${
                p.name
              }</option>`
          )
          .join("");

      pubSel.addEventListener("change", () => {
        const data = Object.fromEntries(new FormData(form).entries());
        loadBooks({ ...data, page: 0 });
      });
    } catch (e) {
      console.warn("[publishers] load fail:", e);
      pubSel.innerHTML = `<option value="">All publishers</option>`;
    }
  }

  // ---------- Load books (support q, min/max, categoryId, publisherId, sort, page/size) ----------
  async function loadBooks(params = {}) {
    if (!grid) return;
    grid.innerHTML = "<div class='muted'>Loading…</div>";
    if (pager) pager.innerHTML = "";

    // Map sortBy/sortDir -> sort (Spring Data style), nhưng vẫn giữ sortBy/sortDir để BE cũ không lỗi
    const { sortBy, sortDir, ...rest } = params;
    const sort = sortBy ? `${sortBy},${sortDir || "asc"}` : undefined;

    const p = {
      page: 0,
      size: 20,
      ...clean({ ...rest, sortBy, sortDir, sort }),
    };

    const hasFilter = [
      "q",
      "title",
      "categoryId",
      "authorId",
      "publisherId",
      "minPrice",
      "maxPrice",
      "sort",
      "sortBy",
      "sortDir",
    ].some((k) => p[k] !== undefined);

    const path = hasFilter ? "/books/search" : "/books";
    const qs = new URLSearchParams(p).toString();

    try {
      const page = await api(`${path}?${qs}`).then(jsonOrThrow);
      const items = page?.content ?? [];

      grid.innerHTML = items.length
        ? items
            .map(
              (b) => `
          <article class="card book" data-id="${b.bookId}">
            <div class="thumb">
              <img src="${b.thumbnail || "/img/placeholder.png"}" alt="${
                b.title
              }"/>
            </div>

            <h3 class="title">
              <a class="title-link" href="/book/${b.bookId}">${b.title}</a>
            </h3>

            <div class="muted">
              ${b.categoryName || "Uncategorized"} · ${
                b.publisherName || b.publisher?.name || "—"
              }
            </div>
            <div><b>${fmt(b.salePrice)}</b>₫</div>

            <a class="btn detail-cta" href="/book/${b.bookId}">Xem chi tiết</a>
          </article>
        `
            )
            .join("")
        : "<div class='muted'>No books found</div>";

      if (pager && page?.totalPages > 1) {
        pager.innerHTML = `
          <button class="btn outline" ${
            page.first ? "disabled" : ""
          } id="pgPrev">← Prev</button>
          <span class="muted" style="padding:8px">Page ${page.number + 1} / ${
          page.totalPages
        }</span>
          <button class="btn outline" ${
            page.last ? "disabled" : ""
          } id="pgNext">Next →</button>
        `;
        $("#pgPrev")?.addEventListener("click", () =>
          loadBooks({ ...p, page: Math.max(0, page.number - 1) })
        );
        $("#pgNext")?.addEventListener("click", () =>
          loadBooks({
            ...p,
            page: Math.min(page.totalPages - 1, page.number + 1),
          })
        );
      }
    } catch (e) {
      grid.innerHTML = `<div class='error'>${e.message || e}</div>`;
    }
  }

  // ---------- (NEW) Gợi ý trong ô search bằng <datalist> ----------
  function debounce(fn, ms = 200) {
    let t;
    return (...args) => {
      clearTimeout(t);
      t = setTimeout(() => fn(...args), ms);
    };
  }

  async function fetchHints(q) {
    if (!q || q.trim().length < 2) return [];
    try {
      // Nếu có endpoint suggest riêng ở BE:
      // const raw = await api(`/books/suggest?${new URLSearchParams({ q, size: 8 })}`).then(jsonOrThrow);
      // return Array.isArray(raw) ? raw.map(x => x.title ?? x) : [];

      // Fallback: dùng search size nhỏ -> rút title/author/publisher để hiển thị gợi ý
      const page = await api(
        `/books/search?${new URLSearchParams({
          q,
          size: 8,
          page: 0,
          sort: "relevance,desc",
        })}`
      )
        .then(jsonOrThrow)
        .catch(() => null);

      const items = page?.content || [];
      const pool = [];
      for (const b of items) {
        if (b.title) pool.push(b.title);
        if (b.authorName || b.author?.name)
          pool.push(b.authorName || b.author?.name);
        if (b.publisherName || b.publisher?.name)
          pool.push(b.publisherName || b.publisher?.name);
      }
      // dedupe + ưu tiên chuỗi có chứa q
      const seen = new Set();
      return pool
        .filter(Boolean)
        .map(String)
        .filter((x) => {
          const k = x.toLowerCase();
          if (seen.has(k)) return false;
          seen.add(k);
          return true;
        })
        .sort(
          (a, b) =>
            a.toLowerCase().indexOf(q.toLowerCase()) -
            b.toLowerCase().indexOf(q.toLowerCase())
        )
        .slice(0, 10);
    } catch {
      return [];
    }
  }

  function renderHints(list) {
    if (!qHints) return;
    qHints.innerHTML = (list || [])
      .map(
        (v) =>
          `<option value="${String(v)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;")}"></option>`
      )
      .join("");
  }

  const updateHints = debounce(async () => {
    const q = qInput?.value?.trim();
    const hints = await fetchHints(q);
    renderHints(hints);
  }, 160);

  // ---------- Search input: bật type="search", clear, và gợi ý ----------
  if (qInput) {
    try {
      if (qInput.type !== "search") qInput.type = "search";
    } catch {}

    // Gợi ý khi gõ/focus
    qInput.addEventListener("input", updateHints);
    qInput.addEventListener("focus", updateHints);

    // Bấm nút ✕ (native) → clear & load full
    qInput.addEventListener("search", () => {
      if (!qInput.value) {
        form.reset();
        const cat = form.querySelector('[name="categoryId"]');
        const pub = form.querySelector('[name="publisherId"]');
        if (cat) cat.value = "";
        if (pub) pub.value = "";
        renderHints([]);
        loadBooks(); // full list
      }
    });

    // Xoá hết bằng phím → auto về full (delay nhẹ tránh giật)
    qInput.addEventListener("input", () => {
      if (qInput.value.trim() === "") {
        clearTimeout(qInput._t);
        qInput._t = setTimeout(() => {
          if (qInput.value.trim() === "") {
            form.reset();
            const cat = form.querySelector('[name="categoryId"]');
            const pub = form.querySelector('[name="publisherId"]');
            if (cat) cat.value = "";
            if (pub) pub.value = "";
            renderHints([]);
            loadBooks();
          }
        }, 150);
      }
    });
  }

  // ---------- Init ----------
  loadCategories(); // fill dropdown
  loadPublishers(); // fill publisher (nếu có select)
  loadBooks(); // first page

  // ---------- Submit search ----------
  form?.addEventListener("submit", (e) => {
    e.preventDefault();
    const data = Object.fromEntries(new FormData(form).entries());
    loadBooks({ ...data, page: 0 });
  });
}

// =================== PAGE: Checkout ===================
if (location.pathname.startsWith("/checkout")) {
  const $ = (s, r = document) => r.querySelector(s);
  const $$ = (s, r = document) => r.querySelectorAll(s);

  const form = $("#checkoutForm");
  const out = $("#checkoutResult");
  const streetInput = $("#shippingAddress");
  const datalist = $("#address-suggestions");
  const mapEl = $("#map");

  // --- Elements for promotion/points/summary ---
  const inpPromo = $("#promoCode");
  const btnPickPromo = $("#pickPromoBtn");
  const popPromo = $("#promoPopover");
  const listPromo = $("#promoList");
  const inpPoints = $("#usePoints");

  const lblSubtotal = $("#sumSubtotal");
  const lblPromo = $("#sumPromo");
  const lblPoints = $("#sumPoints");
  const lblDiscount = $("#sumDiscount");
  const lblShip = $("#sumShip");
  const lblTotal = $("#sumTotal");

  // --- Helpers ---
  const fmt = (n) => Number(n || 0).toLocaleString("vi-VN") + "₫";
  const VND = (n) => Math.max(0, Math.round(Number(n || 0)));

  // FE rules (demo) — BE vẫn tính lại khi submit
  const POINTS_RATE = 1000; // 1 điểm = 1.000đ
  const FREESHIP_THRESHOLD = 300_000;
  const SHIP_FEE = 25_000;

  let cartSubtotal = 0; // lấy từ /cart
  let promoDiscount = 0; // validate từ /promotions/validate
  let pointsDiscount = 0;

  // ---------- Submit form + confirm + redirect ----------
  if (form && out) {
    form.addEventListener("submit", async (e) => {
      e.preventDefault();

      // hỏi xác nhận
      const ok = window.confirm("Xác nhận đặt hàng với thông tin hiện tại?");
      if (!ok) {
        out.classList.remove("error", "success");
        out.textContent = "Đã huỷ thao tác đặt hàng.";
        toast?.("Đã huỷ đặt hàng", false);
        return;
      }

      const btn = form.querySelector('button[type="submit"]');
      out.classList.remove("error", "success");
      out.textContent = "Placing order…";

      // gom payload
      const body = Object.fromEntries(new FormData(form).entries());
      body.usePoints = body.usePoints ? Number(body.usePoints) : 0;
      body.promoCode = body.promoCode?.trim() || null;
      body.paymentMethod =
        form.querySelector('input[name="paymentMethod"]:checked')?.value ||
        "COD";

      // chặn double click
      btn?.setAttribute("data-loading", "1");
      btn?.setAttribute("disabled", "disabled");

      try {
        const res = await fetch(`${API()}/orders/guest-checkout`, {
          method: "POST",
          credentials: "include",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(body),
        }).then(jsonOrThrow);

        out.classList.add("success");
        out.textContent =
          typeof res === "string" ? res : JSON.stringify(res, null, 2);

        const orderId = res?.orderId;

        if (orderId) {
          localStorage.setItem("lastOrderId", orderId);
          toast?.("Đặt hàng thành công ✓");

          // popup nhẹ cho user thấy mã đơn
          alert(`Đặt hàng thành công!\nMã đơn: ${orderId}`);
          const ordersUrl = `/track?highlight=${encodeURIComponent(orderId)}`;
          // dùng assign để thêm vào history (back vẫn về checkout được)
          window.location.assign(ordersUrl);
        } else {
          toast?.("Đặt hàng thành công ✓");
          window.location.assign("/track");
        }
      } catch (err) {
        out.classList.add("error");
        out.textContent = err?.message || String(err);
        toast?.("Checkout failed", false);
      } finally {
        btn?.removeAttribute("data-loading");
        btn?.removeAttribute("disabled");
      }
    });
  }

  // ---------- Summary rendering ----------
  function recompute() {
    // Recompute discounts at FE (chỉ để hiển thị)
    const usePts = Math.max(0, Number(inpPoints?.value || 0));
    pointsDiscount = usePts * POINTS_RATE;

    const totalDiscount = Math.min(
      cartSubtotal,
      promoDiscount + pointsDiscount
    );
    const ship = cartSubtotal >= FREESHIP_THRESHOLD ? 0 : SHIP_FEE;
    const total = Math.max(0, cartSubtotal - totalDiscount + ship);

    lblSubtotal && (lblSubtotal.textContent = fmt(cartSubtotal));
    lblPromo && (lblPromo.textContent = fmt(promoDiscount));
    lblPoints && (lblPoints.textContent = fmt(pointsDiscount));
    lblDiscount && (lblDiscount.textContent = fmt(totalDiscount));
    lblShip && (lblShip.textContent = fmt(ship));
    lblTotal && (lblTotal.textContent = fmt(total));
  }

  // ---------- Load subtotal from /cart ----------
  async function fetchCartSubtotal() {
    try {
      const res = await fetch(`${API()}/cart`, { credentials: "include" }).then(
        jsonOrThrow
      );
      const raw = Array.isArray(res) ? res : res?.items ?? [];
      const items = Array.isArray(raw) ? raw : Object.values(raw || {});
      cartSubtotal = items.reduce(
        (s, it) => s + VND(it.qty) * VND(it.price),
        0
      );
    } catch {
      cartSubtotal = 0;
    } finally {
      recompute();
      const currentCode = (inpPromo?.value || "").trim();
      if (currentCode) applyPromo(currentCode);
    }
  }

  // ---------- Promotion popup ----------
  function openPromoPopover() {
    if (!popPromo) return;
    popPromo.style.display = "block";
    // đóng khi click ra ngoài
    const onDocClick = (ev) => {
      if (!popPromo.contains(ev.target) && ev.target !== btnPickPromo) {
        popPromo.style.display = "none";
        document.removeEventListener("click", onDocClick);
      }
    };
    setTimeout(() => document.addEventListener("click", onDocClick), 0);
  }

  async function loadActivePromos() {
    if (!listPromo) return;
    listPromo.innerHTML = `<div class="muted" style="padding:8px">Đang tải…</div>`;
    try {
      const promos = await fetch(
        `${API()}/promotions/active?subtotal=${encodeURIComponent(
          cartSubtotal
        )}`
      ).then(jsonOrThrow);

      if (!Array.isArray(promos) || promos.length === 0) {
        listPromo.innerHTML = `<div class="muted" style="padding:8px">Chưa có mã khả dụng</div>`;
        return;
      }

      listPromo.innerHTML = promos
        .map(
          (p) => `
        <div class="row" style="justify-content:space-between; gap:8px; padding:8px 10px; border-bottom:1px solid var(--line);">
          <div>
            <div><b>${p.code}</b> — giảm ${fmt(p.discount)}</div>
            <div class="muted" style="font-size:.9em">
              ĐH tối thiểu: ${fmt(p.minValue || 0)}
              ${
                p.expireDate
                  ? ` · HSD: ${new Date(p.expireDate).toLocaleString()}`
                  : ""
              }
            </div>
          </div>
          <button class="btn outline btn-apply" data-code="${
            p.code
          }">Áp dụng</button>
        </div>`
        )
        .join("");

      btnPickPromo?.addEventListener("click", async () => {
        openPromoPopover();
        await loadActivePromos();
      });

      // click chọn mã
      listPromo?.addEventListener("click", async (e) => {
        e.preventDefault();
        const btn = e.target.closest(".btn-apply");
        const row = e.target.closest(".row");
        const code =
          btn?.dataset.code || row?.querySelector(".btn-apply")?.dataset.code;
        if (!code) return;
        await applyPromo(code);
        if (popPromo) popPromo.style.display = "none";
      });
    } catch (e) {
      listPromo.innerHTML = `<div class="error" style="padding:8px">${
        e?.message || "Không tải được danh sách mã"
      }</div>`;
    }
  }

  async function applyPromo(code) {
    try {
      // gọi validate
      const qs = new URLSearchParams({
        code: code || "",
        subtotal: String(cartSubtotal),
      });
      const r = await fetch(`${API()}/promotions/validate?${qs}`).then(
        jsonOrThrow
      );
      // r: { code, discount, minValue, valid: true/false, message? }
      if (r?.valid === false) {
        promoDiscount = 0;
        toast?.(r?.message || "Mã không hợp lệ", false);
      } else {
        promoDiscount = Number(r?.discount || 0);
        if (inpPromo) inpPromo.value = r?.code || code;
        toast?.("Đã áp dụng mã ✓");
      }
    } catch (e) {
      promoDiscount = 0;
      toast?.(e?.message || "Áp dụng mã thất bại", false);
    } finally {
      recompute();
    }
  }

  btnPickPromo?.addEventListener("click", async () => {
    openPromoPopover();
    await loadActivePromos();
  });

  // Khi gõ mã thủ công → validate “nhẹ” ở FE (optional: blur để đỡ spam)
  let tPromo;
  inpPromo?.addEventListener("blur", async () => {
    const code = (inpPromo.value || "").trim();
    if (!code) {
      promoDiscount = 0;
      recompute();
      return;
    }
    clearTimeout(tPromo);
    tPromo = setTimeout(() => applyPromo(code), 0);
  });

  // Điểm dùng → render ngay
  inpPoints?.addEventListener("input", () => {
    recompute();
  });

  // ---------- Leaflet + Autocomplete (Nominatim) ----------
  const DEFAULT = { lat: 10.762622, lng: 106.660172 }; // HCM
  const COUNTRY = "vn";
  const HEADERS = { "Accept-Language": "vi,en" }; // không set User-Agent trong browser
  const debounce = (fn, ms = 300) => {
    let t;
    return (...a) => {
      clearTimeout(t);
      t = setTimeout(() => fn(...a), ms);
    };
  };
  const escapeHtml = (s) =>
    s.replace(
      /[&<>"']/g,
      (c) =>
        ({
          "&": "&amp;",
          "<": "&lt;",
          ">": "&gt;",
          '"': "&quot;",
          "'": "&#39;",
        }[c])
    );

  let map, marker;

  (function initLeaflet() {
    if (!mapEl || !streetInput)
      return leafErr("Thiếu #map hoặc #shippingAddress");
    if (!window.L)
      return document.addEventListener("DOMContentLoaded", initLeaflet);

    map = L.map("map").setView([DEFAULT.lat, DEFAULT.lng], 15);
    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
      maxZoom: 19,
      attribution: "&copy; OpenStreetMap contributors",
    }).addTo(map);

    marker = L.marker([DEFAULT.lat, DEFAULT.lng], { draggable: true }).addTo(
      map
    );

    // Kéo marker → reverse fill
    marker.on("dragend", () => {
      const { lat, lng } = marker.getLatLng();
      reverseGeocode(lat, lng).then((addr) => {
        if (addr) streetInput.value = addr;
      });
    });

    // Gõ chữ → gợi ý theo viewport hiện tại
    streetInput.addEventListener(
      "input",
      debounce(async () => {
        if (!datalist) return;
        const q = streetInput.value.trim();
        if (q.length < 3) {
          datalist.innerHTML = "";
          return;
        }

        const b = map.getBounds();
        const viewbox = [b.getWest(), b.getNorth(), b.getEast(), b.getSouth()]
          .map((n) => n.toFixed(6))
          .join(",");

        const results = await nominatimSearch(q, 8, viewbox);
        datalist.innerHTML = results
          .map(
            (r) =>
              `<option value="${escapeHtml(r.display_name)}" data-lat="${
                r.lat
              }" data-lon="${r.lon}"></option>`
          )
          .join("");
      }, 300)
    );

    // Chọn gợi ý (hoặc free-text → geocode)
    streetInput.addEventListener("change", async () => {
      if (!datalist) return;
      const opt = [...datalist.options].find(
        (o) => o.value === streetInput.value
      );
      const lat = opt?.getAttribute("data-lat");
      const lon = opt?.getAttribute("data-lon");

      if (lat && lon) {
        const la = +lat,
          lo = +lon;
        marker.setLatLng([la, lo]);
        map.setView([la, lo], 18);
      } else {
        const hit = await geocode(streetInput.value);
        if (hit) {
          marker.setLatLng([+hit.lat, +hit.lon]);
          map.setView([+hit.lat, +hit.lon], 18);
        }
      }
    });
  })();

  function leafErr(msg) {
    console.error("[LEAFLET] error:", msg);
    if (mapEl) {
      mapEl.innerHTML = `<div class="error" style="padding:16px;text-align:center;">
        Không thể tải bản đồ. Vui lòng nhập địa chỉ thủ công.
      </div>`;
      mapEl.style.height = "auto";
    }
  }

  async function nominatimSearch(q, limit = 8, viewbox /*W,N,E,S*/) {
    try {
      const params = new URLSearchParams({
        format: "jsonv2",
        q,
        addressdetails: "1",
        countrycodes: COUNTRY,
        limit: String(limit),
      });
      if (viewbox) {
        params.set("viewbox", viewbox);
        params.set("bounded", "1");
      }

      const url = `https://nominatim.openstreetmap.org/search?${params.toString()}`;
      const res = await fetch(url, { headers: HEADERS });
      if (!res.ok) throw new Error(`Nominatim ${res.status}`);
      return await res.json();
    } catch (e) {
      console.warn("[SEARCH] fail:", e);
      return [];
    }
  }

  async function geocode(q) {
    try {
      const url = `https://nominatim.openstreetmap.org/search?format=jsonv2&addressdetails=1&countrycodes=${COUNTRY}&limit=1&q=${encodeURIComponent(
        q
      )}`;
      const res = await fetch(url, { headers: HEADERS });
      const data = await res.json();
      return data?.[0] || null;
    } catch {
      return null;
    }
  }

  async function reverseGeocode(lat, lon) {
    try {
      const url = `https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=${lat}&lon=${lon}`;
      const res = await fetch(url, { headers: HEADERS });
      const data = await res.json();
      return data?.display_name || null;
    } catch {
      return null;
    }
  }

  // Init data
  fetchCartSubtotal();
}

// =================== PAGE: My Orders (optimized) ===================
if (location.pathname.startsWith("/track")) {
  const $ = (s, r = document) => r.querySelector(s);
  const box = $("#myOrdersBox");

  // filter controls
  const frmFilter = $("#orderFilter");
  const inpFrom = $("#fromDate");
  const inpTo = $("#toDate");
  const selStatus = $("#statusSel");
  const btnReset = $("#btnResetFilter");

  // segmented controls
  const modeRadios = frmFilter?.querySelectorAll('input[name="mode"]') || [];
  const modeRange = frmFilter?.querySelector(".mode-range");
  const modeMonth = frmFilter?.querySelector(".mode-month");
  const modeYear = frmFilter?.querySelector(".mode-year");
  const monthPick = frmFilter?.querySelector("#monthPick");
  const yearPick = frmFilter?.querySelector("#yearPick");

  // state
  let allOrders = []; // raw data
  let wiredStatic = false;
  let pendingFilter = 0; // rAF debounce token

  // money utils
  const VND = (n) => Math.max(0, Math.round(Number(n || 0)));
  const fmt = (n) => Number(n || 0).toLocaleString("vi-VN") + "₫";

  // misc utils
  const toDT = (x) =>
    typeof toDateTime === "function" ? toDateTime(x) : x || "";
  const esc = (s = "") =>
    String(s).replace(
      /[&<>"']/g,
      (c) =>
        ({
          "&": "&amp;",
          "<": "&lt;",
          ">": "&gt;",
          '"': "&quot;",
          "'": "&#39;",
        }[c])
    );

  // date helpers
  const dateISO = (d) => d.toISOString().slice(0, 10);
  const firstDayOfMonth = (y, m) => new Date(y, m - 1, 1);
  const lastDayOfMonth = (y, m) => new Date(y, m, 0);
  const firstDayOfYear = (y) => new Date(y, 0, 1);
  const lastDayOfYear = (y) => new Date(y, 11, 31);

  // timestamps
  const getCreatedTsRaw = (o) => {
    const raw =
      o.createAt || o.createdAt || o.created || o.create_at || o.orderDate;
    const t = raw ? new Date(raw).getTime() : NaN;
    return isNaN(t) ? null : t;
  };

  // status helpers
  const isCancellable = (st) => {
    st = (st || "").toUpperCase();
    return st === "PENDING" || st === "CONFIRMED";
  };

  // ===== number extraction & subtotal fallback =====
  function num(...vals) {
    for (const v of vals) {
      const n = Number(v);
      if (Number.isFinite(n) && n >= 0) return n;
    }
    return 0;
  }
  function calcSubtotalFromItems(items) {
    if (!Array.isArray(items)) return 0;
    return items.reduce((s, it) => {
      const qty = Number(it.quantity ?? it.qty ?? 1);
      const price = Number(
        it.price ?? it.unitPrice ?? it.book?.salePrice ?? it.book?.price ?? 0
      );
      return s + Math.max(0, Math.round(qty * price));
    }, 0);
  }

  // ========= init =========
  init();

  async function init() {
    try {
      box.innerHTML = `<div class="muted">Đang kiểm tra đăng nhập…</div>`;

      // nếu app có endpoint me
      let userId = null;
      try {
        const me = await fetch(`${API()}/auth/me`, {
          credentials: "include",
        }).then(jsonOrThrow);
        userId = me?.customerId || me?.id || null;
      } catch {
        // ignore, có thể trang track cho guest => sẽ thử fetch kiểu khác
      }

      // nếu không có userId, vẫn cho fetch theo các endpoint "mine"/query param
      box.innerHTML = `<div class="muted">Đang tải đơn hàng…</div>`;
      const orders = await fetchMyOrders(userId);
      allOrders = (Array.isArray(orders) ? orders : []).map((o) => ({
        ...o,
        _ts: getCreatedTsRaw(o),
      }));

      initDefaultRange30d();
      wireStaticOnce();

      if (!allOrders.length) {
        box.innerHTML = `<div class="muted">Bạn chưa có đơn hàng nào.</div>`;
        return;
      }

      renderOrders(applyFilter());
      wireDynamic();

      // highlight đơn nếu có ?highlight=
      highlightFromQuery();
    } catch (e) {
      box.innerHTML = `<div class="error">${
        e?.message || "Không tải được danh sách đơn hàng"
      }</div>`;
    }
  }

  // thử lần lượt các endpoint phổ biến
  async function fetchMyOrders(customerId) {
    const urls = [
      `${API()}/orders/mine`,
      `${API()}/orders/my`,
      customerId
        ? `${API()}/orders?customerId=${encodeURIComponent(customerId)}`
        : null,
    ].filter(Boolean);

    for (const u of urls) {
      try {
        const res = await fetch(u, { credentials: "include" }).then(
          jsonOrThrow
        );
        const list = Array.isArray(res) ? res : res?.content || [];
        if (Array.isArray(list)) return list;
      } catch {}
    }
    return [];
  }

  function initDefaultRange30d() {
    if (!inpFrom || !inpTo) return;
    const today = new Date();
    const from = new Date();
    from.setDate(today.getDate() - 30);
    inpFrom.value = dateISO(from);
    inpTo.value = dateISO(today);
  }

  // ============== wire once (submit/reset/chips/mode) ==============
  function wireStaticOnce() {
    if (wiredStatic || !frmFilter) return;
    wiredStatic = true;

    // submit (validate + debounce render)
    frmFilter.addEventListener("submit", (e) => {
      e.preventDefault();
      const activeMode =
        [...(modeRadios || [])].find((r) => r.checked)?.value || "range";

      if (activeMode === "range") {
        if (!inpFrom?.value || !inpTo?.value)
          return alert("Điền đủ Từ ngày / Đến ngày nha!");
        if (inpFrom.value > inpTo.value)
          return alert("Ngày bắt đầu phải ≤ ngày kết thúc.");
      } else if (activeMode === "month") {
        if (!monthPick?.value) return alert("Chọn tháng nè!");
      } else if (activeMode === "year") {
        const y = parseInt(yearPick?.value || "", 10);
        if (!y || y < 2000 || y > 2100)
          return alert("Chọn năm hợp lệ (2000–2100).");
      }
      debouncedFilter();
    });

    // reset
    btnReset?.addEventListener("click", () => {
      const r = frmFilter.querySelector('input[name="mode"][value="range"]');
      if (r) r.checked = true;
      showMode("range");
      if (inpFrom) inpFrom.value = "";
      if (inpTo) inpTo.value = "";
      if (monthPick) monthPick.value = "";
      if (yearPick) yearPick.value = "";
      if (selStatus) selStatus.value = "";
      frmFilter
        .querySelectorAll(".chip")
        .forEach((x) => x.classList.remove("active"));
      initDefaultRange30d();
      debouncedFilter();
    });

    // quick chips
    const chips = frmFilter.querySelectorAll(".chip");
    chips.forEach((ch) => {
      ch.addEventListener("click", () => {
        const r = frmFilter.querySelector('input[name="mode"][value="range"]');
        if (r) r.checked = true;
        showMode("range");

        chips.forEach((x) => x.classList.remove("active"));
        ch.classList.add("active");

        const days = Number(ch.getAttribute("data-range")) || 7;
        const end = new Date();
        const start = new Date();
        start.setDate(end.getDate() - (days - 1));
        const f = (d) =>
          `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(
            2,
            "0"
          )}-${String(d.getDate()).padStart(2, "0")}`;
        if (inpFrom) inpFrom.value = f(start);
        if (inpTo) inpTo.value = f(end);
        debouncedFilter();
      });
    });

    // mode switch + month/year sync
    modeRadios.forEach((r) =>
      r.addEventListener("change", (e) => {
        const val = e.target.value;
        showMode(val);
        if (val === "month" && monthPick?.value) {
          applyMonth(monthPick.value);
        } else if (val === "year" && yearPick?.value) {
          applyYear(parseInt(yearPick.value, 10));
        }
        debouncedFilter();
      })
    );
    monthPick?.addEventListener("change", (e) => {
      applyMonth(e.target.value);
      debouncedFilter();
    });
    yearPick?.addEventListener("input", (e) => {
      applyYear(parseInt(e.target.value, 10));
      debouncedFilter();
    });
  }

  // ============== dynamic (rebind sau mỗi render) ==============
  function wireDynamic() {
    // toggle
    box.querySelectorAll(".btn-toggle").forEach((btn) => {
      btn.addEventListener("click", () => {
        const panel = btn
          .closest(".track-card")
          ?.querySelector(".order-detail");
        const open = panel.getAttribute("data-open") === "1";
        panel.style.display = open ? "none" : "block";
        panel.setAttribute("data-open", open ? "0" : "1");
        btn.textContent = open ? "Xem chi tiết" : "Thu gọn";
      });
    });

    // cancel
    box.querySelectorAll(".btn-cancel").forEach((btn) => {
      btn.addEventListener("click", async () => {
        const id = btn.getAttribute("data-id");
        if (!id) return;
        if (!confirm(`Bạn chắc muốn hủy đơn #${id}?`)) return;

        btn.disabled = true;
        btn.textContent = "Đang hủy…";
        try {
          await cancelOrder(id);
          const o = allOrders.find(
            (x) => String(x.orderId || x.id) === String(id)
          );
          if (o) o.status = "CANCELED";
          debouncedFilter(true); // re-render ngay
          toast?.("Đã hủy đơn hàng ✓");
        } catch (e) {
          toast?.(e?.message || "Hủy đơn thất bại", false);
          btn.disabled = false;
          btn.textContent = "Hủy đơn";
        }
      });
    });
  }

  // ============== filter & render ==============
  function applyFilter() {
    const f = inpFrom?.value
      ? new Date(inpFrom.value + "T00:00:00").getTime()
      : null;
    const t = inpTo?.value
      ? new Date(inpTo.value + "T23:59:59").getTime()
      : null;
    const st = (selStatus?.value || "").toUpperCase();

    return allOrders.filter((o) => {
      const ts = o._ts;
      if (f && (ts == null || ts < f)) return false;
      if (t && (ts == null || ts > t)) return false;
      if (st && String(o.status || "").toUpperCase() !== st) return false;
      return true;
    });
  }

  function renderOrders(list) {
    box.innerHTML =
      list.map((o) => renderCard(o)).join("") ||
      `<div class="muted">Không có đơn phù hợp.</div>`;
  }

  function debouncedFilter(force = false) {
    if (pendingFilter) cancelAnimationFrame(pendingFilter);
    pendingFilter = requestAnimationFrame(() => {
      pendingFilter = 0;
      const result = applyFilter();
      renderOrders(result);
      wireDynamic();
    });
    if (force) {
      if (pendingFilter) cancelAnimationFrame(pendingFilter);
      pendingFilter = 0;
      const result = applyFilter();
      renderOrders(result);
      wireDynamic();
    }
  }

  // ============== cancel endpoints (thử nhiều kiểu) ==============
  async function cancelOrder(orderId) {
    const headers = {
      Accept: "application/json",
      ...(typeof csrfHeaders === "function" ? csrfHeaders() : {}),
    };
    const tries = [
      {
        url: `${API()}/orders/${orderId}`,
        opts: {
          method: "PATCH",
          headers: { ...headers, "Content-Type": "application/json" },
          body: JSON.stringify({ status: "CANCELED" }),
        },
      },
      {
        url: `${API()}/orders/${orderId}/cancel`,
        opts: { method: "POST", headers },
      },
      {
        url: `${API()}/orders/${orderId}/cancel`,
        opts: { method: "PUT", headers },
      },
      {
        url: `${API()}/orders/${orderId}/status?value=CANCELED`,
        opts: { method: "POST", headers },
      },
    ];
    let lastErr;
    for (const { url, opts } of tries) {
      try {
        const r = await fetch(url, opts);
        if (r.ok) return true;
        lastErr = new Error(
          (await r.text().catch(() => r.statusText)) || `HTTP ${r.status}`
        );
      } catch (e) {
        lastErr = e;
      }
    }
    throw (
      lastErr || new Error("Không thể hủy đơn – không có endpoint phù hợp.")
    );
  }

  // ============== card renderer (robust fields) ==============
  function renderCard(o) {
    const id = o.orderId || o.id || "-";
    const status = (o.status || "").toUpperCase();
    const created = toDT(
      o.createAt || o.createdAt || o.created || o.create_at || o.orderDate
    );
    const updated = toDT(o.updateAt || o.updatedAt || o.updated || o.update_at);
    const addr = o.shippingAddress || o.address || "";
    const method = o.paymentMethod || "COD";

    const items = Array.isArray(o.items)
      ? o.items
      : Array.isArray(o.orderItems)
      ? o.orderItems
      : [];

    // numbers: derive safely
    const subtotalDerived = calcSubtotalFromItems(items);

    const subtotal = VND(
      num(
        o.subtotal,
        o.subTotal,
        o.itemsSubtotal,
        o.amountBeforeDiscount,
        subtotalDerived
      )
    );
    const discount = VND(
      num(o.discount, o.totalDiscount, o.promoDiscount, o.pointsDiscount, 0)
    );
    const shipFee = VND(
      num(o.shippingFee, o.shipFee, o.deliveryFee, o.transportFee, 0)
    );
    const total = VND(
      num(o.total, o.orderTotal, subtotal - discount + shipFee)
    );

    const badgeClass = (() => {
      switch (status) {
        case "DELIVERED":
          return "green";
        case "CANCELED":
          return "red";
        case "PROCESSING":
        case "SHIPPING":
          return "blue";
        case "PENDING":
        default:
          return "gray";
      }
    })();

    const lines = items
      .map((it) => {
        const title =
          it.title || it.bookTitle || it.book?.title || "(Không rõ)";
        const qty = it.quantity ?? it.qty ?? 1;
        const unit = VND(
          num(it.price, it.unitPrice, it.book?.salePrice, it.book?.price, 0)
        );
        const line = VND(unit * qty);
        return `
        <tr class="row space" style="gap:12px; padding:8px 0; border-bottom:1px dashed #e8eaf1;">
          <td class="col"><b>${esc(title)}</b></td>
          <td class="col center" style="max-width:90px">${qty}</td>
          <td class="col right"  style="max-width:140px">${fmt(unit)}</td>
          <td class="col right"  style="max-width:160px"><b>${fmt(
            line
          )}</b></td>
        </tr>`;
      })
      .join("");

    return `
      <div class="track-card" id="order-${esc(id)}" style="margin-bottom:12px">
        <div class="row space">
          <div>
            <div class="row gap" style="align-items:baseline">
              <h3 style="margin:0">Order #${esc(id)}</h3>
              <span class="badge ${badgeClass}">${status || "UNKNOWN"}</span>
            </div>
            <div class="muted">Tạo: ${created || "-"}${
      updated ? " · Cập nhật: " + updated : ""
    }</div>
            <div class="muted">Thanh toán: <b>${esc(method)}</b></div>
            ${addr ? `<div class="muted">Giao đến: ${esc(addr)}</div>` : ""}
          </div>
          <div class="col right" style="min-width:220px">
            <div class="row space"><span>Tiền hàng</span><b>${fmt(
              subtotal
            )}</b></div>
            <div class="row space"><span>Giảm giá</span><b>-${fmt(
              discount
            )}</b></div>
            <div class="row space"><span>Phí ship</span><b>${fmt(
              shipFee
            )}</b></div>
            <hr/>
            <div class="row space" style="font-size:1.05em"><span><b>Thành tiền</b></span><b>${fmt(
              total
            )}</b></div>
            <button class="btn outline btn-toggle" style="margin-top:8px">Xem chi tiết</button>
            ${
              isCancellable(status)
                ? `<button class="btn danger btn-cancel" data-id="${esc(
                    id
                  )}" style="margin-top:6px">Hủy đơn</button>`
                : ``
            }
          </div>
        </div>
        <div class="order-detail" data-open="0" style="display:none; margin-top:10px; overflow:auto;">
          <table class="table" style="width:100%; border-collapse:collapse">
            <thead class="thead">
              <tr class="row space" style="gap:12px;">
                <th class="col">Sản phẩm</th>
                <th class="col center" style="max-width:90px">SL</th>
                <th class="col right"  style="max-width:140px">Đơn giá</th>
                <th class="col right"  style="max-width:160px">Thành tiền</th>
              </tr>
            </thead>
            <tbody>${
              lines ||
              `<tr><td colspan="4" class="muted" style="padding:10px">Không có sản phẩm.</td></tr>`
            }</tbody>
          </table>
        </div>
      </div>`;
  }

  // ============== helpers for mode/month/year ==============
  function showMode(val) {
    modeRange?.classList.toggle("hidden", val !== "range");
    modeMonth?.classList.toggle("hidden", val !== "month");
    modeYear?.classList.toggle("hidden", val !== "year");
  }
  function applyMonth(mm) {
    if (!mm || !inpFrom || !inpTo) return;
    const [y, m] = mm.split("-").map((x) => parseInt(x, 10));
    inpFrom.value = dateISO(firstDayOfMonth(y, m));
    inpTo.value = dateISO(lastDayOfMonth(y, m));
  }
  function applyYear(y) {
    if (!y || !inpFrom || !inpTo) return;
    inpFrom.value = dateISO(firstDayOfYear(y));
    inpTo.value = dateISO(lastDayOfYear(y));
  }

  // ============== highlight order by ?highlight= ==============
  function highlightFromQuery() {
    const id = new URLSearchParams(location.search).get("highlight");
    if (!id) return;
    const el = document.getElementById(`order-${id}`);
    if (!el) return;
    el.scrollIntoView({ behavior: "smooth", block: "start" });
    el.animate(
      [
        { boxShadow: "0 0 0 0 rgba(46, 170, 220, 0.0)" },
        { boxShadow: "0 0 0 6px rgba(46, 170, 220, 0.35)" },
        { boxShadow: "0 0 0 0 rgba(46, 170, 220, 0.0)" },
      ],
      { duration: 1600, iterations: 2 }
    );
  }
}

// ===== PAGE: Product Detail =====
if (location.pathname.startsWith("/book/")) {
  const id = location.pathname.split("/").pop();

  const $t = (id) => document.getElementById(id);
  const elTitle = $t("p-title");
  const elMeta = $t("p-meta");
  const elPrice = $t("p-price");
  const elThumb = $t("p-thumb");
  const elStock = $t("p-stock");
  const elUpdated = $t("p-updated");
  const btnAdd = $t("btnAddCart");
  const elQty = $t("p-qty"); // 👈 input số lượng

  let currentStock = 0;

  async function loadDetail() {
    try {
      const b = await api(`/books/${id}`).then(jsonOrThrow); // dùng API REST đã có
      elTitle.textContent = b.title;
      elMeta.textContent = `${b.authorName || "Unknown"} · ${
        b.categoryName || "Uncategorized"
      } · ${b.publisherName || ""}`.replace(/\s·\s$/, "");
      elPrice.textContent = fmt(b.salePrice);
      elThumb.src = b.thumbnail || "/img/placeholder.png";
      elThumb.alt = b.title;
      currentStock = Number(b.quantity ?? 0); // 👈 lưu tồn
      elStock.textContent = `Stock: ${currentStock}`;
      elUpdated.textContent = `Updated: ${toDateTime(
        b.updateAt || b.createdAt
      )}`;

      // (optional) load related by category
      if (b.categoryId) loadRelated(b.categoryId, b.bookId);
    } catch (e) {
      elTitle.textContent = "Not found";
      elMeta.textContent = e.message || "Book not found";
    }
  }

  async function loadRelated(categoryId, excludeId) {
    try {
      const page = await api(
        `/books/search?categoryId=${encodeURIComponent(categoryId)}&size=6`
      ).then(jsonOrThrow);
      const rel = (page.content || []).filter((x) => x.bookId !== excludeId);
      if (!rel.length) return;
      const box = document.getElementById("related");
      const grid = document.getElementById("rel-grid");
      grid.innerHTML = rel
        .map(
          (x) => `
      <a class="card book rel" href="/book/${x.bookId}">
        <div class="thumb">
          <img src="${x.thumbnail || "/img/placeholder.png"}" alt="${x.title}">
        </div>
        <h4 class="rel-title">${x.title}</h4>
        <div class="muted">${x.categoryName || ""}</div>
        <div><b>${fmt(x.salePrice)}</b>₫</div>
      </a>
    `
        )
        .join("");
      box.style.display = "";
    } catch {}
  }

  // sanitize qty khi user gõ
  elQty?.addEventListener("input", () => {
    let v = parseInt(elQty.value || "1", 10);
    if (isNaN(v) || v < 1) v = 1;
    if (currentStock > 0) v = Math.min(v, currentStock); // cap theo tồn
    elQty.value = String(v);
  });

  btnAdd?.addEventListener("click", async () => {
    try {
      let qty = parseInt(elQty?.value || "1", 10);
      if (isNaN(qty) || qty < 1) qty = 1;
      if (currentStock > 0) qty = Math.min(qty, currentStock);

      btnAdd.setAttribute("disabled", "disabled");
      await api(`/cart/add?bookId=${encodeURIComponent(id)}&qty=${qty}`, {
        method: "POST",
      }).then(() => true);

      // 👉 hiện popup xác nhận với 2 nút
      showAddConfirm({
        title: "Đã thêm vào giỏ hàng!",
        subtitle: `${qty} × “${elTitle?.textContent || "Sản phẩm"}”`,
      });
    } catch (e) {
      toast("Add to cart failed", false);
    } finally {
      btnAdd.removeAttribute("disabled");
    }
  });

  function showAddConfirm({ title, subtitle }) {
    // tạo overlay
    const ov = document.createElement("div");
    ov.className = "added-overlay";
    ov.innerHTML = `
    <div class="added-card" role="dialog" aria-modal="true">
      <button class="added-close" aria-label="Đóng">✕</button>
      <div class="added-head">
        <div class="ok">✓</div>
        <h3 class="added-title">${title || "Đã thêm vào giỏ"}</h3>
      </div>
      <div class="added-sub">${subtitle || ""}</div>
      <div class="added-actions">
        <button class="btn ghost btn-back">← Quay lại</button>
        <a class="btn primary btn-cart" href="/cart">Xem giỏ hàng →</a>
      </div>
    </div>
  `;

    // đóng overlay helper
    function close() {
      ov.remove();
      document.removeEventListener("keydown", onEsc);
    }
    function onEsc(e) {
      if (e.key === "Escape") close();
    }

    // click ra ngoài để đóng
    ov.addEventListener("click", (e) => {
      if (e.target === ov) close();
    });
    ov.querySelector(".added-close")?.addEventListener("click", close);

    // nút back: về trang trước, fallback về Home
    ov.querySelector(".btn-back")?.addEventListener("click", () => {
      if (history.length > 1) history.back();
      else location.href = "/";
    });

    document.body.appendChild(ov);
    document.addEventListener("keydown", onEsc);
  }

  loadDetail();
}

// =================== PAGE: Profile ===================
if (location.pathname.startsWith("/profile")) {
  // dùng $ từ layout; fallback nhẹ nếu chưa có
  const $ = window.$ || ((s, r = document) => r.querySelector(s));

  const form = $("#profileForm");
  const nameInp = form?.querySelector('[name="name"]');
  const avatar = $("#avatarInitials");

  const pw1 = $("#pw1"); // mật khẩu mới
  const pw2 = $("#pw2"); // nhập lại
  const pwMeter = $("#pwMeter")?.firstElementChild; // thanh meter
  const pwMatchL = $("#pwMatch"); // label khớp/chưa khớp

  // ---------- Avatar initials ----------
  const toInitials = (full) => {
    if (!full) return "U";
    return (
      full
        .trim()
        .split(/\s+/)
        .filter(Boolean)
        .slice(0, 3)
        .map((w) => (w[0] || "").toUpperCase())
        .join("") || "U"
    );
  };
  const colorFrom = (s) => {
    const palette = [
      "#6366f1",
      "#10b981",
      "#f59e0b",
      "#ef4444",
      "#8b5cf6",
      "#14b8a6",
    ];
    let h = 0;
    for (let i = 0; i < s.length; i++) h = (h * 31 + s.charCodeAt(i)) >>> 0;
    return palette[h % palette.length];
  };
  const applyAvatar = () => {
    if (!avatar || !nameInp) return;
    const name = nameInp.value || "User";
    avatar.textContent = toInitials(name);
    avatar.style.background = colorFrom(name);
  };
  applyAvatar();
  nameInp?.addEventListener("input", applyAvatar);

  // ---------- Toggle password visibility ----------
  document.querySelectorAll(".icon-btn[data-toggle]").forEach((btn) => {
    btn.addEventListener("click", () => {
      const id = btn.getAttribute("data-toggle");
      const inp = document.getElementById(id);
      if (!inp) return;
      inp.type = inp.type === "password" ? "text" : "password";
      inp.focus();
    });
  });

  // ---------- Strength + match ----------
  const score = (s) => {
    if (!s) return 0;
    let p = 0;
    if (s.length >= 8) p++;
    if (/[A-Z]/.test(s)) p++;
    if (/[a-z]/.test(s)) p++;
    if (/\d/.test(s)) p++;
    if (/[^A-Za-z0-9]/.test(s)) p++;
    return Math.min(p, 5);
  };

  const renderStrength = () => {
    if (!pw1 || !pwMeter) return;
    const k = score(pw1.value);
    const pct = [0, 20, 40, 60, 80, 100][k];
    pwMeter.style.width = pct + "%";
    pwMeter.style.background =
      k >= 4 ? "#10b981" : k >= 3 ? "#f59e0b" : "#ef4444";
  };

  const renderMatch = () => {
    if (!pwMatchL || !pw1 || !pw2) return;
    if (!pw1.value && !pw2.value) {
      pwMatchL.textContent = "";
      return;
    }
    const ok = pw1.value === pw2.value;
    pwMatchL.textContent = ok ? "Khớp nè ✨" : "Chưa khớp nha";
    pwMatchL.style.color = ok ? "#10b981" : "#ef4444";
  };

  pw1?.addEventListener("input", () => {
    renderStrength();
    renderMatch();
  });
  pw2?.addEventListener("input", renderMatch);

  // ---------- Guard submit ----------
  form?.addEventListener("submit", (e) => {
    // nếu có nhập bất kỳ ô mật khẩu thì bắt buộc khớp
    if ((pw1?.value || pw2?.value) && pw1.value !== pw2.value) {
      e.preventDefault();
      (window.toast || alert)("Mật khẩu nhập lại chưa khớp bro ơi!", false);
      return;
    }
    // (optional) có thể kiểm tra độ mạnh tối thiểu nếu muốn:
    // if (pw1?.value && score(pw1.value) < 3) { e.preventDefault(); toast("Mật khẩu yếu quá", false); }
  });

  // done
}
