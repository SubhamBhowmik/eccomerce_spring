package com.example.eccomerce.models;

import java.util.List;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "Products")
@Data
public class Product {
    @Id
    private String id;

    // Array of image URLs (Cloudinary, etc.)
    private List<String> images;

    private String name;
    private String description;

    private String category;
    private String subcategory;

    private double price;

    private double rating;   // average rating (e.g. 4.5)

    private int reviews;     // total number of reviews

    private List<String> size; // e.g. S, M, L, XL (or storage sizes)

    private int stock;       // available quantity
}
