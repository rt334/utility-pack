# 更多实用功能（utility-pack）

一个个人用的 Mindustry 模组，汇集实用小功能。

## 功能

### 🧩 方块搜索
在方块选择菜单添加搜索栏，可跨全部分类搜索所有合法方块（支持中文与拼音）。

### ⏸ 多人暂停
多人游戏中允许其他玩家请求暂停/解除暂停，服务端可配置模式：
- **关闭**：仅房主可暂停
- **管理员**：管理员可暂停
- **自定义**：通过聊天命令 `!pause grant <玩家名>` 添加白名单成员

聊天命令（房主/管理员）：`!pause on` / `!pause off` / `!pause custom` / `!pause grant <名>` / `!pause revoke <名>` / `!pause list`

### 🔄 自动更新
启动时自动检查 GitHub 新版本（可关闭），有更新时弹出提示，支持游戏内**一键下载安装**（CDN 加速优先，直连兜底）并自动重启生效。

## 设置（游戏内 设置 → 更多实用功能）
- 方块搜索：显示搜索历史、选择后清除输入
- 多人暂停：暂停模式、发送暂停请求、管理暂停白名单
- 更新：自动检查更新、手动检查更新

## 安装
将构建产物 `UtilityPack.jar` 放入游戏 mods 目录（`%APPDATA%\Mindustry\mods`），或通过游戏内更新系统自动下载最新版。

> 注意：请勿与本模组与同样包含方块搜索/多人暂停功能的 Silicon 模组同时启用，以免功能冲突。

## 构建
使用 `C:\dsh\build-mods.ps1`（javac --release 17 + D8 desugar，classpath 为 Mindustry jar），产物输出到 `C:\dsh\new\UtilityPack.jar`（同时含 PC 的 `.class` 与 Android 的 `classes.dex`，兼容 PC 与 Android）。

## 版本历史
- **a0.1.1.0**：新增自动更新系统（游戏内一键下载安装，CDN 加速）
- **a0.1.0.1**：冲突检测仅识别已启用的 Silicon；更换模组图标
- **a0.1.0.0**：方块搜索 + 多人暂停
