const state = { products: {} };

const inventoryBody = document.querySelector('#inventory-table tbody');
const syncLog = document.getElementById('sync-log');
const channelPanels = document.querySelectorAll('.product-list');

function currency(n) {
    return new Intl.NumberFormat('en-UG').format(n);
}

function stockClass(qty) {
    if (qty === 0) return 'stock-out';
    if (qty <= 5) return 'stock-low';
    return '';
}

function renderChannels() {
    channelPanels.forEach(panel => {
        const channel = panel.dataset.channel;
        panel.innerHTML = '';
        Object.values(state.products).forEach(p => {
            const row = document.createElement('div');
            row.className = 'product-row';
            row.innerHTML = `
                <div>
                    <div class="product-name">${p.name}</div>
                    <div class="product-stock">${p.quantity} in stock</div>
                </div>
                <div class="sell-buttons">
                    <button data-sku="${p.sku}" data-channel="${channel}" data-qty="1">Sell 1</button>
                    <button data-sku="${p.sku}" data-channel="${channel}" data-qty="3">Sell 3</button>
                </div>`;
            panel.appendChild(row);
        });
    });

    panelClickBind();
}

function renderInventoryTable(flashSku) {
    inventoryBody.innerHTML = '';
    Object.values(state.products).forEach(p => {
        const tr = document.createElement('tr');
        if (p.sku === flashSku) tr.classList.add('flash');
        tr.innerHTML = `
            <td>${p.sku}</td>
            <td>${p.name}</td>
            <td>${currency(p.price)}</td>
            <td class="${stockClass(p.quantity)}">${p.quantity}</td>`;
        inventoryBody.appendChild(tr);
    });
}

function panelClickBind() {
    document.querySelectorAll('.sell-buttons button').forEach(btn => {
        btn.onclick = () => sell(btn.dataset.channel, btn.dataset.sku, Number(btn.dataset.qty));
    });
}

async function sell(channel, sku, quantity) {
    await fetch('/api/sales', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ channel, sku, quantity })
    });
    // UI updates arrive via the SSE stream, not this response —
    // that's the point: every channel finds out the same way, at the same time.
}

function appendLog(event) {
    const entry = document.createElement('div');
    entry.className = 'log-entry' + (event.type === 'SALE_REJECTED' ? ' rejected' : '');
    const time = new Date(event.timestamp).toLocaleTimeString();
    const latency = event.type === 'SALE_SYNCED' ? ` (${event.syncLatencyMicros}µs)` : '';
    entry.innerHTML = `<span class="log-time">${time}</span>${event.message}${latency}`;
    syncLog.prepend(entry);
    while (syncLog.children.length > 50) syncLog.removeChild(syncLog.lastChild);
}

async function loadProducts() {
    const res = await fetch('/api/products');
    const products = await res.json();
    products.forEach(p => state.products[p.sku] = p);
    renderChannels();
    renderInventoryTable();
}

function connectStream() {
    const source = new EventSource('/api/events/stream');
    source.addEventListener('sync', evt => {
        const event = JSON.parse(evt.data);
        if (state.products[event.sku]) {
            state.products[event.sku].quantity = event.remainingQuantity;
        }
        renderChannels();
        renderInventoryTable(event.sku);
        appendLog(event);
    });
    source.onerror = () => {
        source.close();
        setTimeout(connectStream, 2000);
    };
}

loadProducts().then(connectStream);
