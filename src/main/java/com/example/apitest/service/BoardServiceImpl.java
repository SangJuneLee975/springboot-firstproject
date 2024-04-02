package com.example.apitest.service;

import com.amazonaws.AmazonServiceException;
import com.example.apitest.DTO.Board;
import com.example.apitest.DTO.Hashtag;
import com.example.apitest.DTO.Image;
import com.example.apitest.repository.BoardRepository;
import com.example.apitest.repository.ImageRepository;
import com.example.apitest.util.JsonUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
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
import java.util.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;


@Service
public class BoardServiceImpl implements BoardService {
    private final BoardRepository boardRepository;
    private final JdbcTemplate jdbcTemplate;
    private final AwsS3Service awsS3Service;
    private final ImageRepository imageRepository;

    private static final Logger logger = LoggerFactory.getLogger(BoardServiceImpl.class);

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

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

                //  이미지 URL 목록 조회
                List<String> imageUrls = imageRepository.findImageUrlsByBoardId(board.getId());
                board.setImageUrls(imageUrls);
                return board;
            }
        });
    }


    // 게시판 상세 조회
    @Override
    public Board getBoardById(Long id) {
        String sql = "SELECT b.*, i.image_url " +
                "FROM boards b " +
                "LEFT JOIN images i ON b.id = i.board_id " +
                "WHERE b.id = ?";

        Map<Long, Board> boards = new HashMap<>();
        jdbcTemplate.query(sql, new Object[]{id}, rs -> {
            Long boardId = rs.getLong("id");
            Board board = boards.get(boardId);
            if (board == null) {
                // Board 객체를 새로 생성하고 기본 정보를 설정
                board = new Board();
                board.setId(boardId);
                board.setTitle(rs.getString("title"));
                board.setContent(rs.getString("content"));
                board.setWriter(rs.getString("writer"));
                board.setDate(rs.getTimestamp("date").toLocalDateTime());
                board.setCategoryId(rs.getLong("category_id"));
                board.setImageUrls(new ArrayList<>()); // 이미지 URL을 담을 리스트를 초기화
                boards.put(boardId, board);
            }
            // 이미지 URL이 있으면 리스트에 추가
            String imageUrl = rs.getString("image_url");
            if (imageUrl != null) {
                board.getImageUrls().add(imageUrl);
            }
        });

        // 조회된 게시글이 없으면 null을 반환
        return boards.isEmpty() ? null : boards.values().iterator().next();
    }


    // 게시판 생성
    @Override
    public Board createBoard(Board board, List<MultipartFile> multipartFiles) throws IOException {

        Long boardId = boardRepository.save(board);
        board.setId(boardId);

        List<String> imageUrls = new ArrayList<>();
        if (multipartFiles != null && !multipartFiles.isEmpty()) {
            for (MultipartFile file : multipartFiles) {
                if (!file.isEmpty()) {
                    // 각 파일(이미지)을 AWS S3에 업로드하고, 업로드된 이미지의 URL을 받음
                    String imageUrl = awsS3Service.uploadFileToS3(file);
                    imageUrls.add(imageUrl);

                    // 업로드된 이미지 정보를 데이터베이스에 저장
                    Image image = new Image(null, imageUrl, board.getId());
                    imageRepository.save(image);
                }
            }
        }
        board.setImageUrls(imageUrls);
        return board;
    }




    // 게시판 수정
    @Override
    public void updateBoard(Board board, List<String> deletedImageUrls) throws JsonProcessingException {
        String sqlUpdateBoard = "UPDATE boards SET title = ?, content = ?, category_id = ? WHERE id = ?";
        jdbcTemplate.update(sqlUpdateBoard, board.getTitle(), board.getContent(), board.getCategoryId(), board.getId());

    }



    // 게시판 글 삭제
    @Override
    public void deleteBoard(Long id) {
        Board board = getBoardById(id);
        if (board != null) {
            List<String> imageUrls = board.getImageUrls();
            imageUrls.forEach(imageUrl -> {
                try {
                    awsS3Service.deleteFileFromS3(imageUrl);
                    imageRepository.deleteByImageUrl(imageUrl); // 이미지 URL을 images 테이블에서 삭제
                } catch (AmazonServiceException e) {
                    logger.error("Error deleting image from S3: {}", e.getMessage(), e);
                }
            });
            // DB에서 게시글 삭제
            String sql = "DELETE FROM boards WHERE id = ?";
            jdbcTemplate.update(sql, id);
        }
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