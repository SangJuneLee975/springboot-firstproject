package com.example.apitest.service;

import com.example.apitest.DTO.Comment;
import java.util.List;

public interface CommentService {
   public List<Comment> getCommentsByBoardId(Long boardId); // 특정 게시글에 대한 모든 댓글 조회
   public void addComment(Comment comment); // 댓글 추가
   public void deleteComment(Long id); // 댓글 삭제
   public void updateComment(Comment comment); // 댓글 수정

   public Comment findById(Long id); // ID로 댓글 찾기

   public List<Comment> getReplyByCommentId(Long commentId); // 댓글에 대한 모든 대댓글 조회

}
