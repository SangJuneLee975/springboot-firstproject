package com.example.apitest.controller;

import com.amazonaws.regions.Regions;
import com.example.apitest.DTO.*;
import com.example.apitest.service.*;
import com.example.apitest.config.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/boards")
@CrossOrigin(origins = "http://localhost:3000")
public class BoardController {

    private final UserService userService;
    private final BoardService boardService;
    private final HashtagService hashtagService;
    private final JwtUtils jwtUtils; // JwtUtils 필드 추가
    private final AwsS3Service awsS3Service;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    private static final Logger logger = LoggerFactory.getLogger(BoardController.class);

    @Autowired
    public BoardController(UserService userService, BoardService boardService, JwtUtils jwtUtils, HashtagService hashtagService,
                           AwsS3Service awsS3Service) {

        this.userService = userService;
        this.boardService = boardService;
        this.hashtagService = hashtagService;
        this.jwtUtils = jwtUtils;
        this.awsS3Service = awsS3Service;
    }

    // 게시판 목록 조회
    @GetMapping
    public List<Board> getAllBoards() {
        return boardService.getAllBoards();
    }


     @GetMapping("/{id}")
    public ResponseEntity<Board> getBoardById(@PathVariable Long id) {
      Board board = boardService.getBoardById(id);
       return board != null ? ResponseEntity.ok(board) : ResponseEntity.notFound().build();
   }

    @PostMapping("/new")
    public ResponseEntity<?> createBoard(
            @RequestPart("board") Board board,
            @RequestParam(value = "file", required = false) MultipartFile[] files,
            HttpServletRequest request) throws IOException {

        String token = jwtUtils.extractToken(request);

        if (token == null || !jwtUtils.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("인증되지 않은 사용자입니다.");
        }

        String userId = jwtUtils.extractUserId(token);
        User user = userService.findByUserId(userId);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("사용자를 찾을 수 없습니다.");
        }

        board.setWriter(user.getNickname()); // 게시글 작성자 설정

        // 파일 리스트를 MultipartFile 리스트로 변환
        List<MultipartFile> multipartFiles = files != null ? Arrays.asList(files) : new ArrayList<>();



        // 게시글 정보와 파일 리스트를 함께 저장
        Board createdBoard = boardService.createBoard(board, multipartFiles);


        // 해시태그 처리
        if (board.getHashtags() != null && !board.getHashtags().isEmpty()) {
            for (Hashtag submittedHashtag : board.getHashtags()) {
                Hashtag existingHashtag = hashtagService.findHashtagByName(submittedHashtag.getName());
                if (existingHashtag == null) {
                    existingHashtag = new Hashtag();
                    existingHashtag.setName(submittedHashtag.getName());
                    hashtagService.createHashtag(existingHashtag);
                }
                boardService.addHashtagToBoard(createdBoard.getId(), existingHashtag.getId());
            }
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(createdBoard);
    }


    // 게시글 수정
    @PutMapping("/{id}")
    public ResponseEntity<?> updateBoard(
            @PathVariable Long id,
            @RequestPart("board") Board updatedBoard,
            @RequestParam(value = "file", required = false) MultipartFile[] files,
            HttpServletRequest request) {

        String token = jwtUtils.extractToken(request);
        if (token == null || !jwtUtils.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("승인되지 않음: 토큰이 잘못되었거나 누락되었습니다");
        }


        String userId = jwtUtils.extractUserId(token);
        Board existingBoard = boardService.getBoardById(id);

        // 요청한 사용자가 게시글 작성자와 동일한지 확인
        if (existingBoard != null && userService.findByUserId(userId).getNickname().equals(existingBoard.getWriter())) {
            // 게시글 정보 업데이트
            existingBoard.setTitle(updatedBoard.getTitle());
            existingBoard.setContent(updatedBoard.getContent());

            // 기존의 게시글과 관련된 해시태그들을 제거
            hashtagService.removeHashtagsFromBoard(id);

            // 새 해시태그들을 게시글에 연결
            if (updatedBoard.getHashtags() != null && !updatedBoard.getHashtags().isEmpty()) {
                for (Hashtag hashtag : updatedBoard.getHashtags()) {
                    Hashtag existingHashtag = hashtagService.findHashtagByName(hashtag.getName());
                    if (existingHashtag == null) {
                        // 새로운 해시태그이면 생성
                        existingHashtag = hashtagService.createHashtag(new Hashtag(hashtag.getName()));
                    }
                    // 게시글에 해시태그 연결
                    hashtagService.addHashtagToBoard(id, existingHashtag);
                }
            }

            //파일 로직 추가 필요

            // 게시글 정보 저장
            boardService.updateBoard(existingBoard);

            return ResponseEntity.ok().body("게시글이 성공적으로 수정되었습니다.");
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("수정 권한이 없습니다.");
        }
    }



    // 게시글 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBoard(@PathVariable Long id, HttpServletRequest request) {
        String token = jwtUtils.extractToken(request);
        if (token == null || !jwtUtils.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("승인되지 않음: 토큰이 잘못되었거나 누락되었습니다");
        }

        String userId = jwtUtils.extractUserId(token);
        Board board = boardService.getBoardById(id);

        // 요청한 사용자가 게시글 작성자인지 확인
        if (board != null && userService.findByUserId(userId).getNickname().equals(board.getWriter())) {
            boardService.deleteBoard(id);
            return ResponseEntity.ok().body("게시글이 성공적으로 삭제되었습니다.");
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("삭제 권한이 없습니다.");
        }
    }

    @DeleteMapping("/images")
    public ResponseEntity<?> deleteImage(@RequestParam String imageUrl, HttpServletRequest request) {
        try {
            String token = jwtUtils.extractToken(request);
            if (token == null || !jwtUtils.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("인증되지 않은 사용자입니다.");
            }

            boardService.deleteImage(imageUrl);
            return ResponseEntity.ok().body("이미지가 성공적으로 삭제되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("이미지 삭제 중 오류가 발생했습니다.");
        }
    }

    @GetMapping("/paged")
    public ResponseEntity<Page<Board>> getBoardsPaged(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "5") int size) {
        // 페이지 번호와 크기를 매개변수로 받아서 해당 페이지의 게시글 목록을 조회
        Page<Board> boards = boardService.getBoardsPaged(page, size);

        return ResponseEntity.ok(boards);
    }
}