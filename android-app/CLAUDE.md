# CLAUDE.md — android-app (ClockApp)

## 项目概述

一个展示自适应布局的 Android 时钟应用，基于 Jetpack Compose + Material3 + MVVM 架构。

## 技术栈

| 组件 | 版本 |
|------|------|
| Gradle Wrapper | 8.9 |
| Android Gradle Plugin | 8.7.3 |
| Kotlin | 1.9.25 |
| Compose BOM | 2024.10.01 |
| compileSdk / targetSdk | 35 |
| minSdk | 29 (Android 10) |
| Java（构建用） | 17 (temurin-17) |

## 构建命令

```bash
# Debug APK
./gradlew assembleDebug

# 单元测试
./gradlew test

# 安装到已连接设备/模拟器
./gradlew installDebug

# 清理
./gradlew clean
```

## 项目结构

```
app/src/main/kotlin/com/michaelzhou/clockapp/
├── MainActivity.kt          # 入口，注入 WindowSizeClass
└── ui/
    ├── ClockScreen.kt       # Composable UI，响应式布局
    ├── ClockViewModel.kt    # 时钟状态管理
    └── theme/
        └── Theme.kt         # Material3 主题
```

## 本地环境配置

`local.properties`（不提交）需包含：

```properties
sdk.dir=/Users/<you>/Library/Android/sdk
```

`gradle.properties`（已提交）包含：

```properties
android.useAndroidX=true
org.gradle.java.home=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home
```

系统默认 Java 为 25，Kotlin 1.9.x 编译器不支持，`org.gradle.java.home` 必须在 `gradle.properties` 中显式指定 Java 17。其他开发者若 temurin-17 路径不同需本地覆盖。

## 需求

| REQ ID | 描述 | 状态 |
|--------|------|------|
| REQ-AND-001 | android-app 项目初始化：CLAUDE.md + 需求登记 + CI 接入 | ✅ Verified |
