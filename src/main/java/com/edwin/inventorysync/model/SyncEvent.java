package com.edwin.inventorysync.model;

public record SyncEvent(
        String type,
        long timestamp,
        Channel channel,
        String sku,
        String productName,
        int quantityRequested,
        int remainingQuantity,
        long syncLatencyMicros,
        String message
) {
    public static SyncEvent synced(Channel channel, String sku, String productName,
                                    int quantityRequested, int remainingQuantity, long syncLatencyMicros) {
        String otherChannel = channel == Channel.ONLINE_STORE ? "Physical POS" : "Online Store";
        return new SyncEvent(
                "SALE_SYNCED", System.currentTimeMillis(), channel, sku, productName,
                quantityRequested, remainingQuantity, syncLatencyMicros,
                "%s sold %dx %s → %s now shows %d remaining".formatted(
                        channelLabel(channel), quantityRequested, productName, otherChannel, remainingQuantity)
        );
    }

    public static SyncEvent rejected(Channel channel, String sku, String productName,
                                      int quantityRequested, int available) {
        return new SyncEvent(
                "SALE_REJECTED", System.currentTimeMillis(), channel, sku, productName,
                quantityRequested, available, 0,
                "%s tried to sell %dx %s but only %d in stock — oversell blocked".formatted(
                        channelLabel(channel), quantityRequested, productName, available)
        );
    }

    private static String channelLabel(Channel channel) {
        return channel == Channel.ONLINE_STORE ? "Online Store" : "Physical POS";
    }
}
