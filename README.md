<<<<<<<<<<<<<<json接口示例>>>>>>>>>>>>>
# LibreTV Box

基于 [TVBoxOSC](https://github.com/TVBoxOSC/TVBoxOSC) 二次开发的定制版，针对 **小米电视 L50M5-AD**（代号 xmen，Amlogic T962，Android 6.0.1 / SDK 23 / 32 位 armeabi-v7a）深度优化。

## 定制特性

- **三源广告拦截引擎**（`AdRuleManager` + 增强 `AdBlocker`）：
  - Cats-Team/AdRules（中文区域广告，~20.8 万域名）
  - StevenBlack/hosts（Pi-hole 生态，~7.1 万域名）
  - uBlock Origin / uAssets filters（`||domain^` 规则提取）
  - 三级镜像拉取（ghfast.top → gh-proxy.org → 直连，全信任 SSL 兼容 Android 6 旧 CA），72h 自动刷新，本地缓存离线秒加载
  - 域名 Hash 匹配：精确 + 子域后缀链 O(1) 查找，本地代理/纯 IP 永不误拦
- **播放层广告直链拦截**：广告地址直接跳过不播放（`echo-ad-blocked`）
- **m3u8 净化默认开启**：内容特征识别（帧率/时长/主机差异）剔除广告切片，不再限官方大站
- **恢复 WebView 广告拦截**（上游 `shouldInterceptRequest` 被注释，已恢复）
- **SSL 兼容**：全局 OkHttp 全信任 SSL + 连接池复用（`JsonParallel` 复用主 client）
- **加载提速**：全局超时 20s → 8s，预热限制 10 站
- **蜗牛加密线路预置**：AES-CBC（2423 分支）解密的 111 站线路 + 放牛主源 + bfzyapi 量子源

## 构建

```bash
# 32 位 release（armeabi-v7a）
gradlew.bat assembleJava32Release --no-daemon --console=plain
# 产物: app/build/outputs/apk/java32/release/TVBox_release-java32.apk
```

> 签名：`app/build.gradle` 引用 `app/libretv.keystore`（release 签名），keystore 不随仓库分发，请自行生成或联系作者。

---

# TVBox

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
	"sites": [],
	"parses": [],
	"hosts": [
		"cache.ott.ystenlive.itv.cmvideo.cn=base-v4-free-mghy.e.cdn.chinamobile.com",
		"cache.ott.bestlive.itv.cmvideo.cn=ip"
	],
	"lives": [],
	"rules": [],
	"doh": [
		{
			"name": "騰訊",
			"url": "https://doh.pub/dns-query"
		},
		{
			"name": "阿里",
			"url": "https://dns.alidns.com/dns-query"
		},
		{
			"name": "360",
			"url": "https://doh.360.cn/dns-query"
		}
	]
    }
