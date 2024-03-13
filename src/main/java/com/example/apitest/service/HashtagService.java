package com.example.apitest.service;

import com.example.apitest.DTO.Hashtag;

import java.util.List;

public interface HashtagService {
    public List<Hashtag> findAllHashtags();
    public Hashtag findHashtagById(Long id);
    public Hashtag createHashtag(Hashtag hashtag);
    public void deleteHashtag(Long id);
    public Hashtag findHashtagByName(String name);
}
