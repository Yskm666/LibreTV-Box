# 贡献指南

感谢你愿意为 **LibreTV Box** 贡献力量！无论是提交 Bug、建议新功能、补充广告规则，还是改进代码，都欢迎通过 Issue 和 Pull Request 参与。

## 目录

- [开发环境](#开发环境)
- [构建与测试](#构建与测试)
- [提交规范](#提交规范)
- [提交 Pull Request](#提交-pull-request)
- [新增广告规则域名](#新增广告规则域名)
- [新增站点 / 线路](#新增站点--线路)
- [代码风格](#代码风格)

## 开发环境

| 工具 | 版本要求 |
|---|---|
| JDK | 17（本地构建环境路径见 `gradle.properties`） |
| Android SDK | 含 Build-Tools 30.0.3、platform android-23/30+ |
| Gradle | 7.5（使用仓库内 `gradlew.bat`，无需单独安装） |
| 目标设备 | Android 6.0+（32 位 armeabi-v7a），建议有一台小米电视 L50M5-AD 同款设备做真机验证 |

## 构建与测试

```bash
# 32 位 release（构建前确保根目录存在 keystore.properties，见 README）
gradlew.bat assembleJava32Release --no-daemon --console=plain

# 32 位 debug（无需签名配置，CI 默认使用）
gradlew.bat assembleJava32Debug --no-daemon --console=plain
```

**真机验证清单**（有电视时）:

```bash
adb connect <电视IP>:5555
adb install -r <apk>
adb shell pm clear com.github.tvbox.osc.jun
adb shell monkey -p com.github.tvbox.osc.jun -c android.intent.category.LAUNCHER 1
adb logcat -d | Select-String "echo-adrules|echo-ad-blocked|echo-playM3u8|echo-warm-spider"
```

提交前请至少完成：

- [ ] `assembleJava32Release` 构建通过
- [ ] 改动涉及广告拦截 / 播放链路时，真机 logcat 确认 `echo-adrules`、`echo-ad-blocked` 正常输出
- [ ] 未引入新的警告级编译错误

## 提交规范

- 提交信息使用 **中文**，格式：`<类型>: <简述>`
  - `feat:` 新功能（如 `feat: 新增 XX 线路预置`）
  - `fix:` 修复（如 `fix: 修复 XX 站点解析失败`）
  - `docs:` 文档（如 `docs: 更新故障排除章节`）
  - `ci:` 构建 / CI（如 `ci: 增加 Release 自动构建`）
  - `security:` 安全（如 `security: 签名密码改为注入式读取`）
- 一次提交只做一件事；同一功能的多轮修改在 PR review 后再合并。

## 提交 Pull Request

1. Fork 本仓库，在 `master` 基础上新建分支：`git checkout -b feat/xxx`
2. 完成改动并自测（见上文清单）
3. 推送分支并创建 PR，模板中勾选测试项
4. 等待 review；修改意见会以评论形式提出

> 主分支 `master` 受保护，请勿直接推送；所有改动走 PR。

## 新增广告规则域名

广告拦截基于 `AdRuleManager` 合并的三源规则，日常维护渠道：

- **上游规则源**（AdRules / StevenBlack / uAssets）：直接给上游提 PR，无需动本仓库
- **本仓库内置默认列表**（离线兜底，见 `ApiConfig.java` 的默认广告列表与 `extraAds`）：在 `ads` 数组追加域名即可，格式为纯域名（如 `ad.example.com`），无需 `||domain^` 包裹
- 规则表上限 35 万条，新增前先确认不与其他规则重复

## 新增站点 / 线路

- **聚合线路**：在 `ApiConfig.java` 的 `ensurePresetLinesAndSites()` 预置逻辑中追加，给出线路名、类型（JSON/XML/jar/远程）与地址
- **加密配置源**：需符合 2423 分支 AES-CBC 加密格式（见 `ApiConfig.FindResult()`），提交时附解密说明，不提交私有密钥
- 新线路务必真机验证：首页加载、搜索、播放三条链路各跑一遍

## 代码风格

- 沿用 TVBoxOSC 原有风格：4 空格缩进、类名大驼峰、方法名小驼峰
- 关键业务逻辑（广告拦截、解析链路）保持中文注释，说明意图
- 不引入新依赖除非必要；32 位设备内存有限，避免大内存数据结构
- 涉及 OkHttp / 网络层改动时，保持全信任 SSL 与超时策略的既有约定
