package com.javation.coloringbook.Service;

import com.javation.coloringbook.Entity.ImageBooks;
import com.lowagie.text.Document;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;
import georegression.struct.point.Point2D_I32;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.batik.transcoder.SVGAbstractTranscoder;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class PDFGeneratorService {

    private static final float BORDER_WIDTH = 8f;

    private final ImageProcessingService imageProcessingService;
    private final VectorizationService vectorizationService;

    public byte[] generatePdfFromImageUrls(List<ImageBooks> images, String title) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Document document = new Document(PageSize.A4.rotate(), 0, 0, 0, 0);

        String set = new Random().nextBoolean() ? "1" : "2";
        log.info("Using cover set: {}", set);

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();

            addSvgFullPage(document, writer, "/DefaultPage/" + set + "Capa.svg");

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

            addSvgFullPage(document, writer, "/DefaultPage/" + set + "Contra.svg");

            document.close();
        } catch (Exception e) {
            if (document.isOpen()) document.close();
            throw e;
        }
        return out.toByteArray();
    }


    private void addSvgFullPage(Document document, PdfWriter writer, String classpathResource) {
        document.setMargins(0, 0, 0, 0);
        document.newPage();

        InputStream svgStream = getClass().getResourceAsStream(classpathResource);
        if (svgStream == null) {
            log.warn("SVG resource not found at classpath: '{}'. Skipping.", classpathResource);
            return;
        }

        try {
            Rectangle page = writer.getPageSize();
            float pageW = page.getWidth();
            float pageH = page.getHeight();

            float bleed = 1f;
            float drawW = pageW + bleed * 2;
            float drawH = pageH + bleed * 2;

            float renderScale = 1.0f;
            PNGTranscoder transcoder = new PNGTranscoder();
            transcoder.addTranscodingHint(SVGAbstractTranscoder.KEY_WIDTH,  drawW * renderScale);
            transcoder.addTranscodingHint(SVGAbstractTranscoder.KEY_HEIGHT, drawH * renderScale);

            ByteArrayOutputStream pngOut = new ByteArrayOutputStream();
            transcoder.transcode(new TranscoderInput(svgStream), new TranscoderOutput(pngOut));

            Image img = Image.getInstance(pngOut.toByteArray());
            img.scaleAbsolute(drawW, drawH);
            img.setAbsolutePosition(-bleed, -bleed);

            PdfContentByte cb = writer.getDirectContentUnder();
            cb.addImage(img);

        } catch (Exception e) {
            log.error("Failed to render SVG '{}': {}", classpathResource, e.getMessage(), e);
        }
    }

    private void drawPath(PdfContentByte cb, List<Point2D_I32> points, float h) {
        if (points == null || points.isEmpty()) return;
        cb.moveTo(points.get(0).x, h - points.get(0).y);
        for (int i = 1; i < points.size(); i++) cb.lineTo(points.get(i).x, h - points.get(i).y);
        cb.closePath();
    }
}