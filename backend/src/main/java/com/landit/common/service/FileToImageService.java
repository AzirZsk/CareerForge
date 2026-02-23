package com.landit.common.service;

import cn.hutool.core.util.StrUtil;
import com.landit.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.ai.content.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;

/**
 * 文件转图片服务
 * 支持将图片、PDF、Word 文件转换为图片列表
 *
 * @author Azir
 */
@Slf4j
@Service
public class FileToImageService {

    private static final float PDF_DPI = 150;
    private static final String IMAGE_FORMAT = "PNG";
    private static final int MAX_IMAGE_WIDTH = 2000;
    // 支持的图片扩展名
    private static final Set<String> IMAGE_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp");
    // 支持的文档扩展名
    private static final Set<String> DOCUMENT_EXTENSIONS = Set.of(".docx", ".doc");

    public List<String> convertToImages(MultipartFile file) {
        String filename = file.getOriginalFilename();
        String fileType = getFileType(filename);
        log.info("开始转换文件: {}，类型: {}", filename, fileType);
        try {
            return switch (fileType) {
                case "IMAGE" -> convertImageToBase64(file);
                case "PDF" -> convertPdfToBase64(file);
                case "WORD" -> convertWordFile(file);
                default -> throw new BusinessException("不支持的文件类型: " + fileType);
            };
        } catch (Exception e) {
            log.error("文件转换失败: {}", filename, e);
            throw new BusinessException("文件转换失败: " + e.getMessage());
        }
    }

    /**
     * 将文件转换为 Spring AI Media 对象列表
     * 用于多模态 AI 对话场景
     */
    public List<Media> convertToMedia(MultipartFile file) {
        String filename = file.getOriginalFilename();
        String fileType = getFileType(filename);
        log.info("开始转换文件为Media: {}，类型: {}", filename, fileType);
        try {
            return switch (fileType) {
                case "IMAGE" -> convertImageToMedia(file);
                case "PDF" -> convertPdfToMedia(file);
                case "WORD" -> convertWordToMedia(file);
                default -> throw new BusinessException("不支持的文件类型: " + fileType);
            };
        } catch (Exception e) {
            log.error("文件转换失败: {}", filename, e);
            throw new BusinessException("文件转换失败: " + e.getMessage());
        }
    }

    public boolean isSupported(String contentType, String filename) {
        if (StrUtil.isBlank(filename)) {
            return false;
        }
        String lowerName = filename.toLowerCase();
        return hasExtension(lowerName, IMAGE_EXTENSIONS)
                || lowerName.endsWith(".pdf")
                || hasExtension(lowerName, DOCUMENT_EXTENSIONS);
    }

    public String getFileType(String filename) {
        if (StrUtil.isBlank(filename)) {
            return "UNKNOWN";
        }
        String lowerName = filename.toLowerCase();
        if (hasExtension(lowerName, IMAGE_EXTENSIONS)) {
            return "IMAGE";
        }
        if (lowerName.endsWith(".pdf")) {
            return "PDF";
        }
        if (hasExtension(lowerName, DOCUMENT_EXTENSIONS)) {
            return "WORD";
        }
        return "UNKNOWN";
    }

    /**
     * 检查文件名是否具有指定的扩展名之一
     */
    private boolean hasExtension(String filename, Set<String> extensions) {
        return extensions.stream().anyMatch(filename::endsWith);
    }

    /**
     * 处理图片文件 - 直接转为 Base64
     */
    private List<String> convertImageToBase64(MultipartFile file) throws IOException {
        byte[] bytes = file.getBytes();
        String base64 = Base64.getEncoder().encodeToString(bytes);
        String mimeType = getMimeType(file.getOriginalFilename());
        String dataUrl = "data:" + mimeType + ";base64," + base64;
        log.info("图片文件转换完成，大小: {} bytes", bytes.length);
        return List.of(dataUrl);
    }

    /**
     * 处理图片文件 - 转为 Spring AI Media 对象（统一转换为 PNG 格式）
     */
    private List<Media> convertImageToMedia(MultipartFile file) throws IOException {
        BufferedImage image = ImageIO.read(file.getInputStream());
        if (image == null) {
            throw new IOException("无法读取图片文件: " + file.getOriginalFilename());
        }
        BufferedImage scaledImage = scaleImageIfNeeded(image);
        byte[] bytes = imageToBytes(scaledImage);
        MimeType mimeType = MimeTypeUtils.IMAGE_PNG;
        Media media = new Media(mimeType, new ByteArrayResource(bytes));
        log.info("图片文件转换为Media完成，大小: {} bytes", bytes.length);
        return List.of(media);
    }

    /**
     * 处理 PDF 文件 - 每页转为一张图片（Base64 格式）
     */
    private List<String> convertPdfToBase64(MultipartFile file) throws IOException {
        List<String> images = new ArrayList<>();
        List<byte[]> imageBytes = convertPdfToBytes(file);
        for (byte[] bytes : imageBytes) {
            String base64 = Base64.getEncoder().encodeToString(bytes);
            String dataUrl = "data:image/png;base64," + base64;
            images.add(dataUrl);
        }
        return images;
    }

    /**
     * 处理 PDF 文件 - 每页转为一张图片（Media 格式）
     */
    private List<Media> convertPdfToMedia(MultipartFile file) throws IOException {
        List<Media> mediaList = new ArrayList<>();
        List<byte[]> imageBytes = convertPdfToBytes(file);
        MimeType mimeType = MimeTypeUtils.IMAGE_PNG;
        for (byte[] bytes : imageBytes) {
            mediaList.add(new Media(mimeType, new ByteArrayResource(bytes)));
        }
        return mediaList;
    }

    /**
     * 将 PDF 每页转换为图片字节数组
     */
    private List<byte[]> convertPdfToBytes(MultipartFile file) throws IOException {
        List<byte[]> images = new ArrayList<>();
        byte[] pdfBytes = file.getBytes();
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFRenderer renderer = new PDFRenderer(document);
            int pageCount = document.getNumberOfPages();
            log.info("PDF 文件共 {} 页", pageCount);
            for (int i = 0; i < pageCount; i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, PDF_DPI, ImageType.RGB);
                BufferedImage scaledImage = scaleImageIfNeeded(image);
                byte[] imageBytes = imageToBytes(scaledImage);
                images.add(imageBytes);
                log.debug("PDF 第 {} 页转换完成", i + 1);
            }
        }
        log.info("PDF 文件转换完成，共 {} 张图片", images.size());
        return images;
    }

    /**
     * 处理 Word 文件（暂不支持）
     */
    private List<String> convertWordFile(MultipartFile file) {
        log.warn("Word 文件暂不支持直接转图片，建议上传 PDF 或图片格式");
        throw new UnsupportedOperationException(
                "Word 文件暂不支持直接转换为图片。请将 Word 导出为 PDF 或截图后上传。");
    }

    /**
     * 处理 Word 文件 - 转为 Media（暂不支持）
     */
    private List<Media> convertWordToMedia(MultipartFile file) {
        log.warn("Word 文件暂不支持直接转图片，建议上传 PDF 或图片格式");
        throw new UnsupportedOperationException(
                "Word 文件暂不支持直接转换为图片。请将 Word 导出为 PDF 或截图后上传。");
    }

    /**
     * 图片缩放（如果超过最大宽度）
     */
    private BufferedImage scaleImageIfNeeded(BufferedImage image) {
        if (image.getWidth() <= MAX_IMAGE_WIDTH) {
            return image;
        }
        double scale = (double) MAX_IMAGE_WIDTH / image.getWidth();
        int newWidth = MAX_IMAGE_WIDTH;
        int newHeight = (int) (image.getHeight() * scale);
        BufferedImage scaledImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        scaledImage.getGraphics().drawImage(
                image.getScaledInstance(newWidth, newHeight, BufferedImage.SCALE_SMOOTH),
                0, 0, null);
        return scaledImage;
    }

    /**
     * BufferedImage 转字节数组
     */
    private byte[] imageToBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, IMAGE_FORMAT, baos);
        return baos.toByteArray();
    }

    /**
     * 获取图片 MIME 类型
     */
    private String getMimeType(String filename) {
        if (StrUtil.isBlank(filename)) {
            return "image/png";
        }
        String lowerName = filename.toLowerCase();
        if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lowerName.endsWith(".png")) {
            return "image/png";
        }
        if (lowerName.endsWith(".gif")) {
            return "image/gif";
        }
        if (lowerName.endsWith(".webp")) {
            return "image/webp";
        }
        if (lowerName.endsWith(".bmp")) {
            return "image/bmp";
        }
        return "image/png";
    }

}
