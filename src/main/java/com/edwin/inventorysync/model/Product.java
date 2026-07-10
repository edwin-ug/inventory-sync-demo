package com.edwin.inventorysync.model;

import java.util.concurrent.atomic.AtomicInteger;

public class Product {
    private final String sku;
    private final String name;
    private final double price;
    private final AtomicInteger quantity;

    public Product(String sku, String name, double price, int initialQuantity) {
        this.sku = sku;
        this.name = name;
        this.price = price;
        this.quantity = new AtomicInteger(initialQuantity);
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    public int getQuantity() {
        return quantity.get();
    }

    public boolean compareAndSetQuantity(int expected, int newValue) {
        return quantity.compareAndSet(expected, newValue);
    }
}
