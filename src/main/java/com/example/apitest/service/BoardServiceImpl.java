package com.example.apitest.service;

import com.amazonaws.AmazonServiceException;
import com.example.apitest.DTO.Board;
import com.example.apitest.DTO.Hashtag;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Optional;
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
        return boardRepository.findById(id);
    }


    // 게시판 생성
    @Override
    public Board createBoard(Board board, List<MultipartFile> multipartFiles) throws IOException {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        String sql = "INSERT INTO boards (title, content, writer, date, category_id, image_urls) VALUES (?, ?, ?, ?, ?, ?)";

        try {
            List<String> imageUrls = new ArrayList<>();
            if (multipartFiles != null && !multipartFiles.isEmpty()) {
                // 이미지 파일들을 S3에 업로드하고, URL을 리스트로 수집
                for (MultipartFile file : multipartFiles) {
                    if (!file.isEmpty()) {
                        String imageUrl = awsS3Service.uploadFileToS3(file);
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            imageUrls.add(imageUrl);
                            logger.info("Image uploaded to S3 and URL added to list: {}", imageUrl);
                        } else {
                            // 파일 업로드 실패 처리
                            logger.error("Failed to upload file to S3: {}", file.getOriginalFilename());

                        }
                    }
                }
            }


            //String imageUrlsJson = JsonUtil.listToJson(board.getImageUrls()); // imageUrls를 JSON으로 변환

            // imageUrls 리스트를 JSON 문자열로 변환하여 저장
         //   String imageUrlsJson = JsonUtil.convertListToJson(imageUrls); // 수정된 부분

            String imageUrlsJson = JsonUtil.convertListToJson(board.getImageUrls()); // 수정된 부분



            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, board.getTitle());
                ps.setString(2, board.getContent());
                ps.setString(3, board.getWriter());
                ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
                ps.setLong(5, board.getCategoryId());
                ps.setString(6, imageUrlsJson); // JSON 문자열 저장
                return ps;
            }, keyHolder);

            // 새로 생성된 게시글 ID를 Board 객체에 설정
            Long boardId = keyHolder.getKey().longValue();
            board.setId(boardId);
            board.setImageUrls(imageUrls); // 이미지 URL 리스트 설정

            // images 테이블에 이미지 URL들 저장
            if (!imageUrls.isEmpty()) {
                logger.info("Saving image URLs to the images table for boardId: {}", boardId);
                imageRepository.saveImageUrls(imageUrls, boardId);
            }


            return board;

        } catch (DataAccessException | IOException e) {

            logger.error("Error creating board: {}", e.getMessage(), e);
            throw new RuntimeException("게시글 생성 중 문제가 발생했습니다.", e);
        }
    }



    // 게시판 수정
    @Override
    public void updateBoard(Board board, List<String> deletedImageUrls) throws JsonProcessingException {
        String sqlUpdateBoard = "UPDATE boards SET title = ?, content = ?, category_id = ? WHERE id = ?";
        jdbcTemplate.update(sqlUpdateBoard, board.getTitle(), board.getContent(), board.getCategoryId(), board.getId());

        if (deletedImageUrls != null && !deletedImageUrls.isEmpty()) {
            deletedImageUrls.forEach(url -> {
                try {
                    deleteImage(url);
                } catch (RuntimeException e) { // deleteImage에서 RuntimeException을 던짐
                    logger.error("Error deleting image: {}", e.getMessage(), e);
                }
            });
        }

        // 변경된 이미지 URL 리스트를 데이터베이스에 업데이트합니다.
        updateBoardImageUrls(board.getId(), board.getImageUrls());
    }

    @Override
    public void updateBoardImageUrls(Long boardId, List<String> imageUrls) throws JsonProcessingException {
        String imageUrlsJson = JsonUtil.convertListToJson(imageUrls); // 이미 JSON 변환 메서드를 호출
        String sqlUpdateImageUrls = "UPDATE boards SET image_urls = ? WHERE id = ?";
        jdbcTemplate.update(sqlUpdateImageUrls, imageUrlsJson, boardId);
    }


    // 게시판 글 삭제
    @Override
    public void deleteBoard(Long id) {
        Board board = getBoardById(id);
        if (board != null) {
            List<String> imageUrls = board.getImageUrls(); // 이미 리스트이기 때문에 변환 과정 생략
            imageUrls.forEach(imageUrl -> {
                try {
                    awsS3Service.deleteFileFromS3(imageUrl);
                } catch (AmazonServiceException e) {
                    logger.error("Error deleting image from S3: {}", e.getMessage(), e);
                }
            });
            // DB에서 게시글 삭제
            String sql = "DELETE FROM boards WHERE id = ?";
            jdbcTemplate.update(sql, id);
        }
    }


//    @Override
//    public void deleteBoard(Long id) {
//        // 게시글에 연결된 이미지 URL들을 조회
//        Board board = getBoardById(id);
//        if (board != null) {
//            List<String> imageUrls = board.getImageUrls();
//            if (imageUrls != null) {
//                for (String imageUrl : imageUrls) {
//                    // S3 버킷에서 이미지 삭제
//                    awsS3Service.deleteFileFromS3(imageUrl);
//                }
//            }
//            // DB에서 게시글 삭제
//            String sql = "DELETE FROM boards WHERE id = ?";
//            jdbcTemplate.update(sql, id);
//        }
//    }


    @Override
    public void deleteImage(String imageUrl) {
        try {
            awsS3Service.deleteFileFromS3(imageUrl);
            imageRepository.deleteByImageUrl(imageUrl);
        } catch (AmazonServiceException e) {
            logger.error("Error deleting image from S3: {}", e.getMessage(), e);
            throw new RuntimeException("Error deleting image from S3", e);
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



    // 이미지 URL 리스트를 JSON 문자열로 변환하는 메서드
    private String convertListToJson(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return "[]"; // 빈 리스트는 빈 JSON 배열로 변환
        }

        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(imageUrls);
        } catch (JsonProcessingException e) {
            logger.error("JSON 변환 중 오류 발생", e);
            throw new RuntimeException("JSON 변환 중 오류 발생", e);
        }
    }



}