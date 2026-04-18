package com.iflytek.smartprep.rag.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * 文档解析工具
 * 支持 PDF、Word、TXT 等多种格式
 */
@Component
public class DocumentParser {

    private final Tika tika = new Tika();

    /**
     * 解析上传的文档文件
     *
     * @param file 上传的文件
     * @return 解析结果（包含文本内容和元数据）
     */
    public ParseResult parseDocument(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        String contentType = file.getContentType();

        if (filename == null) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        // 根据文件扩展名选择解析器
        String extension = getFileExtension(filename).toLowerCase();

        try (InputStream inputStream = file.getInputStream()) {
            return switch (extension) {
                case "pdf" -> parsePDF(inputStream, filename);
                case "doc", "docx" -> parseWord(inputStream, filename);
                case "txt", "md" -> parseText(inputStream, filename);
                default -> parseWithTika(inputStream, filename);
            };
        }
    }

    /**
     * 解析 PDF 文档
     */
    private ParseResult parsePDF(InputStream inputStream, String filename) throws IOException {
        // 创建临时文件
        java.io.File tempFile = java.io.File.createTempFile("pdf_", ".pdf");
        try {
            // 将输入流写入临时文件
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile)) {
                inputStream.transferTo(fos);
            }

            // 使用临时文件加载PDF (PDFBox 3.x API)
            try (PDDocument document = org.apache.pdfbox.Loader.loadPDF(tempFile)) {
                PDFTextStripper stripper = new PDFTextStripper();
                String content = stripper.getText(document);

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("filename", filename);
                metadata.put("pageCount", document.getNumberOfPages());
                metadata.put("format", "PDF");

                // 提取PDF元数据
                if (document.getDocumentInformation() != null) {
                    metadata.put("title", document.getDocumentInformation().getTitle());
                    metadata.put("author", document.getDocumentInformation().getAuthor());
                    metadata.put("subject", document.getDocumentInformation().getSubject());
                }

                return new ParseResult(content, metadata);
            }
        } finally {
            // 删除临时文件
            tempFile.delete();
        }
    }

    /**
     * 解析 Word 文档
     */
    private ParseResult parseWord(InputStream inputStream, String filename) throws IOException {
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            StringBuilder content = new StringBuilder();

            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String text = paragraph.getText();
                if (text != null && !text.trim().isEmpty()) {
                    content.append(text).append("\n");
                }
            }

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("filename", filename);
            metadata.put("paragraphCount", document.getParagraphs().size());
            metadata.put("format", "Word");

            return new ParseResult(content.toString(), metadata);
        }
    }

    /**
     * 解析纯文本文档
     */
    private ParseResult parseText(InputStream inputStream, String filename) throws IOException {
        String content = new String(inputStream.readAllBytes(), "UTF-8");

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("filename", filename);
        metadata.put("format", "Text");
        metadata.put("lineCount", content.split("\n").length);

        return new ParseResult(content, metadata);
    }

    /**
     * 使用 Apache Tika 解析未知格式
     */
    private ParseResult parseWithTika(InputStream inputStream, String filename) throws IOException {
        try {
            String content = tika.parseToString(inputStream);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("filename", filename);
            metadata.put("format", "Unknown (parsed by Tika)");

            return new ParseResult(content, metadata);
        } catch (org.apache.tika.exception.TikaException e) {
            throw new IOException("Tika解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex + 1);
        }
        return "";
    }

    /**
     * 解析结果封装类
     */
    public static class ParseResult {
        private final String content;
        private final Map<String, Object> metadata;

        public ParseResult(String content, Map<String, Object> metadata) {
            this.content = content;
            this.metadata = metadata;
        }

        public String getContent() {
            return content;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public String getFilename() {
            return (String) metadata.get("filename");
        }

        public String getFormat() {
            return (String) metadata.get("format");
        }

        public Integer getPageCount() {
            return (Integer) metadata.get("pageCount");
        }
    }
}
