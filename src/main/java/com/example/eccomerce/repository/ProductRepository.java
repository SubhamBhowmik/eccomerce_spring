package com.example.eccomerce.repository;

import com.example.eccomerce.models.Product;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {
    List<Product> findByCategory(String category);

    List<Product> findByCategoryAndSubcategory(String category, String subcategory);
}
