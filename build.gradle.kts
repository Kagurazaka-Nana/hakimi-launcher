import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.4.20"
    kotlin("plugin.serialization") version "2.4.20"
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.20"
    id("org.jetbrains.compose") version "1.12.0"
    jacoco
    id("org.sonarqube") version "7.5.0.8588"
    // Roborazzi：Compose Desktop 截图测试（让测试产出可见 PNG）
    id("io.github.takahirom.roborazzi") version "1.74.0"
}

group = "com.minecraft"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

kotlin {
    jvmToolchain(25)
}

sourceSets {
    main {
        java.srcDir("src/main/java")
        kotlin.srcDir("src/main/kotlin")
    }
    test {
        java.srcDir("src/test/java")
        kotlin.srcDir("src/test/kotlin")
    }
}

dependencies {
    implementation(compose.desktop.currentOs)
    // 图标资源（ImageVector 素材，非 Material 视觉层）
    implementation(compose.materialIconsExtended)

    // Compose Unstyled：无样式行为原语（按钮/焦点/键盘/弹窗），视觉由自建设计系统负责
    implementation("com.composables:composeunstyled:2.10.0")
    // Coil 3：加载 mod/资源包 图标等网络图片
    implementation("io.coil-kt.coil3:coil-compose:3.6.3")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.6.3")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.22.2")
    implementation("net.java.dev.jna:jna:5.19.1")
    // OSHI：系统状态栏的真实指标采集（CPU / 内存 / 网卡 / 磁盘 IO 计数器）
    implementation("com.github.oshi:oshi-core:6.6.6")
    implementation("org.slf4j:slf4j-api:2.0.18")
    implementation("ch.qos.logback:logback-classic:1.6.3")

    compileOnly("org.projectlombok:lombok:1.18.46")
    annotationProcessor("org.projectlombok:lombok:1.18.46")

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")

    // Roborazzi 桌面截图测试（JUnit4 承载，经 vintage 引擎与现有 JUnit5 并存）
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose-desktop:1.74.0")
    testImplementation("org.jetbrains.compose.ui:ui-test:1.12.0")
    testImplementation("org.jetbrains.compose.ui:ui-test-junit4:1.12.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation(kotlin("test"))
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

compose.desktop {
    application {
        mainClass = "com.minecraft.launcher.ui.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "hakimi-launcher"
            packageVersion = "1.0.0"
        }
    }
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

// SonarCloud 参数通过命令行系统属性注入（见 .github/workflows/ci.yml），
// 避免 Gradle 9 下 sonarqube 插件 Kotlin DSL 的 `properties {}` 块解析问题。
