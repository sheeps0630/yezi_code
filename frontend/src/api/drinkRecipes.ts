/** 固定整理的在家饮品配方，不从模型动态编造。 */
export interface DrinkRecipe {
  id: string
  name: string
  category: string
  ingredients: string[]
  steps: string[]
  preparationMinutes: number
  tips: string
  containsAlcohol: boolean
}

/** 首批家庭饮品配方，每款以一人份为准。 */
export const drinkRecipes: DrinkRecipe[] = [
  {
    id: 'drink-recipe-lemon-passionfruit',
    name: '柠檬百香果茶',
    category: 'fruit-tea',
    ingredients: ['红茶 150 毫升（冷却）', '百香果 1 个', '柠檬 2 片', '蜂蜜 10 毫升', '冰块适量'],
    steps: ['红茶泡好后放凉。', '杯中加入百香果果肉、柠檬片和蜂蜜。', '倒入红茶与冰块，搅拌均匀即可。'],
    preparationMinutes: 10,
    tips: '蜂蜜可按个人口味增减。',
    containsAlcohol: false,
  },
  {
    id: 'drink-recipe-peach-oolong',
    name: '蜜桃乌龙果茶',
    category: 'fruit-tea',
    ingredients: ['乌龙茶 180 毫升（冷却）', '熟桃子 1/2 个', '柠檬汁 5 毫升', '冰块适量'],
    steps: ['乌龙茶泡好后放凉。', '桃子切小块，轻压出汁。', '加入茶汤、柠檬汁和冰块，搅拌后饮用。'],
    preparationMinutes: 12,
    tips: '选择成熟多汁的桃子，通常无需额外加糖。',
    containsAlcohol: false,
  },
  {
    id: 'drink-recipe-pearl-milk-tea',
    name: '珍珠奶茶',
    category: 'milk-tea',
    ingredients: ['红茶 150 毫升', '牛奶 100 毫升', '熟珍珠 50 克', '糖浆 5 毫升（可选）'],
    steps: ['红茶泡浓后放凉。', '珍珠按包装说明煮熟并沥干。', '杯中放入珍珠，加入红茶和牛奶搅匀。', '按口味加入少量糖浆。'],
    preparationMinutes: 15,
    tips: '珍珠煮熟后尽快食用，口感更佳。',
    containsAlcohol: false,
  },
  {
    id: 'drink-recipe-caramel-milk-tea',
    name: '焦糖奶茶',
    category: 'milk-tea',
    ingredients: ['红茶 180 毫升', '牛奶 120 毫升', '焦糖酱 10 克'],
    steps: ['红茶泡好备用。', '小锅加入牛奶和焦糖酱，小火加热并搅拌至融化。', '加入红茶搅匀，温热饮用或放凉后加冰。'],
    preparationMinutes: 10,
    tips: '小火加热牛奶，避免煮沸溢出。',
    containsAlcohol: false,
  },
  {
    id: 'drink-recipe-iced-americano',
    name: '冰美式',
    category: 'coffee',
    ingredients: ['浓缩咖啡 30 毫升', '冷水 150 毫升', '冰块适量'],
    steps: ['将浓缩咖啡萃取到耐热杯中。', '另取杯子装入冰块和冷水。', '把咖啡缓缓倒入冷水中即可。'],
    preparationMinutes: 5,
    tips: '咖啡因敏感者可改用低咖啡因咖啡或减少份量。',
    containsAlcohol: false,
  },
  {
    id: 'drink-recipe-coconut-latte',
    name: '生椰拿铁',
    category: 'coffee',
    ingredients: ['浓缩咖啡 30 毫升', '椰奶 120 毫升', '冰块适量'],
    steps: ['杯中装入冰块并倒入椰奶。', '将浓缩咖啡缓缓倒在椰奶上。', '饮用前轻轻搅匀。'],
    preparationMinutes: 5,
    tips: '选择无糖椰奶可更清晰地品尝咖啡香。',
    containsAlcohol: false,
  },
  {
    id: 'drink-recipe-lime-mint-soda',
    name: '青柠薄荷气泡水',
    category: 'sparkling',
    ingredients: ['青柠 1/2 个', '薄荷叶 6 片', '苏打水 200 毫升', '冰块适量'],
    steps: ['青柠切片并挤出少量汁液。', '薄荷叶轻拍后与青柠放入杯中。', '加入冰块和苏打水，轻轻搅匀。'],
    preparationMinutes: 5,
    tips: '不要过度捣碎薄荷，以免带出苦味。',
    containsAlcohol: false,
  },
  {
    id: 'drink-recipe-grapefruit-soda',
    name: '葡萄柚苏打',
    category: 'sparkling',
    ingredients: ['葡萄柚汁 80 毫升', '苏打水 150 毫升', '蜂蜜 5 毫升（可选）', '冰块适量'],
    steps: ['葡萄柚榨汁并滤去果籽。', '杯中加入冰块和葡萄柚汁。', '倒入苏打水，按口味加蜂蜜并轻轻搅匀。'],
    preparationMinutes: 8,
    tips: '柑橘类水果可能影响部分药物的代谢；服药期间请先咨询专业人士。',
    containsAlcohol: false,
  },
  {
    id: 'drink-recipe-banana-milkshake',
    name: '香蕉奶昔',
    category: 'shake',
    ingredients: ['熟香蕉 1 根', '牛奶 180 毫升', '原味酸奶 50 克'],
    steps: ['香蕉去皮切段。', '香蕉、牛奶和酸奶放入搅拌机。', '搅打至细腻顺滑后倒入杯中。'],
    preparationMinutes: 5,
    tips: '乳糖不耐受者可用无乳糖奶或植物奶替代。',
    containsAlcohol: false,
  },
  {
    id: 'drink-recipe-mango-smoothie',
    name: '芒果酸奶冰沙',
    category: 'shake',
    ingredients: ['芒果果肉 150 克', '原味酸奶 120 克', '冰块 4 块'],
    steps: ['芒果去皮去核并切块。', '芒果、酸奶和冰块放入搅拌机。', '搅打至冰沙状后立即饮用。'],
    preparationMinutes: 8,
    tips: '可用冷冻芒果替代冰块，果味更浓。',
    containsAlcohol: false,
  },
  {
    id: 'drink-recipe-red-date-longan',
    name: '红枣桂圆茶',
    category: 'traditional',
    ingredients: ['红枣 4 颗', '桂圆肉 6 克', '清水 350 毫升'],
    steps: ['红枣洗净去核。', '红枣、桂圆肉和清水放入小锅。', '煮沸后转小火煮 12 分钟，稍凉后饮用。'],
    preparationMinutes: 18,
    tips: '红枣和桂圆含天然糖分，可不额外加糖。',
    containsAlcohol: false,
  },
  {
    id: 'drink-recipe-brown-sugar-ginger',
    name: '黑糖姜茶',
    category: 'traditional',
    ingredients: ['生姜 15 克', '黑糖 12 克', '清水 300 毫升'],
    steps: ['生姜洗净切薄片。', '姜片与清水煮沸后转小火煮 8 分钟。', '关火加入黑糖，搅拌融化后温热饮用。'],
    preparationMinutes: 15,
    tips: '糖量可按口味调整；这款饮品不能替代药物治疗。',
    containsAlcohol: false,
  },
  {
    id: 'drink-recipe-mojito',
    name: '莫吉托（含酒精）',
    category: 'cocktail',
    ingredients: ['白朗姆酒 30 毫升', '青柠 1/2 个', '薄荷叶 8 片', '苏打水 120 毫升', '糖 1 茶匙', '冰块适量'],
    steps: ['青柠切块，与糖和薄荷叶轻压出汁。', '加入冰块和白朗姆酒。', '倒入苏打水，轻轻搅匀并装饰薄荷叶。'],
    preparationMinutes: 8,
    tips: '含酒精饮品仅限成年人适量饮用；未成年人请勿饮酒，饮酒后请勿开车。',
    containsAlcohol: true,
  },
  {
    id: 'drink-recipe-virgin-mojito',
    name: '无酒精莫吉托',
    category: 'cocktail',
    ingredients: ['青柠 1/2 个', '薄荷叶 8 片', '苏打水 180 毫升', '糖 1 茶匙（可选）', '冰块适量'],
    steps: ['青柠切块，与薄荷叶和糖轻压出汁。', '杯中加入冰块。', '倒入苏打水并轻轻搅匀。'],
    preparationMinutes: 6,
    tips: '使用无酒精配方，不添加朗姆酒。',
    containsAlcohol: false,
  },
]

/** 饮品分类展示名。 */
export const drinkRecipeCategoryNames: Record<string, string> = {
  'fruit-tea': '果茶',
  'milk-tea': '奶茶',
  coffee: '咖啡',
  sparkling: '气泡饮 / 苏打',
  shake: '奶昔 / 冰沙',
  traditional: '传统热饮',
  cocktail: '调酒 / 无酒精特调',
}
