package com.javation.coloringbook.Service;

import com.javation.coloringbook.Entity.Books;
import com.javation.coloringbook.Entity.ImageBooks;
import com.javation.coloringbook.Repository.ImageBooksRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ImageBooksService {
    private final ImageBooksRepository imageBooksRepository;
    private final CloudinaryService cloudinaryService;
    private final ImageProcessingService imageProcessingService;

    public List<ImageBooks> findByBookId(Long id){
        return imageBooksRepository.findByBookIdId(id);
    }

    public List<ImageBooks> processAndSaveImages(Books book, List<MultipartFile> files) throws IOException{
        int index = 0;
        for (MultipartFile file: files){
            BufferedImage original = ImageIO.read(file.getInputStream());
            BufferedImage processed = imageProcessingService.convertToSketch(original);

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(processed, "png", os);
            os.flush();
            byte[] imageByte = os.toByteArray();
            os.close();

            String imageUrl = cloudinaryService.uploadFile(imageByte);

            ImageBooks imageBooks = new ImageBooks();
            imageBooks.setBookId(book);
            imageBooks.setImageUrl(imageUrl);
            imageBooks.setOrderIndex(index);
            imageBooksRepository.save(imageBooks);

            index++;
        }
        return imageBooksRepository.findByBookIdIdOrderByOrderIndexAsc(book.getId());
    }


}
