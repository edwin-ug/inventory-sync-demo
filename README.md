# Inventory Sync

**Live site:** https://inventory-sync-demo-7d5u.onrender.com — a marketing landing page
**Live demo:** https://inventory-sync-demo-7d5u.onrender.com/demo — the interactive dashboard
*(hosted on Render's free tier — spins down after 15 min idle, first load may take ~30-60s
to wake up; inventory resets to seed data on each wake-up)*

A working demo of the problem most multi-channel retailers have: they sell the same product
on an online storefront **and** in a physical store, but the two systems don't talk to each
other. Someone sells the last unit in-store, the website doesn't know, and an online customer
buys a product that no longer exists — an oversold order, a refund, and an angry customer.

This project is a small Spring Boot service that acts as the **single source of truth** for
inventory across both channels. A sale on either channel is written through a shared
inventory service and broadcast to every connected client in real time over Server-Sent
Events (SSE) — so both channels see the same stock count within microseconds, and a sale
that would oversell is rejected outright instead of silently corrupting the count.

It's built as a portfolio/demo piece — synthetic data, no real client system attached — to
show the kind of "technical CFO / systems advisor" work described here: not just reporting
on a business's numbers after the fact, but building the plumbing that keeps those numbers
correct in real time.

## What it demonstrates

- **Real-time sync**: selling from the "Online Store" panel instantly updates the "Physical
  POS" panel's stock count (and vice versa) via SSE, with no page refresh.
- **Oversell prevention**: once a SKU hits zero, a sale attempt from either channel is
  rejected with a clear reason, instead of allowing stock to go negative.
- **An audit trail**: every sale (accepted or rejected) is logged with a timestamp and the
  processing latency, which is the kind of trail a business owner needs to trust the system.

## Running it

Requires Java 21+ and Maven.

```bash
mvn spring-boot:run
```

Then open http://localhost:8080 for the landing page, or http://localhost:8080/demo to go
straight to the dashboard — sell a few units from either the "Online Store" or "Physical POS"
panel and watch the other channel, the live inventory table, and the sync log update together.

## Architecture

```
Online Store UI  ──┐
                    ├──►  InventoryService (single in-memory source of truth)  ──► SSE broadcast ──► both UIs
Physical POS UI  ──┘
```

- `InventoryService` holds all product state and is the only place stock is mutated —
  every sale, from either channel, goes through the same code path and the same lock,
  so there's no window where two channels can both sell the last unit.
- `GET /api/events/stream` is an SSE endpoint every connected browser subscribes to; any
  mutation is broadcast to all of them immediately.
- `POST /api/sales` is the write path both channel panels call.

In a real engagement this in-memory service would be replaced with a durable store and the
"channels" would be actual integrations (a Shopify/Jumia webhook, a POS terminal API, an
accounting system like QuickBooks) — the sync guarantee is the same, only the number and
type of connected systems changes.

## Stack

Spring Boot 3 (Java 21), plain HTML/CSS/JS frontend (no build step), Server-Sent Events for
push updates. Deliberately dependency-light so it's easy to read end-to-end.
