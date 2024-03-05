package com.example.apitest.repository;

import com.example.apitest.DTO.Comment;
import java.util.List;
import java.util.Optional;

public interface CommentRepository {
   public List<Comment> findByBoardId(Long boardId); // 특정 게시글에 대한 모든 댓글 조회
   public void create(Comment comment); // 새 댓글 생성
   public void deleteById(Long id); // 댓글 삭제
   public void update(Comment comment); // 댓글 수정

   public Comment findById(Long id); // ID로 댓글 찾기

   public List<Comment> findByParentId(Long parentId); // 부모 댓글에 대한 모든 대댓글 조회


}
