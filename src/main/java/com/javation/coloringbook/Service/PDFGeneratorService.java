package com.javation.coloringbook.Service;

import com.javation.coloringbook.Entity.ImageBooks;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PDFGeneratorService {

    private static final float A4_WIDTH = PageSize.A4.getWidth();
    private static final float A4_HEIGHT = PageSize.A4.getHeight();
    private static final float MARGIN = 30f;
    private static final float BORDER_WIDTH = 8f;

    private final ImageProcessingService imageProcessingService;

    public byte[] generatePdfFromImageUrls(List<ImageBooks> images, String title) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, MARGIN, MARGIN, MARGIN, MARGIN);
        PdfWriter.getInstance(document, out);

        document.open();

        addCoverPage(document, title);

        // Remove margins for coloring pages to fill the entire page
        document.setMargins(0, 0, 0, 0);

        for (ImageBooks imageBook : images) {
            try {
                BufferedImage original = ImageIO.read(new URL(imageBook.getImageUrl()));
                BufferedImage sketch = imageProcessingService.convertToSketch(original);

                BufferedImage borderedImage = addBlackBorder(sketch, (int) BORDER_WIDTH);

                ByteArrayOutputStream imageBytes = new ByteArrayOutputStream();
                ImageIO.write(borderedImage, "png", imageBytes);

                Image pdfImage = Image.getInstance(imageBytes.toByteArray());
                
                // Check if image is landscape and rotate if needed to fit A4 portrait better
                if (pdfImage.getWidth() > pdfImage.getHeight()) {
                    pdfImage.setRotationDegrees(90);
                }

                // Scale to fit the entire A4 page while maintaining aspect ratio
                pdfImage.scaleToFit(A4_WIDTH, A4_HEIGHT);
                
                // Center the image on the page
                float x = (A4_WIDTH - pdfImage.getScaledWidth()) / 2;
                float y = (A4_HEIGHT - pdfImage.getScaledHeight()) / 2;
                pdfImage.setAbsolutePosition(x, y);

                document.newPage();
                document.add(pdfImage);
            } catch (Exception e) {
                log.error("Failed to process image {}: {}", imageBook.getImageUrl(), e.getMessage());
            }
        }

        // Restore margins for the end page
        document.setMargins(MARGIN, MARGIN, MARGIN, MARGIN);
        addEndPage(document);

        document.close();
        return out.toByteArray();
    }

    private void addCoverPage(Document document, String title) throws Exception {
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 42, Color.BLACK);
        Paragraph titlePara = new Paragraph(title.toUpperCase(), titleFont);
        titlePara.setAlignment(Element.ALIGN_CENTER);
        titlePara.setSpacingBefore(180);
        document.add(titlePara);

        Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 18, Color.GRAY);
        Paragraph subPara = new Paragraph("Personalized Coloring Book", subFont);
        subPara.setAlignment(Element.ALIGN_CENTER);
        subPara.setSpacingBefore(20);
        document.add(subPara);

        Font instructionFont = FontFactory.getFont(FontFactory.HELVETICA, 12, Color.LIGHT_GRAY);
        Paragraph instructionPara = new Paragraph("\n\n\n\n\n\n\n\n\n\nGrab your crayons and have fun!", instructionFont);
        instructionPara.setAlignment(Element.ALIGN_CENTER);
        document.add(instructionPara);
    }

    private void addEndPage(Document document) throws Exception {
        document.newPage();
        Font endFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 30, Color.BLACK);
        Paragraph endPara = new Paragraph("THE END", endFont);
        endPara.setAlignment(Element.ALIGN_CENTER);
        endPara.setSpacingBefore(200);
        document.add(endPara);

        Font thanksFont = FontFactory.getFont(FontFactory.HELVETICA, 16, Color.GRAY);
        Paragraph thanksPara = new Paragraph("Thank you for coloring with us!", thanksFont);
        thanksPara.setAlignment(Element.ALIGN_CENTER);
        thanksPara.setSpacingBefore(30);
        document.add(thanksPara);
    }

    private BufferedImage addBlackBorder(BufferedImage image, int borderWidth) {
        int newWidth = image.getWidth() + (2 * borderWidth);
        int newHeight = image.getHeight() + (2 * borderWidth);

        BufferedImage borderedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);

        Graphics2D g2d = borderedImage.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, newWidth, newHeight);

        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, newWidth, borderWidth);
        g2d.fillRect(0, newHeight - borderWidth, newWidth, borderWidth);
        g2d.fillRect(0, 0, borderWidth, newHeight);
        g2d.fillRect(newWidth - borderWidth, 0, borderWidth, newHeight);

        g2d.drawImage(image, borderWidth, borderWidth, null);
        g2d.dispose();

        return borderedImage;
    }
}
