package com.edwin.inventorysync.web;

import com.edwin.inventorysync.model.Product;
import com.edwin.inventorysync.model.SaleRequest;
import com.edwin.inventorysync.model.SyncEvent;
import com.edwin.inventorysync.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Collection;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/products")
    public Collection<Product> getProducts() {
        return inventoryService.getProducts();
    }

    @PostMapping("/sales")
    public ResponseEntity<SyncEvent> sell(@Valid @RequestBody SaleRequest request) {
        SyncEvent event = inventoryService.processSale(request.channel(), request.sku(), request.quantity());
        if ("SALE_REJECTED".equals(event.type())) {
            return ResponseEntity.unprocessableEntity().body(event);
        }
        return ResponseEntity.ok(event);
    }

    @GetMapping("/events/stream")
    public SseEmitter stream() {
        return inventoryService.subscribe();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleUnknownSku(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}
