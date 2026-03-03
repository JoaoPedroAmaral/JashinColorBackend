package com.javation.coloringbook.Service;

import com.javation.coloringbook.Entity.ImageBooks;
import com.lowagie.text.Document;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PDFGeneratorService {

    private final ImageProcessingService imageProcessingService;

    public byte[] generatePdfFromImageUrls(List<ImageBooks> images, String title) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 20, 20, 20, 20);
        PdfWriter.getInstance(document, out);

        document.open();
        document.add(new Paragraph(title));

        for (ImageBooks imageBook : images) {
            try {
                BufferedImage original = ImageIO.read(new URL(imageBook.getImageUrl()));
                BufferedImage sketch = imageProcessingService.convertToSketch(original);

                ByteArrayOutputStream imageBytes = new ByteArrayOutputStream();
                ImageIO.write(sketch, "png", imageBytes);

                Image pdfImage = Image.getInstance(imageBytes.toByteArray());
                pdfImage.scaleToFit(PageSize.A4.getWidth() - 40, PageSize.A4.getHeight() - 100);
                pdfImage.setAlignment(Image.MIDDLE);

                document.newPage();
                document.add(pdfImage);
            } catch (Exception e) {
                log.error("Failed to process image {}: {}", imageBook.getImageUrl(), e.getMessage());
            }
        }

        document.close();
        return out.toByteArray();
    }
}
