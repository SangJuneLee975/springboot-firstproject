package com.example.apitest.controller;

import com.example.apitest.DTO.Hashtag;
import com.example.apitest.service.HashtagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hashtags") // 이 경로로 요청이 들어오면 처리하는 컨트롤러
public class HashtagController {


    private final HashtagService hashtagService;
    @Autowired
    public HashtagController(HashtagService hashtagService) {
        this.hashtagService = hashtagService;
    }

    // 모든 해시태그 조회
    @GetMapping
    public ResponseEntity<List<Hashtag>> getAllHashtags() {
        List<Hashtag> hashtags = hashtagService.findAllHashtags();
        return ResponseEntity.ok(hashtags);
    }

    // 해시태그 생성
    @PostMapping
    public ResponseEntity<Hashtag> createHashtag(@RequestBody Hashtag hashtag) {
        Hashtag createdHashtag = hashtagService.createHashtag(hashtag);
        return ResponseEntity.ok(createdHashtag);
    }

    // 해시태그 수정
    @PutMapping("/{id}")
    public ResponseEntity<Hashtag> updateHashtag(@PathVariable Long id, @RequestBody Hashtag hashtag) {
        Hashtag existingHashtag = hashtagService.findHashtagById(id);
        if (existingHashtag == null) {
            return ResponseEntity.notFound().build();
        }
        existingHashtag.setName(hashtag.getName());
        Hashtag updatedHashtag = hashtagService.updateHashtag(id, existingHashtag);
        return ResponseEntity.ok(updatedHashtag);
    }

    // 해시태그 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHashtag(@PathVariable Long id) {
        hashtagService.deleteHashtag(id);
        return ResponseEntity.ok().build();
    }

    // 게시글 ID를 기반으로 해시태그 조회
    @GetMapping("/board/{boardId}")
    public ResponseEntity<List<Hashtag>> getHashtagsByBoardId(@PathVariable Long boardId) {
        List<Hashtag> hashtags = hashtagService.findHashtagsByBoardId(boardId);
        return ResponseEntity.ok(hashtags);
    }
}