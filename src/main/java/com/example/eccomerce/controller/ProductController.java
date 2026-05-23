package com.example.eccomerce.controller;

import com.example.eccomerce.models.Product;
import com.example.eccomerce.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@CrossOrigin
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    @Autowired  // ← add this
    private MongoTemplate mongoTemplate;



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

    @PostMapping("/{productId}/decrement-stock")
    public ResponseEntity<?> decrementStock(
            @PathVariable String productId,
            @RequestBody Map<String, Integer> body) {

        Query query = new Query(
                Criteria.where("_id").is(productId)
                        .and("stock").gte(body.get("quantity"))
        );
        Update update = new Update()
                .inc("stock", -body.get("quantity"));

        Product result = mongoTemplate.findAndModify(
                query, update,
                FindAndModifyOptions.options().returnNew(true),
                Product.class
        );

        if (result == null) {
            return ResponseEntity.status(409)
                    .body(Map.of("error", "OUT_OF_STOCK"));
        }

        return ResponseEntity.ok(Map.of(
                "message", "Stock decremented",
                "remainingStock", result.getStock()
        ));
    }

    @PostMapping("/{productId}/restore-stock")
    public ResponseEntity<?> restoreStock(
            @PathVariable String productId,
            @RequestBody Map<String, Integer> body) {
        Map<String, Object> result =
                productService.restoreStock(productId, body.get("quantity"));
        return ResponseEntity.ok(result);
    }


}
