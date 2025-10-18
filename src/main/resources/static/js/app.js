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

// =================== PAGE: Cart ===================
if (location.pathname.startsWith("/cart")) {
  const table = document.querySelector("#cartTable");
  const btnClear = document.querySelector("#btnClear");
  const btnCheckout = document.querySelector('a[href="/checkout"]');

  // === CHỈNH Ở ĐÂY: base URL cho cart API ===
  const CART_API = "/cart";

  function toNumber(x) {
    return Number(x ?? 0) || 0;
  }
  function money(n) {
    return toNumber(n).toLocaleString() + "₫";
  }

  async function loadCart() {
    if (!table) return;
    table.innerHTML = "<div class='muted'>Loading…</div>";
    try {
      // GET /api/v1/cart
      const res = await api(CART_API, { credentials: "include" }).then(jsonOrThrow);

      // HỢP NHẤT MỌI KIỂU PAYLOAD:
      // - Array: [ {...}, ... ]
      // - Object: { items: [...] } hoặc { items: {id: obj, ...} }
      const raw = Array.isArray(res) ? res : res?.items ?? [];
      const items = Array.isArray(raw) ? raw : Object.values(raw || {});

      // total có thể không gửi => tự tính
      const total = typeof res?.total === "number"
        ? res.total
        : items.reduce((s, it) => s + toNumber(it.qty) * toNumber(it.price), 0);

      if (items.length === 0) {
        table.innerHTML = "<div class='muted'>Your cart is empty.</div>";
        btnCheckout?.classList.add("ghost");
        return;
      }

      table.innerHTML = `
        <div class="table">
          <div class="thead row">
            <div class="col">Item</div>
            <div class="col center">Qty</div>
            <div class="col right">Price</div>
            <div class="col right">Subtotal</div>
          </div>
          ${items.map(it => `
            <div class="row item">
              <div class="col">
                <div class="row gap">
                  <img src="${it.thumbnail || "/img/placeholder.png"}"
                       alt="" style="width:48px;height:48px;object-fit:cover;border-radius:8px">
                  <div>
                    <a href="/book/${it.bookId || ""}"><b>${it.title || "Unknown"}</b></a>
                    <div class="muted">${it.bookId || ""}</div>
                  </div>
                </div>
              </div>
              <div class="col center">${toNumber(it.qty)}</div>
              <div class="col right">${money(it.price)}</div>
              <div class="col right"><b>${money(toNumber(it.qty) * toNumber(it.price))}</b></div>
            </div>
          `).join("")}
          <div class="row right" style="margin-top:12px">
            <div style="flex:1"></div>
            <div><b>Total: ${money(total)}</b></div>
          </div>
        </div>
      `;
      btnCheckout?.classList.remove("ghost");
    } catch (e) {
      // nếu 401 -> có thể chưa login
      if (e?.status === 401) {
        table.innerHTML = "<div class='error'>Please sign in to view your cart.</div>";
      } else {
        table.innerHTML = `<div class='error'>${e.message || e}</div>`;
      }
      btnCheckout?.classList.add("ghost");
    }
  }

  btnClear?.addEventListener("click", () => {
    // DELETE /api/v1/cart/clear
    api(`${CART_API}/clear`, { method: "DELETE", credentials: "include" })
      .then(() => {
        toast("Cart cleared");
        loadCart();
      })
      .catch((e) => toast(e.message || "Clear failed", false));
  });

  loadCart();
}


// =================== PAGE: Browse books ===================
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

  const clean = (obj) =>
    Object.fromEntries(
      Object.entries(obj).filter(([, v]) => v != null && v !== "")
    );

  // ---- Load categories vào dropdown ----
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
              `<option value="${c.categoryId || c.categoryid}">${
                c.name
              }</option>`
          )
          .join("");

      // auto filter khi đổi category (optional)
      catSel.addEventListener("change", () => {
        const data = Object.fromEntries(new FormData(form).entries());
        loadBooks({ ...data, page: 0 });
      });
    } catch (e) {
      console.warn("[categories] load fail:", e);
      catSel.innerHTML = `<option value="">All categories</option>`;
    }
  }

  // ---- Load books (support q, min/max, categoryId, sort, page/size) ----
  async function loadBooks(params = {}) {
    if (!grid) return;
    grid.innerHTML = "<div class='muted'>Loading…</div>";
    if (pager) pager.innerHTML = "";

    const p = { page: 0, size: 20, ...clean(params) };

    const hasFilter = [
      "q",
      "title",
      "categoryId",
      "authorId",
      "publisherId",
      "minPrice",
      "maxPrice",
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
              <a class="card book" href="/book/${b.bookId}">
                <div class="thumb">
                  <img src="${b.thumbnail || "/img/placeholder.png"}" alt="${
                b.title
              }"/>
                </div>
                <h3>${b.title}</h3>
                <div class="muted">${b.categoryName || "Uncategorized"}</div>
                <div><b>${fmt(b.salePrice)}</b>₫</div>
              </a>`
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

  // ---- Search input: bật type="search" + handle nút ✕ ----
  const qInput = form?.querySelector('[name="q"]');
  if (qInput) {
    // đảm bảo là search để có nút ✕ native
    try {
      if (qInput.type !== "search") qInput.type = "search";
    } catch {}

    // Bấm nút ✕ (native) → clear & load full
    qInput.addEventListener("search", () => {
      if (!qInput.value) {
        form.reset();
        const cat = form.querySelector('[name="categoryId"]');
        if (cat) cat.value = ""; // chắc kèo clear category
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
            if (cat) cat.value = "";
            loadBooks();
          }
        }, 150);
      }
    });
  }

  // ---- Init ----
  loadCategories(); // fill dropdown
  loadBooks(); // first page

  // ---- Submit search ----
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

  // ---------- Submit form ----------
  if (form && out) {
    form.addEventListener("submit", async (e) => {
      e.preventDefault();
      const btn = form.querySelector('button[type="submit"]');

      out.classList.remove("error", "success");
      out.textContent = "Placing order…";

      const body = Object.fromEntries(new FormData(form).entries());
      // Clean payload
      body.usePoints = body.usePoints ? Number(body.usePoints) : 0;
      body.promoCode = body.promoCode?.trim() || null;
      // Gửi paymentMethod (radio)
      body.paymentMethod =
        form.querySelector('input[name="paymentMethod"]:checked')?.value ||
        "COD";

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
          toast?.("Order placed ✓");
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
        </div>
      `
        )
        .join("");

      btnPickPromo?.addEventListener("click", async () => {
        openPromoPopover();
        await loadActivePromos();
      });

      // ⬇️ THÊM NGAY DƯỚI ĐÂY
      listPromo?.addEventListener("click", async (e) => {
        e.preventDefault();
        const btn = e.target.closest(".btn-apply");
        const row = e.target.closest(".row");
        const code =
          btn?.dataset.code || row?.querySelector(".btn-apply")?.dataset.code;
        if (!code) return;
        await applyPromo(code);
        popPromo && (popPromo.style.display = "none");
      });
    } catch (e) {
      listPromo.innerHTML = `<div class="error" style="padding:8px">${
        e?.message || "Không tải được danh sách mã"
      }</div>`;
    }
  }

  async function applyPromo(code) {
    try {
      // gọi validate (nếu chưa có endpoint, có thể tính FE: discount = p.discount nếu subtotal >= p.minValue)
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
        inpPromo && (inpPromo.value = r?.code || code);
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

// =================== PAGE: My Orders ===================
if (location.pathname.startsWith("/track")) {
  const $  = (s, r=document) => r.querySelector(s);
  const box = $("#myOrdersBox");

  const VND = (n) => Math.max(0, Math.round(Number(n||0)));
  const fmt = (n) => (Number(n||0)).toLocaleString("vi-VN") + "₫";
  const toDT = (x) => (typeof toDateTime === "function" ? toDateTime(x) : (x||""));
  const esc = (s="") => String(s).replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));

  init();

  async function init(){
    try{
      box.innerHTML = `<div class="muted">Đang kiểm tra đăng nhập…</div>`;
      const me = await fetch(`${API()}/auth/me`, { credentials:"include" }).then(jsonOrThrow);
      const userId = me?.customerId;
      if(!userId){
        box.innerHTML = `<div class="error">Bạn chưa đăng nhập. Vui lòng đăng nhập để xem đơn hàng.</div>`;
        return;
      }

      box.innerHTML = `<div class="muted">Đang tải đơn hàng…</div>`;
      const orders = await fetchMyOrders(userId);

      if(!orders.length){
        box.innerHTML = `<div class="muted">Bạn chưa có đơn hàng nào.</div>`;
        return;
      }

      renderOrders(orders);
      wireToggles();
    }catch(e){
      box.innerHTML = `<div class="error">${e?.message || "Không tải được danh sách đơn hàng"}</div>`;
    }
  }

  async function fetchMyOrders(customerId){
    // Ưu tiên 1 endpoint "mine"; có thể fallback các biến thể khác
    const urls = [
      `${API()}/orders/mine`,
      `${API()}/orders/my`,
      `${API()}/orders?customerId=${encodeURIComponent(customerId)}`
    ];
    for(const u of urls){
      try{
        const res  = await fetch(u, { credentials:"include" }).then(jsonOrThrow);
        const list = Array.isArray(res) ? res : (res?.content || []);
        if(Array.isArray(list)) return list;
      }catch{}
    }
    return [];
  }

  function renderOrders(list){
    box.innerHTML = list.map(o => renderCard(o)).join("");
  }

  function wireToggles(){
    box.querySelectorAll(".btn-toggle").forEach(btn=>{
      btn.addEventListener("click", ()=>{
        const panel = btn.closest(".track-card")?.querySelector(".order-detail");
        const open  = panel.getAttribute("data-open")==="1";
        panel.style.display = open? "none" : "block";
        panel.setAttribute("data-open", open? "0":"1");
        btn.textContent = open? "Xem chi tiết" : "Thu gọn";
      });
    });
  }

  function renderCard(o){
    const id       = o.orderId || o.id || "-";
    const status   = (o.status || "").toUpperCase();
    const created  = toDT(o.createAt || o.createdAt || o.created || o.create_at);
    const updated  = toDT(o.updateAt || o.updatedAt || o.updated || o.update_at);
    const addr     = o.shippingAddress || o.address || "";
    const method   = o.paymentMethod || "COD";
    const subtotal = VND(o.subtotal);
    const discount = VND(o.discount);
    const shipFee  = VND(o.shippingFee);
    const total    = VND(o.total);

    const items = Array.isArray(o.items) ? o.items
               : Array.isArray(o.orderItems) ? o.orderItems
               : [];

    const badgeClass = (() => {
      switch (status) {
        case "CONFIRMED":
        case "SHIPPING":
        case "DELIVERED": return "green";
        case "CANCELLED":
        case "CANCELED":
        case "FAILED":    return "red";
        case "PENDING":
        default:          return "gray";
      }
    })();

    const lines = items.map(it=>{
      const title = it.title || it.bookTitle || it.book?.title || "(Không rõ)";
      const qty   = it.quantity ?? it.qty ?? 1;
      const price = VND(it.price || it.unitPrice || it.book?.salePrice || 0);
      const line  = VND(price * qty);
      return `
        <tr class="row space" style="gap:12px; padding:8px 0; border-bottom:1px dashed #e8eaf1;">
          <td class="col"><b>${esc(title)}</b></td>
          <td class="col center" style="max-width:90px">${qty}</td>
          <td class="col right"  style="max-width:140px">${fmt(price)}</td>
          <td class="col right"  style="max-width:160px"><b>${fmt(line)}</b></td>
        </tr>
      `;
    }).join("");

    return `
      <div class="track-card" style="margin-bottom:12px">
        <div class="row space">
          <div>
            <div class="row gap" style="align-items:baseline">
              <h3 style="margin:0">Order #${id}</h3>
              <span class="badge ${badgeClass}">${status || "UNKNOWN"}</span>
            </div>
            <div class="muted">Tạo: ${created || "-"} ${updated ? " · Cập nhật: " + updated : ""}</div>
            <div class="muted">Thanh toán: <b>${esc(method)}</b></div>
            ${addr ? `<div class="muted">Giao đến: ${esc(addr)}</div>` : ""}
          </div>
          <div class="col right" style="min-width:220px">
            <div class="row space"><span>Tiền hàng</span><b>${fmt(subtotal)}</b></div>
            <div class="row space"><span>Giảm giá</span><b>-${fmt(discount)}</b></div>
            <div class="row space"><span>Phí ship</span><b>${fmt(shipFee)}</b></div>
            <hr/>
            <div class="row space" style="font-size:1.05em"><span><b>Thành tiền</b></span><b>${fmt(total)}</b></div>
            <button class="btn outline btn-toggle" style="margin-top:8px">Xem chi tiết</button>
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
            <tbody>${lines || `<tr><td colspan="4" class="muted" style="padding:10px">Không có sản phẩm.</td></tr>`}</tbody>
          </table>
        </div>
      </div>
    `;
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
        <a class="card" href="/book/${x.bookId}">
          <div class="thumb"><img src="${
            x.thumbnail || "/img/placeholder.png"
          }" alt="${x.title}"></div>
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
      // 👉 lấy qty từ input (fallback = 1), cap theo tồn
      let qty = parseInt(elQty?.value || "1", 10);
      if (isNaN(qty) || qty < 1) qty = 1;
      if (currentStock > 0) qty = Math.min(qty, currentStock);

      btnAdd.setAttribute("disabled", "disabled");
      await api(`/cart/add?bookId=${encodeURIComponent(id)}&qty=${qty}`, {
        method: "POST",
        // credentials: "include", // bật nếu api() không tự include cookie
      });
      toast(`Added ${qty} to cart ✓`);
    } catch (e) {
      toast("Add to cart failed", false);
    } finally {
      btnAdd.removeAttribute("disabled");
    }
  });

  loadDetail();
}

