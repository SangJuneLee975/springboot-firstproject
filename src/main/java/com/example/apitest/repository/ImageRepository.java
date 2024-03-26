package com.example.apitest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.apitest.DTO.Image;

import java.util.List;


public interface ImageRepository {
   public List<Image> findAll();
   public Image findById(Long id);
   public void save(Image image);
   public void update(Image image);
   public void delete(Long id);

   public void saveImageUrls(List<String> imageUrls, Long boardId);

   public void deleteByImageUrl(String imageUrl); //데이터베이스에서 이미지 정보를 삭제하는 메서드

   // 게시글 ID로 연결된 이미지 URL 조회
   public List<String> findImageUrlsByBoardId(Long boardId);
}