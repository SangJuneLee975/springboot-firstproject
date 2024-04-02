package com.example.apitest.service;

import com.example.apitest.DTO.Image;
import com.example.apitest.repository.ImageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ImageServiceImpl implements ImageService {

    private final ImageRepository imageRepository;

    @Autowired
    public ImageServiceImpl(ImageRepository imageRepository) {
        this.imageRepository = imageRepository;
    }

    @Override
    public List<Image> findAllImages() {
        return imageRepository.findAll();
    }

    @Override
    public Image findImageById(Long id) {
        return imageRepository.findById(id);
    }

    @Override
    public void saveImage(Image image) {
        imageRepository.save(image);
    }

    @Override
    public void updateImage(Image image) {
        imageRepository.update(image);
    }

    @Override
    public void deleteImage(Long id) {
        imageRepository.delete(id);
    }

}