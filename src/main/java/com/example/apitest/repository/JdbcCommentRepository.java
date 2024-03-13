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
        comment.setNickname(rs.getString("nickname"));
        comment.setDate(rs.getTimestamp("date").toLocalDateTime());
        comment.setBoardId(rs.getLong("board_id"));
        comment.setDepth(rs.getInt("depth"));
        comment.setOrder(rs.getInt("comment_order"));
        return comment;
    };

    @Override
    public List<Comment> findByBoardId(Long boardId) {
        String sql = "SELECT c.*, u.nickname FROM comment c " +
                "JOIN user u ON c.user_id = u.userId " +
                "WHERE c.board_id = ? " +
                "ORDER BY c.parent_id ASC, c.comment_order ASC, c.date ASC";
        return jdbcTemplate.query(sql, new Object[]{boardId}, rowMapper);
    }

    @Override
    public void create(Comment comment) {

        Integer newDepth = 0;      // parentId가 null이 아닌 경우 부모 댓글의 depth를 기반으로 새 depth
        Integer newOrder = 0; // 새로운 comment_order 값을 저장할 변수
        if (comment.getParentId() != null) {
            String depthSql = "SELECT depth FROM comment WHERE id = ?";
            Integer parentDepth = jdbcTemplate.queryForObject(depthSql, new Object[]{comment.getParentId()}, Integer.class);
            newDepth = parentDepth != null ? parentDepth + 1 : newDepth;

            String orderSql = "SELECT MAX(comment_order) FROM comment WHERE parent_id = ?";
            Integer maxOrder = jdbcTemplate.queryForObject(orderSql, new Object[]{comment.getParentId()}, Integer.class);
            newOrder = maxOrder != null ? maxOrder + 1 : 0; // 최대값이 null이면 0으로 시작, 아니면 +1
        }

        String sql = "INSERT INTO comment (board_id, user_id, content, parent_id, depth, comment_order, date) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, comment.getBoardId(), comment.getUserId(), comment.getContent(),
                comment.getParentId(), newDepth, newOrder, Timestamp.valueOf(LocalDateTime.now()));
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
            String sql = "SELECT c.*, u.nickname FROM comment c JOIN user u ON c.user_id = u.userId WHERE c.id = ?";
            return jdbcTemplate.queryForObject(sql, new Object[]{id}, rowMapper);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public List<Comment> findByParentId(Long parentId) {
        String sql = "SELECT * FROM comment WHERE parent_id = ? ORDER BY date ASC";
        return jdbcTemplate.query(sql, new Object[]{parentId}, rowMapper);
    }
}
