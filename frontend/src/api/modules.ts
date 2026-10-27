/**
 * 全部后端接口的调用方法，按业务模块分组。
 * 供各页面直接调用，页面不关心 HTTP 细节。
 */
import { request } from './client'
import type {
  CookMenuRequest,
  DrinkProduct,
  DrinkRecommendRequest,
  Dish,
  EatOutRecommendRequest,
  FamilyMember,
  FamilyRecipeItem,
  FavoriteItem,
  FoodCategory,
  MenuResponse,
  Recipe,
  Restaurant,
  UserProfile,
} from './types'

/* ==================== 出去吃 ==================== */

/**
 * 查询用户的全部自建食用类。
 *
 * @returns 食用类列表
 */
export function listFoodCategories(): Promise<FoodCategory[]> {
  return request<FoodCategory[]>('/api/eat-out/categories', 'GET')
}

/**
 * 查询演示门店池中实际存在的美食类型。
 *
 * @returns 可供附近偏好筛选的类型名称
 */
export function listNearbyFoodTypes(): Promise<string[]> {
  return request<string[]>('/api/eat-out/types', 'GET')
}

/**
 * 新建自建食用类。
 *
 * @param name   类目名称
 * @param weight 抽签权重
 * @returns 新建的食用类
 */
export function createFoodCategory(name: string, weight: number): Promise<FoodCategory> {
  return request<FoodCategory>('/api/eat-out/categories', 'POST', { name, weight })
}

/**
 * 更新食用类（改名或调权重）。
 *
 * @param id     类目标识
 * @param name   新名称（空则不变）
 * @param weight 新权重（空则不变）
 * @returns 更新后的食用类
 */
export function updateFoodCategory(id: string, name: string, weight: number): Promise<FoodCategory> {
  return request<FoodCategory>(`/api/eat-out/categories/${id}`, 'PUT', { name, weight })
}

/**
 * 删除自建食用类。
 *
 * @param id 类目标识
 */
export function deleteFoodCategory(id: string): Promise<void> {
  return request<void>(`/api/eat-out/categories/${id}`, 'DELETE')
}

/**
 * 推荐一家门店（含排除与随机去重）。
 *
 * @param payload 推荐请求参数
 * @returns 推荐的门店
 */
export function recommendEatOut(payload: EatOutRecommendRequest): Promise<Restaurant> {
  return request<Restaurant>('/api/eat-out/recommend', 'POST', payload)
}

/* ==================== 在家吃 ==================== */

/**
 * 获取在家吃饭的种类列表。
 *
 * @returns 种类标识列表
 */
export function listCookCategories(): Promise<string[]> {
  return request<string[]>('/api/cook/categories', 'GET')
}

/**
 * 获取菜类对应的可选食材标签。
 *
 * @returns 菜类到食材标签的映射
 */
export function listCookIngredientTypes(): Promise<Record<string, string[]>> {
  return request<Record<string, string[]>>('/api/cook/ingredient-types', 'GET')
}

/**
 * 获取某种类下的全部菜品（供选主菜）。
 *
 * @param category 种类标识
 * @returns 菜品列表
 */
export function listDishesByCategory(category: string): Promise<Dish[]> {
  return request<Dish[]>(`/api/cook/dishes?category=${category}`, 'GET')
}

/**
 * 根据所选种类与主菜生成一餐菜点。
 *
 * @param payload 生成请求
 * @returns 菜点组合
 */
export function generateMenu(payload: CookMenuRequest): Promise<MenuResponse> {
  return request<MenuResponse>('/api/cook/menu', 'POST', payload)
}

/**
 * 查询一道菜的真实菜谱。
 *
 * @param dishId 菜品标识
 * @returns 菜谱详情
 */
export function getRecipe(dishId: string): Promise<Recipe> {
  return request<Recipe>(`/api/cook/recipes/${dishId}`, 'GET')
}

/* ==================== 喝什么 ==================== */

/**
 * 获取饮品种类列表。
 *
 * @returns 种类标识列表
 */
export function listDrinkCategories(): Promise<string[]> {
  return request<string[]>('/api/drink/categories', 'GET')
}

/**
 * 推荐一款饮品（附近/随机，含去重）。
 *
 * @param payload 推荐请求参数
 * @returns 推荐的饮品
 */
export function recommendDrink(payload: DrinkRecommendRequest): Promise<DrinkProduct> {
  return request<DrinkProduct>('/api/drink/recommend', 'POST', payload)
}

/* ==================== 个人信息 ==================== */

/**
 * 获取当前用户个人信息。
 *
 * @returns 个人信息
 */
export function getProfile(): Promise<UserProfile> {
  return request<UserProfile>('/api/user/profile', 'GET')
}

/**
 * 更新当前用户个人信息。
 *
 * @param profile 新的个人信息
 * @returns 更新后的个人信息
 */
export function updateProfile(profile: UserProfile): Promise<UserProfile> {
  return request<UserProfile>('/api/user/profile', 'PUT', profile)
}

/**
 * 查询收藏夹。
 *
 * @returns 收藏列表
 */
export function listFavorites(): Promise<FavoriteItem[]> {
  return request<FavoriteItem[]>('/api/user/favorites', 'GET')
}

/**
 * 收藏一个对象。
 *
 * @param targetType 对象类型
 * @param targetId   对象标识
 * @param title      对象标题
 * @returns 收藏条目
 */
export function addFavorite(targetType: string, targetId: string, title: string): Promise<FavoriteItem> {
  return request<FavoriteItem>('/api/user/favorites', 'POST', { targetType, targetId, title })
}

/**
 * 更新收藏条目的置顶状态。
 *
 * @param favoriteId 收藏标识
 * @param pinned 是否置顶
 * @returns 更新后的收藏条目
 */
export function setFavoritePinned(favoriteId: string, pinned: boolean): Promise<FavoriteItem> {
  return request<FavoriteItem>(`/api/user/favorites/${favoriteId}/pin`, 'PUT', { pinned })
}

/**
 * 取消收藏。
 *
 * @param favoriteId 收藏条目标识
 */
export function removeFavorite(favoriteId: string): Promise<void> {
  return request<void>(`/api/user/favorites/${favoriteId}`, 'DELETE')
}

/**
 * 查询家庭菜谱。
 *
 * @returns 家庭菜谱条目列表
 */
export function listFamilyRecipes(): Promise<FamilyRecipeItem[]> {
  return request<FamilyRecipeItem[]>('/api/user/family-recipes', 'GET')
}

/**
 * 把一道菜加入家庭菜谱。
 * 不传添加人时由后端取当前用户昵称，避免前端硬编码导致改名后不同步。
 *
 * @param dishId  菜品标识
 * @param addedBy 添加人（可选，默认由后端取当前昵称）
 * @returns 家庭菜谱条目
 */
export function addFamilyRecipe(dishId: string, addedBy?: string | null): Promise<FamilyRecipeItem> {
  return request<FamilyRecipeItem>('/api/user/family-recipes', 'POST', { dishId, addedBy: addedBy ?? null })
}

/**
 * 从家庭菜谱移除一道菜。
 *
 * @param itemId 条目标识
 */
export function removeFamilyRecipe(itemId: string): Promise<void> {
  return request<void>(`/api/user/family-recipes/${itemId}`, 'DELETE')
}

/**
 * 查询家庭成员。
 *
 * @returns 家庭成员列表
 */
export function listFamilyMembers(): Promise<FamilyMember[]> {
  return request<FamilyMember[]>('/api/user/family-members', 'GET')
}

/**
 * 新增一位家庭成员。
 *
 * @param name     成员名称
 * @param relation 与当前用户的关系
 * @param phone    联系手机号（演示环境只记录联系方式）
 * @returns 新增的成员
 */
export function addFamilyMember(name: string, relation: string, phone: string): Promise<FamilyMember> {
  return request<FamilyMember>('/api/user/family-members', 'POST', { name, relation, phone })
}

/**
 * 更新家庭成员资料及联系方式。
 *
 * @param memberId 成员标识
 * @param name 成员名称
 * @param relation 与当前用户的关系
 * @param phone 联系手机号
 * @returns 更新后的成员
 */
export function updateFamilyMember(
  memberId: string,
  name: string,
  relation: string,
  phone: string,
): Promise<FamilyMember> {
  return request<FamilyMember>(`/api/user/family-members/${memberId}`, 'PUT', { name, relation, phone })
}

/**
 * 删除一位家庭成员。
 *
 * @param memberId 成员标识
 */
export function removeFamilyMember(memberId: string): Promise<void> {
  return request<void>(`/api/user/family-members/${memberId}`, 'DELETE')
}
