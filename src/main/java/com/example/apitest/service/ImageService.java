package com.example.apitest.service;

import com.example.apitest.DTO.Image;
import java.util.List;

public interface ImageService {
    public List<Image> findAllImages();
    public Image findImageById(Long id);
    public void saveImage(Image image);
    public void updateImage(Image image);
    public void deleteImage(Long id);

}