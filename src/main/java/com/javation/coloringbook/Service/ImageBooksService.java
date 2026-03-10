package com.javation.coloringbook.Service;

import com.javation.coloringbook.Entity.Books;
import com.javation.coloringbook.Entity.ImageBooks;
import com.javation.coloringbook.Repository.ImageBooksRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageBooksService {

    private final ImageBooksRepository imageBooksRepository;
    private final CloudinaryService cloudinaryService;
    private final ImageProcessingService imageProcessingService;

    @Transactional
    public List<ImageBooks> processAndSaveImages(Books book, List<MultipartFile> files) throws IOException {
        List<ImageBooks> savedImages = new ArrayList<>();

        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            try {
                BufferedImage original = ImageIO.read(file.getInputStream());
                BufferedImage processed = imageProcessingService.convertToSketch(original);

                ByteArrayOutputStream os = new ByteArrayOutputStream();
                ImageIO.write(processed, "png", os);
                byte[] imageBytes = os.toByteArray();

                String imageUrl = cloudinaryService.uploadImage(imageBytes);

                ImageBooks imageBook = ImageBooks.builder()
                        .bookId(book)
                        .imageUrl(imageUrl)
                        .orderIndex(i)
                        .build();

                savedImages.add(imageBooksRepository.save(imageBook));
            } catch (Exception e) {
                log.error("Error processing file {}: {}", file.getOriginalFilename(), e.getMessage());
            }
        }
        return savedImages;
    }

    public List<ImageBooks> findByBookId(Long bookId) {
        return imageBooksRepository.findByBookIdIdOrderByOrderIndexAsc(bookId);
    }
}
