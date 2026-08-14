package com.eliteretail.pos.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductCategoryTest {
    @Test
    void customCategoryCannotReuseBuiltInPrefix() {
        assertThrows(IllegalArgumentException.class,
                () -> ProductCategory.addCustomCategory("Duplicate Dairy", "1000"));
    }
}
