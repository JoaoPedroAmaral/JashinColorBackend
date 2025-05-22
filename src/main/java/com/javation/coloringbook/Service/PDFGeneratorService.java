package com.javation.coloringbook.Service;

import com.javation.coloringbook.Entity.ImageBooks;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.List;

@Service
public class PDFGeneratorService {

    public byte[] generatePdfFromImageUrls(List<ImageBooks> images) throws Exception {
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        PdfWriter.getInstance(document, outputStream);
        document.open();

        for (ImageBooks imageBook : images) {
            Image img = Image.getInstance(new URL(imageBook.getImageUrl()));
            img.scaleToFit(PageSize.A4.getWidth(), PageSize.A4.getHeight());
            img.setAlignment(Image.ALIGN_CENTER);
            document.add(img);
            document.newPage();
        }

        document.close();
        return outputStream.toByteArray();
    }
}
