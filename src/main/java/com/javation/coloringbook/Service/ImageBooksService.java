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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageBooksService {

    private final ImageBooksRepository imageBooksRepository;
    private final CloudinaryService cloudinaryService;
    private final ImageProcessingService imageProcessingService; // Processamento LOCAL

    public List<ImageBooks> findByBookId(Long id){
        return imageBooksRepository.findByBookIdId(id);
    }

    /**
     * Processa e salva imagens - 100% LOCAL
     */
    public List<ImageBooks> processAndSaveImages(Books book, List<MultipartFile> files) throws IOException {
        int index = 0;

        for (MultipartFile file : files) {
            log.info("Processando imagem {}/{}: {}", index + 1, files.size(), file.getOriginalFilename());

            try {
                // 1. Ler imagem original
                BufferedImage original = ImageIO.read(file.getInputStream());
                if (original == null) {
                    log.error("Não foi possível ler a imagem: {}", file.getOriginalFilename());
                    throw new IOException("Imagem inválida ou corrompida: " + file.getOriginalFilename());
                }

                log.info("Imagem original carregada: {}x{} pixels", original.getWidth(), original.getHeight());

                // 2. Processar imagem (conversão para desenho de colorir)
                BufferedImage processed = imageProcessingService.convertToSketch(original);

                // 3. Converter para bytes (PNG)
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                ImageIO.write(processed, "png", os);
                os.flush();
                byte[] imageBytes = os.toByteArray();
                os.close();

                log.info("Imagem processada: {} bytes", imageBytes.length);

                // 4. Upload para Cloudinary
                String imageUrl = cloudinaryService.uploadFile(imageBytes);
                log.info("Upload concluído: {}", imageUrl);

                // 5. Salvar registro no banco
                ImageBooks imageBooks = new ImageBooks();
                imageBooks.setBookId(book);
                imageBooks.setImageUrl(imageUrl);
                imageBooks.setOrderIndex(index);
                imageBooksRepository.save(imageBooks);

                log.info("✓ Imagem {} salva com sucesso", index + 1);

            } catch (Exception e) {
                log.error("Erro ao processar imagem {}: {}", file.getOriginalFilename(), e.getMessage(), e);
                throw new IOException("Falha ao processar " + file.getOriginalFilename() + ": " + e.getMessage(), e);
            }

            index++;
        }

        log.info("=== TODAS AS {} IMAGENS PROCESSADAS COM SUCESSO ===", files.size());
        return imageBooksRepository.findByBookIdIdOrderByOrderIndexAsc(book.getId());
    }
}