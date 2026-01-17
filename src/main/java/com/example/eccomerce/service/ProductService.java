package com.example.eccomerce.service;

import com.example.eccomerce.models.Product;

import java.util.List;

public interface ProductService {
    Product create(Product product);

    List<Product> getAll();

    Product getById(String id);

    List<Product> getByCategory(String category);

    List<Product> getByCategoryAndSubcategory(String category, String subcategory);

    Product update(String id, Product product);

    void delete(String id);
}
