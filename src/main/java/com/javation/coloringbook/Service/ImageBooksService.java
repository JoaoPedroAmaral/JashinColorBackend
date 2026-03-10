package com.javation.coloringbook.Service;

import com.javation.coloringbook.Entity.Books;
import com.javation.coloringbook.Entity.ImageBooks;
import com.javation.coloringbook.Repository.ImageBooksRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageBooksService {

    private final ImageBooksRepository imageBooksRepository;
    private final CloudinaryService cloudinaryService;
    private final ImageProcessingService imageProcessingService;

    public List<ImageBooks> processAndSaveImages(Books book, List<MultipartFile> files) {
        log.info("Iniciando processamento paralelo de {} arquivos", files.size());

        // Processa todas as imagens simultaneamente usando CompletableFuture
        List<CompletableFuture<ImageBooks>> futures = IntStream.range(0, files.size())
            .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                MultipartFile file = files.get(i);
                BufferedImage downloaded = null;
                BufferedImage original = null;
                BufferedImage processed = null;
                
                try {
                    downloaded = ImageIO.read(file.getInputStream());
                    if (downloaded == null) return null;

                    original = imageProcessingService.resizeForProcessing(downloaded);
                    if (downloaded != original) downloaded.flush();

                    processed = imageProcessingService.convertToSketch(original);
                    original.flush();

                    byte[] imageBytes = imageProcessingService.convertToBytes(processed);
                    processed.flush();

                    String imageUrl = cloudinaryService.uploadImage(imageBytes);

                    return ImageBooks.builder()
                            .bookId(book)
                            .imageUrl(imageUrl)
                            .orderIndex(i)
                            .build();
                } catch (Exception e) {
                    log.error("Erro no processamento da imagem {}: {}", i, e.getMessage());
                    return null;
                } finally {
                    if (downloaded != null) downloaded.flush();
                    if (original != null) original.flush();
                    if (processed != null) processed.flush();
                }
            }))
            .collect(Collectors.toList());

        // Aguarda todas as tarefas terminarem (ou dar timeout)
        List<ImageBooks> results = futures.stream()
            .map(CompletableFuture::join)
            .filter(img -> img != null)
            .collect(Collectors.toList());

        // Salva todos os registros no banco de uma vez
        log.info("Salvando {} registros de imagens no banco", results.size());
        return imageBooksRepository.saveAll(results);
    }

    public List<ImageBooks> findByBookId(Long bookId) {
        return imageBooksRepository.findByBookIdIdOrderByOrderIndexAsc(bookId);
    }
}
