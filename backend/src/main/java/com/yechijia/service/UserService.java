package com.yechijia.service;

import com.yechijia.common.BusinessException;
import com.yechijia.model.FavoriteItem;
import com.yechijia.model.FamilyMember;
import com.yechijia.model.FamilyRecipeItem;
import com.yechijia.model.FoodCategory;
import com.yechijia.model.UserProfile;
import com.yechijia.repository.InMemoryUserStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Comparator;

/**
 * 用户服务：个人信息、收藏夹、家庭菜谱、家庭成员与自建食用类的读写。
 * 当前版本为内存存储的演示实现，接口契约与生产版保持一致。
 */
@Service
public class UserService {

    /** 用户数据存储 */
    private final InMemoryUserStore userStore;

    /** 静态数据加载器：用于校验菜品是否存在 */
    private final com.yechijia.repository.StaticDataLoader staticDataLoader;

    /**
     * 构造服务：注入用户存储与静态数据加载器。
     *
     * @param userStore       用户数据存储
     * @param staticDataLoader 静态数据加载器
     */
    public UserService(InMemoryUserStore userStore,
                        com.yechijia.repository.StaticDataLoader staticDataLoader) {
        this.userStore = userStore;
        this.staticDataLoader = staticDataLoader;
    }

    /**
     * 获取当前用户个人信息。
     *
     * @return 个人信息
     */
    public UserProfile getProfile() {
        return userStore.getProfile();
    }

    /**
     * 更新当前用户个人信息。
     *
     * @param newProfile 新的个人信息
     * @return 更新后的个人信息
     */
    public UserProfile updateProfile(UserProfile newProfile) {
        if (newProfile == null || newProfile.nickname() == null || newProfile.nickname().isBlank()) {
            throw new BusinessException("昵称不能为空");
        }
        // 手机号格式校验：允许为空、脱敏格式（如 138****8888）或 11 位大陆手机号
        String phone = newProfile.phone();
        if (phone != null && !phone.isBlank()
                && !phone.matches("\\d{3}\\*{4}\\d{4}") && !phone.matches("1[3-9]\\d{9}")) {
            throw new BusinessException("手机号格式不正确");
        }
        userStore.updateProfile(newProfile);
        return userStore.getProfile();
    }

    /**
     * 查询全部自建食用类。
     *
     * @return 食用类列表
     */
    public List<FoodCategory> listCategories() {
        return userStore.findAllCategories();
    }

    /**
     * 新建一个自建食用类。
     *
     * @param name   类目名称
     * @param weight 抽签权重（空则默认 1）
     * @return 新建的食用类
     */
    public FoodCategory createCategory(String name, Integer weight) {
        if (name == null || name.isBlank()) {
            throw new BusinessException("类目名称不能为空");
        }
        FoodCategory category = new FoodCategory(
                userStore.nextId("cat"), name.trim(), weight == null ? 1 : Math.max(weight, 1), false);
        userStore.saveCategory(category);
        return category;
    }

    /**
     * 更新食用类的权重（或名称）。
     *
     * @param id     类目标识
     * @param name   新名称（空则不变）
     * @param weight 新权重（空则不变）
     * @return 更新后的食用类
     */
    public FoodCategory updateCategory(String id, String name, Integer weight) {
        FoodCategory existing = requireCategory(id);
        String newName = (name == null || name.isBlank()) ? existing.name() : name.trim();
        int newWeight = weight == null ? existing.weight() : Math.max(weight, 1);
        FoodCategory updated = new FoodCategory(existing.id(), newName, newWeight, existing.builtIn());
        userStore.saveCategory(updated);
        return updated;
    }

    /**
     * 删除自建食用类（内置类目不允许删除）。
     *
     * @param id 类目标识
     */
    public void deleteCategory(String id) {
        FoodCategory existing = requireCategory(id);
        if (existing.builtIn()) {
            throw new BusinessException("内置类目不允许删除");
        }
        userStore.deleteCategoryById(id);
    }

    /**
     * 按标识获取食用类，不存在时抛业务异常。
     *
     * @param id 类目标识
     * @return 对应食用类
     */
    private FoodCategory requireCategory(String id) {
        FoodCategory category = userStore.findCategoryById(id);
        if (category == null) {
            throw new BusinessException("类目不存在");
        }
        return category;
    }

    /**
     * 查询收藏夹。
     *
     * @return 收藏列表
     */
    public List<FavoriteItem> listFavorites() {
        return userStore.findAllFavorites().stream()
                .sorted(Comparator.comparing(FavoriteItem::pinned).reversed()
                        .thenComparing(item -> item.pinned()
                                ? item.pinnedAt()
                                : item.createdAt(), Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    /**
     * 收藏一个对象（菜品/门店/饮品）。
     * 重复收藏直接返回已有条目，保证幂等。
     *
     * @param targetType 对象类型
     * @param targetId   对象标识
     * @param title      对象标题（菜名/店名/产品名）
     * @return 收藏条目
     */
    public FavoriteItem addFavorite(String targetType, String targetId, String title) {
        if (targetType == null || targetId == null || title == null) {
            throw new BusinessException("收藏参数不完整");
        }
        // 原子幂等：按复合键 putIfAbsent 写入，并发收藏同一对象也只落一条
        FavoriteItem item = new FavoriteItem(
                userStore.nextId("fav"), targetType, targetId, title, userStore.now(), false, null);
        return userStore.saveFavoriteIfAbsent(item);
    }

    /**
     * 更新收藏条目的置顶状态。
     *
     * @param favoriteId 收藏标识
     * @param pinned 是否置顶
     * @return 更新后的收藏条目
     */
    public FavoriteItem updateFavoritePinned(String favoriteId, boolean pinned) {
        FavoriteItem updated = userStore.updateFavoritePinned(favoriteId, pinned);
        if (updated == null) {
            throw new BusinessException("收藏不存在或已删除");
        }
        return updated;
    }

    /**
     * 取消收藏。
     *
     * @param favoriteId 收藏条目标识
     */
    public void removeFavorite(String favoriteId) {
        if (!userStore.deleteFavoriteById(favoriteId)) {
            throw new BusinessException("收藏不存在或已删除");
        }
    }

    /**
     * 查询家庭菜谱。
     *
     * @return 家庭菜谱条目列表
     */
    public List<FamilyRecipeItem> listFamilyRecipes() {
        return userStore.findAllFamilyRecipes();
    }

    /**
     * 把一道菜加入家庭菜谱（需求：菜谱最下方「加入家庭菜谱」按钮）。
     * 幂等：已加入则直接返回已有条目。
     *
     * @param dishId 菜品标识
     * @param addedBy 添加人名称
     * @return 家庭菜谱条目
     */
    public FamilyRecipeItem addFamilyRecipe(String dishId, String addedBy) {
        // 校验菜品存在，防止把不存在的菜加入家庭菜谱
        boolean dishExists = staticDataLoader.getDishes().stream()
                .anyMatch(d -> d.id().equals(dishId));
        if (!dishExists) {
            throw new BusinessException("菜品不存在，无法加入家庭菜谱");
        }
        String dishName = staticDataLoader.getDishes().stream()
                .filter(d -> d.id().equals(dishId))
                .map(d -> d.name())
                .findFirst()
                .orElse("");
        // 原子幂等：按 dishId putIfAbsent 写入，并发加入同一道菜也只落一条
        FamilyRecipeItem item = new FamilyRecipeItem(
                userStore.nextId("frecipe"), dishId, dishName,
                addedBy == null ? userStore.getProfile().nickname() : addedBy, userStore.now());
        return userStore.saveFamilyRecipeIfAbsent(item);
    }

    /**
     * 从家庭菜谱移除一道菜。
     *
     * @param itemId 条目标识
     */
    public void removeFamilyRecipe(String itemId) {
        if (!userStore.deleteFamilyRecipeById(itemId)) {
            throw new BusinessException("家庭菜谱条目不存在或已删除");
        }
    }

    /**
     * 查询家庭成员。
     *
     * @return 家庭成员列表
     */
    public List<FamilyMember> listFamilyMembers() {
        return userStore.findAllFamilyMembers();
    }

    /**
     * 新增一位家庭成员。
     *
     * @param name     成员名称
     * @param relation 与当前用户的关系
     * @param phone    联系手机号
     * @return 新增的成员
     */
    public FamilyMember addFamilyMember(String name, String relation, String phone) {
        if (name == null || name.isBlank()) {
            throw new BusinessException("成员名称不能为空");
        }
        validateFamilyMemberPhone(phone);
        FamilyMember member = new FamilyMember(userStore.nextId("member"), name.trim(),
                relation == null ? "" : relation.trim(), phone.trim());
        userStore.saveFamilyMember(member);
        return member;
    }

    /**
     * 更新家庭成员资料及联系手机号。
     *
     * @param memberId 成员标识
     * @param name     成员名称
     * @param relation 与当前用户的关系
     * @param phone    联系手机号
     * @return 更新后的成员
     */
    public FamilyMember updateFamilyMember(String memberId, String name, String relation, String phone) {
        FamilyMember existing = userStore.findAllFamilyMembers().stream()
                .filter(member -> member.id().equals(memberId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("成员不存在或已删除"));
        if (name == null || name.isBlank()) {
            throw new BusinessException("成员名称不能为空");
        }
        validateFamilyMemberPhone(phone);
        FamilyMember updated = new FamilyMember(existing.id(), name.trim(),
                relation == null ? "" : relation.trim(), phone.trim());
        userStore.saveFamilyMember(updated);
        return updated;
    }

    /**
     * 校验家庭成员手机号格式。
     *
     * @param phone 联系手机号
     */
    private void validateFamilyMemberPhone(String phone) {
        if (phone == null || !phone.trim().matches("1[3-9]\\d{9}")) {
            throw new BusinessException("请输入有效的 11 位手机号");
        }
    }

    /**
     * 删除一位家庭成员。
     *
     * @param memberId 成员标识
     */
    public void removeFamilyMember(String memberId) {
        if (!userStore.deleteFamilyMemberById(memberId)) {
            throw new BusinessException("成员不存在或已删除");
        }
    }
}
