package com.example.apitest.repository;

import com.example.apitest.DTO.Category;
import java.util.List;

public interface CategoryRepository {
    public List<Category> findAll();
    // 기타 필요한 메소드 정의 가능
}
