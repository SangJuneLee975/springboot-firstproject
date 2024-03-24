package com.example.apitest.repository;

import com.example.apitest.DTO.Board;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.Arrays;
import java.util.List;

import com.example.apitest.DTO.Board;
import org.springframework.jdbc.core.RowMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

@Repository
public class JdbcBoardRepository implements BoardRepository {
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public JdbcBoardRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }


    @Override
    public List<Board> findAll() {
        String sql = "SELECT * FROM boards";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Board.class));
    }

    @Override
    public Board findById(Long id) {
        String sql = "SELECT * FROM boards WHERE id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, new Object[]{id}, new BeanPropertyRowMapper<>(Board.class));
        } catch (EmptyResultDataAccessException e) {
            return null; // 결과가 없을 경우 null 반환
        }
    }


    @Override
    public Long create(Board board) {
        final String sql = "INSERT INTO boards (title, content, writer, date, category_id) VALUES (?, ?, ?, NOW(), ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[] {"id"}); // "id"는 자동 생성된 키의 컬럼명
            ps.setString(1, board.getTitle());
            ps.setString(2, board.getContent());
            ps.setString(3, board.getWriter());
            ps.setLong(4, board.getCategoryId());
            return ps;
        }, keyHolder);

        return keyHolder.getKey().longValue(); // 생성된 게시글 ID 반환
    }


    @Override
    public void update(Board board) {
        String sql = "UPDATE boards SET title = ?, content = ? WHERE id = ?";
        jdbcTemplate.update(sql, board.getTitle(), board.getContent(), board.getId());
    }

    @Override
    public void delete(Long id) {
        String sql = "DELETE FROM boards WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    // 게시글과 해시태그 연결 로직 구현
    public void addHashtagToBoard(Long boardId, Long hashtagId) {
        String sql = "INSERT INTO board_hashtag (board_id, hashtag_id) VALUES (?, ?)";
        jdbcTemplate.update(sql, boardId, hashtagId);
    }


    // JdbcBoardRepository 클래스에 추가할 saveImageUrls 메서드
    public void saveImageUrls(List<String> imageUrls, Long boardId) {
        String sql = "INSERT INTO images (image_url, board_id) VALUES (?, ?)";
        for (String imageUrl : imageUrls) {
            jdbcTemplate.update(sql, imageUrl, boardId);
        }

    }
}