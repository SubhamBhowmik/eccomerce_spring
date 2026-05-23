package com.example.eccomerce.service.impl;

import com.example.eccomerce.models.Product;
import com.example.eccomerce.repository.ProductRepository;
import com.example.eccomerce.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;


@Slf4j
@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    @Autowired  // ← add this
    private MongoTemplate mongoTemplate;

    // Constructor Injection
    public ProductServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }
    @Override
    public Product create(Product product) {
        product.setRating(0);
        product.setReviews(0);
        return productRepository.save(product);
    }

    @Override
    public List<Product> getAll() {
        return productRepository.findAll();
    }

    @Override
    public Product getById(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
    }

    @Override
    public List<Product> getByCategory(String category) {
        return productRepository.findByCategory(category);
    }

    @Override
    public List<Product> getByCategoryAndSubcategory(String category, String subcategory) {
        return productRepository.findByCategoryAndSubcategory(category, subcategory);
    }

    @Override
    public Product update(String id, Product updated) {
        Product product = getById(id);

        product.setName(updated.getName());
        product.setDescription(updated.getDescription());
        product.setImages(updated.getImages());
        product.setCategory(updated.getCategory());
        product.setSubcategory(updated.getSubcategory());
        product.setPrice(updated.getPrice());
        product.setSize(updated.getSize());
        product.setStock(updated.getStock());

        return productRepository.save(product);
    }


    @Override
    public void delete(String id) {
        productRepository.deleteById(id);
    }

    @Override
    public Map<String, Object> restoreStock(String productId, Integer quantity) {
        Query query = new Query(
                Criteria.where("_id").is(productId)
        );
        Update update = new Update().inc("stock", quantity); // ← increment back

        Product result = mongoTemplate.findAndModify(
                query, update,
                FindAndModifyOptions.options().returnNew(true),
                Product.class
        );

        if (result == null) {
            return Map.of("error", "PRODUCT_NOT_FOUND");
        }

        log.info("Stock restored: {} | new stock: {}", productId, result.getStock());
        return Map.of(
                "message", "Stock restored",
                "newStock", result.getStock()
        );
    }


}
