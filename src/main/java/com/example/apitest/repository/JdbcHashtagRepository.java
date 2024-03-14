package com.example.apitest.repository;

import com.example.apitest.DTO.Hashtag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JdbcHashtagRepository implements HashtagRepository {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public JdbcHashtagRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private RowMapper<Hashtag> rowMapper = (rs, rowNum) -> {
        Hashtag hashtag = new Hashtag();
        hashtag.setId(rs.getLong("id"));
        hashtag.setName(rs.getString("name"));
        return hashtag;
    };

    @Override
    public List<Hashtag> findAll() {
        return jdbcTemplate.query("SELECT * FROM hashtag", rowMapper);
    }

    @Override
    public Hashtag findById(Long id) {
        return jdbcTemplate.queryForObject("SELECT * FROM hashtag WHERE id = ?", new Object[]{id}, rowMapper);
    }

    @Override
    public Hashtag create(Hashtag hashtag) {
        jdbcTemplate.update("INSERT INTO hashtag (name) VALUES (?)", hashtag.getName());
        return findByName(hashtag.getName()); // Simplified for demonstration
    }

    @Override
    public void delete(Long id) {
        jdbcTemplate.update("DELETE FROM hashtag WHERE id = ?", id);
    }

    @Override
    public Hashtag findByName(String name) {
        try {
            return jdbcTemplate.queryForObject("SELECT * FROM hashtag WHERE name = ?", new Object[]{name}, rowMapper);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public List<Hashtag> findHashtagsByBoardId(Long boardId) {
        String sql = "SELECT h.* FROM hashtag h INNER JOIN board_hashtag bh ON h.id = bh.hashtag_id WHERE bh.board_id = ?";
        return jdbcTemplate.query(sql, new Object[]{boardId}, rowMapper);
    }
}
