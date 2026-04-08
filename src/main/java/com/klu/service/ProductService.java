package com.klu.service;

import com.klu.model.Product;
import com.klu.repository.ProductRepository;
import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);
    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // CREATE
    public Product addProduct(Product product) {
        logger.info("Adding new product: {}", product.getName());
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        return productRepository.save(product);
    }

    // READ ALL
    public List<Product> getAllProducts() {
        logger.debug("Fetching all products");
        return productRepository.findAll();
    }

    // READ BY ID
    public Product getProductById(Long id) {
        logger.debug("Fetching product by id: {}", id);
        return productRepository.findById(id).orElse(null);
    }

    // UPDATE - with optimistic locking
    public Product updateProduct(Long id, Product product) {
        logger.info("Updating product with id: {}", id);
        try {
            Product existing = productRepository.findById(id).orElse(null);
            if (existing != null) {
                existing.setName(product.getName());
                existing.setDescription(product.getDescription());
                existing.setResourceType(product.getResourceType());
                existing.setImageUrl(product.getImageUrl());
                existing.setUpdatedAt(LocalDateTime.now());
                Product updated = productRepository.save(existing);
                logger.info("Successfully updated product with id: {}", id);
                return updated;
            }
            logger.warn("Product not found with id: {}", id);
            return null;
        } catch (OptimisticLockException e) {
            logger.error("Concurrent modification detected for product id: {}. Please retry.", id);
            throw new RuntimeException("Product was modified by another user. Please refresh and try again.", e);
        }
    }

    // DELETE
    public void deleteProduct(Long id) {
        logger.info("Deleting product with id: {}", id);
        productRepository.deleteById(id);
    }
}