package com.example.apitest.service;

import com.example.apitest.DTO.Board;
import com.example.apitest.DTO.Hashtag;
import com.example.apitest.repository.BoardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BoardServiceImpl implements BoardService {
    private final BoardRepository boardRepository;
    private final JdbcTemplate jdbcTemplate;


    @Autowired
    public BoardServiceImpl(BoardRepository boardRepository, JdbcTemplate jdbcTemplate) {
        this.boardRepository = boardRepository;
        this.jdbcTemplate = jdbcTemplate;
    }
    // 모든 게시판 조회
    @Override
    public List<Board> getAllBoards() {
        return boardRepository.findAll();
    }

    @Override
    public Board getBoardById(Long id) {
        return boardRepository.findById(id);
    }

    // 게시판 생성
    @Override
    public Board createBoard(Board board) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        String sql = "INSERT INTO boards (title, content, writer, date, category_id) VALUES (?, ?, ?, ?, ?)";

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, board.getTitle());
            ps.setString(2, board.getContent());
            ps.setString(3, board.getWriter());
            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(5, board.getCategoryId());
            return ps;
        }, keyHolder);

        // 새로 생성된 게시글 ID를 Board 객체에 설정
        board.setId(keyHolder.getKey().longValue());

        // 해시태그 연결 로직 호출 가능
        if (board.getHashtags() != null) {
            for (Hashtag hashtag : board.getHashtags()) {
                // 해시태그 처리 로직 (생략)
            }
        }

        return board;
    }

//    // 게시판 생성
//    @Override
//    public Board createBoard(Board board) {
//        String sql = "INSERT INTO boards (title, content, writer, date, category_id) VALUES (?, ?, ?, ?, ?)";
//        jdbcTemplate.update(sql, board.getTitle(), board.getContent(), board.getWriter(), LocalDateTime.now(), board.getCategoryId());
//
//        return board;
//    }

    // 게시판 수정
    @Override
    public void updateBoard(Board board) {
        String sql = "UPDATE boards SET title = ?, content = ?, category_id = ? WHERE id = ?";
        jdbcTemplate.update(sql, board.getTitle(), board.getContent(), board.getCategoryId(), board.getId());
    }

    // 게시판 글 삭제
    @Override
    public void deleteBoard(Long id) {
        String sql = "DELETE FROM boards WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    // 게시글 페이징 처리
    @Override
    public Page<Board> getBoardsPaged(int page, int size) {

        String countQuery = "SELECT COUNT(*) FROM boards";
        int totalElements = jdbcTemplate.queryForObject(countQuery, Integer.class);

        // 총 페이지 수를 계산. 전체 게시글 수를 페이지 크기로 나누어 올림 처리
        int totalPages = (int) Math.ceil((double) totalElements / size);

        String sql = "SELECT * FROM boards ORDER BY date DESC LIMIT ? OFFSET ?"; // 현재 페이지에 해당하는 게시글 목록을 조회하는 SQL 쿼리

        int offset = page * size;   // 현재 페이지의 첫 번째 게시글의 인덱스 계산

        List<Board> boards = jdbcTemplate.query(  // JdbcTemplate을 사용하여 SQL 쿼리 실행
                sql,
                new Object[]{size, offset},
                new BeanPropertyRowMapper<>(Board.class));

        return new PageImpl<>(boards, PageRequest.of(page, size), totalElements); // 조회된 게시글 목록과 페이징 정보를 포함하는 Page 객체를 생성하여 반환
    }

    // 게시글과 해시태그 관계
    @Override
    public void addHashtagToBoard(Long boardId, Long hashtagId) {
        // 게시글과 해시태그 연결 로직 구현
        boardRepository.addHashtagToBoard(boardId, hashtagId);
    }
}