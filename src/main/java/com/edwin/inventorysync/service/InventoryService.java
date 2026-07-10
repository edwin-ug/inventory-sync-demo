package com.edwin.inventorysync.service;

import com.edwin.inventorysync.model.Channel;
import com.edwin.inventorysync.model.Product;
import com.edwin.inventorysync.model.SyncEvent;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class InventoryService {

    private final Map<String, Product> products = new ConcurrentHashMap<>();
    private final List<SseEmitter> subscribers = new CopyOnWriteArrayList<>();
    private final Object lock = new Object();

    @PostConstruct
    void seed() {
        addProduct(new Product("SKU-1001", "Wireless Earbuds", 89000, 14));
        addProduct(new Product("SKU-1002", "Blue T-Shirt (M)", 35000, 10));
        addProduct(new Product("SKU-1003", "A5 Notebook", 8000, 40));
        addProduct(new Product("SKU-1004", "Stainless Water Bottle", 25000, 6));
    }

    private void addProduct(Product product) {
        products.put(product.getSku(), product);
    }

    public Collection<Product> getProducts() {
        return products.values();
    }

    public SyncEvent processSale(Channel channel, String sku, int quantity) {
        Product product = products.get(sku);
        if (product == null) {
            throw new IllegalArgumentException("Unknown SKU: " + sku);
        }

        SyncEvent event;
        synchronized (lock) {
            long start = System.nanoTime();
            int current = product.getQuantity();
            if (current < quantity) {
                event = SyncEvent.rejected(channel, sku, product.getName(), quantity, current);
            } else {
                int remaining = current - quantity;
                product.compareAndSetQuantity(current, remaining);
                long latencyMicros = (System.nanoTime() - start) / 1_000;
                event = SyncEvent.synced(channel, sku, product.getName(), quantity, remaining, latencyMicros);
            }
        }
        broadcast(event);
        return event;
    }

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);
        subscribers.add(emitter);
        emitter.onCompletion(() -> subscribers.remove(emitter));
        emitter.onTimeout(() -> subscribers.remove(emitter));
        emitter.onError(e -> subscribers.remove(emitter));
        return emitter;
    }

    private void broadcast(SyncEvent event) {
        for (SseEmitter emitter : subscribers) {
            try {
                emitter.send(SseEmitter.event().name("sync").data(event));
            } catch (Exception e) {
                subscribers.remove(emitter);
            }
        }
    }
}
