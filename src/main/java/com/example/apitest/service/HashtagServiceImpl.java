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

    // 모든 해시태그를 조회하는 메소드
    @Override
    public List<Hashtag> findAllHashtags() {
        return hashtagRepository.findAll();
    }

    // ID를 통해 해시태그를 조회하는 메소드
    @Override
    public Hashtag findHashtagById(Long id) {
        return hashtagRepository.findById(id);
    }

    // 해시태그를 생성하는 메소드
    @Override
    public Hashtag createHashtag(Hashtag hashtag) {
        return hashtagRepository.create(hashtag);
    }

    // ID를 통해 해시태그를 삭제하는 메소드
    @Override
    public void deleteHashtag(Long id) {
        hashtagRepository.delete(id);
    }

    // 이름을 통해 해시태그를 조회하는 메소드
    @Override
    public Hashtag findHashtagByName(String name) {
        return hashtagRepository.findByName(name);
    }


    // 게시판 ID를 통해 연결된 해시태그를 조회하는 메소드
    @Override
    public List<Hashtag> findHashtagsByBoardId(Long boardId) {
      return hashtagRepository.findHashtagsByBoardId(boardId);
    }

    // ID를 통해 해시태그 정보를 갱신하는 메소드
    @Override
    public Hashtag updateHashtag(Long id, Hashtag hashtag) {
        return hashtagRepository.update(id, hashtag);
    }

    // 게시글에 연결된 해시태그를 모두 제거
    @Override
    public void removeHashtagsFromBoard(Long boardId) {
        hashtagRepository.removeHashtagsFromBoard(boardId);
    }

    // 게시글에 해시태그를 연결
    @Override
    public void addHashtagToBoard(Long boardId, Hashtag hashtag) {
        Hashtag existingHashtag = findHashtagByName(hashtag.getName());
        if (existingHashtag == null) {
            existingHashtag = createHashtag(hashtag);
        }
        hashtagRepository.addHashtagToBoard(boardId, existingHashtag);
    }
}