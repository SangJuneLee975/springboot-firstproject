package com.example.apitest.service;

import com.example.apitest.DTO.Hashtag;

import java.util.List;

public interface HashtagService {
    public List<Hashtag> findAllHashtags();
    public Hashtag findHashtagById(Long id);
    public Hashtag createHashtag(Hashtag hashtag);
    public void deleteHashtag(Long id);
    public Hashtag findHashtagByName(String name);

    public List<Hashtag> findHashtagsByBoardId(Long boardId); //게시글 ID를 기반으로 해시태그를 조회
}
