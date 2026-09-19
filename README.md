# 🐾 hakimi-launcher 喵！

> 极简 MC 启动器：后端仍用纯 Java，前端用 Kotlin Compose Multiplatform Material 搭桌面 UI。喵~ 🐱✨

![Java](https://img.shields.io/badge/Java-25-orange.svg?style=flat-square)
![License](https://img.shields.io/badge/license-MIT-blue.svg?style=flat-square)
![Status](https://img.shields.io/badge/Cat-Powered-ff69b4.svg?style=flat-square)

---

## 🐾 这是个啥东西喵？

主人在写一种很新的 Minecraft 启动器！后端用 **JDK 25 纯 Java**，前端用 **Kotlin Compose Multiplatform Material** 搭桌面 UI。目前 UI 处于前端框架搭建阶段，通过 `LauncherBackend` 接口预留后端接入点，尚未接线真实下载/启动逻辑。

## 📁 本猫的肚子里的结构

```text
launcher/
├── build.gradle.kts            # Gradle + Kotlin + Compose Multiplatform
├── settings.gradle.kts
├── src/main/java/              # Java 后端：manifest / version / rule 模型与下载
│   └── com/minecraft/launcher/
│       ├── HttpDownloader.java
│       ├── Main.java
│       └── model/...
├── src/main/kotlin/            # Kotlin Compose UI 前端
│   └── com/minecraft/launcher/
│       ├── backend/            # LauncherBackend 接口 + StubLauncherBackend 占位
│       └── ui/                 # Compose Multiplatform Material UI 框架
└── temp/                       # 偷偷藏 Manifest 缓存的小窝
```

## 🐾 怎么把本猫跑起来？

### 准备小零食（前置要求）

* **JDK 25**（本地示例：`C:\Users\ColaPig\.jdks\graalvm-jdk-25`）
* **Gradle**（使用本仓库的 Gradle wrapper）

### 开始抓挠（构建与运行）

1. 把本猫抓到本地：
   ```bash
   git clone git@github.com:Kagurazaka-Nana/hakimi-launcher.git
   cd hakimi-launcher
   ```

2. 揉搓并编译：
   ```bash
   ./gradlew.bat build
   ```

3. 跑测试：
   ```bash
   ./gradlew.bat test
   ```

4. 启动 Compose UI（当前为前端框架，展示占位界面）：
   ```bash
   ./gradlew.bat run
   ```

> 🐱 **喵提示**：后端 JNA 在 Windows 上加载 `ntdll.dll` 获取精确系统版本时需要 native access。打包后的启动器以 `--enable-native-access=ALL-UNNAMED` 启动。

---

*Made with 💕, JDK 25 + Kotlin Compose, and lots of meows.*