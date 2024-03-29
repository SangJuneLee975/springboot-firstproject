package com.example.apitest.repository;

import com.example.apitest.DTO.Board;
import com.example.apitest.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import com.fasterxml.jackson.core.JsonProcessingException;

import com.example.apitest.util.JsonUtil;
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

    private static final Logger logger = LoggerFactory.getLogger(JdbcBoardRepository.class);

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
    public void create(Board board) {
        String sql = "INSERT INTO boards (title, content) VALUES (?, ?)";
        jdbcTemplate.update(sql, board.getTitle(), board.getContent());
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
            try {
                jdbcTemplate.update(sql, imageUrl, boardId);
            } catch (DataAccessException e) {
                logger.error("Cannot save image url: {}", imageUrl, e);
                throw new RuntimeException("이미지 URL 저장 중 문제가 발생했습니다.", e);
            }
        }
    }
}