package com.eliteretail.pos.model;

import java.time.LocalDateTime;

public record RecentTransaction(
        String billNumber,
        double total,
        String paymentMethod,
        LocalDateTime createdAt
) {}
