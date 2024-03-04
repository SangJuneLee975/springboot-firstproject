package com.example.apitest.controller;

import com.example.apitest.DTO.Comment;
import com.example.apitest.service.CommentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.apitest.config.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    private final JwtUtils jwtUtils;

    private final CommentService commentService;

    @Autowired
    public CommentController(CommentService commentService, JwtUtils jwtUtils) {
        this.commentService = commentService;
        this.jwtUtils = jwtUtils;
    }

    // 특정 게시글에 대한 모든 댓글 조회
    @GetMapping("/board/{boardId}")
    public ResponseEntity<List<Comment>> getCommentsByBoardId(@PathVariable Long boardId) {

        List<Comment> comments = commentService.getCommentsByBoardId(boardId);
        if (comments.isEmpty()) {

            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(comments);
    }

    // 특정 게시글에 새 댓글 추가
    @PostMapping("/board/{boardId}")
    public ResponseEntity<?> addCommentToBoard(@PathVariable Long boardId, @RequestBody Comment comment, HttpServletRequest request) {
        String token = jwtUtils.extractToken(request);
        if (token != null && jwtUtils.validateToken(token)) {
            String userId = jwtUtils.extractUserId(token);
            comment.setUserId(userId);
            comment.setBoardId(boardId);
            commentService.addComment(comment);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("인증 실패");
        }
    }

    // 댓글 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteComment(@PathVariable Long id, HttpServletRequest request) {
        String token = jwtUtils.extractToken(request);
        if (token != null && jwtUtils.validateToken(token)) {
            commentService.deleteComment(id);
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("인증 실패");
        }
    }

    // 댓글 수정
    @PutMapping("/{id}")
    public ResponseEntity<?> updateComment(@PathVariable Long id, @RequestBody Comment updatedComment, HttpServletRequest request) {
        String token = jwtUtils.extractToken(request);
        if (token == null || !jwtUtils.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("인증 실패");
        }

        String userId = jwtUtils.extractUserId(token);
        Comment existingComment = commentService.findById(id);

        if (existingComment == null) {
            return ResponseEntity.notFound().build();
        }

        if (!existingComment.getUserId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("수정 권한 없음");
        }

        updatedComment.setId(id);
        commentService.updateComment(updatedComment);
        return ResponseEntity.ok().build();
    }


}
