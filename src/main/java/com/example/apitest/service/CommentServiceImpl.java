package com.example.apitest.service;

import com.example.apitest.DTO.Comment;
import com.example.apitest.repository.CommentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository; // 댓글 관련 데이터 접근을 위한 리포지토리

    @Autowired
    public CommentServiceImpl(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    @Override
    public List<Comment> getCommentsByBoardId(Long boardId) {
        return commentRepository.findByBoardId(boardId);
    }

    @Override
    public void addComment(Comment comment) {
        commentRepository.create(comment);
    }
}
