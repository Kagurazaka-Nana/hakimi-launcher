# 🐾 hakimi-launcher 喵！

> Yet another MC launcher, now powered by **JDK 25 + Kotlin Compose Multiplatform Material**!  
> 别看了喵，这是一个继续用 Java 后端打地基、再用 Kotlin Compose 搭漂亮桌面 UI 的极简 MC 启动器，喵~ 🐱✨

![Java](https://img.shields.io/badge/Java-25-orange.svg?style=flat-square)
![Kotlin](https://img.shields.io/badge/Kotlin-Compose%20Multiplatform-purple.svg?style=flat-square)
![Gradle](https://img.shields.io/badge/build-Gradle-blue.svg?style=flat-square)
![License](https://img.shields.io/badge/license-MIT-blue.svg?style=flat-square)
![Status](https://img.shields.io/badge/Cat-Powered-ff69b4.svg?style=flat-square)

---

## 🐾 这是个啥东西喵？

主人在写一种很新的 Minecraft 启动器！现在的路线是：

* **JDK 25 后端猫爪**：manifest、version JSON、rules、下载器和启动逻辑继续放在 `src/main/java` 里慢慢打磨。
* **Kotlin Compose UI 猫窝**：桌面界面放在 `src/main/kotlin`，使用 Compose Multiplatform Material。
* **前后端分离小鱼干**：当前 UI 只搭首页框架，通过 `LauncherBackend` 预留接口，暂时用 `StubLauncherBackend` 提供占位数据，不会真的下载文件或启动游戏。
* **文档子模块猫抓板**：`docs/` 是私有文档仓库 `Kagurazaka-Nana/hakimi-launcher-docs`，规则、参数、测试风格等设计说明都放在那里。

## 📁 本猫的肚子里的结构

```text
hakimi-launcher/
├── build.gradle.kts                         # Gradle + JDK 25 + Kotlin + Compose 配置
├── settings.gradle.kts                      # Gradle 项目名和仓库配置
├── gradlew / gradlew.bat                    # Gradle wrapper，构建入口喵
├── docs/                                    # 私有文档仓库子模块
├── src/main/java/com/minecraft/launcher/    # Java 后端核心
│   ├── Main.java                            # 旧 CLI/下载入口（走 FileDownloader）
│   ├── model/manifest/                      # Mojang version_manifest.json 模型
│   ├── model/version/                       # client version JSON 模型
│   ├── model/rule/                          # Mojang rules 评估器
│   ├── monitor/                             # OSHI 系统指标采样（CPU/内存/网络/磁盘）
│   └── util/                                # JSON / 平台 / Windows 版本工具
├── src/main/kotlin/com/minecraft/launcher/  # Kotlin 前端 + 后端契约
│   ├── backend/                             # LauncherBackend + StubLauncherBackend
│   ├── download/                            # 猫爪下载器 🐾：FileDownloader 接口 + BitDownloader 分片引擎
│   └── ui/                                  # Compose Multiplatform Material 界面
└── temp/                                    # 偷偷藏 Manifest 缓存的小窝
```

## 🐾 怎么把本猫抓到本地？

### 准备小零食（前置要求）

* **JDK 25**
  * 本地推荐路径：`C:\Users\ColaPig\.jdks\graalvm-jdk-25`
* **Git**
* **Gradle wrapper**
  * 仓库自带 `gradlew` / `gradlew.bat`，一般不用另外装 Gradle。

如果当前终端不是 JDK 25，可以在 Git Bash 里临时喂它这两口猫粮：

```bash
export JAVA_HOME="C:\\Users\\ColaPig\\.jdks\\graalvm-jdk-25"
export PATH="$JAVA_HOME/bin:$PATH"
```

如果访问 GitHub 需要本地代理，可以这样设置：

```bash
git config http.proxy http://127.0.0.1:10808
git config https.proxy http://127.0.0.1:10808
```

### 克隆时把 docs 子模块也带上喵

推荐：

```bash
git clone --recurse-submodules git@github.com:Kagurazaka-Nana/hakimi-launcher.git
cd hakimi-launcher
```

如果已经克隆过主仓库，再补 docs 子模块：

```bash
git submodule update --init --recursive
```

> 🐱 **喵提示**：GitHub 上显示 `docs @ 323bc21` 是正常的。`docs/` 是 Git submodule，主仓库只记录它指向文档仓库的哪个 commit，而不是把 docs 的全部文件直接摊开显示。

## 🐾 怎么启动 UI？

在 Windows / Git Bash 中运行：

```bash
./gradlew.bat run
```

当前首页是前端框架预览，包含：

* 账号卡片
* 版本选择卡片
* 启动操作卡片
* 状态卡片
* 后续接入计划卡片

这些按钮目前只调用 `StubLauncherBackend`，不会访问网络、不会写入 `.minecraft`、也不会真的拉起 Minecraft，喵。

## 🐾 怎么测试和构建？

跑全部测试：

```bash
./gradlew.bat test
```

跑完整构建：

```bash
./gradlew.bat build
```

只跑某个猫爪测试：

```bash
./gradlew.bat test --tests com.minecraft.launcher.model.rule.RuleEvaluatorTest
./gradlew.bat test --tests com.minecraft.launcher.model.version.arguments.game.GameDeserializerTest
./gradlew.bat test --tests com.minecraft.launcher.model.version.arguments.jvm.JvmDeserializerTest
```

JaCoCo XML 会吐在这里：

```text
build/reports/jacoco/test/jacocoTestReport.xml
```

## 🐾 JNA native access 小纸条

后端 `OSVersionUtil` 在 Windows 上会通过 JNA 调用 `ntdll!RtlGetVersion` 获取精确系统版本。真实后端启动链路接入后，运行时需要：

```text
--enable-native-access=ALL-UNNAMED
```

当前 Compose 首页阶段还不会触发真实下载/启动逻辑，但这个提示先贴在猫窝门口，防止以后踩尾巴。

## 🐾 开发流程喵

* 开发基线分支：`dev`
* Kotlin UI 工作分支：`dev-kotlin`
* 修改 rules、arguments、启动流程前，请先读 `docs/` 里的对应文档。
* PR 前至少跑：

```bash
./gradlew.bat test
./gradlew.bat build
```

CI 会跑 Gradle 测试并生成 JaCoCo 报告。测试和 CI 都绿了，再把 PR 递给主人，喵。

---

*Made with 💕, JDK 25, Kotlin Compose, and lots of meows.*
