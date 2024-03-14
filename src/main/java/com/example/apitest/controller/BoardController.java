package com.example.apitest.controller;

import com.example.apitest.DTO.Board;
import com.example.apitest.DTO.Hashtag;
import com.example.apitest.DTO.User;
import com.example.apitest.config.JwtUtils;
import com.example.apitest.service.BoardService;
import com.example.apitest.service.HashtagService;
import com.example.apitest.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/boards")
@CrossOrigin(origins = "http://localhost:3000")
public class BoardController {

    private final UserService userService;
    private final BoardService boardService;
    private final HashtagService hashtagService;
    private final JwtUtils jwtUtils; // JwtUtils 필드 추가

    @Autowired
    public BoardController(UserService userService, BoardService boardService, JwtUtils jwtUtils, HashtagService hashtagService) {

        this.userService = userService;
        this.boardService = boardService;
        this.hashtagService = hashtagService;
        this.jwtUtils = jwtUtils;
    }

    // 게시판 목록 조회
    @GetMapping
    public List<Board> getAllBoards() {
        return boardService.getAllBoards();
    }

    // 게시판 상세 조회
    @GetMapping("/{id}")
    public ResponseEntity<Board> getBoardById(@PathVariable Long id) {
        Board board = boardService.getBoardById(id);
        return board != null ? ResponseEntity.ok(board) : ResponseEntity.notFound().build();
    }

    @PostMapping("/new")
    public ResponseEntity<?> createBoard(@RequestBody Board board, HttpServletRequest request) {
        String token = jwtUtils.extractToken(request);
        if (token != null && jwtUtils.validateToken(token)) {
            String userId = jwtUtils.extractUserId(token);
            User user = userService.findByUserId(userId);
            if (user != null) {
                board.setWriter(user.getNickname()); // 게시글 작성자 설정

                // 게시글 정보 저장 (작성자, 내용 등)
                Board createdBoard = boardService.createBoard(board);

                // 해시태그 처리
                if (board.getHashtags() != null && !board.getHashtags().isEmpty()) {
                    for (Hashtag submittedHashtag : board.getHashtags()) {
                        Hashtag existingHashtag = hashtagService.findHashtagByName(submittedHashtag.getName());
                        if (existingHashtag == null) {
                            // 새로운 해시태그면 생성
                            existingHashtag = new Hashtag();
                            existingHashtag.setName(submittedHashtag.getName());
                            existingHashtag = hashtagService.createHashtag(existingHashtag);
                        }
                        // 게시글에 해시태그 연결
                        boardService.addHashtagToBoard(createdBoard.getId(), existingHashtag.getId());
                    }
                }

                return ResponseEntity.status(HttpStatus.CREATED).body(createdBoard);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("사용자를 찾을 수 없습니다.");
            }
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("토큰이 잘못되었거나 누락되었습니다.");
        }
    }


    // 게시글 수정
    @PutMapping("/{id}")
    public ResponseEntity<?> updateBoard(@PathVariable Long id, @RequestBody Board updatedBoard, HttpServletRequest request) {
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

    @GetMapping("/paged")
    public ResponseEntity<Page<Board>> getBoardsPaged(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "5") int size) {
        // 페이지 번호와 크기를 매개변수로 받아서 해당 페이지의 게시글 목록을 조회
        Page<Board> boards = boardService.getBoardsPaged(page, size);

        return ResponseEntity.ok(boards);
    }
}