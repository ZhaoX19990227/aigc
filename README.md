# AIGC 自动化内容工厂

这是一个可直接运行的一期项目，目标是把“热点抓取 -> 选题 -> 脚本 -> 资产 -> 审核 -> 发布”做成可演示、可扩展的业务闭环。

当前实现说明：

1. 后端使用 `Spring Boot 3.4.x + LangChain4j + MyBatis-Plus + H2`
2. 前端管理台直接集成在 Spring Boot 静态资源中
3. 热点抓取和发布能力默认使用本地模拟适配器
4. 素材链路已接入真实本地执行：封面图生成、`say` 语音合成、SRT 字幕生成、`ffmpeg` 视频合成
5. `LangChain4j` 已接入真实 `OpenAiChatModel` 初始化入口，配置 `OPENAI_API_KEY` 并开启 `app.ai.enabled=true` 后可继续扩展成真实调用

## 目录结构

```text
aigc-content-factory/
├── aigc-content-factory-server/   # Spring Boot 后端 + 内嵌前端
├── aigc-content-factory-web/      # 预留前端独立化目录
└── docs/
    └── requirements.md            # 需求与技术实现文档
```

## 快速启动

```bash
cd /Users/zhaox/IdeaProjects/aigc-content-factory/aigc-content-factory-server
./mvnw spring-boot:run
```

如果 `8080` 已被占用：

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=18080
```

## 本地安全配置

支持两种本地配置方式：

1. `.env`
2. `application-local.yml`

推荐步骤：

```bash
cd /Users/zhaox/IdeaProjects/aigc-content-factory/aigc-content-factory-server
cp .env.example .env
cp application-local.yml.example application-local.yml
```

说明：

1. `.env` 已通过 `spring-dotenv` 自动加载
2. `application-local.yml` 已通过 `spring.config.import` 自动导入
3. 这两个文件都已加入 `.gitignore`

启动后可访问：

1. 管理台：[http://localhost:8080](http://localhost:8080)
2. H2 控制台：[http://localhost:8080/h2-console](http://localhost:8080/h2-console)
3. 健康检查：[http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)

启用真实 OpenAI 调用：

```bash
OPENAI_API_KEY=your_key ./mvnw spring-boot:run -Dspring-boot.run.arguments="--app.ai.enabled=true"
```

启用千问兼容 OpenAI 网关：

```bash
APP_AI_ENABLED=true \
APP_AI_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1 \
APP_AI_MODEL=qwen-plus \
APP_AI_API_KEY=your_dashscope_key \
./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=18081"
```

H2 连接信息：

```text
jdbc:h2:file:./data/content_factory
username: sa
password:
```

## 当前一期范围

已经实现：

1. 热点采集接口与落库
2. 基于 AI 门面的选题与脚本生成
3. 任务状态流转
4. 本地真实素材生成与文件服务
5. 审核、驳回、发布流程
6. B站扫码登录后自动填充标题/简介/视频
7. 管理后台操作界面

暂未实现真实生产级外部能力：

1. 平台官方 API 对接
2. 真实大模型结构化输出
3. Stable Diffusion / ComfyUI 图片生成
4. 抖音/小红书扫码后自动填充发布

这些能力在当前代码中都已保留为可替换适配层，不是写死在主流程里的 demo 逻辑。

## 已接入的本地免费能力

1. `say`：生成中文语音
2. `ffmpeg`：将封面图和语音合成为 MP4
3. Java2D：生成封面图
4. SRT：生成字幕文件并对外提供访问
5. 平台手动发布辅助：自动打开上传页，并回写上传说明
6. Playwright + Chromium：B站扫码后自动填充视频、标题和简介

生成文件目录：

`/Users/zhaox/IdeaProjects/aigc-content-factory/aigc-content-factory-server/runtime/generated`

## 需要凭证或前置条件的能力

1. OpenAI 或兼容模型网关：需要 `OPENAI_API_KEY`
2. 小红书、抖音、B站真实发布：需要平台账号、Cookie 或开放平台凭证
3. 本地大模型：可选安装 `Ollama`，再把 `app.ai.base-url` 指向本地兼容网关

## 人工扫码发布

当前已接入：

1. B站上传页辅助增强版
2. 抖音上传页辅助
3. 小红书上传页辅助

执行发布后系统会：

1. B站：启动独立 Chromium 辅助进程，未登录则扫码，登录后自动填视频、标题、简介，最后由你手动点击发布
2. 抖音/小红书：打开对应上传页并写入操作说明
3. 在发布记录中写入视频路径、标题建议、描述建议、浏览器配置目录和日志路径
4. 将任务状态标记为 `PUBLISHING / PARTIAL_SUCCESS`，表示等待你完成最后提交

## B站辅助发布依赖

项目内置的 B站辅助发布依赖本机 Python Playwright：

```bash
python3 -m pip install --user playwright
python3 -m playwright install chromium
```

辅助脚本位置：

`/Users/zhaox/IdeaProjects/aigc-content-factory/aigc-content-factory-server/scripts/bilibili_publish_helper.py`
