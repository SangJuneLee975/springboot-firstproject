package com.example.apitest.service;

import com.example.apitest.DTO.Board;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface BoardService {
 public List<Board> getAllBoards(); //모든 게시판 조회
 public Board getBoardById(Long id);

 // 이미지 파일을 포함하여 게시판을 생성하는 메서드
 public Board createBoard(Board board, List<MultipartFile> multipartFiles) throws IOException;
 public void updateBoard(Board board); //게시판 수정
 public void deleteBoard(Long id); //게시판 삭제

 public Page<Board> getBoardsPaged(int page, int size);

 public void deleteImage(String imageUrl) throws Exception;

 public void addHashtagToBoard(Long boardId, Long hashtagId); // 게시글과 해시태그 관계

 public void removeImageUrlFromBoard(Long boardId, String imageUrl) throws Exception; // 게시글에서 이미지 url삭제

 public void updateBoardImageUrls(Long boardId, String imageUrlsJson);// 게시글에서 이미지 업데이트

}