package com.caishen.foodlab.service;

import com.caishen.foodlab.common.BusinessException;
import com.caishen.foodlab.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * 图片上传：保存到本地 uploads 目录，返回可访问 URL（供首页「上传图片」识别食材用）
 */
@Service
@RequiredArgsConstructor
public class UploadService {

    private static final List<String> ALLOWED_EXT = List.of("jpg", "jpeg", "png", "gif", "webp");

    private final AppProperties appProperties;

    public Map<String, Object> uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的图片");
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = original.contains(".")
                ? original.substring(original.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BusinessException("仅支持 jpg / jpeg / png / gif / webp 格式的图片");
        }

        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path dir = Paths.get(appProperties.getUploadDir(), datePath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(dir);
            file.transferTo(dir.resolve(fileName).toFile());
        } catch (IOException e) {
            throw new BusinessException("图片上传失败：" + e.getMessage());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("fileName", fileName);
        result.put("url", appProperties.getUploadUrlPrefix() + "/" + datePath + "/" + fileName);
        return result;
    }
}
