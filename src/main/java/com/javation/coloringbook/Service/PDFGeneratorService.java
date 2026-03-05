package com.javation.coloringbook.Service;

import com.javation.coloringbook.Entity.ImageBooks;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;
import georegression.struct.point.Point2D_I32;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PDFGeneratorService {

    private static final float MARGIN = 30f;
    private static final float BORDER_WIDTH = 8f;

    private final ImageProcessingService imageProcessingService;
    private final VectorizationService vectorizationService;

    public byte[] generatePdfFromImageUrls(List<ImageBooks> images, String title) throws Exception {
        log.info("Generating high-quality full-page PDF (thick vector lines) for: '{}'", title);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        // Use A4 in Landscape orientation (842 x 595)
        Document document = new Document(PageSize.A4.rotate(), 0, 0, 0, 0);
        
        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();

            // Cover Page
            addCoverPage(document, title);

            float pageWidth = document.getPageSize().getWidth();
            float pageHeight = document.getPageSize().getHeight();

            for (ImageBooks imageBook : images) {
                try {
                    log.info("Processing page: {}", imageBook.getImageUrl());
                    BufferedImage original = ImageIO.read(new URL(imageBook.getImageUrl()));
                    if (original == null) continue;

                    // 1. Preprocess for vectorization
                    BufferedImage preprocessed = imageProcessingService.preprocessForVectorization(original);
                    
                    // 2. Get simplified contours (outlines of thick lines)
                    List<VectorizationService.SimplifiedContour> contours = vectorizationService.getSimplifiedContours(preprocessed);

                    document.newPage();
                    PdfContentByte cb = writer.getDirectContent();

                    float sketchW = preprocessed.getWidth();
                    float sketchH = preprocessed.getHeight();
                    
                    float scaleX = pageWidth / sketchW;
                    float scaleY = pageHeight / sketchH;

                    cb.saveState();
                    cb.concatCTM(scaleX, 0, 0, scaleY, 0, 0);

                    // A. White Background
                    cb.setColorFill(Color.WHITE);
                    cb.rectangle(0, 0, sketchW, sketchH);
                    cb.fill();

                    // B. Black Border
                    cb.setColorStroke(Color.BLACK);
                    cb.setLineWidth(BORDER_WIDTH / Math.min(scaleX, scaleY));
                    cb.rectangle(0, 0, sketchW, sketchH);
                    cb.stroke();

                    // C. Thick Vector Lines (Filled Areas)
                    cb.setColorFill(Color.BLACK);
                    
                    if (contours != null && !contours.isEmpty()) {
                        for (VectorizationService.SimplifiedContour contour : contours) {
                            // Draw External Path
                            drawPath(cb, contour.getExternal(), sketchH);
                            
                            // Draw Internal Holes
                            for (List<Point2D_I32> hole : contour.getInternal()) {
                                drawPath(cb, hole, sketchH);
                            }
                            
                            // Fill using even-odd rule (handles holes correctly)
                            cb.eoFill();
                        }
                    } else {
                        // Image Fallback
                        BufferedImage sketch = imageProcessingService.convertToSketch(original);
                        Image img = Image.getInstance(imageProcessingService.convertToBytes(sketch));
                        img.scaleAbsolute(sketchW, sketchH);
                        img.setAbsolutePosition(0, 0);
                        cb.addImage(img);
                    }
                    
                    cb.restoreState();
                } catch (Exception e) {
                    log.error("Error on page {}: {}", imageBook.getImageUrl(), e.getMessage());
                }
            }

            document.setMargins(MARGIN, MARGIN, MARGIN, MARGIN);
            addEndPage(document);
            document.close();
        } catch (Exception e) {
            log.error("Fatal PDF error: {}", e.getMessage(), e);
            if (document.isOpen()) document.close();
            throw e;
        }
        return out.toByteArray();
    }

    private void drawPath(PdfContentByte cb, List<Point2D_I32> points, float h) {
        if (points == null || points.isEmpty()) return;
        Point2D_I32 start = points.get(0);
        cb.moveTo(start.x, h - start.y);
        for (int i = 1; i < points.size(); i++) {
            Point2D_I32 p = points.get(i);
            cb.lineTo(p.x, h - p.y);
        }
        cb.closePath();
    }

    private void addCoverPage(Document document, String title) throws Exception {
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 42, Color.BLACK);
        Paragraph titlePara = new Paragraph(title.toUpperCase(), titleFont);
        titlePara.setAlignment(Element.ALIGN_CENTER);
        titlePara.setSpacingBefore(120);
        document.add(titlePara);

        Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 18, Color.GRAY);
        Paragraph subPara = new Paragraph("Personalized Coloring Book", subFont);
        subPara.setAlignment(Element.ALIGN_CENTER);
        subPara.setSpacingBefore(20);
        document.add(subPara);

        Font instructionFont = FontFactory.getFont(FontFactory.HELVETICA, 12, Color.LIGHT_GRAY);
        Paragraph instructionPara = new Paragraph("\n\n\n\nGrab your crayons and have fun!", instructionFont);
        instructionPara.setAlignment(Element.ALIGN_CENTER);
        document.add(instructionPara);
    }

    private void addEndPage(Document document) throws Exception {
        document.newPage();
        Font endFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 30, Color.BLACK);
        Paragraph endPara = new Paragraph("THE END", endFont);
        endPara.setAlignment(Element.ALIGN_CENTER);
        endPara.setSpacingBefore(150);
        document.add(endPara);

        Font thanksFont = FontFactory.getFont(FontFactory.HELVETICA, 16, Color.GRAY);
        Paragraph thanksPara = new Paragraph("Thank you for coloring with us!", thanksFont);
        thanksPara.setAlignment(Element.ALIGN_CENTER);
        thanksPara.setSpacingBefore(30);
        document.add(thanksPara);
    }
}
