# Modes and Buttons

Top bar buttons change how RTSBuilding handles your actions. The most important ones are the mode buttons on the left: the selected mode decides what happens when you click blocks in the world.

## Interact

Interact is the default mode and is closest to vanilla play.

In Interact mode:

- Right click can use blocks, machines, chests, doors, or place the currently selected item.
- Left click can mine blocks.
- If you selected a block in the bottom bar, right-clicking a block face places that selected block.
- If placement is not possible, right click falls back to normal interaction, such as opening a machine or chest.

To place on a chest, machine, or other interactable block, hold `Shift` while right-clicking.

You can also aim at a block in the world and press the Pick Block key, usually middle mouse, to select it as the next placement item.

## Storage Binding

Storage Binding tells RTSBuilding which container or storage system to use for materials.

Usage:

1. Click the Storage Binding button in the top bar.
2. Aim at a chest, barrel, drawer, storage controller, or other storage block.
3. Left-click it to bind it as RTS storage that supports both storing and extracting.
4. After binding succeeds, the top hint shows the bound storage name.

Right click can also bind storage, but right-click binding is extract-only. It is useful when you want RTSBuilding to take items from a container without putting items back into it.

After entering Storage Binding mode, normal right-click interaction is temporarily disabled. This prevents you from accidentally opening machines or chests while trying to bind them.

## Funnel

Funnel mode is used to plan item transport from the RTS view. It is useful when items need to be sent to a target or moved between storage and machines.

Usage:

1. Click the Funnel button in the top bar.
2. Aim at the target you want to operate on.
3. Follow the current hint and click as prompted.

If you only need Funnel temporarily, hold the quick funnel hotkey. The default is `F`. While held, Funnel controls are active; when released, RTSBuilding returns to the previous mode.

## Rotate

Rotate mode changes block orientation from the overhead RTS camera.

Usage:

1. Click the Rotate button in the top bar.
2. Aim at a block that should rotate.
3. Right-click the block.

This is useful for stairs, machines, and other rotatable blocks. Whether a block can rotate depends on the block itself.

## Quick Build

Quick Build places groups of blocks at once. It is useful for roads, floors, walls, circles, boxes, and repeated building tasks.

Usage:

1. Click the Quick Build button to open the panel.
2. Choose a shape.
3. Choose a fill mode, such as filled, hollow, or frame.
4. Follow the top hint and set points in the world.
5. When the preview is correct, right-click to confirm.

Different shapes need different steps:

- Single block: place directly.
- Line, square, wall, and circle: set point A, then point B.
- Box: set point A and point B, then adjust height.

If the preview is wrong, click Quick Build again or close the panel to cancel the preview.

Use `Ctrl + Z` to undo the last group and `Ctrl + Y` to redo it. Undo and redo usually require the selected item to match the block used for the build.

## Ultimine

Ultimine breaks a group of connected blocks of the same type.

Usage:

1. Click the Ultimine button in the top bar to open the panel.
2. Set the maximum number of blocks.
3. Aim at a block and hold the mining action, usually left click.
4. After the first block breaks, connected matching blocks are broken together.

Use `-`, `+`, `MIN`, and `MAX` in the panel to adjust the limit, or click the number to type directly.

If you release the mining action early, Ultimine will not trigger. The first block must fully break before the group is processed.

## Chunk Display

The chunk display button shows or hides chunk borders.

It is useful for farms, machine arrays, base modules, and builds that need to align to chunk edges.

Click the button again to turn chunk display off.

## Tutorial Button

The `i` button in the top bar opens tutorial text for the current area.

If you do not know what a button or panel does, click `i`. The tutorial pop-up appears above the RTS interface, and you can close it to continue playing.

## Settings Button

The gear button on the right opens RTS control settings.

Here you can adjust RTS UI scale, camera feel, auto-store, Shift Store, smooth camera, damage warnings, and related options.

These settings affect your personal controls and visuals. Survival balancing rules such as material cost, build range, and server rules are controlled by the server or mod configuration.
