package com.example.apitest.repository;

import com.example.apitest.DTO.Hashtag;

import java.util.List;

public interface HashtagRepository {
    public List<Hashtag> findAll();
    public Hashtag findById(Long id);
    public Hashtag create(Hashtag hashtag);
    public void delete(Long id);
    public Hashtag findByName(String name);
}
