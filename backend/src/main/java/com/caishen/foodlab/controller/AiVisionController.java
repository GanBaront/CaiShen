package com.caishen.foodlab.controller;

import com.caishen.foodlab.common.Result;
import com.caishen.foodlab.security.AuthContext;
import com.caishen.foodlab.service.IngredientRecognitionService;
import com.caishen.foodlab.service.UploadService;
import com.caishen.foodlab.vo.RecognitionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * AI 视觉能力：菜品 / 食材图片识别。
 *
 * 两个入口：
 * - /api/ai/recognize      传已上传图片的 URL（首页先上传再识别，两段式）
 * - /api/ai/recognize/file 直接传文件，一次完成「上传 + 识别」（AI 识别页用，省一次往返）
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiVisionController {

    private final UploadService uploadService;
    private final IngredientRecognitionService recognitionService;

    /** 对已上传的图片做识别 */
    @PostMapping("/recognize")
    public Result<RecognitionVO> recognize(@RequestBody Map<String, String> body) {
        return Result.success(recognitionService.recognize(
                AuthContext.getUserId(), body == null ? null : body.get("imageUrl")));
    }

    /** 上传并立即识别（返回体额外带上 imageUrl，便于前端回显与后续复用） */
    @PostMapping("/recognize/file")
    public Result<RecognitionVO> recognizeFile(@RequestParam("file") MultipartFile file) {
        Map<String, Object> uploaded = uploadService.uploadImage(file);
        String url = String.valueOf(uploaded.get("url"));
        RecognitionVO vo = recognitionService.recognize(AuthContext.getUserId(), url);
        vo.setImageUrl(url);
        return Result.success(vo);
    }
}
