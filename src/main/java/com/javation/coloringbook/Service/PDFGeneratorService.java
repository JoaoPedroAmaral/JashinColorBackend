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
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 0, 0, 0, 0);
        
        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();
            addCoverPage(document, title);

            float pW = document.getPageSize().getWidth();
            float pH = document.getPageSize().getHeight();

            for (ImageBooks imageBook : images) {
                try {
                    BufferedImage original = ImageIO.read(new URL(imageBook.getImageUrl()));
                    if (original == null) continue;

                    BufferedImage pre = imageProcessingService.preprocessForVectorization(original);
                    List<VectorizationService.SimplifiedContour> contours = vectorizationService.getSimplifiedContours(pre);

                    document.newPage();
                    PdfContentByte cb = writer.getDirectContent();

                    float sW = pre.getWidth(), sH = pre.getHeight();
                    float scaleX = pW / sW, scaleY = pH / sH;

                    cb.saveState();
                    cb.concatCTM(scaleX, 0, 0, scaleY, 0, 0);

                    cb.setColorFill(Color.WHITE);
                    cb.rectangle(0, 0, sW, sH);
                    cb.fill();

                    cb.setColorStroke(Color.BLACK);
                    cb.setLineWidth(BORDER_WIDTH / Math.min(scaleX, scaleY));
                    cb.rectangle(0, 0, sW, sH);
                    cb.stroke();

                    cb.setColorFill(Color.BLACK);
                    if (contours != null && !contours.isEmpty()) {
                        for (VectorizationService.SimplifiedContour contour : contours) {
                            drawPath(cb, contour.getExternal(), sH);
                            for (List<Point2D_I32> hole : contour.getInternal()) drawPath(cb, hole, sH);
                            cb.eoFill();
                        }
                    } else {
                        BufferedImage sketch = imageProcessingService.convertToSketch(original);
                        Image img = Image.getInstance(imageProcessingService.convertToBytes(sketch));
                        img.scaleAbsolute(sW, sH);
                        img.setAbsolutePosition(0, 0);
                        cb.addImage(img);
                    }
                    cb.restoreState();
                } catch (Exception e) {
                    log.error("Page error ({}): {}", imageBook.getImageUrl(), e.getMessage());
                }
            }
            document.setMargins(MARGIN, MARGIN, MARGIN, MARGIN);
            addEndPage(document);
            document.close();
        } catch (Exception e) {
            if (document.isOpen()) document.close();
            throw e;
        }
        return out.toByteArray();
    }

    private void drawPath(PdfContentByte cb, List<Point2D_I32> points, float h) {
        if (points == null || points.isEmpty()) return;
        cb.moveTo(points.get(0).x, h - points.get(0).y);
        for (int i = 1; i < points.size(); i++) cb.lineTo(points.get(i).x, h - points.get(i).y);
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
