# RTSBuilding Agent Notes

This repository is the public mainline for RTSBuilding.

## User / Conversation

- Prefer Chinese when talking with the user.
- The workspace root is `E:\RTSbuilding`.
- Before changing code, inspect the relevant local files first.
- Do not revert user changes or unrelated local changes.

## Project Layout

- `main` is the Minecraft 1.21.1 NeoForge line.
- `forge-1.20.1` is the Minecraft 1.20.1 Forge branch.
- The local Forge working copy lives at `sister-projects\rtsbuilding-forge-1.20.1`.
- Do not publish `sister-projects` as a nested directory in `main`.
- Use the Forge working copy only as the work-tree for the `forge-1.20.1` branch.

## Local Memory Files

- `detailed_project.txt` and `Journal.txt` are local working-memory files.
- Keep them updated after meaningful project changes.
- They are intentionally local-only and should not be committed or pushed.
- They are ignored through `.git/info/exclude`, not the public `.gitignore`.

## Collaboration Lessons

- Community PRs should be treated as valuable design and code input, but do not wholesale-merge a PR if it would overwrite mainline hotfixes, cross-version sync work, or local architecture decisions. Prefer partial adoption with clear attribution and a short comment explaining what was absorbed.
- When a feature or fix is intended for release, consider both active lines by default: `main` / NeoForge 1.21.1 and `forge-1.20.1` / Forge 1.20.1. If a change is intentionally single-version, say so explicitly.
- Keep dirty-worktree discipline. If another agent or the user has active work, inspect it only as needed and avoid touching it unless the user asks to take it over.
- Release artifacts should use the compact naming pattern `rtsbuilding-(neoforge|forge)-<gameversion>-<modversion>`.

## Implementation Cautions

- For remote storage, tools, energy items, durability, NBT-heavy items, and capability-backed stacks, do not match by item id alone and do not discard mutations on copied `ItemStack`s. Preserve the real extracted stack, run the action, then return the mutated remainder to the original source or an explicit fallback.
- For RTS rendering, avoid ending or flushing Minecraft's shared render buffers from custom render stages unless the lifecycle is proven safe. Prefer private buffers or narrowly scoped draw paths, especially around Sodium/Embeddium compatibility.
- For UI work, check high RTS UI scale, Minecraft GUI scale, English, Simplified Chinese, Traditional Chinese Taiwan, Traditional Chinese Hong Kong, scissor regions, and layer ordering. Settings/tutorial/pop-up panels should render above RTS overlays and should not leak mouse wheel input to the camera.
- For keybind work, distinguish "button clicked" from "action held". Mouse-bound drag actions and keyboard-bound drag actions may need different input paths.

## Gemini UI Review Workflow

- Use Gemini primarily as a UI / UX / interaction-design reviewer, not as the main coding authority. Its strongest role in this project is reviewing layout, control flow, information hierarchy, discoverability, tutorial wording, panel density, and multi-language UI risks.
- Before calling Gemini, search official Google documentation for the current Pro text model name. Prefer the latest Gemini Pro model available through the API, such as `gemini-3.1-pro-preview` when current. Do not rely on stale model names, and do not use Flash for important design review unless Pro is unavailable and the user accepts the fallback.
- Use the local `GEMINI_API_KEY` environment variable. Never print, log, paste, or commit the key. If the key is missing, continue with local judgment and tell the user briefly.
- Ask Gemini for UI review when the task involves a new or complex screen, modal, overlay, tutorial, blueprint/capture workflow, settings layout, high-scale layout, multi-step interaction, or a confusing player-facing state. For small code fixes, networking bugs, build errors, or server-only logic, prefer local code inspection and primary source documentation.
- Give Gemini the full product context instead of a one-line question:
  - What feature is being built and what problem it solves.
  - The current screen layout, where each panel/button lives, and what the player is trying to do.
  - The exact interaction flow, including click/drag/keyboard states, cancel/undo paths, and disabled/empty/error states.
  - Screenshots when available, or a precise text description of the screenshot if image upload is not practical.
  - Relevant code snippets for layout constants, render order, input handling, scissor/clipping, and state transitions.
  - Constraints: Minecraft GUI scale, RTS UI scale, English/Simplified Chinese/Traditional Chinese Taiwan/Traditional Chinese Hong Kong text length, mouse wheel leakage, client-only classloading, and dual-version sync.
- Good Gemini question shapes:
  - "Given this screenshot and these layout constants, which UI elements are fighting for attention or likely to overlap at high scale?"
  - "How should this multi-step capture workflow expose current state, next action, cancel, preview, and save without crowding the bottom RTS panel?"
  - "Which labels/tooltips would make this action discoverable in English and Chinese without becoming too long?"
  - "What states should this modal/panel handle: empty, loading, selected, missing materials, creative bypass, disabled config, and error?"
  - "Which controls should be grouped, moved, hidden, collapsed, or promoted based on the player's main task?"
- Treat Gemini output as design critique, not instructions to blindly implement. Extract the useful recommendations, reject anything that conflicts with Minecraft conventions or local architecture, then implement using this repository's existing UI patterns.
- After using Gemini for UI work, briefly record the actionable takeaways in `Journal.txt` or `detailed_project.txt` when they affect project direction.

## Issue Tracking

- Use only two public issue title categories:
  - `[问题]` for bugs, regressions, crashes, bad compatibility, or confusing behavior that should be fixed.
  - `[提议]` for feature requests, UX improvements, design directions, or long-term architecture ideas.
- When closing an issue, leave a short comment that states what changed or where the work is now tracked. If the issue is a duplicate, point to the canonical issue.
- Prefer splitting large mixed feedback threads into focused issues once the direction is clear.

## Release Update Notes

- Write release notes from the actual commit/change record, not from memory alone. Check recent commits, local diff, and any release-specific notes before drafting.
- Keep the structure consistent with prior Modrinth/CurseForge posts: one English section and one Simplified Chinese section for each released loader/game-version line.
- Mention the loader and game version in the heading, for example `### NeoForge 1.21.1 1.0.5` and `### Forge 1.20.1 0.0.5`.
- Lead with the release intent, then list concrete player-facing changes. Group related details under one bullet when possible.
- Call out risky or compatibility-sensitive changes directly, such as dedicated server fixes, rendering fixes, storage/tool handling, camera changes, or rollback advice.
- If a version is experimental or less tested, say that clearly and tell users which recent stable release to return to.
- Keep the notes paste-ready Markdown. Avoid internal-only implementation details unless they explain a visible fix.

Sample release note shape:

```markdown
### NeoForge 1.21.1 1.0.5
For Minecraft 1.21.1 / NeoForge.

This update focuses on smoother RTS camera control, safer storage/tool handling, and several UI fixes reported by players.

- Improved RTS camera smoothing.
  - Smooth camera movement is now enabled by default.
  - Camera input should feel less jittery in multiplayer and large modpacks.
- Optimized large quick-build placement.
  - Large placement jobs are now processed in batches on the server.
  - Storage refreshes are reduced during bulk placement to avoid unnecessary UI stalls.
- Fixed several RTS UI issues.
  - Settings and pop-up panels now stay above the RTS overlay.
  - High UI scale layouts were adjusted to avoid clipped controls.
- Compatibility notes.
  - If you hit a serious regression, please temporarily return to the latest stable release and report the issue with logs.

### NeoForge 1.21.1 1.0.5
适用于 Minecraft 1.21.1 / NeoForge。

本次更新重点改进 RTS 镜头手感、储存/工具处理安全性，并修复若干玩家反馈的 UI 问题。

- 改进 RTS 镜头平滑。
  - 平滑镜头现在默认开启。
  - 多人和大型整合包环境下，镜头移动应当更稳定。
- 优化大规模快速建造。
  - 大型放置任务现在由服务端分批处理。
  - 批量放置期间会减少不必要的储存刷新，降低 UI 卡顿。
- 修复若干 RTS UI 问题。
  - 设置和弹窗面板现在会正确显示在 RTS overlay 上方。
  - 调整了高 UI 缩放下的布局，减少控件被裁切的问题。
- 兼容性说明。
  - 如果遇到严重回归，请先退回最近的稳定版本，并带日志反馈问题。
```

## Build Commands

Main 1.21.1:

```powershell
cd E:\RTSbuilding
$env:JAVA_HOME='C:\Program Files\Microsoft\jdk-21.0.11.10-hotspot'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
$env:GRADLE_USER_HOME='E:\RTSbuilding\.gradle-user-home'
.\gradlew.bat build --no-daemon --no-configuration-cache
```

Forge 1.20.1:

```powershell
cd E:\RTSbuilding\sister-projects\rtsbuilding-forge-1.20.1
$env:JAVA_HOME='C:\Program Files\Microsoft\jdk-21.0.11.10-hotspot'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
$env:GRADLE_USER_HOME='E:\RTSbuilding\sister-projects\rtsbuilding-forge-1.20.1\.gradle-user-home'
.\gradlew.bat build --no-daemon --no-configuration-cache
```

## Forge Branch Commit Pattern

When committing sister-project changes to `forge-1.20.1`, use a temporary index so the root `main` index is not polluted:

```powershell
$repo = 'E:\RTSbuilding'
$work = 'E:\RTSbuilding\sister-projects\rtsbuilding-forge-1.20.1'
$tmpIndex = Join-Path $repo '.git\forge-1.20.1.work.index'
Remove-Item -LiteralPath $tmpIndex -Force -ErrorAction SilentlyContinue
$env:GIT_INDEX_FILE = $tmpIndex
try {
    git --git-dir "$repo\.git" --work-tree "$work" read-tree forge-1.20.1
    git --git-dir "$repo\.git" --work-tree "$work" add <paths>
    $tree = git --git-dir "$repo\.git" --work-tree "$work" write-tree
    $parent = git --git-dir "$repo\.git" rev-parse forge-1.20.1
    $commit = git --git-dir "$repo\.git" commit-tree $tree -p $parent -m '<message>'
    git --git-dir "$repo\.git" update-ref refs/heads/forge-1.20.1 $commit
} finally {
    Remove-Item Env:\GIT_INDEX_FILE -ErrorAction SilentlyContinue
    Remove-Item -LiteralPath $tmpIndex -Force -ErrorAction SilentlyContinue
}
```
