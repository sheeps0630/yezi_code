package com.yechijia.repository;

import com.yechijia.model.FavoriteItem;
import com.yechijia.model.FamilyMember;
import com.yechijia.model.FamilyRecipeItem;
import com.yechijia.model.FoodCategory;
import com.yechijia.model.UserProfile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 内存数据存储（演示版）。
 * 统一保存用户个人信息、自建食用类、收藏夹、家庭菜谱与家庭成员；
 * 使用线程安全的 ConcurrentHashMap，服务重启后数据回到初始状态。
 * 生产环境应替换为数据库实现，接口契约保持不变。
 */
@Component
public class InMemoryUserStore {

    /** 演示用户标识：当前版本不做登录体系，全局使用这一个用户 */
    public static final String DEMO_USER_ID = "demo-user";

    /** 自建食用类（含内置类目与用户创建的类目） */
    private final Map<String, FoodCategory> foodCategories = new ConcurrentHashMap<>();

    /** 收藏夹 */
    private final Map<String, FavoriteItem> favorites = new ConcurrentHashMap<>();

    /** 收藏幂等索引：targetType:targetId -> 收藏条目，并发写入同一对象时保证只落一条 */
    private final Map<String, FavoriteItem> favoriteIndex = new ConcurrentHashMap<>();

    /** 家庭菜谱 */
    private final Map<String, FamilyRecipeItem> familyRecipes = new ConcurrentHashMap<>();

    /** 家庭菜谱幂等索引：dishId -> 条目，并发加入同一道菜时保证只落一条 */
    private final Map<String, FamilyRecipeItem> familyRecipeIndex = new ConcurrentHashMap<>();

    /** 家庭成员 */
    private final Map<String, FamilyMember> familyMembers = new ConcurrentHashMap<>();

    /** 用户个人信息（单用户演示） */
    private volatile UserProfile profile = new UserProfile("椰子", "https://example.com/avatar.png", "138****8888");

    /** 自增 id 生成器：为收藏/家庭菜谱/成员/类目生成唯一标识 */
    private final AtomicLong idGenerator = new AtomicLong(100);

    /**
     * 构造时写入演示初始数据：内置食用类与默认家庭成员，
     * 保证服务一启动三条链路就能直接演示。
     */
    public InMemoryUserStore() {
        seedDefaults();
    }

    /** 写入内置演示数据 */
    private void seedDefaults() {
        // 内置食用类：权重越大越容易被抽中
        saveCategory(new FoodCategory("cat-hotpot", "火锅", 3, true));
        saveCategory(new FoodCategory("cat-bbq", "烧烤", 2, true));
        saveCategory(new FoodCategory("cat-japanese", "日料", 2, true));
        saveCategory(new FoodCategory("cat-noodles", "面馆", 1, true));
        // 默认家庭成员
        familyMembers.put("member-1", new FamilyMember("member-1", "椰子", "本人", ""));
        familyMembers.put("member-2", new FamilyMember("member-2", "椰爸", "爸爸", ""));
    }

    /**
     * 生成一个新的唯一标识。
     *
     * @param prefix 标识前缀（如 cat / fav）
     * @return 唯一标识字符串
     */
    public String nextId(String prefix) {
        return prefix + "-" + idGenerator.incrementAndGet();
    }

    /**
     * 保存或更新一个食用类。
     *
     * @param category 食用类对象
     */
    public void saveCategory(FoodCategory category) {
        foodCategories.put(category.id(), category);
    }

    /** 获取全部食用类 */
    public List<FoodCategory> findAllCategories() {
        return new ArrayList<>(foodCategories.values());
    }

    /**
     * 按标识查找食用类。
     *
     * @param id 类目标识
     * @return 对应食用类，不存在返回 null
     */
    public FoodCategory findCategoryById(String id) {
        return foodCategories.get(id);
    }

    /**
     * 删除食用类。
     *
     * @param id 类目标识
     * @return 是否删除成功（不存在则失败）
     */
    public boolean deleteCategoryById(String id) {
        return foodCategories.remove(id) != null;
    }

    /** 获取用户个人信息 */
    public UserProfile getProfile() {
        return profile;
    }

    /**
     * 更新用户个人信息。
     *
     * @param newProfile 新的个人信息
     */
    public void updateProfile(UserProfile newProfile) {
        this.profile = newProfile;
    }

    /** 获取全部收藏 */
    public List<FavoriteItem> findAllFavorites() {
        return new ArrayList<>(favorites.values());
    }

    /**
     * 原子幂等写入一条收藏：按 targetType:targetId 复合键判重，
     * 已存在时返回已有条目，不存在时写入新条目。
     * 用 putIfAbsent 保证并发下同一对象也只会落一条记录。
     *
     * @param item 待写入的收藏条目
     * @return 实际生效的收藏条目（已存在时为旧条目）
     */
    public FavoriteItem saveFavoriteIfAbsent(FavoriteItem item) {
        String compositeKey = item.targetType() + ":" + item.targetId();
        FavoriteItem existing = favoriteIndex.putIfAbsent(compositeKey, item);
        if (existing != null) {
            return existing;
        }
        favorites.put(item.id(), item);
        return item;
    }

    /**
     * 更新收藏置顶状态。
     *
     * @param id 收藏标识
     * @param pinned 是否置顶
     * @return 更新后的收藏，找不到时返回 null
     */
    public synchronized FavoriteItem updateFavoritePinned(String id, boolean pinned) {
        FavoriteItem existing = favorites.get(id);
        if (existing == null) {
            return null;
        }
        LocalDateTime pinnedAt = pinned ? now() : null;
        FavoriteItem updated = new FavoriteItem(existing.id(), existing.targetType(), existing.targetId(),
                existing.title(), existing.createdAt(), pinned, pinnedAt);
        if (!favorites.replace(id, existing, updated)) {
            return null;
        }
        favoriteIndex.replace(existing.targetType() + ":" + existing.targetId(), existing, updated);
        return updated;
    }

    /**
     * 删除一条收藏。
     *
     * @param id 收藏标识
     * @return 是否删除成功
     */
    public boolean deleteFavoriteById(String id) {
        FavoriteItem removed = favorites.remove(id);
        if (removed == null) {
            return false;
        }
        // 同步清理幂等索引，允许删除后重新收藏
        favoriteIndex.remove(removed.targetType() + ":" + removed.targetId(), removed);
        return true;
    }

    /** 获取全部家庭菜谱条目 */
    public List<FamilyRecipeItem> findAllFamilyRecipes() {
        return new ArrayList<>(familyRecipes.values());
    }

    /**
     * 原子幂等写入一条家庭菜谱条目：按 dishId 判重，
     * 已存在时返回已有条目，不存在时写入新条目。
     * 用 putIfAbsent 保证并发下同一道菜也只会落一条记录。
     *
     * @param item 待写入的家庭菜谱条目
     * @return 实际生效的条目（已存在时为旧条目）
     */
    public FamilyRecipeItem saveFamilyRecipeIfAbsent(FamilyRecipeItem item) {
        FamilyRecipeItem existing = familyRecipeIndex.putIfAbsent(item.dishId(), item);
        if (existing != null) {
            return existing;
        }
        familyRecipes.put(item.id(), item);
        return item;
    }

    /**
     * 删除一条家庭菜谱条目。
     *
     * @param id 条目标识
     * @return 是否删除成功
     */
    public boolean deleteFamilyRecipeById(String id) {
        FamilyRecipeItem removed = familyRecipes.remove(id);
        if (removed == null) {
            return false;
        }
        // 同步清理幂等索引，允许移除后重新加入
        familyRecipeIndex.remove(removed.dishId(), removed);
        return true;
    }

    /** 获取全部家庭成员 */
    public List<FamilyMember> findAllFamilyMembers() {
        return new ArrayList<>(familyMembers.values());
    }

    /**
     * 保存一位家庭成员。
     *
     * @param member 家庭成员对象
     */
    public void saveFamilyMember(FamilyMember member) {
        familyMembers.put(member.id(), member);
    }

    /**
     * 删除一位家庭成员。
     *
     * @param id 成员标识
     * @return 是否删除成功
     */
    public boolean deleteFamilyMemberById(String id) {
        return familyMembers.remove(id) != null;
    }

    /**
     * 获取当前时间（用于时间戳字段），便于测试时统一替换。
     *
     * @return 当前时间
     */
    public LocalDateTime now() {
        return LocalDateTime.now();
    }
}
