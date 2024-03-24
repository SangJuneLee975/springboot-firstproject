package com.example.apitest.service;

import com.example.apitest.DTO.Board;
import com.example.apitest.DTO.Hashtag;
import com.example.apitest.repository.BoardRepository;
import com.example.apitest.repository.ImageRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Optional;
import com.fasterxml.jackson.databind.ObjectMapper;


@Service
public class BoardServiceImpl implements BoardService {
    private final BoardRepository boardRepository;
    private final JdbcTemplate jdbcTemplate;
    private final AwsS3Service awsS3Service;
    private final ImageRepository imageRepository;

    private static final Logger logger = LoggerFactory.getLogger(BoardServiceImpl.class);

    @Autowired
    public BoardServiceImpl(BoardRepository boardRepository, JdbcTemplate jdbcTemplate, AwsS3Service awsS3Service, ImageRepository imageRepository) {
        this.boardRepository = boardRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.awsS3Service = awsS3Service;
        this.imageRepository = imageRepository;
    }
    // 모든 게시판 조회
    @Override
    public List<Board> getAllBoards() {
        return jdbcTemplate.query("SELECT * FROM boards", new RowMapper<Board>() {
            @Override
            public Board mapRow(ResultSet rs, int rowNum) throws SQLException {
                Board board = new Board();
                board.setId(rs.getLong("id"));
                board.setTitle(rs.getString("title"));
                board.setContent(rs.getString("content"));
                board.setWriter(rs.getString("writer"));
                board.setDate(rs.getTimestamp("date").toLocalDateTime());
                board.setCategoryId(rs.getLong("category_id"));
                // 여기서 추가 데이터 조회가 필요한 경우 해당 로직을 구현
                // 예: 이미지 URL 목록 조회
                List<String> imageUrls = imageRepository.findImageUrlsByBoardId(board.getId());
                board.setImageUrls(imageUrls);
                return board;
            }
        });
    }

        @Override
    public Board getBoardById(Long id) {
            Board board = boardRepository.findById(id);
            if (board != null) {
                List<String> imageUrls = imageRepository.findImageUrlsByBoardId(board.getId());
                board.setImageUrls(imageUrls);
            }
            return board;
        }

 //   @Override
 //   public Board findById(Long id) {
 //       String sql = "SELECT * FROM boards WHERE id = ?";
 //       Board board = jdbcTemplate.queryForObject(sql, new Object[]{id}, new BeanPropertyRowMapper<>(Board.class));
  //      if (board != null) {
   //         List<String> imageUrls = jdbcTemplate.query("SELECT image_url FROM images WHERE board_id = ?",
    //                new Object[]{id}, (rs, rowNum) -> rs.getString("image_url"));
    //        board.setImageUrls(imageUrls);
    //    }
    //    return board;
  //  }


//    @Override
//    public Board getBoardById(Long id) {
//        return boardRepository.findById(id);
//    }

 //   @Override
  //  public List<String> getImageUrlsByBoardId(Long boardId) {
  //      Board board = boardRepository.findById(boardId);
 //       if (board != null && board.getImageUrls() != null) {
 //           return board.getImageUrls();
////        }
//        return new ArrayList<>();
//    }


    // 게시판 생성
    @Override
    public Board createBoard(Board board, List<MultipartFile> multipartFiles) throws IOException {

        // 파일 URL 리스트를 세미콜론으로 구분된 하나의 문자열로 변환

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

        Long boardId = boardRepository.create(board);
        logger.info("Board created with ID: {}", boardId);

        if (multipartFiles != null && !multipartFiles.isEmpty()) {
            List<String> imageUrls = new ArrayList<>();
            for (MultipartFile file : multipartFiles) {
                String imageUrl = awsS3Service.uploadFileToS3(file);
                imageUrls.add(imageUrl);
                logger.info("Image uploaded with URL: {}", imageUrl);
            }
            boardRepository.saveImageUrls(imageUrls, boardId);
            logger.info("Image URLs saved for board ID: {}", boardId);
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

    private List<String> convertJsonToList(String jsonImageUrls) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(jsonImageUrls, new TypeReference<List<String>>(){});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 변환 중 오류 발생", e);
        }
    }


}