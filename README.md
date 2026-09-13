# AI Quota / 额度看板

An Android home-screen widget that displays AI service usage quotas. Supports multiple AI providers with different quota structures.

Android桌面小组件，显示各种AI服务的用量配额。支持多种AI服务商，适配不同的配额结构。

## Features / 功能特点

- **Home-screen widget** showing quota usage at a glance
- **Multi-provider architecture** — designed for multiple AI services
- **Secure token storage** using Android Keystore / EncryptedSharedPreferences
- **Periodic auto-refresh** via WorkManager (15–120 min configurable)
- **Demo mode** for testing without authentication
- **Dark theme** optimized for AMOLED displays

---

- **桌面小组件**：一目了然查看配额用量
- **多服务商架构**：为多种AI服务设计
- **安全存储**：使用Android Keystore加密存储令牌
- **自动刷新**：通过WorkManager定时刷新（15–120分钟可配置）
- **演示模式**：无需认证即可测试
- **深色主题**：针对AMOLED屏幕优化

## Supported Providers / 支持的服务商

### Cursor (implemented / 已实现)

Cursor has **two monthly quota pools** plus optional on-demand spending:

| Quota Type | Description | Unit | Reset |
|------------|-------------|------|-------|
| Cursor Models | First-party models (Composer, Cursor Grok, etc.) | Requests | Monthly (billing cycle) |
| Other Models | Third-party models (Pro ~$20, Pro+ ~$70, Ultra ~$400) | Dollars | Monthly (billing cycle) |
| On-demand | Pay-as-you-go beyond included usage (optional) | Dollars | N/A |

**Important**: Cursor does **NOT** have weekly quotas or rolling 5-hour windows. The reset is tied to your subscription billing anniversary, not the calendar month start.

---

Cursor有**两个月度配额池**，外加可选的按需消费：

| 配额类型 | 说明 | 单位 | 重置 |
|---------|------|------|------|
| Cursor Models | 第一方模型（Composer、Cursor Grok等） | 请求数 | 每月（账单周期） |
| Other Models | 第三方模型（Pro约$20、Pro+约$70、Ultra约$400） | 美元 | 每月（账单周期） |
| On-demand | 超出额度后的按需付费（可选） | 美元 | 不重置 |

**重要**：Cursor **没有**每周配额或滚动5小时窗口。重置日期与您的订阅计费周年日绑定，而非日历月初。

### Future Providers / 未来支持

The architecture supports providers with different quota windows:

| Provider | Monthly | Weekly | Rolling Hours | Notes |
|----------|---------|--------|---------------|-------|
| Cursor | ✓ | ✗ | ✗ | Two pools + on-demand |
| Kimi | ✓ | ✓ | ✓ (5h) | Three-tier quota system |
| OpenAI | ✓ | ✗ | ✗ | Dollar/token based |
| Claude | ✓ | ✗ | ✗ | Token based |

---

架构支持具有不同配额窗口的服务商：

| 服务商 | 月度 | 每周 | 滚动小时 | 备注 |
|--------|------|------|----------|------|
| Cursor | ✓ | ✗ | ✗ | 双池+按需 |
| Kimi | ✓ | ✓ | ✓ (5小时) | 三层配额系统 |
| OpenAI | ✓ | ✗ | ✗ | 美元/Token |
| Claude | ✓ | ✗ | ✗ | Token |

## Architecture / 架构

```
com.jameszp98.aiquota/
├── domain/                 # Vendor-agnostic domain models
│   ├── Models.kt          # ProviderId, QuotaWindow, QuotaBucket, ProviderUsage
│   └── QuotaProvider.kt   # Provider interface
├── data/
│   ├── provider/
│   │   └── cursor/        # Cursor-specific implementation
│   │       ├── CursorApi.kt
│   │       ├── CursorApiModels.kt
│   │       ├── CursorResponseParser.kt
│   │       └── CursorQuotaProvider.kt
│   ├── storage/
│   │   ├── TokenStorage.kt    # Encrypted token storage
│   │   └── SettingsStore.kt   # App settings (DataStore)
│   └── ProviderRegistry.kt    # Provider management
├── ui/
│   ├── widget/            # Glance-based home screen widget
│   ├── settings/          # Jetpack Compose settings screen
│   └── theme/
└── worker/
    └── UsageRefreshWorker.kt  # WorkManager periodic refresh
```

### Domain Models / 领域模型

```kotlin
// Quota time windows (different providers use different windows)
enum class QuotaWindow {
    MONTHLY,        // Cursor, OpenAI, Claude
    WEEKLY,         // Kimi
    ROLLING_HOURS,  // Kimi (5-hour rolling window)
    DAILY,          // Some providers
    NONE            // On-demand / no reset
}

// A single quota bucket
data class QuotaBucket(
    val name: String,           // e.g., "Cursor Models", "Other Models"
    val used: Double,           // Current usage
    val limit: Double?,         // Limit (null if unlimited)
    val unit: QuotaUnit,        // REQUESTS, DOLLARS, TOKENS, CREDITS
    val window: QuotaWindow,    // Reset window
    val resetsAt: Instant?,     // Next reset time
    val isUnlimited: Boolean,
    val rollingWindowHours: Int? // For ROLLING_HOURS window
)
```

## Setup / 配置

### Getting Your Cursor Token / 获取Cursor令牌

1. Open [cursor.com](https://cursor.com) in your browser
2. Log in to your account
3. Open DevTools (F12) → Application → Cookies
4. Copy the value of `WorkosCursorSessionToken`

Or for Bearer token, check the Network tab for API requests to `api2.cursor.sh`.

---

1. 在浏览器中打开 [cursor.com](https://cursor.com)
2. 登录您的账户
3. 打开开发者工具（F12）→ Application → Cookies
4. 复制 `WorkosCursorSessionToken` 的值

或者查看Network标签页中对 `api2.cursor.sh` 的API请求以获取Bearer令牌。

## Building / 构建

### Requirements / 要求

- JDK 17
- Android Gradle Plugin 8.5.2
- Gradle 8.7
- Android SDK 34

### Build Commands / 构建命令

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run all checks
./gradlew check
```

## Disclaimer / 免责声明

⚠️ **UNOFFICIAL API**

This app uses community-documented APIs that are **not officially supported** by Cursor or any other provider. These APIs may change or break without notice. Use at your own risk.

The app stores authentication tokens locally using Android's EncryptedSharedPreferences. Tokens are never logged or transmitted except to the official API endpoints.

---

⚠️ **非官方API**

本应用使用社区记录的API，这些API**未获得**Cursor或其他服务商的官方支持。这些API可能随时变更或失效，请自行承担风险。

应用使用Android的EncryptedSharedPreferences在本地存储认证令牌。令牌不会被记录或传输到官方API端点以外的地方。

## License / 许可证

MIT License

## Contributing / 贡献

Contributions are welcome! To add a new provider:

1. Create a new package under `data/provider/`
2. Implement `QuotaProvider` interface
3. Define API models and response parser
4. Register in `ProviderRegistry`
5. Add token storage for the provider
6. Update README with provider-specific quota information

---

欢迎贡献！添加新服务商的步骤：

1. 在 `data/provider/` 下创建新包
2. 实现 `QuotaProvider` 接口
3. 定义API模型和响应解析器
4. 在 `ProviderRegistry` 中注册
5. 为该服务商添加令牌存储
6. 更新README，添加服务商特定的配额信息
