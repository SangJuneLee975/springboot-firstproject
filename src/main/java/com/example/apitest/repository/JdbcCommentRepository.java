package com.example.apitest.repository;

import com.example.apitest.DTO.Comment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
        comment.setDepth(rs.getInt("depth"));
        comment.setOrder(rs.getInt("comment_order"));
        return comment;
    };

    @Override
    public List<Comment> findByBoardId(Long boardId) {
        String sql = "SELECT * FROM comment WHERE board_id = ?";
        return jdbcTemplate.query(sql, new Object[]{boardId}, rowMapper);
    }

    @Override
    public void create(Comment comment) {

        Integer newDepth = 0;      // parentId가 null이 아닌 경우 부모 댓글의 depth를 기반으로 새 depth
        if (comment.getParentId() != null) {
            String depthSql = "SELECT depth FROM comment WHERE id = ?";
            Integer parentDepth = jdbcTemplate.queryForObject(depthSql, new Object[]{comment.getParentId()}, Integer.class);
            newDepth = parentDepth != null ? parentDepth + 1 : newDepth;
        }

        String sql = "INSERT INTO comment (board_id, user_id, content, parent_id, depth, comment_order, date) VALUES (?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, comment.getBoardId(), comment.getUserId(), comment.getContent(), comment.getParentId(), newDepth, comment.getOrder(), Timestamp.valueOf(LocalDateTime.now()));
    }


    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM comment WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    @Override
    public void update(Comment comment) {
        String sql = "UPDATE comment SET content = ? WHERE id = ?";
        jdbcTemplate.update(sql, comment.getContent(), comment.getId());
    }

    @Override
    public Comment findById(Long id) {
        try {
            String sql = "SELECT * FROM comment WHERE id = ?";
            return jdbcTemplate.queryForObject(sql, new Object[]{id}, rowMapper);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public List<Comment> findByParentId(Long parentId) {
        String sql = "SELECT * FROM comment WHERE parent_id = ?";
        return jdbcTemplate.query(sql, new Object[]{parentId}, rowMapper);
    }
}
