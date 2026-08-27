package com.platform.task.platform.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/uploads")
public class ImageUploadController {
    private final Path directory;

    public ImageUploadController(@Value("${task.upload-directory}") String directory) {
        this.directory = Path.of(directory).toAbsolutePath().normalize();
    }

    @PostMapping("/images")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) throw new IllegalArgumentException("请选择图片");
        byte[] bytes = file.getBytes();
        boolean png = bytes.length >= 8 && bytes[0] == (byte) 0x89 && bytes[1] == 0x50
                && bytes[2] == 0x4e && bytes[3] == 0x47 && bytes[4] == 0x0d && bytes[5] == 0x0a
                && bytes[6] == 0x1a && bytes[7] == 0x0a;
        boolean jpeg = bytes.length >= 3 && bytes[0] == (byte) 0xff && bytes[1] == (byte) 0xd8
                && bytes[2] == (byte) 0xff;
        if (!png && !jpeg) throw new IllegalArgumentException("仅支持真实的 JPG、JPEG、PNG 图片");
        Files.createDirectories(directory);
        String suffix = png ? ".png" : ".jpg";
        Path target = directory.resolve(UUID.randomUUID() + suffix).normalize();
        if (!target.startsWith(directory)) throw new IllegalArgumentException("非法文件名");
        Files.write(target, bytes);
        return Map.of("path", target.toString(), "name", file.getOriginalFilename() == null ? target.getFileName().toString() : file.getOriginalFilename());
    }
}
