package com.example.apitest.repository;

import com.example.apitest.DTO.Image;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

@Repository
public class JdbcImageRepository implements ImageRepository {

    private final JdbcTemplate jdbcTemplate;

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
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO images (image_url, board_id) VALUES (?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, image.getImageUrl());
            ps.setLong(2, image.getBoardId());
            return ps;
        }, keyHolder);

        // 데이터베이스의 ID 컬럼이 'id'라고 가정
        Number key = keyHolder.getKey();
        if (key != null) {
            image.setId(key.longValue());
            System.out.println("DB에 이미지 ID와 함께 저장됨: " + image.getId());
        } else {
            System.out.println("저장된 이미지의 ID를 가져오기 실패함.");
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
