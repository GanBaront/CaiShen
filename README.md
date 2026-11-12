# 材神食验室 · CaiShen Food Lab

一个「**拍照/输入食材 → AI 菜谱推荐 → 缺料一键下单**」的家常菜实验平台。

用户输入家里现有的食材（或拍一张菜品照片），系统结合口味、饮食方式、忌口与人数推荐菜谱，
标出缺少的食材，并可直接从合作商家下单补齐；站内没有合适菜谱时，大模型会按现有食材现场创作。

---

## 技术栈

| 层 | 技术 |
|---|---|
| 用户端 / 管理端 | Vue 3 + TypeScript + Vite + Pinia + Vue Router |
| 后端 | Spring Boot 3 + MyBatis-Plus + MySQL 8（JDK 21） |
| AI 文本 | DeepSeek `deepseek-chat`（OpenAI 兼容 `/chat/completions`） |
| AI 视觉 | 智谱 `glm-4v-flash`（菜品 / 食材图片识别，免费） |

文本与视觉是两套独立端点（DeepSeek 不支持视觉），可分别配置 `base-url` / `model` / `api-key`。

## 目录

```
CaiShen/
├── backend/          Spring Boot 后端（源码 + 可直接运行的 target/foodlab-1.0.0.jar）
├── front/            用户端 Vue 3
├── admin-web/        管理端 Vue 3
├── sql/              建库建表主脚本、菜谱/商品种子、老库升级脚本
├── design/           界面设计稿
├── start-all.bat     一键启动（自动注入本机 AI 密钥）
├── ai-keys.example.bat  密钥模板（空值，可提交）
├── 部署说明.md       完整部署 / 配置 / 排错文档
└── 设计方案.md       产品与技术方案
```

## 快速开始

```bash
# 1. 建库（MySQL 8，字符集 utf8mb4）
mysql -uroot -p < sql/一键建库建表sql.txt

# 2. 配置 AI 密钥：复制模板后填入自己的 Key
copy ai-keys.example.bat ai-keys.bat

# 3. 一键启动：后端 8080 / 用户端 5173 / 管理端 5174
start-all.bat
```

演示账号：用户 `demo / 123456`，管理员 `admin / admin123`。
接口文档：http://localhost:8080/doc.html

完整步骤、老库升级、AI 能力说明与常见问题见 **[部署说明.md](部署说明.md)**。

## AI 密钥与安全

**密钥只从环境变量注入，优先级 `环境变量 > 后台系统配置`，不写进代码、不落库。**
本机由 `start-all.bat` 读取 `ai-keys.bat` 注入；IDEA / CI 请自行配置环境变量。

> ⚠️ `ai-keys.bat` 含真实密钥，已被 `.gitignore` 排除，**请勿提交、请勿打包转发**。
> 仓库中只保留空值模板 `ai-keys.example.bat`。

不配置 Key 也能运行：菜谱推荐退化为按匹配度排序，图片识别会提示未启用，其余功能不受影响。

`application.yml` 中的数据库密码与 JWT secret 仅为本地开发默认值，支持
`DB_PASSWORD` / `JWT_SECRET` 等环境变量覆盖；部署到公网环境时务必改为强凭据。

## 主要功能

- 食材篮 / 忌口 / 口味偏好与每周免费次数、会员计费
- AI 菜谱推荐排序与推荐理由、别名归一化（番茄 ↔ 西红柿）
- 拍照识别菜品与食材、识别结果匹配站内做法
- 站内零匹配时 AI 现场创作菜谱（落库为「待审核」）
- 菜谱详情、食材替代、购物清单、缺料一键下单、订单与评价
- 管理端内容审核、系统配置与「测试 AI 连接」
