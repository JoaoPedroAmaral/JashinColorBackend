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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageBooksService {

    private final ImageBooksRepository imageBooksRepository;
    private final CloudinaryService cloudinaryService;
    private final ImageProcessingService imageProcessingService;

    // Criamos um pool fixo de 2 threads. 
    // Isso garante que no máximo 2 imagens estejam sendo processadas na RAM ao mesmo tempo.
    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    public List<ImageBooks> processAndSaveImages(Books book, List<MultipartFile> files) {
        log.info("Iniciando processamento controlado de {} arquivos (Máx 2 simultâneos)", files.size());

        List<CompletableFuture<ImageBooks>> futures = IntStream.range(0, files.size())
            .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                MultipartFile file = files.get(i);
                BufferedImage downloaded = null;
                BufferedImage original = null;
                BufferedImage processed = null;
                
                try {
                    log.debug("Processando imagem {}/{}", i + 1, files.size());
                    downloaded = ImageIO.read(file.getInputStream());
                    if (downloaded == null) return null;

                    original = imageProcessingService.resizeForProcessing(downloaded);
                    if (downloaded != original) {
                        downloaded.flush();
                        downloaded = null;
                    }

                    processed = imageProcessingService.convertToSketch(original);
                    original.flush();
                    original = null;

                    byte[] imageBytes = imageProcessingService.convertToBytes(processed);
                    processed.flush();
                    processed = null;

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
                    // Garantia absoluta de limpeza de recursos
                    if (downloaded != null) downloaded.flush();
                    if (original != null) original.flush();
                    if (processed != null) processed.flush();
                    System.gc(); // Sugere limpeza após cada tarefa para liberar memória nativa
                }
            }, executor)) // USANDO O EXECUTOR LIMITADO
            .collect(Collectors.toList());

        // Aguarda a conclusão de todas
        List<ImageBooks> results = futures.stream()
            .map(CompletableFuture::join)
            .filter(img -> img != null)
            .collect(Collectors.toList());

        log.info("Processamento concluído. Salvando {} imagens.", results.size());
        return imageBooksRepository.saveAll(results);
    }

    public List<ImageBooks> findByBookId(Long bookId) {
        return imageBooksRepository.findByBookIdIdOrderByOrderIndexAsc(bookId);
    }
}
