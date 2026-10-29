package com.eliteretail.pos.service;

import com.eliteretail.pos.model.ProductItem;
import com.eliteretail.pos.repository.ProductRepository;

import java.util.List;

public class ProductService {
    private final ProductRepository productRepository = new ProductRepository();

    private void requireSignedIn() {
        RoleGuard.requireSignedIn(AuthService.getInstance().getCurrentUser());
    }

    private void requireManager() {
        RoleGuard.requireManager(AuthService.getInstance().getCurrentUser());
    }

    public List<ProductItem> getAllProducts() {
        requireSignedIn();
        return productRepository.findAll();
    }

    public List<ProductItem> getAvailableProducts() {
        requireSignedIn();
        return productRepository.findAvailable();
    }

    public boolean addProduct(ProductItem product) {
        requireManager();
        validate(product);
        return productRepository.insert(product);
    }

    public boolean updateProduct(ProductItem product) {
        requireManager();
        validate(product);
        return productRepository.update(product);
    }

    public boolean deleteProduct(String code) {
        requireManager();
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Product code is required.");
        }
        return productRepository.delete(code.trim());
    }

    public ProductItem findProductByCode(String code) {
        requireSignedIn();
        if (code == null || code.isBlank()) {
            return null;
        }
        return productRepository.findByCode(code.trim());
    }

    private void validate(ProductItem product) {
        if (product == null) {
            throw new IllegalArgumentException("Product is required.");
        }
        if (product.getCode() == null || product.getCode().isBlank()) {
            throw new IllegalArgumentException("Product code is required.");
        }
        if (product.getName() == null || product.getName().isBlank()) {
            throw new IllegalArgumentException("Product name is required.");
        }
        if (product.getUnitPrice() < 0) {
            throw new IllegalArgumentException("Price cannot be negative.");
        }
        if (product.getStockQuantity() < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative.");
        }
    }
}
