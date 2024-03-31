package com.example.apitest.service;

import com.example.apitest.DTO.Board;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface BoardService {
 public List<Board> getAllBoards(); //모든 게시판 조회
 public Board getBoardById(Long id);

 // 이미지 파일을 포함하여 게시판을 생성하는 메서드
 public Board createBoard(Board board, List<MultipartFile> multipartFiles) throws IOException;
 public void updateBoard(Board board, List<String> deletedImageUrls) throws JsonProcessingException ; //게시판 수정
 public void deleteBoard(Long id); //게시판 삭제

 public Page<Board> getBoardsPaged(int page, int size);

 public void deleteImage(String imageUrl);

 public void addHashtagToBoard(Long boardId, Long hashtagId); // 게시글과 해시태그 관계

 public void updateBoardImageUrls(Long boardId, List<String> imageUrls) throws JsonProcessingException; // 이미지 URL 리스트를 데이터베이스에 업데이트하는 메서드

}