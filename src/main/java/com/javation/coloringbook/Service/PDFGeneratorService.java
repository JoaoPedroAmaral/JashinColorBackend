package com.javation.coloringbook.Service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.javation.coloringbook.Entity.ImageBooks;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PDFGeneratorService {

    // Cores para o design
    private static final BaseColor PRIMARY_COLOR = new BaseColor(102, 51, 153); // Roxo
    private static final BaseColor SECONDARY_COLOR = new BaseColor(255, 153, 0); // Laranja
    private static final BaseColor ACCENT_COLOR = new BaseColor(51, 153, 255); // Azul
    private static final BaseColor TEXT_COLOR = new BaseColor(50, 50, 50); // Cinza escuro

    public byte[] generatePdfFromImageUrls(List<ImageBooks> images) throws Exception {
        return generatePdfFromImageUrls(images, null);
    }

    /**
     * Gera PDF completo com capa personalizada, índice e páginas de colorir
     */
    public byte[] generatePdfFromImageUrls(List<ImageBooks> images, String bookTitle) throws Exception {
        Document document = new Document(PageSize.A4, 0, 0, 0, 0);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        PdfWriter writer = PdfWriter.getInstance(document, outputStream);

        // Adicionar event handler para cabeçalho/rodapé
        PageEventHelper eventHelper = new PageEventHelper(bookTitle);
        writer.setPageEvent(eventHelper);

        // Adicionar metadados
        document.addTitle(bookTitle != null ? bookTitle : "Meu Livro de Colorir");
        document.addAuthor("ColoringBook App");
        document.addCreator("ColoringBook App");
        document.addSubject("Livro de Colorir Personalizado");
        document.addKeywords("colorir, arte, desenho");

        document.open();

        // Página 1: Capa frontal
        addCoverPage(document, bookTitle, images.size());
        document.newPage();

        // Página 2: Página de apresentação
        addWelcomePage(document);
        document.newPage();

        // Página 3: Índice
        addIndexPage(document, images.size());
        document.newPage();

        // Página 4: Dicas de colorir
        addColoringTipsPage(document);
        document.newPage();

        // Marcar onde começam as páginas de colorir
        eventHelper.setColoringPagesStarted(true);

        // Páginas de colorir (cada imagem em uma página)
        for (int i = 0; i < images.size(); i++) {
            ImageBooks imageBook = images.get(i);
            addColoringPage(document, writer, imageBook, i + 1);
            if (i < images.size() - 1) {
                document.newPage();
            }
        }

        // Resetar flag
        eventHelper.setColoringPagesStarted(false);
        document.newPage();

        // Última página: Galeria em branco para criações próprias
        addBlankPagesSection(document, 3);
        document.newPage();

        // Contracapa
        addBackCover(document);

        document.close();
        return outputStream.toByteArray();
    }

    /**
     * Cria a capa frontal do livro
     */
    private void addCoverPage(Document document, String bookTitle, int totalPages) throws DocumentException {
        PdfPTable coverTable = new PdfPTable(1);
        coverTable.setWidthPercentage(100);
        coverTable.setSpacingBefore(0);

        PdfPCell backgroundCell = new PdfPCell();
        backgroundCell.setBackgroundColor(PRIMARY_COLOR);
        backgroundCell.setMinimumHeight(PageSize.A4.getHeight());
        backgroundCell.setBorder(Rectangle.NO_BORDER);
        backgroundCell.setPadding(40);
        backgroundCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        Paragraph coverContent = new Paragraph();
        coverContent.setAlignment(Element.ALIGN_CENTER);

        // Título principal
        Font titleFont = new Font(Font.FontFamily.HELVETICA, 48, Font.BOLD, BaseColor.WHITE);
        Paragraph title = new Paragraph(bookTitle != null ? bookTitle : "MEU LIVRO\nDE COLORIR", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingBefore(100);
        coverContent.add(title);

        // Subtítulo
        Font subtitleFont = new Font(Font.FontFamily.HELVETICA, 20, Font.NORMAL, SECONDARY_COLOR);
        Paragraph subtitle = new Paragraph("\n\nCriatividade sem limites", subtitleFont);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingBefore(20);
        coverContent.add(subtitle);

        // Informações do livro
        Font infoFont = new Font(Font.FontFamily.HELVETICA, 14, Font.NORMAL, BaseColor.WHITE);
        Paragraph info = new Paragraph(
                "\n\n\n" + totalPages + " páginas para colorir\n" +
                        "Criado em " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                infoFont
        );
        info.setAlignment(Element.ALIGN_CENTER);
        info.setSpacingBefore(50);
        coverContent.add(info);

        // Elemento decorativo
        Font decorativeFont = new Font(Font.FontFamily.HELVETICA, 72, Font.BOLD, ACCENT_COLOR);
        Paragraph decorative = new Paragraph("\n\n\n✎ ✿ ❋", decorativeFont);
        decorative.setAlignment(Element.ALIGN_CENTER);
        coverContent.add(decorative);

        backgroundCell.addElement(coverContent);
        coverTable.addCell(backgroundCell);

        document.add(coverTable);
    }

    /**
     * Página de boas-vindas
     */
    private void addWelcomePage(Document document) throws DocumentException {
        Paragraph spacer = new Paragraph("\n\n\n");
        document.add(spacer);

        Font titleFont = new Font(Font.FontFamily.HELVETICA, 32, Font.BOLD, PRIMARY_COLOR);
        Paragraph title = new Paragraph("Bem-vindo ao seu Livro de Colorir!", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        document.add(new Paragraph("\n\n"));

        Font textFont = new Font(Font.FontFamily.HELVETICA, 14, Font.NORMAL, TEXT_COLOR);
        Paragraph welcome = new Paragraph(
                "Este é o seu espaço criativo! Cada página foi especialmente preparada para você " +
                        "explorar suas habilidades artísticas e se divertir colorindo.\n\n" +
                        "Não existem regras - use as cores que quiser, misture técnicas, experimente! " +
                        "Este livro é totalmente seu.\n\n" +
                        "Divirta-se e deixe sua imaginação fluir!",
                textFont
        );
        welcome.setAlignment(Element.ALIGN_JUSTIFIED);
        welcome.setIndentationLeft(50);
        welcome.setIndentationRight(50);
        document.add(welcome);

        document.add(new Paragraph("\n\n"));

        // Box de dica
        PdfPTable tipBox = new PdfPTable(1);
        tipBox.setWidthPercentage(80);
        tipBox.setHorizontalAlignment(Element.ALIGN_CENTER);

        PdfPCell tipCell = new PdfPCell();
        tipCell.setBackgroundColor(new BaseColor(255, 248, 220));
        tipCell.setPadding(20);
        tipCell.setBorder(Rectangle.BOX);
        tipCell.setBorderColor(SECONDARY_COLOR);
        tipCell.setBorderWidth(2);

        Font tipTitleFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, SECONDARY_COLOR);
        Font tipTextFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL, TEXT_COLOR);

        Paragraph tipTitle = new Paragraph("💡 Dica Especial\n\n", tipTitleFont);
        Paragraph tipText = new Paragraph(
                "Para melhores resultados, use lápis de cor, canetinhas ou giz de cera. " +
                        "Experimente também técnicas de sombreamento e degradê!",
                tipTextFont
        );

        tipCell.addElement(tipTitle);
        tipCell.addElement(tipText);
        tipBox.addCell(tipCell);

        document.add(tipBox);
    }

    /**
     * Página de índice
     */
    private void addIndexPage(Document document, int totalPages) throws DocumentException {
        Paragraph spacer = new Paragraph("\n\n");
        document.add(spacer);

        Font titleFont = new Font(Font.FontFamily.HELVETICA, 28, Font.BOLD, PRIMARY_COLOR);
        Paragraph title = new Paragraph("Índice de Páginas", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        document.add(new Paragraph("\n\n"));

        PdfPTable indexTable = new PdfPTable(2);
        indexTable.setWidthPercentage(80);
        indexTable.setHorizontalAlignment(Element.ALIGN_CENTER);
        indexTable.setWidths(new int[]{3, 1});

        Font headerFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, BaseColor.WHITE);
        Font contentFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL, TEXT_COLOR);

        // Cabeçalho
        PdfPCell headerCell1 = new PdfPCell(new Phrase("Desenho", headerFont));
        headerCell1.setBackgroundColor(PRIMARY_COLOR);
        headerCell1.setPadding(10);
        headerCell1.setHorizontalAlignment(Element.ALIGN_CENTER);

        PdfPCell headerCell2 = new PdfPCell(new Phrase("Página", headerFont));
        headerCell2.setBackgroundColor(PRIMARY_COLOR);
        headerCell2.setPadding(10);
        headerCell2.setHorizontalAlignment(Element.ALIGN_CENTER);

        indexTable.addCell(headerCell1);
        indexTable.addCell(headerCell2);

        // Conteúdo
        for (int i = 1; i <= totalPages; i++) {
            PdfPCell cell1 = new PdfPCell(new Phrase("Desenho " + i, contentFont));
            cell1.setPadding(8);
            cell1.setBackgroundColor(i % 2 == 0 ? new BaseColor(245, 245, 245) : BaseColor.WHITE);

            PdfPCell cell2 = new PdfPCell(new Phrase(String.valueOf(i + 4), contentFont));
            cell2.setPadding(8);
            cell2.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell2.setBackgroundColor(i % 2 == 0 ? new BaseColor(245, 245, 245) : BaseColor.WHITE);

            indexTable.addCell(cell1);
            indexTable.addCell(cell2);
        }

        document.add(indexTable);
    }

    /**
     * Página com dicas de colorir
     */
    private void addColoringTipsPage(Document document) throws DocumentException {
        Paragraph spacer = new Paragraph("\n\n");
        document.add(spacer);

        Font titleFont = new Font(Font.FontFamily.HELVETICA, 28, Font.BOLD, PRIMARY_COLOR);
        Paragraph title = new Paragraph("Dicas para Colorir", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        document.add(new Paragraph("\n\n"));

        Font textFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL, TEXT_COLOR);

        String[] tips = {
                "Comece com cores claras e vá adicionando tons mais escuros",
                "Use lápis de cor para criar texturas e sombras suaves",
                "Experimente misturar cores diferentes para criar novos tons",
                "Não tenha medo de sair das linhas - arte é liberdade!",
                "Teste suas cores em um papel antes de aplicar no desenho",
                "Use canetinhas para áreas grandes e lápis para detalhes",
                "Guarde seus materiais organizados para durar mais tempo",
                "Compartilhe suas criações com amigos e família!"
        };

        for (int i = 0; i < tips.length; i++) {
            Paragraph tipNumber = new Paragraph((i + 1) + ". " + tips[i] + "\n\n", textFont);
            tipNumber.setIndentationLeft(50);
            tipNumber.setIndentationRight(50);
            document.add(tipNumber);
        }

        document.add(new Paragraph("\n"));

        // Box motivacional
        PdfPTable motivationBox = new PdfPTable(1);
        motivationBox.setWidthPercentage(80);
        motivationBox.setHorizontalAlignment(Element.ALIGN_CENTER);

        PdfPCell motivationCell = new PdfPCell();
        motivationCell.setBackgroundColor(new BaseColor(230, 230, 250));
        motivationCell.setPadding(20);
        motivationCell.setBorder(Rectangle.BOX);
        motivationCell.setBorderColor(ACCENT_COLOR);
        motivationCell.setBorderWidth(2);

        Font motivationFont = new Font(Font.FontFamily.HELVETICA, 16, Font.ITALIC, PRIMARY_COLOR);
        Paragraph motivation = new Paragraph(
                "\"A criatividade é a inteligência se divertindo.\"\n- Albert Einstein",
                motivationFont
        );
        motivation.setAlignment(Element.ALIGN_CENTER);

        motivationCell.addElement(motivation);
        motivationBox.addCell(motivationCell);

        document.add(motivationBox);
    }

    /**
     * Adiciona uma página de colorir com o desenho
     */
    private void addColoringPage(Document document, PdfWriter writer, ImageBooks imageBook, int pageNumber) throws Exception {
        // Carregar imagem
        Image img = Image.getInstance(new URL(imageBook.getImageUrl()));

        // Escalar imagem para caber na página com margem
        float pageWidth = PageSize.A4.getWidth() - 40;
        float pageHeight = PageSize.A4.getHeight() - 100; // Mais espaço para o rodapé

        img.scaleToFit(pageWidth, pageHeight);

        // Centralizar imagem
        float x = (PageSize.A4.getWidth() - img.getScaledWidth()) / 2;
        float y = (PageSize.A4.getHeight() - img.getScaledHeight()) / 2 + 20;
        img.setAbsolutePosition(x, y);

        document.add(img);

        // O número da página será adicionado pelo PageEventHelper
    }

    /**
     * Adiciona páginas em branco para desenhos livres
     */
    private void addBlankPagesSection(Document document, int numberOfPages) throws DocumentException {
        for (int i = 0; i < numberOfPages; i++) {
            Paragraph spacer = new Paragraph("\n\n\n\n\n");
            document.add(spacer);

            Font titleFont = new Font(Font.FontFamily.HELVETICA, 24, Font.BOLD, PRIMARY_COLOR);
            Paragraph title = new Paragraph("Espaço para Suas Criações", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            document.add(new Paragraph("\n\n"));

            Font textFont = new Font(Font.FontFamily.HELVETICA, 14, Font.ITALIC, TEXT_COLOR);
            Paragraph instruction = new Paragraph(
                    "Use este espaço para desenhar e colorir o que quiser!",
                    textFont
            );
            instruction.setAlignment(Element.ALIGN_CENTER);
            document.add(instruction);

            if (i < numberOfPages - 1) {
                document.newPage();
            }
        }
    }

    /**
     * Contracapa
     */
    private void addBackCover(Document document) throws DocumentException {
        PdfPTable backCoverTable = new PdfPTable(1);
        backCoverTable.setWidthPercentage(100);

        PdfPCell backgroundCell = new PdfPCell();
        backgroundCell.setBackgroundColor(SECONDARY_COLOR);
        backgroundCell.setMinimumHeight(PageSize.A4.getHeight());
        backgroundCell.setBorder(Rectangle.NO_BORDER);
        backgroundCell.setPadding(40);
        backgroundCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        Paragraph backContent = new Paragraph();
        backContent.setAlignment(Element.ALIGN_CENTER);

        Font titleFont = new Font(Font.FontFamily.HELVETICA, 36, Font.BOLD, BaseColor.WHITE);
        Paragraph thanks = new Paragraph("Obrigado!", titleFont);
        thanks.setAlignment(Element.ALIGN_CENTER);
        thanks.setSpacingBefore(150);
        backContent.add(thanks);

        Font textFont = new Font(Font.FontFamily.HELVETICA, 16, Font.NORMAL, BaseColor.WHITE);
        Paragraph message = new Paragraph(
                "\n\nEsperamos que você tenha se divertido\ncolorindo este livro!\n\n" +
                        "Continue criando e explorando\nsua criatividade.",
                textFont
        );
        message.setAlignment(Element.ALIGN_CENTER);
        message.setSpacingBefore(30);
        backContent.add(message);

        Font footerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.ITALIC, BaseColor.WHITE);
        Paragraph footer = new Paragraph(
                "\n\n\n\n\nColoringBook App\n" +
                        LocalDateTime.now().getYear(),
                footerFont
        );
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(100);
        backContent.add(footer);

        backgroundCell.addElement(backContent);
        backCoverTable.addCell(backgroundCell);

        document.add(backCoverTable);
    }

    /**
     * Event handler para adicionar cabeçalho e rodapé
     */
    private static class PageEventHelper extends PdfPageEventHelper {
        private String bookTitle;
        private boolean coloringPagesStarted = false;
        private int coloringPageNumber = 1;

        public PageEventHelper(String bookTitle) {
            this.bookTitle = bookTitle;
        }

        public void setColoringPagesStarted(boolean started) {
            this.coloringPagesStarted = started;
            if (!started) {
                coloringPageNumber = 1;
            }
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            // Adicionar número de página apenas nas páginas de colorir
            if (coloringPagesStarted) {
                PdfContentByte cb = writer.getDirectContent();

                Font pageNumberFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, TEXT_COLOR);
                Phrase pageNumber = new Phrase("Página " + coloringPageNumber, pageNumberFont);

                // Posicionar no rodapé centralizado
                ColumnText.showTextAligned(
                        cb,
                        Element.ALIGN_CENTER,
                        pageNumber,
                        (document.right() - document.left()) / 2 + document.leftMargin(),
                        document.bottom() - 10,
                        0
                );

                coloringPageNumber++;
            }
        }
    }
}