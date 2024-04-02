package com.example.apitest.repository;

import com.example.apitest.DTO.Image;
import com.example.apitest.service.BoardServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.List;

@Repository
public class JdbcImageRepository implements ImageRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final Logger logger = LoggerFactory.getLogger(JdbcImageRepository.class);

    @Autowired
    public JdbcImageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Image> findAll() {
        return jdbcTemplate.query("SELECT * FROM images", new ImageRowMapper());
    }

    @Override
    public Image findById(Long id) {
        return jdbcTemplate.queryForObject("SELECT * FROM images WHERE id = ?", new Object[]{id}, new ImageRowMapper());
    }

    @Override
    public void save(Image image) {
        String sql = "INSERT INTO images (image_url, board_id) VALUES (?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, image.getImageUrl());
            ps.setObject(2, image.getBoardId(), Types.BIGINT); // boardId가 null인 경우를 처리
            return ps;
        }, keyHolder);

        if (keyHolder.getKey() != null) {
            image.setId(keyHolder.getKey().longValue());
            logger.info("이미지가 저장되었습니다. Image ID: {}", image.getId());
        } else {
            logger.error("이미지 저장에 실패했습니다. Image URL: {}", image.getImageUrl());
        }
    }

    @Override
    public void update(Image image) {
        jdbcTemplate.update("UPDATE images SET image_url = ?, board_id = ? WHERE id = ?", image.getImageUrl(), image.getBoardId(), image.getId());
    }

    @Override
    public void delete(Long id) {
        jdbcTemplate.update("DELETE FROM images WHERE id = ?", id);
    }

    @Override
    public void saveImageUrls(List<String> imageUrls, Long boardId) {
        String sql = "INSERT INTO images (image_url, board_id) VALUES (?, ?)";
        for (String imageUrl : imageUrls) {
            jdbcTemplate.update(sql, imageUrl, boardId);
        }
    }

    @Override
    public void deleteByImageUrl(String imageUrl) {
        String sql = "DELETE FROM images WHERE image_url = ?";
        int affectedRows = jdbcTemplate.update(sql, imageUrl);
        if(affectedRows > 0){
            logger.info("이미지가 데이터베이스에서 삭제되었습니다: {}", imageUrl);
        } else {
            logger.warn("삭제할 이미지를 찾지 못했습니다: {}", imageUrl);
        }
    }

    @Override
    public List<String> findImageUrlsByBoardId(Long boardId) {
        String sql = "SELECT image_url FROM images WHERE board_id = ?";
        return jdbcTemplate.queryForList(sql, String.class, boardId);
    }

    private static final class ImageRowMapper implements RowMapper<Image> {
        @Override
        public Image mapRow(ResultSet rs, int rowNum) throws SQLException {
            Long id = rs.getLong("id");
            String imageUrl = rs.getString("image_url");
            Long boardId = rs.getLong("board_id");
            return new Image(id, imageUrl, boardId);
        }
    }
}
