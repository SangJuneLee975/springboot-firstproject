package com.example.apitest.service;

import com.example.apitest.DTO.Comment;
import java.util.List;

public interface CommentService {
   public List<Comment> getCommentsByBoardId(Long boardId); // 특정 게시글에 대한 모든 댓글 조회
   public void addComment(Comment comment); // 새 댓글 추가
}
