package com.example.apitest.service;

import com.example.apitest.DTO.Hashtag;
import com.example.apitest.repository.HashtagRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HashtagServiceImpl implements HashtagService {

    private final HashtagRepository hashtagRepository;

    @Autowired
    public HashtagServiceImpl(HashtagRepository hashtagRepository) {
        this.hashtagRepository = hashtagRepository;
    }

    @Override
    public List<Hashtag> findAllHashtags() {
        return hashtagRepository.findAll();
    }

    @Override
    public Hashtag findHashtagById(Long id) {
        return hashtagRepository.findById(id);
    }

    @Override
    public Hashtag createHashtag(Hashtag hashtag) {
        return hashtagRepository.create(hashtag);
    }

    @Override
    public void deleteHashtag(Long id) {
        hashtagRepository.delete(id);
    }

    @Override
    public Hashtag findHashtagByName(String name) {
        return hashtagRepository.findByName(name);
    }


    @Override
    public List<Hashtag> findHashtagsByBoardId(Long boardId) {
      return hashtagRepository.findHashtagsByBoardId(boardId);
    }

}