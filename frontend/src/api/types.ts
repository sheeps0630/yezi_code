/**
 * 与后端模型一一对应的 TypeScript 类型定义。
 * 字段命名与后端 JSON（驼峰）保持一致。
 */

/** 统一响应包装结构 */
export interface ApiEnvelope<T> {
  /** 响应码：0 表示成功 */
  code: number
  /** 提示信息 */
  message: string
  /** 业务数据 */
  data: T
}

/** 自建食用类（出去吃） */
export interface FoodCategory {
  /** 类目标识 */
  id: string
  /** 类目名称 */
  name: string
  /** 抽签权重 */
  weight: number
  /** 是否内置类目（不可删除） */
  builtIn: boolean
}

/** 门店（出去吃推荐结果） */
export interface Restaurant {
  /** 门店标识 */
  id: string
  /** 门店名称 */
  name: string
  /** 所属食用类名称 */
  categoryName: string
  /** 门店地址 */
  address: string
  /** 距用户的距离（米，示例数据） */
  distanceMeters: number
  /** 评分 */
  rating: number
}

/** 「出去吃」推荐请求参数 */
export interface EatOutRecommendRequest {
  /** 推荐模式：custom-自建类抽签 / nearby-周边美食 */
  mode: 'custom' | 'nearby'
  /** 用户纬度 */
  latitude?: number
  /** 用户经度 */
  longitude?: number
  /** 不想吃的类目标识 */
  excludedCategoryIds: string[]
  /** 心动的类目标识（nearby 模式优先推荐） */
  preferredCategoryIds: string[]
  /** 本会话已看过的门店标识（随机去重） */
  excludedRestaurantIds: string[]
  /** 附近模式不喜欢的门店类型 */
  excludedCategoryNames: string[]
  /** 附近模式喜欢的门店类型 */
  preferredCategoryNames: string[]
}

/** 一道菜（在家吃） */
export interface Dish {
  /** 菜品标识 */
  id: string
  /** 菜名 */
  name: string
  /** 种类：vegetable / meat / dessert / seafood / soup */
  category: string
  /** 难度：easy / medium / hard */
  difficulty: string
  /** 预计烹饪分钟数 */
  cookMinutes: number
  /** 可供筛选的食材标签 */
  ingredientTags: string[]
}

/** 菜谱（真实数据，禁止编造） */
export interface Recipe {
  /** 菜品标识 */
  dishId: string
  /** 菜名 */
  dishName: string
  /** 食材清单 */
  ingredients: string[]
  /** 烹饪步骤 */
  steps: string[]
  /** 小贴士 */
  tips: string
}

/** 「在家吃」生成菜点结果 */
export interface MenuResponse {
  /** 主菜（可为空，表示未指定） */
  mainDish: Dish | null
  /** 本餐全部菜品 */
  dishes: Dish[]
}

/** 「在家吃」生成菜点请求 */
export interface CookMenuRequest {
  /** 选择的种类（必选） */
  categories: string[]
  /** 主菜标识（可选） */
  mainDishId?: string
  /** 各菜类所选食材标签 */
  ingredientFilters: Record<string, string[]>
}

/** 一款饮品（喝什么推荐结果） */
export interface DrinkProduct {
  /** 产品标识 */
  id: string
  /** 产品名称 */
  productName: string
  /** 商家名称 */
  brand: string
  /** 种类：milk-tea / coffee / others */
  category: string
  /** 参考价格（元） */
  priceYuan: number
  /** 推荐备注 */
  note: string
}

/** 「喝什么」推荐请求参数 */
export interface DrinkRecommendRequest {
  /** 是否选附近店铺 */
  useNearby: boolean
  /** 饮品种类（可选） */
  category?: string
  /** 用户纬度 */
  latitude?: number
  /** 用户经度 */
  longitude?: number
  /** 本会话已看过的饮品标识（随机去重） */
  excludedProductIds: string[]
}

/** 个人信息 */
export interface UserProfile {
  /** 昵称 */
  nickname: string
  /** 头像地址 */
  avatarUrl: string
  /** 手机号 */
  phone: string
}

/** 收藏条目 */
export interface FavoriteItem {
  /** 收藏标识 */
  id: string
  /** 对象类型：dish / restaurant / drink */
  targetType: string
  /** 对象标识 */
  targetId: string
  /** 对象标题 */
  title: string
  /** 收藏时间 */
  createdAt: string
  /** 是否置顶 */
  pinned: boolean
  /** 最近一次置顶时间 */
  pinnedAt: string | null
}

/** 家庭菜谱条目 */
export interface FamilyRecipeItem {
  /** 条目标识 */
  id: string
  /** 菜品标识 */
  dishId: string
  /** 菜名 */
  dishName: string
  /** 添加人 */
  addedBy: string
  /** 添加时间 */
  createdAt: string
}

/** 家庭成员 */
export interface FamilyMember {
  /** 成员标识 */
  id: string
  /** 成员名称 */
  name: string
  /** 与当前用户的关系 */
  relation: string
  /** 联系手机号；演示环境只记录联系方式，不关联其他账号 */
  phone: string
}
