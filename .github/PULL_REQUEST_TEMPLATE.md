---
name: 拉取请求
about: 提交代码改动
title: ""
labels: []
assignees: ""
---

## 描述

请简要说明本次改动的内容与目的。

## 改动类型

- [ ] 新功能（feat）
- [ ] Bug 修复（fix）
- [ ] 文档（docs）
- [ ] 构建 / CI（ci）
- [ ] 安全（security）
- [ ] 其他

## 测试清单

- [ ] `gradlew.bat assembleJava32Release --no-daemon --console=plain` 构建通过
- [ ] 涉及广告拦截 / 播放链路时，真机 logcat 验证 `echo-adrules`、`echo-ad-blocked` 正常输出
- [ ] 未引入新增编译警告
- [ ] 本地路径、密钥、token 等敏感信息未进入提交

## 关联 Issue

Fixes #（如有）

## 截图（可选）

<!-- 界面改动请附前后对比截图 -->
