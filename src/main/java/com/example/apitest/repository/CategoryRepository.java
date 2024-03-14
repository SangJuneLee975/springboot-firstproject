package com.example.apitest.repository;

import com.example.apitest.DTO.Category;
import java.util.List;

public interface CategoryRepository {
    public List<Category> findAll();

}
