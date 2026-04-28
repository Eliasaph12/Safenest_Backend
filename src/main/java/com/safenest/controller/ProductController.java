package com.safenest.controller;

import com.safenest.service.PlatformService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final PlatformService platformService;

    public ProductController(PlatformService platformService) {
        this.platformService = platformService;
    }

    @GetMapping
    public List<Map<String, Object>> getAllProducts() {
        return platformService.getAllResources();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getProductById(@PathVariable Long id) {
        Map<String, Object> resource = platformService.getResourceById(id);
        return resource == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(resource);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createProduct(@RequestBody Map<String, Object> request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(platformService.createResource(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateProduct(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        Map<String, Object> updated = platformService.updateResource(id, request);
        return updated == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteProduct(@PathVariable Long id) {
        return platformService.deleteResource(id)
            ? ResponseEntity.ok(Map.of("message", "Product deleted successfully"))
            : ResponseEntity.notFound().build();
    }
}
