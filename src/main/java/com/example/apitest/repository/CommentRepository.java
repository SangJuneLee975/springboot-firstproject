package com.example.apitest.repository;

import com.example.apitest.DTO.Comment;
import java.util.List;
public interface CommentRepository {
   public List<Comment> findByBoardId(Long boardId); // 특정 게시글에 대한 모든 댓글 조회
   public void create(Comment comment); // 새 댓글 생성
}
