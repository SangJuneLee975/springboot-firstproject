package com.example.apitest.repository;

import com.example.apitest.DTO.Comment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class JdbcCommentRepository implements CommentRepository {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public JdbcCommentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private RowMapper<Comment> rowMapper = (rs, rowNum) -> {
        Comment comment = new Comment();
        comment.setId(rs.getLong("id"));
        comment.setContent(rs.getString("content"));
        comment.setUserId(rs.getString("user_id"));
        comment.setDate(rs.getTimestamp("date").toLocalDateTime());
        comment.setBoardId(rs.getLong("board_id"));
        return comment;
    };

    @Override
    public List<Comment> findByBoardId(Long boardId) {
        String sql = "SELECT * FROM comments WHERE board_id = ?";
        return jdbcTemplate.query(sql, new Object[]{boardId}, rowMapper);
    }

    @Override
    public void create(Comment comment) {
        String sql = "INSERT INTO comments (content, user_id, date, board_id) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sql, comment.getContent(), comment.getUserId(), LocalDateTime.now(), comment.getBoardId());
    }
}
