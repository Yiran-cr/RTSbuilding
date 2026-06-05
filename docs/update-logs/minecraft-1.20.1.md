# RTSBuilding Update Log - Minecraft 1.20.1

## English

### Forge 1.20.1 1.1.1 Pilot

For Minecraft 1.20.1 / Forge.

1.1.1 is still an early Pilot build. If this version feels unstable or causes serious issues, 1.0.5 is currently the safest release to return to. 1.1.0 is also reasonably stable if you want to keep newer Pilot features. Detailed usage docs are available at https://rts.ciallo.ltd.

Compared with 1.1.0, the Forge line received the same major usability fixes as the 1.21.1 line where the loader APIs allow it, with extra attention to 1.20.1-specific UI strings and resource validation.

- Improved blueprint workflow.
  - Added Litematic blueprint import.
  - Blueprint previews can now be nudged and rotated around their center.
  - Blueprint placement now requires a real block hit, reducing accidental placement into invalid air positions.
  - Blueprint preview placement polish from the NeoForge line was synced to Forge.
  - Added a clear-selected-blueprint action and polished Forge blueprint tab labels.
  - Fixed blueprint capture block count previews and several status text placement problems.
  - Fixed missing Forge 1.20.1 translation keys for Create schematic sync buttons/status text and empty-hand placement hints.

- Fixed remote storage and remote mining issues.
  - Remote mining now keeps selected storage tools more reliably after use.
  - Storage refresh after remote mining is deferred, reducing visible stutter while mining.
  - Remote chest/container close handling was fixed, preventing invalid remote menus from staying open.
  - RTS chest remote menu validation is stricter, reducing bad container states in multiplayer or modpacks.

- Improved storage UI experience.
  - Storage page size now follows the visible item grid instead of using a fixed page size.
  - Storage category labels are localized.
  - Item hover display was refined.
  - Default RTS storage import behavior was toned down to avoid surprising players.

- Improved RTS Home and config UI.
  - RTS Home and config screens were clarified for Forge.
  - RTS Home relocation now has a 20 in-game-day cooldown unless Field Deployment is unlocked.
  - Skill cost settings now show skill names in the Forge config screen.
  - The config screen was split into scalable tabs so high GUI scale reduces visible rows instead of overlapping controls.

- Compatibility fixes.
  - Pipez interaction reach fixes were absorbed into the Forge line.
  - Forge RTS UI and placement updates were synced from the NeoForge line where applicable.

- Internal structure, briefly.
  - Blueprint capture, blueprint dialogs, storage session state, storage services, and blueprint panel helpers were split into smaller modules.
  - These changes are mostly maintenance work; the player-facing goal is fewer UI glitches, safer blueprint placement, and smoother remote storage/mining behavior.

## 简体中文

### Forge 1.20.1 1.1.1 Pilot

适用于 Minecraft 1.20.1 / Forge。

1.1.1 仍然是早期 Pilot 测试版本。如果这个版本体验不稳定，或者遇到严重问题，当前最稳的回退版本是 1.0.5。如果想保留较新的 Pilot 功能，1.1.0 的稳定性也还可以。详细使用说明可以查看 https://rts.ciallo.ltd。

相比 1.1.0，Forge 线在加载器 API 允许的范围内同步了 1.21.1 线的主要体验修复，并额外处理了 1.20.1 自己的 UI 文本和资源验证问题。

- 改进蓝图流程。
  - 新增 Litematic 蓝图导入。
  - 蓝图预览现在可以微调位置，并围绕中心旋转。
  - 蓝图放置现在要求命中真实方块，减少误放到无效空中位置的情况。
  - 同步了 NeoForge 线的蓝图预览放置体验修正。
  - 新增清除已选蓝图的操作，并调整 Forge 蓝图标签文本。
  - 修复蓝图捕获方块数量预览、状态提示位置等问题。
  - 补齐 Forge 1.20.1 的缺失翻译 key，包括机械动力蓝图同步按钮/状态文本和空手放置提示。

- 修复远程储存和远程采矿问题。
  - 远程采矿后会更可靠地保留已选工具。
  - 远程采矿后的储存刷新改为延后处理，减少采矿时的可见卡顿。
  - 修复远程箱子/容器关闭处理，减少无效远程菜单残留。
  - 加强 RTS 箱子远程菜单校验，降低多人和整合包环境下的异常容器状态。

- 改进储存界面体验。
  - 储存分页现在会根据可见网格数量调整。
  - 储存分类名称已本地化。
  - 优化物品悬浮显示。
  - 下调默认储存导入行为的激进程度，减少误导玩家的情况。

- 改进 RTS 家园和配置界面。
  - 调整 Forge 线的 RTS 家园和配置界面说明。
  - 切换 RTS 家园现在有 20 个游戏日冷却；解锁野外部署后可绕过。
  - Forge 配置界面的技能材料设置现在会显示技能名称。
  - 配置界面拆成可缩放分页，高 GUI 缩放下会减少可见行数，而不是让控件互相覆盖。

- 兼容性修复。
  - 吸收 Pipez 交互距离相关修复。
  - 在适用范围内同步 NeoForge 线的 RTS UI 和放置体验修正。

- 内部结构简述。
  - 拆分了蓝图捕获、蓝图弹窗、储存会话状态、储存服务和蓝图面板 helper。
  - 这些主要是维护性工作；对玩家来说，本次重点是减少 UI 错误、让蓝图放置更安全、让远程储存和采矿更顺。
