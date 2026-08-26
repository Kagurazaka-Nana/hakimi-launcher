# 🐾 hakimi-launcher 喵！

> Yet another MC launcher, but written with raw JDK 21 and zero useless dependencies!  
> 别看了喵，就是一个用纯血 Java 21 砸出来的极简 MC 启动器，喵~ 🐱✨

![Java](https://img.shields.io/badge/Java-21-orange.svg?style=flat-square)
![License](https://img.shields.io/badge/license-MIT-blue.svg?style=flat-square)
![Status](https://img.shields.io/badge/Cat-Powered-ff69b4.svg?style=flat-square)

---

## 🐾 这是个啥东西喵？

主人在写一种很新的 Minecraft 启动器！因为讨厌那些笨重又臃肿的垃圾框架，所以决定直接用原汁原味的 **Java 21** 手搓一个！

* **纯血 Java 21 驱动**：不加任何奇奇怪怪的臃肿依赖，跑得比被踩到尾巴的猫还快！
* **零臃肿硬核架构**：没有花里胡哨的广告和冗余代码，只做最纯粹的启动和下载，喵！
* **超能力网络引擎**：内置异步下载器，抓取 Version Manifest 和补齐资源文件都是瞬间搞定！

## 📁 本猫的肚子里的结构

```text
launcher/
├── src/main/java/com/minecraft/launcher/
│   ├── HttpDownloader.java       # 抓取资源的猫爪下载器 🐾
│   ├── Main.java                 # 启动器入口，点这里启动！
│   └── versionmanifest/          # 各种版本的 JSON 偷吃与解析
│       ├── VersionInfo.java
│       ├── VersionManifest.java
│       └── VersionType.java
└── temp/                         # 偷偷藏 Manifest 缓存的小窝
```
## 🐾 怎么把本猫跑起来？

### 准备小零食（前置要求）

* **JDK 21** 或更高的神圣猫粮
* **Maven 3.8+**

### 开始抓挠（构建与运行）

1. 把本猫抓到本地：
   git clone git@github.com:Kagurazaka-Nana/hakimi-launcher.git
   cd hakimi-launcher

2. 揉搓并编译：
   mvn clean package

3. 拍拍屁股启动！
   java --enable-native-access=ALL-UNNAMED -jar target/launcher-1.0-SNAPSHOT.jar

> 🐱 **喵提示**：如果用的是 JDK 22+，启动时必须带上 `--enable-native-access=ALL-UNNAMED`。
> 否则 JNA 加载 `ntdll.dll` 获取精确系统版本时会打印 restricted method 警告，未来版本甚至会被直接禁止调用喵～

---

*Made with 💕, raw JDK 21, and lots of meows.*