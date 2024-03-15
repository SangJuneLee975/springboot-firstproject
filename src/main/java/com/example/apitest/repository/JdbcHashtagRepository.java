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

    // 모든 해시태그를 조회
    public List<Hashtag> findAll() {
        return jdbcTemplate.query("SELECT * FROM hashtag", rowMapper);
    }

    // ID를 통해 특정 해시태그를 조회
    @Override
    public Hashtag findById(Long id) {
        return jdbcTemplate.queryForObject("SELECT * FROM hashtag WHERE id = ?", new Object[]{id}, rowMapper);
    }

    // 새로운 해시태그를 생성
    @Override
    public Hashtag create(Hashtag hashtag) {
        jdbcTemplate.update("INSERT INTO hashtag (name) VALUES (?)", hashtag.getName());
        return findByName(hashtag.getName()); // Simplified for demonstration
    }

    // ID를 통해 특정 해시태그를 삭제
    @Override
    public void delete(Long id) {
        jdbcTemplate.update("DELETE FROM hashtag WHERE id = ?", id);
    }

    // 이름을 통해 특정 해시태그를 조회
    @Override
    public Hashtag findByName(String name) {
        try {
            return jdbcTemplate.queryForObject("SELECT * FROM hashtag WHERE name = ?", new Object[]{name}, rowMapper);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    // 특정 게시판 ID에 연결된 모든 해시태그를 조회
    @Override
    public List<Hashtag> findHashtagsByBoardId(Long boardId) {
        String sql = "SELECT h.* FROM hashtag h INNER JOIN board_hashtag bh ON h.id = bh.hashtag_id WHERE bh.board_id = ?";
        return jdbcTemplate.query(sql, new Object[]{boardId}, rowMapper);
    }

    // ID를 통해 해시태그 정보를 갱신
    @Override
    public Hashtag update(Long id, Hashtag hashtag) {
        jdbcTemplate.update("UPDATE hashtag SET name = ? WHERE id = ?", hashtag.getName(), id);
        return findById(id);
    }

    // 특정 게시판에 연결된 모든 해시태그를 제거
    @Override
    public void removeHashtagsFromBoard(Long boardId) {
        jdbcTemplate.update("DELETE FROM board_hashtag WHERE board_id = ?", boardId);
    }

    // 게시글에 해시태그를 연결하는 메소드
    @Override
    public void addHashtagToBoard(Long boardId, Hashtag hashtag) {
        // 먼저 해시태그가 존재하는지 확인하고, 없으면 생성
        Hashtag existingHashtag = findByName(hashtag.getName());
        Long hashtagId = existingHashtag != null ? existingHashtag.getId() : create(hashtag).getId();

        // 게시글과 해시태그 연결을 추가
        jdbcTemplate.update("INSERT INTO board_hashtag (board_id, hashtag_id) VALUES (?, ?)", boardId, hashtagId);
    }
}
