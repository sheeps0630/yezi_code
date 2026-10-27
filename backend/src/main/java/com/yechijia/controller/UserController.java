package com.yechijia.controller;

import com.yechijia.common.ApiResponse;
import com.yechijia.dto.FavoriteRequest;
import com.yechijia.dto.FavoritePinRequest;
import com.yechijia.dto.FamilyMemberRequest;
import com.yechijia.dto.FamilyRecipeRequest;
import com.yechijia.model.FavoriteItem;
import com.yechijia.model.FamilyMember;
import com.yechijia.model.FamilyRecipeItem;
import com.yechijia.model.UserProfile;
import com.yechijia.service.UserService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 个人信息接口：资料、收藏夹、家庭菜谱、家庭成员。
 * 对应主菜单「个人信息」：头像、名称、手机号、收藏夹、家庭菜单、家庭成员。
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    /** 用户业务服务 */
    private final UserService userService;

    /**
     * 构造控制器：注入业务服务。
     *
     * @param userService 用户服务
     */
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 获取当前用户个人信息。
     *
     * @return 个人信息
     */
    @GetMapping("/profile")
    public ApiResponse<UserProfile> getProfile() {
        return ApiResponse.ok(userService.getProfile());
    }

    /**
     * 更新当前用户个人信息。
     *
     * @param profile 新的个人信息
     * @return 更新后的个人信息
     */
    @PutMapping("/profile")
    public ApiResponse<UserProfile> updateProfile(@RequestBody UserProfile profile) {
        return ApiResponse.ok(userService.updateProfile(profile));
    }

    /**
     * 查询收藏夹。
     *
     * @return 收藏列表
     */
    @GetMapping("/favorites")
    public ApiResponse<List<FavoriteItem>> listFavorites() {
        return ApiResponse.ok(userService.listFavorites());
    }

    /**
     * 收藏一个对象（菜品/门店/饮品）。
     * 请求体为 { targetType, targetId, title }。
     *
     * @param request 收藏参数
     * @return 收藏条目
     */
    @PostMapping("/favorites")
    public ApiResponse<FavoriteItem> addFavorite(@RequestBody FavoriteRequest request) {
        return ApiResponse.ok(userService.addFavorite(
                request.targetType(), request.targetId(), request.title()));
    }

    /**
     * 取消收藏。
     *
     * @param favoriteId 收藏条目标识
     * @return 空数据成功响应
     */
    @DeleteMapping("/favorites/{favoriteId}")
    public ApiResponse<Void> removeFavorite(@PathVariable String favoriteId) {
        userService.removeFavorite(favoriteId);
        return ApiResponse.ok(null);
    }

    /**
     * 更新收藏条目的置顶状态。
     *
     * @param favoriteId 收藏标识
     * @param request 置顶状态
     * @return 更新后的收藏条目
     */
    @PutMapping("/favorites/{favoriteId}/pin")
    public ApiResponse<FavoriteItem> updateFavoritePinned(@PathVariable String favoriteId,
                                                           @RequestBody FavoritePinRequest request) {
        return ApiResponse.ok(userService.updateFavoritePinned(favoriteId, request.pinned()));
    }

    /**
     * 查询家庭菜谱。
     *
     * @return 家庭菜谱条目列表
     */
    @GetMapping("/family-recipes")
    public ApiResponse<List<FamilyRecipeItem>> listFamilyRecipes() {
        return ApiResponse.ok(userService.listFamilyRecipes());
    }

    /**
     * 把一道菜加入家庭菜谱。
     * 请求体为 { dishId, addedBy }；addedBy 可空，空时取当前用户昵称。
     *
     * @param request 添加参数
     * @return 家庭菜谱条目
     */
    @PostMapping("/family-recipes")
    public ApiResponse<FamilyRecipeItem> addFamilyRecipe(@RequestBody FamilyRecipeRequest request) {
        return ApiResponse.ok(userService.addFamilyRecipe(request.dishId(), request.addedBy()));
    }

    /**
     * 从家庭菜谱移除一道菜。
     *
     * @param itemId 条目标识
     * @return 空数据成功响应
     */
    @DeleteMapping("/family-recipes/{itemId}")
    public ApiResponse<Void> removeFamilyRecipe(@PathVariable String itemId) {
        userService.removeFamilyRecipe(itemId);
        return ApiResponse.ok(null);
    }

    /**
     * 查询家庭成员。
     *
     * @return 家庭成员列表
     */
    @GetMapping("/family-members")
    public ApiResponse<List<FamilyMember>> listFamilyMembers() {
        return ApiResponse.ok(userService.listFamilyMembers());
    }

    /**
     * 新增一位家庭成员。
     * 请求体为 { name, relation, phone }；手机号仅作为联系方式保存。
     *
     * @param request 成员参数
     * @return 新增的成员
     */
    @PostMapping("/family-members")
    public ApiResponse<FamilyMember> addFamilyMember(@RequestBody FamilyMemberRequest request) {
        return ApiResponse.ok(userService.addFamilyMember(
                request.name(), request.relation(), request.phone()));
    }

    /**
     * 更新家庭成员资料及联系手机号。
     *
     * @param memberId 成员标识
     * @param request 成员参数
     * @return 更新后的成员
     */
    @PutMapping("/family-members/{memberId}")
    public ApiResponse<FamilyMember> updateFamilyMember(@PathVariable String memberId,
                                                         @RequestBody FamilyMemberRequest request) {
        return ApiResponse.ok(userService.updateFamilyMember(
                memberId, request.name(), request.relation(), request.phone()));
    }

    /**
     * 删除一位家庭成员。
     *
     * @param memberId 成员标识
     * @return 空数据成功响应
     */
    @DeleteMapping("/family-members/{memberId}")
    public ApiResponse<Void> removeFamilyMember(@PathVariable String memberId) {
        userService.removeFamilyMember(memberId);
        return ApiResponse.ok(null);
    }
}
