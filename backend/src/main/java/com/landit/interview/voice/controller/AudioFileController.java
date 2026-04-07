package com.landit.interview.voice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 音频文件控制器
 * 提供临时音频文件下载服务，用于阿里云 Transcription API 访问
 *
 * @author Azir
 */
@Tag(name = "audio-files", description = "音频文件下载")
@RestController
@RequestMapping("/audio-files")
public class AudioFileController {

    @Value("${landit.voice.aliyun.file-asr.storage-path:./data/audio-uploads}")
    private String storagePath;

    @Operation(summary = "下载音频文件")
    @GetMapping("/{filename}")
    public ResponseEntity<Resource> downloadAudioFile(@PathVariable String filename) {
        try {
            // 安全检查：防止路径穿越攻击
            if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                return ResponseEntity.badRequest().build();
            }

            Path filePath = Paths.get(storagePath).resolve(filename).normalize();

            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new UrlResource(filePath.toUri());

            // 根据文件扩展名确定 Content-Type
            String contentType = getContentType(filename);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header("Content-Disposition", "inline; filename=\"" + filename + "\"")
                    .body(resource);

        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 根据文件扩展名获取 Content-Type
     */
    private String getContentType(String filename) {
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        return switch (extension) {
            case "mp3" -> "audio/mpeg";
            case "wav" -> "audio/wav";
            case "m4a" -> "audio/mp4";
            case "aac" -> "audio/aac";
            case "ogg" -> "audio/ogg";
            case "flac" -> "audio/flac";
            case "opus" -> "audio/opus";
            case "mp4" -> "video/mp4";
            default -> "application/octet-stream";
        };
    }
}
