package com.example.apitest.service;

import com.example.apitest.DTO.Board;
import com.example.apitest.DTO.Hashtag;
import com.example.apitest.repository.BoardRepository;
import com.example.apitest.repository.ImageRepository;
import com.example.apitest.util.JsonUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
                            // 필요한 경우, 사용자에게 오류를 알리는 로직 추가
                        }
                    }
                }
            }

            // imageUrls 리스트를 JSON 문자열로 변환, 리스트가 비어있다면 기본값으로 빈 배열의 JSON 문자열을 사용
          //  String imageUrlsJson = imageUrls.isEmpty() ? "[]" : JsonUtil.listToJson(imageUrls);

            String imageUrlsJson = JsonUtil.listToJson(board.getImageUrls()); // imageUrls를 JSON으로 변환

          //  String imageUrlsJson = objectMapper.writeValueAsString(imageUrls);


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
    public void updateBoard(Board board) {
        String sql = "UPDATE boards SET title = ?, content = ?, category_id = ? WHERE id = ?";
        jdbcTemplate.update(sql, board.getTitle(), board.getContent(), board.getCategoryId(), board.getId());
    }

    // 게시판 글 삭제
    @Override
    public void deleteBoard(Long id) {
        // 게시글에 연결된 이미지 URL들을 조회
        Board board = getBoardById(id);
        if (board != null) {
            List<String> imageUrls = board.getImageUrls();
            if (imageUrls != null) {
                for (String imageUrl : imageUrls) {
                    // S3 버킷에서 이미지 삭제
                    awsS3Service.deleteFileFromS3(imageUrl);
                }
            }
            // DB에서 게시글 삭제
            String sql = "DELETE FROM boards WHERE id = ?";
            jdbcTemplate.update(sql, id);
        }
    }

    @Override
    public void deleteImage(String imageUrl) throws Exception {
        try {
            // S3에서 이미지 파일 삭제
            awsS3Service.deleteFileFromS3(imageUrl);

            // DB에서 이미지 정보 삭제
            imageRepository.deleteByImageUrl(imageUrl);
        } catch (Exception e) {
            logger.error("Error deleting image: {}", e.getMessage(), e);
            throw new Exception("이미지 삭제 중 오류가 발생했습니다.");
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

    // 게시글에서 한 개 이상의 이미지 URL을 제거하는 메서드
    @Override
    public void removeImageUrlFromBoard(Long boardId, String imageUrl) throws Exception {
        Board board = getBoardById(boardId);
        if (board != null) {
            List<String> currentImageUrls = board.getImageUrls();
            boolean isRemoved = currentImageUrls.removeIf(url -> url.equals(imageUrl));
            if (isRemoved) {
                // 이미지 URL 리스트를 JSON 문자열로 변환하여 데이터베이스를 업데이트합니다.
                String updatedImageUrlsJson = convertListToJson(currentImageUrls);
                updateBoardImageUrls(boardId, updatedImageUrlsJson); // JSON 문자열을 전달합니다.
            } else {
                throw new Exception("해당 이미지 URL을 찾을 수 없습니다.");
            }
        } else {
            throw new Exception("게시글을 찾을 수 없습니다.");
        }
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

    public void updateBoardImageUrls(Long boardId, String imageUrlsJson) {
        String sql = "UPDATE boards SET image_urls = ? WHERE id = ?";
        jdbcTemplate.update(sql, imageUrlsJson, boardId);
    }

}