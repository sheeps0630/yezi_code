# 椰吃家（小程序 Demo）

当前发布版本：**v0.2.0**（2026-10-27）。解决「不知道吃什么 / 喝什么」的全栈演示项目：三大核心流程 + 个人中心，基于「椰吃家」需求文档 v0.1 开发。

版本变更记录见 [CHANGELOG.md](./CHANGELOG.md)。

## 技术栈

- **后端**：Java 17 + Spring Boot 3.2.5（Maven），仅依赖 spring-boot-starter-web
- **前端**：Vue 3 + TypeScript（严格模式）+ Vite + vue-router
- **数据**：演示环境为内存存储（ConcurrentHashMap）+ 类路径静态 JSON，无数据库依赖

## 代码规范（已全量校验通过）

| 规范 | 落实方式 | 校验结果 |
| --- | --- | --- |
| 所有代码注释使用中文 | Java Javadoc / 行内注释、TS 与 Vue 注释全部中文 | 全部中文 |
| 使用 TypeScript 而非 JavaScript | 前端 0 个 .js 文件，tsconfig 开启 strict | 0 个 .js 文件 |
| 变量命名使用驼峰式（camelCase） | Java / TS / Vue 均按 camelCase 命名 | 全量通过 |
| 每个函数都要有简洁的注释说明 | 脚本扫描 Java 方法与 TS 函数 | 缺注释数 = 0 |

## 目录结构

```
yechijia/
├── backend/                        # Spring Boot 后端
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/yechijia/      # 31 个 Java 文件
│       │   ├── config/             # 跨域配置
│       │   ├── common/             # 统一响应、业务异常、全局异常处理
│       │   ├── model/              # 领域模型（Java record）
│       │   ├── dto/                # 请求/响应对象
│       │   ├── repository/         # 静态数据加载、内存用户存储
│       │   ├── service/            # 业务逻辑（含 MapClient 地图接口）
│       │   └── controller/         # REST 接口
│       └── resources/
│           ├── application.yml     # 端口 18080
│           └── data/               # 菜品/菜谱/门店/饮品静态数据
└── frontend/                       # Vue 3 + TypeScript 前端
    ├── package.json / vite.config.ts / tsconfig.json
    └── src/
        ├── api/                    # 接口封装与类型定义
        ├── components/             # 出去吃 / 在家做饭面板
        ├── views/                  # 吃什么 / 喝什么 / 我的
        └── router/                 # 三 Tab 路由
```

## 本地运行

### 后端（端口 18080）

```bash
cd backend
mvn package
java -jar target/yechijia-backend-0.2.0.jar
```

### 前端（端口 5173，已配置 /api 代理到 18080）

```bash
cd frontend
npm install
npm run dev
```

浏览器访问 http://localhost:5173 即可。

## 接口一览（统一前缀 /api，响应格式 {code, message, data}）

- 出去吃：`GET/POST/PUT/DELETE /eat-out/categories`（自定义分类与权重）、`GET /eat-out/types`（附近门店实际支持的类型）、`POST /eat-out/recommend`（推荐 / 换一个，支持排除项与心动类型优先）
- 在家做：`GET /cook/categories`、`GET /cook/ingredient-types`、`GET /cook/dishes?category=`、`POST /cook/menu`（按菜类与食材偏好生成菜单）、`GET /cook/recipes/{dishId}`（菜谱详情）
- 喝什么：`GET /drink/categories`、`POST /drink/recommend`（附近 / 随机，支持排除）
- 我的：`GET/PUT /user/profile`、`GET/POST/DELETE /user/favorites`、`PUT /user/favorites/{id}/pin`、`GET/POST/DELETE /user/family-recipes`、`GET/POST/PUT/DELETE /user/family-members`

## 需求覆盖情况

- **出去吃**：自定义分类权重（累加权重区间随机）、地图附近模式、推荐结果排除（不想吃）、「换一个」随机重抽
- **在家做**：素菜/荤菜/甜品必选多选 + 可选主菜（主菜必须属于所选类别）、菜单生成；菜谱为 15 道真实经典菜谱静态数据，查不到时明确报错——**绝不胡乱生成菜谱**；菜谱底部「加入收藏 / 加入家庭菜谱」按钮
- **喝什么**：先问附近有无门店（3km），有则按类别匹配品牌门店，无则纯随机；推荐「产品 + 商家」组合
- **我的**：昵称 / 头像 / 手机号、收藏、家庭菜谱、家庭成员

## 演示边界（与真实产品的差距）

- **地图数据为演示桩**：`MapClient` 接口 + `StubMapClient` 示例实现，接真实高德/腾讯地图只需替换实现类，业务代码无需改动；页面上已标注「演示环境为示例门店数据」
- **数据存储为内存**：重启后用户数据重置；接入数据库只需替换 `InMemoryUserStore`
- **无登录鉴权**：演示使用固定用户 demo-user
- **菜品 / 门店 / 饮品为样例数据**：门店距离、评分均为示例值

## 已验证 / 未验证

- 已验证：后端 Maven 打包通过；8 个接口冒烟测试通过（加权推荐、排除逻辑、附近饮品匹配、菜单生成、收藏/家庭菜谱写入）；前端 `vue-tsc --noEmit` 严格类型检查与生产构建通过；三个页面在 Chromium 无头浏览器中截图核对，后端数据正常渲染
- 未验证：真实地图 API、数据库持久化、多用户并发、移动端真机兼容性（见上节演示边界）


## 修复记录（v2 · 按代码质检报告清单）

依据全栈代码质检报告（2026-10-22）完成的修复与调优：

1. **[P2] 出去吃类目抽签无回退**：抽签前先剔除「排除已看门店后无可用门店」的类目，抽签池里每个类目都有门店可选；全部耗尽时给出可读提示。新建类目表单补充说明——无匹配门店的类目会被自动跳过。
2. **[P3] nearby 模式未传经纬度**：前端周边模式统一传演示坐标（39.9, 116.4，与喝什么页口径一致，注释标明生产改接定位）；后端对 nearby 缺坐标直接抛业务异常，不再静默以 (0,0) 搜索。
3. **[P3] 弱类型入参 + 异常兜底吞错误**：收藏 / 家庭菜谱 / 家庭成员三个写接口改用类型化 DTO（`FavoriteRequest` / `FamilyRecipeRequest` / `FamilyMemberRequest`）；全局异常处理新增请求体不可读、参数类型不匹配、缺少必填参数三类客户端错误的 400 分支，返回可读提示。
4. **[P3] 收藏 / 家庭菜谱并发竞态**：`InMemoryUserStore` 改为「targetType:targetId 复合键 + putIfAbsent」与「dishId + putIfAbsent」原子幂等写入，删除时同步清理幂等索引；并发的重复收藏 / 重复加入只落一条。
5. **[P3] 家庭菜谱「添加人」硬编码**：前端不再硬编码「椰子」，不传添加人，由后端取当前昵称（改名后新记录自动同步）。
6. **候选风险**：「喝什么」不限种类时附近搜索关键词留空（不再只搜「饮品」类门店）；出去吃 / 喝什么两页新增「清空本轮已看」入口，门店 / 饮品看光后无需刷新页面。
7. **生产化调优**：手机号格式校验（支持脱敏格式与 11 位大陆手机号，非法值拦截）；CORS 来源改为配置项 `yechijia.cors.allowed-origins`（默认 `*`，生产收敛为白名单）。

**修复后回归验证**：后端 Maven 重新打包通过；14 项回归断言全部通过（含质检报告给出的两条 P2 回归断言：无门店高权重类目连续推荐 30 次 0 失败、排除某类目全部门店后结果不再返回该类目）；前端 vue-tsc 严格类型检查与生产构建通过；三个页面无头浏览器截图核对渲染正常。

## 界面与交互更新（2026-10-27）

- 全局改为椰乳白、椰叶绿与椰壳棕配色，并优化移动端布局。
- 「椰子美食转盘」允许自定义吃什么、设置权重并另存多个方案；方案保存在当前浏览器，刷新后仍可使用。转盘抽选内容不依赖演示门店，周边推荐仍使用门店示例数据。
- 周边美食支持「心动类型」（优先推荐）与「讨厌类型」（排除），心动类型没有可用门店时会回退到其他未排除类型。
- 在家做饭的主菜候选按已选菜类分组展示具体菜名；收藏夹中的菜品与家庭菜谱可进入菜谱详情页。
- 头像经压缩后仅保存在当前浏览器。家庭成员手机号只记录并校验联系方式，不做短信验证或账号关联；应用仍是固定演示用户、内存数据，后端重启会重置相关资料。
- 吃什么与喝什么的收藏列表均支持多条置顶、按最近置顶/收藏时间排序和删除二次确认；“已看”展示当前场景本轮结果，离开场景或刷新后重新统计。
- 做饭选择素菜、肉菜、甜品、海鲜、汤羹；食材标签可搜索、多选与折叠。每类食材标签均有固定菜谱覆盖，标签在同类内按“或”筛选。喝什么分为“外出觅饮 / 在家自制”，后者包含 14 个一人份配方（含酒精与无酒精特调分开标注）。

## 发布说明

- 发布包命名：`releases/yechijia-v0.2.0.zip`
- 发布包包含项目源码、前端生产构建文件和可运行的后端 JAR；不包含 `node_modules`、Maven 中间文件或本机配置。
- 前后端版本号分别记录于 `frontend/package.json` 和 `backend/pom.xml`，均与本发布版本一致。
- Git 发布标签：`v0.2.0`；版本源码由 Git commit 保存，发布包可从下方 ZIP 文件直接分发。
