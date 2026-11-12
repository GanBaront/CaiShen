package com.caishen.foodlab.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.caishen.foodlab.common.BusinessException;
import com.caishen.foodlab.dto.PreferenceUpdateDTO;
import com.caishen.foodlab.dto.ProfileUpdateDTO;
import com.caishen.foodlab.entity.Recipe;
import com.caishen.foodlab.entity.RecipeFavorite;
import com.caishen.foodlab.entity.RecipeHistory;
import com.caishen.foodlab.entity.User;
import com.caishen.foodlab.entity.UserAvoidItem;
import com.caishen.foodlab.entity.UserPreference;
import com.caishen.foodlab.mapper.RecipeFavoriteMapper;
import com.caishen.foodlab.mapper.RecipeHistoryMapper;
import com.caishen.foodlab.mapper.RecipeMapper;
import com.caishen.foodlab.mapper.UserAvoidItemMapper;
import com.caishen.foodlab.mapper.UserMapper;
import com.caishen.foodlab.mapper.UserPreferenceMapper;
import com.caishen.foodlab.vo.FreeQuotaVO;
import com.caishen.foodlab.vo.PreferenceVO;
import com.caishen.foodlab.vo.RecipeCardVO;
import com.caishen.foodlab.vo.UserInfoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 用户与偏好：基本资料、饮食偏好、收藏、历史、今日免费次数
 */
@Service
@RequiredArgsConstructor
public class UserService {

    /** 忌口食材 */
    public static final int TYPE_AVOID = 1;
    /** 过敏源 */
    public static final int TYPE_ALLERGEN = 2;

    private final UserMapper userMapper;
    private final UserPreferenceMapper preferenceMapper;
    private final UserAvoidItemMapper avoidItemMapper;
    private final RecipeFavoriteMapper favoriteMapper;
    private final RecipeHistoryMapper historyMapper;
    private final RecipeMapper recipeMapper;
    private final FreeQuotaService freeQuotaService;
    private final AuthService authService;
    private final RecipeService recipeService;

    public UserInfoVO getProfile(Long userId) {
        return authService.me(userId);
    }

    public UserInfoVO updateProfile(Long userId, ProfileUpdateDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return authService.me(userId);
        }
        String phone = dto.getPhone() == null ? null : dto.getPhone().trim();
        if (phone != null && !phone.isEmpty() && !phone.matches("^1[3-9]\\d{9}$")) {
            throw new BusinessException("手机号格式不正确，请输入 11 位手机号");
        }
        User update = new User();
        update.setId(userId);
        update.setNickname(dto.getNickname());
        update.setPhone(phone);
        if (dto.getAvatar() != null && !dto.getAvatar().isBlank()) {
            update.setAvatar(dto.getAvatar().trim());
        }
        userMapper.updateById(update);
        return authService.me(userId);
    }

    public PreferenceVO getPreference(Long userId) {
        PreferenceVO vo = new PreferenceVO();
        UserPreference preference = preferenceMapper.selectOne(new LambdaQueryWrapper<UserPreference>()
                .eq(UserPreference::getUserId, userId));
        if (preference != null) {
            vo.setTastes(split(preference.getTastes()));
            if (preference.getDietType() != null && !preference.getDietType().isBlank()) {
                vo.setDietType(preference.getDietType());
            }
            if (preference.getDefaultServings() != null) {
                vo.setDefaultServings(preference.getDefaultServings());
            }
        }
        vo.setAvoidItems(avoidNames(userId, TYPE_AVOID));
        vo.setAllergenItems(avoidNames(userId, TYPE_ALLERGEN));
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public PreferenceVO savePreference(Long userId, PreferenceUpdateDTO dto) {
        String tastes = dto.getTastes() == null ? "" : String.join(",", dto.getTastes());
        UserPreference existed = preferenceMapper.selectOne(new LambdaQueryWrapper<UserPreference>()
                .eq(UserPreference::getUserId, userId));
        UserPreference preference = new UserPreference();
        preference.setUserId(userId);
        preference.setTastes(tastes);
        preference.setDietType(dto.getDietType() == null || dto.getDietType().isBlank() ? "不限" : dto.getDietType());
        preference.setDefaultServings(dto.getDefaultServings() == null ? 2 : dto.getDefaultServings());
        if (existed == null) {
            preferenceMapper.insert(preference);
        } else {
            preference.setId(existed.getId());
            preferenceMapper.updateById(preference);
        }

        avoidItemMapper.delete(new LambdaQueryWrapper<UserAvoidItem>()
                .eq(UserAvoidItem::getUserId, userId));
        saveAvoid(userId, TYPE_AVOID, dto.getAvoidItems());
        saveAvoid(userId, TYPE_ALLERGEN, dto.getAllergenItems());
        return getPreference(userId);
    }

    /** 我的收藏 */
    public List<RecipeCardVO> favorites(Long userId) {
        List<RecipeFavorite> favorites = favoriteMapper.selectList(new LambdaQueryWrapper<RecipeFavorite>()
                .eq(RecipeFavorite::getUserId, userId)
                .orderByDesc(RecipeFavorite::getCreateTime));
        return buildCards(userId, favorites.stream().map(RecipeFavorite::getRecipeId).collect(Collectors.toList()));
    }

    /** 历史记录 */
    public List<RecipeCardVO> history(Long userId) {
        List<RecipeHistory> histories = historyMapper.selectList(new LambdaQueryWrapper<RecipeHistory>()
                .eq(RecipeHistory::getUserId, userId)
                .orderByDesc(RecipeHistory::getViewTime)
                .last("LIMIT 50"));
        Set<Long> ids = new LinkedHashSet<>();
        for (RecipeHistory history : histories) {
            ids.add(history.getRecipeId());
            if (ids.size() >= 20) {
                break;
            }
        }
        return buildCards(userId, new ArrayList<>(ids));
    }

    /** 今日免费次数 */
    public FreeQuotaVO freeQuota(Long userId) {
        return freeQuotaService.quota(userId);
    }

    /** 用户忌口 + 过敏源名称 */
    public List<String> avoidNames(Long userId, int type) {
        if (userId == null) {
            return new ArrayList<>();
        }
        return avoidItemMapper.selectList(new LambdaQueryWrapper<UserAvoidItem>()
                        .eq(UserAvoidItem::getUserId, userId)
                        .eq(UserAvoidItem::getType, type))
                .stream().map(UserAvoidItem::getName).collect(Collectors.toList());
    }

    private List<RecipeCardVO> buildCards(Long userId, List<Long> recipeIds) {
        if (recipeIds == null || recipeIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<Recipe> recipes = recipeMapper.selectBatchIds(recipeIds);
        Map<Long, Recipe> map = recipes.stream().collect(Collectors.toMap(Recipe::getId, Function.identity()));
        List<Recipe> ordered = recipeIds.stream().map(map::get).filter(java.util.Objects::nonNull).collect(Collectors.toList());
        return recipeService.buildCards(userId, ordered);
    }

    private void saveAvoid(Long userId, int type, List<String> names) {
        if (names == null) {
            return;
        }
        for (String name : names) {
            if (name == null || name.isBlank()) {
                continue;
            }
            UserAvoidItem item = new UserAvoidItem();
            item.setUserId(userId);
            item.setType(type);
            item.setName(name.trim());
            avoidItemMapper.insert(item);
        }
    }

    private List<String> split(String value) {
        if (value == null || value.isBlank()) {
            return new ArrayList<>();
        }
        return Arrays.stream(value.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
