# LibreTV Box

> 让电视回归电视 —— 为小米电视 L50M5-AD 深度定制的 TVBoxOSC 增强版

基于 [TVBoxOSC](https://github.com/TVBoxOSC/TVBoxOSC) 二次开发的开源电视盒子应用，针对 **小米电视 L50M5-AD**（代号 `xmen`，Amlogic T962，Android 6.0.1 / SDK 23 / 32 位 armeabi-v7a）做了全链路优化：融合三大开源广告规则项目实现系统级广告过滤、预置加密聚合线路、修复 Android 6 旧系统 CA 导致的 SSL 问题、显著降低加载延迟。

## 目录

- [说明](#说明)
- [工作原理](#工作原理)
  - [广告拦截三层架构](#广告拦截三层架构)
  - [线路与资源加载](#线路与资源加载)
  - [兼容性工程](#兼容性工程)
- [快速入门](#快速入门)
  - [环境要求](#环境要求)
  - [构建 APK](#构建-apk)
  - [安装到电视](#安装到电视)
  - [初始配置](#初始配置)
- [你将获得什么](#你将获得什么)
- [故障排除](#故障排除)
- [广告过滤](#广告过滤)
  - [规则源](#规则源)
  - [匹配策略](#匹配策略)
  - [更新与缓存](#更新与缓存)
- [资源接入源](#资源接入源)
  - [预置线路](#预置线路)
  - [自定义接入](#自定义接入)
- [执照](#执照)

---

## 说明

LibreTV Box 解决家庭电视场景的三个核心痛点：

1. **广告多** —— 系统广告、片头广告、源注入广告三层清理
2. **操作复杂** —— 聚合 111+ 影视站点，搜索/点播一步直达
3. **设备老旧** —— Android 6.0.1 32 位设备跑不动的卡顿、证书报错逐一修复

项目保持 TVBoxOSC 全部核心能力（多线路聚合、加密源解析、直播、嗅探），在此之上新增一套独立的广告过滤引擎与若干稳定化改造。

## 工作原理

### 广告拦截三层架构

```
┌─────────────────────────────────────────────────────────┐
│ 第 3 层  m3u8 净化（内容特征识别）                        │
│  播放 m3u8 时分析切片：帧率/时长/主机差异 → 剔除广告段   │
│  （默认开启，不再限官方大站）                            │
├─────────────────────────────────────────────────────────┤
│ 第 2 层  播放层直链拦截（AdBlocker.isAd）                │
│  播放地址若命中广告域名 → 直接跳过不播放（echo-ad-blocked）│
├─────────────────────────────────────────────────────────┤
│ 第 1 层  解析链路拦截（WebView / XWalk）                 │
│  解析页面资源请求逐条检查，命中广告域名返回空资源        │
│  （上游被注释的 shouldInterceptRequest 已恢复）           │
├─────────────────────────────────────────────────────────┤
│ 规则引擎  AdRuleManager + AdBlocker（26.3 万域名）       │
│  Cats-Team/AdRules ∪ StevenBlack/hosts ∪ uBlock uAssets │
└─────────────────────────────────────────────────────────┘
```

### 线路与资源加载

- **预置多线路**：放牛主源（63 站）、蜗牛加密线路（AES-CBC 破解复刻，111 站）、bfzyapi 量子源、多仓订阅
- **加载提速**：全局超时 20s → 8s；解析并发请求复用全局连接池（消除每次新建 client 的开销）；搜索 spider 预热限制 10 站上限
- **加密源支持**：内置 2423 分支 AES-CBC 解密逻辑，直接加载加密配置源

### 兼容性工程

- **SSL 兼容**：全局 OkHttp 全信任 SSL（`SSLSocketFactoryCompat` + 宽松 HostnameVerifier），绕过 Android 6 系统 CA 库过期问题；解析器并发请求复用同一信任 client
- **内存保护**：广告域名表上限 35 万条，防止 32 位进程 OOM

## 快速入门

### 环境要求

| 项目 | 要求 |
|---|---|
| 构建机 | JDK 17、Android SDK、Gradle 7.5 |
| 目标设备 | Android 6.0+（32 位 armeabi-v7a），开启 ADB 调试 |
| 网络 | 电视可访问 GitHub 镜像（ghfast.top / gh-proxy.org），国内网络建议开启代理 |

### 构建 APK

```bash
# 32 位 release（armeabi-v7a，目标电视架构）
gradlew.bat assembleJava32Release --no-daemon --console=plain

# 产物路径
# app/build/outputs/apk/java32/release/TVBox_release-java32.apk
```

> 签名：`app/build.gradle` 引用 `app/libretv.keystore`（release 签名）。keystore 为私有文件**不随仓库分发**，构建前请在 `app/` 目录放置同名 keystore 或修改 `signingConfigs`。

### 安装到电视

```bash
# 1. 连接电视（电视需开启"开发者选项 → USB 调试"）
adb connect <电视IP>:5555

# 2. 安装（-r 覆盖升级保留数据）
adb install -r LibreTV_Box_java32_v7.apk

# 3. 全新安装时清数据（可选）
adb shell pm clear com.github.tvbox.osc.jun

# 4. 启动
adb shell monkey -p com.github.tvbox.osc.jun -c android.intent.category.LAUNCHER 1
```

### 初始配置

1. 打开 App 进入「设置 → 线路」
2. 选择预置线路（推荐 **蜗牛线路 111 站** 或 **放牛主源**）
3. 首页即出现聚合内容；搜索跨 111 站聚合
4. 广告规则首次启动后约 10~20 秒自动加载完成（`echo-adrules` 日志确认），之后离线秒加载

## 你将获得什么

- **26.3 万广告域名库**：系统级拦截，三层过滤链路
- **111 站聚合搜索**：蜗牛加密线路破解预置，开箱即用
- **m3u8 广告切片净化**：自动剔除片头/插播广告段，默认开启
- **流畅加载**：8s 超时 + 连接池复用 + 预热限制
- **Android 6 兼容**：SSL 全信任，旧 CA 不再报错
- **离线可用**：广告规则本地缓存，断网也能按缓存拦截
- **低内存占用**：规则表 Hash 化 O(1) 匹配，32 位设备无压力（实测进程 ~158MB）

## 故障排除

| 现象 | 排查方法 |
|---|---|
| 播放报「Trust anchor not found」 | 该报错来自源 jar 内部备用线路检测（UrlChecker），App 主链路已全信任 SSL，多数自动切换 http 备用地址；纯 https 站点可换线路，或给电视安装用户 CA 证书 |
| 广告拦截不生效 | ① 检查 `echo-adrules` 日志确认规则已加载（cache:263727 或 total:263727）② 确认播放的是 m3u8 流（直链 mp4 广告无法净化）③ 源内嵌片头广告属源侧行为，可反馈域名补进规则 |
| 加载慢/卡顿 | 首次启动需下载 111 站 jar（一次性，之后缓存）；检查网络是否可访问 ghfast.top；超时已优化为 8s |
| 规则更新失败 | 自动三级镜像降级（ghfast → gh-proxy → 直连）；全部失败保留旧缓存不破坏拦截 |
| 播放卡在解析 | `adb logcat -s TVBox-runtime:*` 查看 `echo-resolvePlayUrl failed, try next line`（自动换线路属正常容错） |
| 瓜子/西瓜等站点空响应 | 该类 spider 依赖 jar 内置本地代理（go 二进制），电视上执行失败属 jar 侧问题，App 无法修复，请换源 |

**常用日志关键词**：`echo-api-config-----------load`（配置加载）、`echo-adrules`（规则状态）、`echo-ad-blocked`（播放层拦截）、`echo-playM3u8`（净化链路）、`echo-fixAdM3u8 ... removed: N segments`（广告切片清除数）、`echo-warm-spider`（预热进度）。

## 广告过滤

### 规则源

本项目融合三大开源广告规则项目，在 `AdRuleManager.java` 中统一拉取、解析、合并：

| 规则源 | 项目 | 格式 | 规模 |
|---|---|---|---|
| **Cats-Team/AdRules** | [Cats-Team/AdRules](https://github.com/Cats-Team/AdRules) | `dns.txt`（adblock `\|\|domain^`） | ~20.8 万域名 |
| **Pi-hole 生态** | [StevenBlack/hosts](https://github.com/StevenBlack/hosts) | hosts（`0.0.0.0 domain`） | ~7.1 万域名 |
| **uBlock Origin** | [uBlockOrigin/uAssets](https://github.com/uBlockOrigin/uAssets) | `filters.txt`（提取 `\|\|domain^`） | 41 域名 |

合计去重 **263,727 个广告域名**（2026-10 实测）。

### 匹配策略

- 域名 **HashSet** 存储，精确 + 子域后缀链匹配（`x.ad.com` → `ad.com`），单次判定 O(1)
- **本地代理 / 纯 IP 地址永不拦截**（`127.0.0.1` 净化代理、内网源站不受影响）
- 内置默认列表（放牛 ads + Google 广告域）离线兜底，配置 `ads` 字段自动追加

### 更新与缓存

- 每 **72h** 后台刷新一次，三级镜像降级拉取，走全信任 SSL（Android 6 兼容）
- 规则落盘 `files/adblock_rules.txt`（~5.5MB），启动先加载缓存 → 离线秒用
- 合并超 35 万自动截断（防 32 位进程 OOM）

## 资源接入源

### 预置线路

| 线路 | 类型 | 说明 |
|---|---|---|
| 放牛主源 | 聚合 63 站 | 默认主源，含站内 ads 域名自动注入拦截 |
| 蜗牛线路 | 加密配置 111 站 | AES-CBC（2423 分支）破解复刻预置 |
| bfzyapi 量子源 | CMS V10 JSON | 标准采集源，searchable |
| 多仓订阅 | 远程订阅 JSON | GitHub 托管聚合 |

预置逻辑见 `ApiConfig.java` 的 `ensurePresetLinesAndSites()`。

### 自定义接入

沿用 TVBoxOSC 标准配置（设置 → 配置/线路），支持：

- **聚合接口**：JSON（CMS V10）/ XML / jar spider（type=3）/ 远程
- **加密配置**：2423 分支 AES-CBC 自动解密（`ApiConfig.FindResult()`）
- **解析器**：嗅探 / 解析直链，支持自定义 header
- **广告列表**：配置 `ads` 字段追加拦截域名

上游完整配置语法见文末附录（JSON 接口示例）。

## 执照

本项目基于 [TVBoxOSC/TVBoxOSC](https://github.com/TVBoxOSC/TVBoxOSC) 二次开发，遵循其上游协议 **GPL-3.0**。

融合的规则数据来源于：
- [Cats-Team/AdRules](https://github.com/Cats-Team/AdRules)（GPL-3.0 / CC BY-SA 4.0 等，详见上游）
- [StevenBlack/hosts](https://github.com/StevenBlack/hosts)（MIT）
- [uBlockOrigin/uAssets](https://github.com/uBlockOrigin/uAssets)（GPL-3.0）

本仓库代码同样以 **GPL-3.0** 授权。使用本项目时请遵守上游及各规则源的许可条款。

---

## 附录：上游配置参考（TVBoxOSC JSON 接口示例）

=== Source Code - Editing the app default settings ===

    //searchable:搜索开关	        0:关闭 1:启用
    //filterable:首页可选	        0:否 1:是
    //playerType:播放器类型	        0:系统 1:IJK 2:EXO
    //采集接口类型		        0:xml 1:json 3:jar 4:remote
    //parses解析类型		        0:嗅探,自带播放器 1:解析,返回直链
    //直播参数说明                   ua:用户自定义ua epg:节目网址 logo:台标网址
    
    {
	"spider": "./your.jar",
	"wallpaper": "./api/img",
	"lives": "http://...",
	"sites": [
		{
			"key": "csp_DianShi",     // 唯一标识，不可重复
			"name": "电视",            // 名称
			"type": 3,                 // 采集接口类型 0:xml 1:json 3:jar 4:remote
			"api": "./tv.jar",         // 接口地址
			"searchable": 1,           // 搜索开关
			"quickSearch": 1,          // 快捷搜索
			"filterable": 1            // 首页可选
		},
		{
			"key": "csp_BFZY",
			"name": "量子资源",
			"type": 1,
			"api": "https://bfzyapi.com/api.php/provide/vod",
			"searchable": 1,
			"quickSearch": 0,
			"filterable": 1
		}
	],
	"parses": [
		{
			"name": "默认解析",
			"type": 0,
			"url": "",
			"header": "",
			"ext": ""
		}
	],
	"rules": [],
	"ads": ["ad.example.com"],
	"doh": ["https://doh.pub/dns-query"],
	"flags": []
    }
