package com.example.eccomerce.controller;

import com.example.eccomerce.models.Product;
import com.example.eccomerce.service.ProductService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@CrossOrigin
public class ProductController {

    private final ProductService productService;

    // Constructor Injection
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public Product create(@RequestBody Product product) {
        return productService.create(product);
    }

    @GetMapping
    public List<Product> getAll() {
        return productService.getAll();
    }

    @GetMapping("/{id}")
    public Product getById(@PathVariable String id) {
        return productService.getById(id);
    }

    @GetMapping("/category/{category}")
    public List<Product> getProductsByCategory(@PathVariable String category) {
        return productService.getByCategory(category);
    }
    // GET /api/products/category/electronics/subcategory/mobile
    @GetMapping("/category/{category}/subcategory/{subcategory}")
    public List<Product> getByCategoryAndSubcategory(
            @PathVariable String category,
            @PathVariable String subcategory) {
        return productService.getByCategoryAndSubcategory(category, subcategory);
    }
}
