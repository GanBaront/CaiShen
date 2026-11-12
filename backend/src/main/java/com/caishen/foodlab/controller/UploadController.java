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
 * 图片上传 + 食材/菜品识别（供首页「上传图片」与 AI 识别页使用）
 */
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    private final UploadService uploadService;
    private final IngredientRecognitionService recognitionService;

    @PostMapping("/image")
    public Result<Map<String, Object>> uploadImage(@RequestParam("file") MultipartFile file) {
        return Result.success(uploadService.uploadImage(file));
    }

    /**
     * 旧版识别入口（首页卡片在用，保留兼容）。
     * 返回结构已扩展：ingredients / dishes / matchedRecipes。
     */
    @PostMapping("/recognize-ingredients")
    public Result<RecognitionVO> recognizeIngredients(@RequestBody Map<String, String> body) {
        return Result.success(recognitionService.recognize(
                AuthContext.getUserId(), body == null ? null : body.get("imageUrl")));
    }
}
