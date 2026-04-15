const state = {
    members: [],
    coupons: [],
    products: [],
    cart: null,
    orders: [],
    memberId: 1,
    keyword: "",
    selectedCoupon: "",
    paymentMethod: "CARD",
};

const el = {
    memberSelect: document.getElementById("memberSelect"),
    productGrid: document.getElementById("productGrid"),
    couponList: document.getElementById("couponList"),
    cartItems: document.getElementById("cartItems"),
    cartTotal: document.getElementById("cartTotal"),
    ordersBoard: document.getElementById("ordersBoard"),
    searchForm: document.getElementById("searchForm"),
    searchInput: document.getElementById("searchInput"),
    clearSearchButton: document.getElementById("clearSearchButton"),
    searchSummary: document.getElementById("searchSummary"),
    shippingAddressInput: document.getElementById("shippingAddressInput"),
    paymentReferenceInput: document.getElementById("paymentReferenceInput"),
    checkoutButton: document.getElementById("checkoutButton"),
    selectedCouponLabel: document.getElementById("selectedCouponLabel"),
    orderCountLabel: document.getElementById("orderCountLabel"),
    metricProducts: document.getElementById("metricProducts"),
    metricOrders: document.getElementById("metricOrders"),
    metricCart: document.getElementById("metricCart"),
    reindexButton: document.getElementById("reindexButton"),
    toast: document.getElementById("toast"),
};

document.addEventListener("DOMContentLoaded", init);

async function init() {
    bindEvents();
    await loadInitialData();
}

function bindEvents() {
    el.memberSelect.addEventListener("change", async (event) => {
        state.memberId = Number(event.target.value);
        state.selectedCoupon = "";
        await Promise.all([loadCart(), loadOrders()]);
        renderCoupons();
        renderCheckoutSummary();
    });

    el.searchForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        state.keyword = el.searchInput.value.trim();
        await loadProducts();
    });

    el.clearSearchButton.addEventListener("click", async () => {
        state.keyword = "";
        el.searchInput.value = "";
        await loadProducts();
    });

    document.querySelectorAll(".keyword-chip").forEach((chip) => {
        chip.addEventListener("click", async () => {
            state.keyword = chip.dataset.keyword || "";
            el.searchInput.value = state.keyword;
            await loadProducts();
        });
    });

    document.querySelectorAll(".payment-button").forEach((button) => {
        button.addEventListener("click", () => {
            state.paymentMethod = button.dataset.method;
            document.querySelectorAll(".payment-button").forEach((item) => item.classList.remove("active"));
            button.classList.add("active");
        });
    });

    el.checkoutButton.addEventListener("click", checkout);
    el.reindexButton.addEventListener("click", reindexProducts);

    el.productGrid.addEventListener("click", async (event) => {
        const button = event.target.closest("[data-product-id]");
        if (!button) {
            return;
        }

        const productId = Number(button.dataset.productId);
        await addToCart(productId, 1);
    });

    el.couponList.addEventListener("click", (event) => {
        const button = event.target.closest("[data-coupon-code]");
        if (!button) {
            return;
        }

        const code = button.dataset.couponCode || "";
        state.selectedCoupon = state.selectedCoupon === code ? "" : code;
        renderCoupons();
        renderCheckoutSummary();
    });

    el.ordersBoard.addEventListener("click", async (event) => {
        const actionButton = event.target.closest("[data-order-action]");
        if (!actionButton) {
            return;
        }

        const orderId = Number(actionButton.dataset.orderId);
        const action = actionButton.dataset.orderAction;

        if (action === "ship") {
            const trackingNumber = window.prompt("Tracking number", "CJ-");
            if (!trackingNumber) {
                return;
            }
            await updateDelivery(orderId, "SHIPPED", trackingNumber);
        }

        if (action === "deliver") {
            await updateDelivery(orderId, "DELIVERED", "");
        }
    });
}

async function loadInitialData() {
    await Promise.all([loadMembers(), loadCoupons()]);

    if (state.members.length > 0) {
        state.memberId = state.members[0].id;
        el.memberSelect.value = String(state.memberId);
    }

    await Promise.all([loadProducts(), loadCart(), loadOrders()]);
}

async function loadMembers() {
    state.members = await fetchJson("/api/members");
    renderMembers();
}

async function loadCoupons() {
    state.coupons = await fetchJson("/api/coupons");
    renderCoupons();
}

async function loadProducts() {
    if (state.keyword) {
        const result = await fetchJson(`/api/products/search?keyword=${encodeURIComponent(state.keyword)}&page=0&size=12`);
        state.products = result.content;
        el.searchSummary.textContent = `${result.totalElements} results for "${result.keyword}"`;
    } else {
        state.products = await fetchJson("/api/products");
        el.searchSummary.textContent = "All active products";
    }

    renderProducts();
    renderMetrics();
}

async function loadCart() {
    state.cart = await fetchJson(`/api/members/${state.memberId}/cart`);
    renderCart();
    renderMetrics();
}

async function loadOrders() {
    state.orders = await fetchJson(`/api/members/${state.memberId}/orders`);
    renderOrders();
    renderCheckoutSummary();
    renderMetrics();
}

function renderMembers() {
    el.memberSelect.innerHTML = state.members
        .map((member) => `<option value="${member.id}">${escapeHtml(member.name)} / ${escapeHtml(member.email)}</option>`)
        .join("");
}

function renderCoupons() {
    if (state.coupons.length === 0) {
        el.couponList.innerHTML = `<div class="empty-state">No coupons available.</div>`;
        return;
    }

    el.couponList.innerHTML = state.coupons.map((coupon) => `
        <div class="coupon-chip ${state.selectedCoupon === coupon.code ? "active" : ""}">
            <div>
                <strong>${escapeHtml(coupon.code)}</strong>
                <div>${escapeHtml(coupon.name)}</div>
            </div>
            <button type="button" data-coupon-code="${escapeHtml(coupon.code)}">
                ${state.selectedCoupon === coupon.code ? "Selected" : "Apply"}
            </button>
        </div>
    `).join("");
}

function renderProducts() {
    if (state.products.length === 0) {
        el.productGrid.innerHTML = `<div class="empty-state">No products match the current query.</div>`;
        return;
    }

    el.productGrid.innerHTML = state.products.map((product) => `
        <article class="product-card">
            <div class="product-visual">
                <span class="product-pill">${escapeHtml(product.brand)}</span>
                <div class="product-name-display">${escapeHtml(product.name)}</div>
            </div>
            <div class="product-body">
                <div class="product-meta">
                    <div>
                        <p class="eyebrow">curated item</p>
                        <strong>${escapeHtml(product.name)}</strong>
                    </div>
                    <div class="price-tag">${formatCurrency(product.price)}</div>
                </div>
                <p class="product-description">${escapeHtml(product.description)}</p>
                <div class="product-actions">
                    <span class="stock-badge">stock ${product.stockQuantity}</span>
                    <button class="add-cart-button" type="button" data-product-id="${product.id}">Add to cart</button>
                </div>
            </div>
        </article>
    `).join("");
}

function renderCart() {
    const items = state.cart?.items || [];

    if (items.length === 0) {
        el.cartItems.innerHTML = `<div class="empty-state">Cart is empty. Add products from the catalog.</div>`;
        el.cartTotal.textContent = formatCurrency(0);
        renderCheckoutSummary();
        return;
    }

    el.cartItems.innerHTML = items.map((item) => `
        <div class="cart-item">
            <div class="cart-item-row">
                <strong>${escapeHtml(item.productName)}</strong>
                <span>${formatCurrency(item.unitPrice)}</span>
            </div>
            <div class="cart-item-row">
                <span>qty ${item.quantity}</span>
                <strong>${formatCurrency(item.lineTotal)}</strong>
            </div>
        </div>
    `).join("");

    el.cartTotal.textContent = formatCurrency(state.cart.totalAmount);
    renderCheckoutSummary();
}

function renderOrders() {
    if (state.orders.length === 0) {
        el.ordersBoard.innerHTML = `<div class="empty-state">No orders yet. Complete checkout to start the order feed.</div>`;
        return;
    }

    el.ordersBoard.innerHTML = state.orders.map((order) => {
        const actions = [];

        if (order.deliveryStatus === "PREPARING") {
            actions.push(`<button type="button" class="inline-button" data-order-action="ship" data-order-id="${order.orderId}">Mark shipped</button>`);
        }

        if (order.deliveryStatus === "SHIPPED") {
            actions.push(`<button type="button" class="inline-button secondary" data-order-action="deliver" data-order-id="${order.orderId}">Mark delivered</button>`);
        }

        return `
            <article class="order-card">
                <div class="order-top">
                    <div>
                        <p class="eyebrow">order #${order.orderId}</p>
                        <h4>${formatCurrency(order.payAmount)}</h4>
                    </div>
                    <div class="order-tags">
                        <span class="order-tag dark">${escapeHtml(order.status)}</span>
                        <span class="order-tag">${escapeHtml(order.deliveryStatus)}</span>
                    </div>
                </div>
                <div class="order-meta">
                    <span>Total ${formatCurrency(order.totalAmount)}</span>
                    <span>Discount ${formatCurrency(order.discountAmount)}</span>
                    <span>Ordered ${formatDate(order.orderedAt)}</span>
                </div>
                <div class="order-bottom">
                    <div class="order-actions">${actions.join("") || `<span class="stock-badge">No action available</span>`}</div>
                </div>
            </article>
        `;
    }).join("");
}

function renderCheckoutSummary() {
    el.selectedCouponLabel.textContent = state.selectedCoupon || "none";
    el.orderCountLabel.textContent = String(state.orders.length);
}

function renderMetrics() {
    el.metricProducts.textContent = String(state.products.length);
    el.metricOrders.textContent = String(state.orders.length);
    const cartCount = (state.cart?.items || []).reduce((sum, item) => sum + item.quantity, 0);
    el.metricCart.textContent = String(cartCount);
}

async function addToCart(productId, quantity) {
    await fetchJson(`/api/members/${state.memberId}/cart/items`, {
        method: "POST",
        body: JSON.stringify({ productId, quantity }),
    });
    await loadCart();
    showToast("Added to cart.");
}

async function checkout() {
    const shippingAddress = el.shippingAddressInput.value.trim();
    const paymentReference = el.paymentReferenceInput.value.trim();

    if (!shippingAddress || !paymentReference) {
        showToast("Shipping address and payment reference are required.");
        return;
    }

    const order = await fetchJson("/api/orders/checkout", {
        method: "POST",
        body: JSON.stringify({
            memberId: state.memberId,
            paymentMethod: state.paymentMethod,
            paymentReference,
            shippingAddress,
            couponCode: state.selectedCoupon || null,
        }),
    });

    state.selectedCoupon = "";
    await Promise.all([loadCart(), loadOrders(), loadProducts()]);
    renderCoupons();

    if (order.status === "PAYMENT_FAILED") {
        showToast(`Payment failed: ${order.payment?.failedReason || "unknown reason"}`);
        return;
    }

    showToast("Checkout completed.");
}

async function updateDelivery(orderId, deliveryStatus, trackingNumber) {
    await fetchJson(`/api/orders/${orderId}/delivery`, {
        method: "PATCH",
        body: JSON.stringify({ deliveryStatus, trackingNumber }),
    });
    await loadOrders();
    showToast(`Delivery updated: ${deliveryStatus}`);
}

async function reindexProducts() {
    const message = await fetchText("/api/products/search/reindex", { method: "POST" });
    showToast(message);
}

async function fetchJson(url, options = {}) {
    const response = await fetch(url, withJsonHeaders(options));
    const payload = await readPayload(response);

    if (!response.ok) {
        throwAndToast(payload.message || payload.error || "Request failed.");
    }

    return payload;
}

async function fetchText(url, options = {}) {
    const response = await fetch(url, options);
    const text = await response.text();

    if (!response.ok) {
        throwAndToast(text || "Request failed.");
    }

    return text;
}

function withJsonHeaders(options) {
    return {
        ...options,
        headers: {
            "Content-Type": "application/json",
            ...(options.headers || {}),
        },
    };
}

async function readPayload(response) {
    const contentType = response.headers.get("content-type") || "";
    if (contentType.includes("application/json")) {
        return response.json();
    }
    return {};
}

function throwAndToast(message) {
    showToast(message);
    throw new Error(message);
}

function showToast(message) {
    el.toast.hidden = false;
    el.toast.textContent = message;
    window.clearTimeout(showToast.timeoutId);
    showToast.timeoutId = window.setTimeout(() => {
        el.toast.hidden = true;
    }, 2800);
}

function formatCurrency(value) {
    return new Intl.NumberFormat("ko-KR", {
        style: "currency",
        currency: "KRW",
        maximumFractionDigits: 0,
    }).format(Number(value || 0));
}

function formatDate(value) {
    return new Intl.DateTimeFormat("ko-KR", {
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit",
    }).format(new Date(value));
}

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll("\"", "&quot;")
        .replaceAll("'", "&#39;");
}
