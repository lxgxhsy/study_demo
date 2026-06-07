package com.example.concurrencylab.orderlab;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lab/orders")
public class OrderLabController {

    private final OrderLabService service;

    public OrderLabController(OrderLabService service) {
        this.service = service;
    }

    @PostMapping
    public OrderResponse createOrder(@Valid @RequestBody OrderRequest request) {
        return service.createOrder(request);
    }
}
