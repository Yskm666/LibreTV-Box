# TV-box

![平台](https://img.shields.io/badge/平台-Android%206.0%2B-blue)
![许可](https://img.shields.io/github/license/Yskm666/TV-box)
![构建](https://img.shields.io/github/actions/workflow/status/Yskm666/TV-box/build-apk.yml)
![版本](https://img.shields.io/github/v/release/Yskm666/TV-box)
![Star](https://img.shields.io/github/stars/Yskm666/TV-box)

>  —— 为小米电视 L50M5-AD 深度定制的 TVBoxOSC 增强版

基于 [TVBoxOS（TVBoxOSC 系）](https://github.com/q215613905/TVBoxOS) 二次开发，针对小米电视 L50M5-AD（代号 `xmen`，Amlogic T962，Android 6.0.1 / 32 位）进行全链路优化：融合三大开源广告规则项目实现系统级广告过滤、预置加密聚合线路、修复 Android 6 旧 CA 导致的 SSL 报错、显著降低加载延迟。

## 目录

- [说明](#说明)
- [核心功能](#核心功能)
- [工作原理](#工作原理)
- [快速开始](#快速开始)
- [广告过滤](#广告过滤)
- [资源接入源](#资源接入源)
- [常见问题](#常见问题)
- [执照](#执照)

## 说明

TV-box 面向家庭电视场景，集中解决三个核心痛点：

1. **广告多** —— 系统广告、片头广告、源注入广告三层清理
2. **操作复杂** —— 聚合 111+ 影视站点，搜索、点播一步直达
3. **设备陈旧** —— Android 6.0.1 的卡顿与证书报错问题逐一修复

项目保留 TVBoxOSC 全部核心能力（多线路聚合、加密源解析、直播、嗅探），并在此基础上新增一套独立的广告过滤引擎与若干稳定性改造。

参与贡献请阅读 [CONTRIBUTING.md](CONTRIBUTING.md)。

## 核心功能

- **26.3 万广告域名库**：融合 AdRules、StevenBlack/hosts、uBlock Origin 三大规则源，三层链路系统级拦截
- **111 站聚合搜索**：蜗牛加密线路破解后预置，开箱即用
- **m3u8 广告切片净化**：自动识别并剔除片头、插播广告段，默认开启
- **流畅加载**：全局超时 8s、连接池复用、Spider 预热限流
- **Android 6 兼容**：全信任 SSL，旧系统 CA 不再报错
- **离线可用**：广告规则本地缓存，断网也能按缓存拦截
- **低内存占用**：规则表 Hash 化 O(1) 匹配，32 位设备无压力（实测内存约 158MB）

## 工作原理

### 广告拦截三层架构

```
┌──────────────────────────────────────────────────────────┐
│ 第 3 层  m3u8 净化（内容特征识别）                         │
│  播放时分析切片：帧率 / 时长 / 主机差异 → 剔除广告段       │
├──────────────────────────────────────────────────────────┤
│ 第 2 层  播放层直链拦截                                    │
│  播放地址命中广告域名 → 直接跳过不播放                     │
├──────────────────────────────────────────────────────────┤
│ 第 1 层  解析链路拦截（WebView / XWalk）                   │
│  解析页面资源逐条检查，命中广告域名返回空资源              │
├──────────────────────────────────────────────────────────┤
│ 规则引擎  AdRuleManager + AdBlocker（26.3 万域名）         │
└──────────────────────────────────────────────────────────┘
```

### 线路与资源加载

- **预置多线路**：放牛主源（63 站）、蜗牛加密线路（AES-CBC 破解复刻，111 站）、bfzyapi 量子源、多仓订阅
- **加密源支持**：内置 2423 分支 AES-CBC 解密逻辑，直接加载加密配置
- **加载提速**：全局超时 20s → 8s；解析并发请求复用全局连接池，消除反复新建连接的开销；搜索 Spider 预热限制为 10 个站点

### 兼容性工程

- **SSL 兼容**：全局 OkHttp 全信任 SSL，绕过 Android 6 系统 CA 库过期问题；解析器并发请求复用同一信任连接
- **内存保护**：广告域名表超过 35 万条自动截断，防止 32 位进程内存溢出

## 快速开始

### 环境要求

| 项目 | 要求 |
|---|---|
| 构建机 | JDK 17、Android SDK、Gradle 7.5 |
| 目标设备 | Android 6.0+（32 位 armeabi-v7a），开启 ADB 调试 |

### 构建 APK

```bash
# 32 位 release（armeabi-v7a，目标电视架构）
gradlew.bat assembleJava32Release --no-daemon --console=plain

# 产物：app/build/outputs/apk/java32/release/TVBox_release-java32.apk
```

> 签名：release 签名从工程根 `keystore.properties` 读取（该文件已被 `.gitignore` 排除，不入库，避免密码泄露）。构建前请在工程根创建 `keystore.properties`，包含 `storeFile`、`storePassword`、`keyAlias`、`keyPassword` 四项，或修改 `app/build.gradle` 的 `signingConfigs`。

### 安装到电视

```bash
# 连接电视（需开启"开发者选项 → USB 调试"）
adb connect <电视IP>:5555

# 安装（-r 覆盖升级并保留数据）
adb install -r TV-box_java32_v7.apk

# 全新安装时清数据（可选）
adb shell pm clear com.github.tvbox.osc.jun

# 启动
adb shell monkey -p com.github.tvbox.osc.jun -c android.intent.category.LAUNCHER 1
```

### 初始配置

1. 打开应用，进入「设置 → 线路」
2. 选择预置线路（推荐 **蜗牛线路 111 站** 或 **放牛主源**）
3. 首页即可浏览聚合内容，搜索跨 111 站聚合
4. 广告规则首次启动后约 10～20 秒自动加载完成，之后离线秒加载

## 广告过滤

### 规则源

本项目融合三大开源广告规则项目，由 `AdRuleManager.java` 统一拉取、解析、合并：

| 规则源 | 项目 | 格式 | 规模 |
|---|---|---|---|
| **Cats-Team/AdRules** | [Cats-Team/AdRules](https://github.com/Cats-Team/AdRules) | `dns.txt`（adblock `\|\|domain^`） | ~20.8 万域名 |
| **Pi-hole 生态** | [StevenBlack/hosts](https://github.com/StevenBlack/hosts) | hosts（`0.0.0.0 domain`） | ~7.1 万域名 |
| **uBlock Origin** | [uBlockOrigin/uAssets](https://github.com/uBlockOrigin/uAssets) | `filters.txt`（提取 `\|\|domain^`） | 41 域名 |

合计去重 **263,727 个广告域名**（2026-10 实测）。

### 匹配策略

- 域名以 **HashSet** 存储，精确 + 子域后缀链匹配（`x.ad.com` → `ad.com`），单次判定 O(1)
- **本地代理与纯 IP 地址永不拦截**（`127.0.0.1` 净化代理、内网源站不受影响）
- 内置默认列表（放牛 ads + Google 广告域）离线兜底，配置 `ads` 字段自动追加

### 更新与缓存

- 每 **72 小时** 后台刷新一次，三级镜像降级拉取（ghfast.top → gh-proxy.org → 直连），走全信任 SSL
- 规则落盘 `files/adblock_rules.txt`（约 5.5MB），启动先加载缓存，离线秒用
- 合并超过 35 万条自动截断，防止 32 位进程内存溢出

## 资源接入源

### 预置线路

| 线路 | 类型 | 说明 |
|---|---|---|
| 放牛主源 | 聚合 63 站 | 默认主源，站内 ads 域名自动注入拦截 |
| 蜗牛线路 | 加密配置 111 站 | AES-CBC（2423 分支）破解复刻预置 |
| bfzyapi 量子源 | CMS V10 JSON | 标准采集源，支持搜索 |
| 多仓订阅 | 远程订阅 JSON | GitHub 托管聚合 |

预置逻辑见 `ApiConfig.java` 的 `ensurePresetLinesAndSites()`。

### 自定义接入

沿用 TVBoxOSC 标准配置（设置 → 配置/线路），支持：

- **聚合接口**：JSON（CMS V10）、XML、jar Spider（type=3）、远程
- **加密配置**：2423 分支 AES-CBC 自动解密（`ApiConfig.FindResult()`）
- **解析器**：嗅探、解析直链，支持自定义 header
- **广告列表**：配置 `ads` 字段追加拦截域名

完整配置语法参考上游 [TVBoxOS](https://github.com/q215613905/TVBoxOS) 文档。

## 常见问题

| 现象 | 排查方法 |
|---|---|
| 播放报「Trust anchor not found」 | 报错来自源 jar 内部备用线路检测，App 主链路已全信任 SSL，多数自动切换 http 备用地址；纯 https 站点请换线路 |
| 广告拦截不生效 | ① 检查 `echo-adrules` 日志确认规则已加载（cache:263727）② 确认播放的是 m3u8 流（直链 mp4 无法净化）③ 源内嵌广告属源侧行为，可反馈域名补进规则 |
| 加载慢 / 卡顿 | 首次启动需下载 111 站 jar（一次性，之后缓存）；确认网络可访问 ghfast.top |
| 规则更新失败 | 自动三级镜像降级，全部失败时保留旧缓存，不破坏现有拦截 |
| 瓜子、西瓜等站点空响应 | 该类 Spider 依赖 jar 内置本地代理，电视上执行失败属 jar 侧问题，请换源 |

常用日志关键词：`echo-adrules`（规则状态）、`echo-ad-blocked`（播放层拦截）、`echo-playM3u8`（净化链路）、`echo-fixAdM3u8 ... removed: N segments`（广告切片清除数）、`echo-warm-spider`（预热进度）。

## 执照

本项目基于 [TVBoxOS（TVBoxOSC 系）](https://github.com/q215613905/TVBoxOS) 二次开发，遵循上游协议 **GPL-3.0**。

融合的规则数据来源于：

- [Cats-Team/AdRules](https://github.com/Cats-Team/AdRules)（GPL-3.0 / CC BY-SA 4.0 等，详见上游）
- [StevenBlack/hosts](https://github.com/StevenBlack/hosts)（MIT）
- [uBlockOrigin/uAssets](https://github.com/uBlockOrigin/uAssets)（GPL-3.0）

本仓库代码同样以 **GPL-3.0** 授权，使用时请遵守上游及各规则源的许可条款。
