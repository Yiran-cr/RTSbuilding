# RTSBuilding Update Log - Minecraft 1.21.1

## English

### NeoForge 1.21.1 1.1.1 Pilot

For Minecraft 1.21.1 / NeoForge.

1.1.1 is still an early Pilot build. If this version feels unstable or causes serious issues, 1.0.5 is currently the safest release to return to. 1.1.0 is also reasonably stable if you want to keep newer Pilot features. Detailed usage docs are available at https://rts.ciallo.ltd.

Compared with 1.1.0, this update mainly improves blueprint usability, remote storage stability, RTS Home/config screens, and several modpack compatibility issues.

- Improved blueprint workflow.
  - Added Litematic blueprint import.
  - Blueprint previews can now be nudged and rotated around their center.
  - Blueprint placement now requires a real block hit, reducing accidental placement into invalid air positions.
  - High-air preview placement was adjusted so previews stay easier to control.
  - Added a clear-selected-blueprint action, so players can cancel an unwanted large ghost preview.
  - Improved creative-mode blueprint placement and preview feedback.
  - Fixed blueprint capture block count previews and several status text placement problems.

- Fixed remote storage and remote mining issues.
  - Remote mining now keeps selected storage tools more reliably after use.
  - Storage refresh after remote mining is deferred, reducing the "mine one block, wait, mine one block" feeling.
  - Remote chest/container close handling was fixed, preventing invalid remote menus from staying open.
  - RTS chest remote menu validation is stricter, reducing bad container states in multiplayer or modpacks.

- Improved storage UI experience.
  - Storage page size now follows the visible item grid instead of using a fixed page size.
  - Storage category labels are localized.
  - Empty hand slot display and item hover text were cleaned up.
  - Default RTS storage import behavior was toned down to avoid surprising players.

- Improved RTS Home and config UI.
  - RTS Home no longer blurs the whole screen behind it.
  - RTS Home relocation now has a 20 in-game-day cooldown unless Field Deployment is unlocked.
  - The home screen shows the remaining cooldown beside the home coordinates.
  - The config screen was split into scalable tabs: gameplay/blueprints and skill tree material costs.
  - Skill cost settings now show skill names instead of raw override strings.

- Compatibility fixes.
  - Remote POV reach was synced for Pipez-style interactions.
  - Old Beyond Dimensions network names are handled.
  - Several render/UI layering issues were cleaned up around RTS overlays and config/home screens.

- Internal structure, briefly.
  - Blueprint, storage, network, and builder-screen logic were split into smaller service/helper modules.
  - This should make future fixes safer, but the player-facing focus of this release is stability and usability rather than architecture.

## 简体中文

### NeoForge 1.21.1 1.1.1 Pilot

适用于 Minecraft 1.21.1 / NeoForge。

1.1.1 仍然是早期 Pilot 测试版本。如果这个版本体验不稳定，或者遇到严重问题，当前最稳的回退版本是 1.0.5。如果想保留较新的 Pilot 功能，1.1.0 的稳定性也还可以。详细使用说明可以查看 https://rts.ciallo.ltd。

相比 1.1.0，本次更新主要改进蓝图体验、远程储存稳定性、RTS 家园/配置界面，以及若干整合包兼容问题。

- 改进蓝图流程。
  - 新增 Litematic 蓝图导入。
  - 蓝图预览现在可以微调位置，并围绕中心旋转。
  - 蓝图放置现在要求命中真实方块，减少误放到无效空中位置的情况。
  - 调整高空预览放置逻辑，让蓝图预览更容易控制。
  - 新增清除已选蓝图的操作，玩家可以取消不想要的大型跟随预览。
  - 改进创造模式下的蓝图放置和预览反馈。
  - 修复蓝图捕获方块数量预览、状态提示位置等问题。

- 修复远程储存和远程采矿问题。
  - 远程采矿后会更可靠地保留已选工具。
  - 远程采矿后的储存刷新改为延后处理，减少“挖一格卡一下”的体感。
  - 修复远程箱子/容器关闭处理，减少无效远程菜单残留。
  - 加强 RTS 箱子远程菜单校验，降低多人和整合包环境下的异常容器状态。

- 改进储存界面体验。
  - 储存分页现在会根据可见网格数量调整。
  - 储存分类名称已本地化。
  - 简化空手槽图标，优化物品悬浮显示。
  - 下调默认储存导入行为的激进程度，减少误导玩家的情况。

- 改进 RTS 家园和配置界面。
  - RTS 家园界面不再把整个背景糊掉。
  - 切换 RTS 家园现在有 20 个游戏日冷却；解锁野外部署后可绕过。
  - RTS 家园会在坐标旁显示剩余切换冷却。
  - 配置界面拆成可缩放分页：玩法/蓝图、技能树材料。
  - 技能材料设置会显示技能名称，不再只显示原始配置字符串。

- 兼容性修复。
  - 同步远程 POV reach，改善 Pipez 等交互兼容。
  - 兼容旧版 Beyond Dimensions 网络名称。
  - 清理部分 RTS overlay、配置界面、家园界面的层级和显示问题。

- 内部结构简述。
  - 拆分了蓝图、储存、网络、建造界面等模块。
  - 这部分主要是为了后续维护更安全；本次对玩家最重要的是稳定性和操作体验改进。
