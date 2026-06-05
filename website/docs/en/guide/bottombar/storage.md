# Storage Space

Storage Space shows the items and fluids RTSBuilding can currently use.

It can show the player hotbar, RTS quick slots, bound chests, machines, storage systems, and supported fluid storage.

## View Storage Space

Open the Storage Space tab in the bottom bar. If storage is bound, its items are shown. If no storage is bound, only player hotbar items are shown.

Storage Space is roughly split into several areas:

- Top: search box and page buttons.
- Left: fluid bar. It appears only when the bound storage has usable fluids.
- Center: main item area. These are items available in the bound storage.
- Right: recent items and fluids.
- Bottom: player hotbar and RTS quick slots.

## Search Items

Click the search box and type an item name, material name, or mod name to filter items. Clear the search box to return to the default view.

## Categories and Sorting

The category bar narrows the item list. Click a category to show only items in that category.

Use the letter buttons on the left to change item order:

- Click `S` to switch sort mode.
- Click `A` / `D` to switch ascending or descending order.

## Select Items

Click an item in Storage Space to make it the current placement item.

After selection, the top hint shows the current item. In Interact mode, aim at a block and right-click to place it.

To cancel the selected item, click the Empty Hand button in the hotbar.

## Player Hotbar

The lower-left part of the bottom bar shows player hotbar slots 1 to 9.

Click a hotbar slot to make RTSBuilding use that tool or item. For example, if you carry a pickaxe, wrench, block, or bucket, you can select it here.

If Shift Store is enabled, hold `Shift` and click a player hotbar slot in the bottom bar to send that slot's item into the current RTS storage.

Right-click a bucket or fluid container in the player hotbar to try transferring one bucket of fluid into RTS fluid storage.

## Empty Hand

Click the Empty Hand button in the hotbar to clear item selection so right-click can open doors, chests, machines, and other normal interactions.

## RTS Quick Slots

RTS quick slots save frequently used materials.

Left-click an item in Storage Space, hover over an empty RTS quick slot, and press `P` to bind it. `P` is the default Bind Quick Slot key.

You can also bind tools or items from the player hotbar. This keeps common tools and materials near the bottom bar without repeated searching.

Click a bound quick slot to select that item. Hold `Shift` and click a quick slot to clear it.

If there are too many quick slots, the last slot becomes a page button. Click it to switch to the next page.

Right-click a bound bucket or fluid container to try transferring one bucket of fluid into RTS fluid storage.

## Recent Items

The recent area keeps items and fluids you just used.

Click an entry in Recent to switch back to it without searching again.

This is useful when alternating between materials such as stone bricks, glass, stairs, lamps, water, or lava.

## Fluid Storage

After fluid-related abilities are unlocked, RTSBuilding can show fluids from the currently bound storage.

When usable fluids exist, an orange-bordered fluid bar appears on the left side of Storage Space. Each fluid slot shows a bucket-style preview and an amount in buckets.

Click a fluid slot to select that fluid. The top bar shows the selected fluid; right-click a block in the world to place it remotely like a block.

Fluids also enter Recent after use.

## Store Fluids into RTS

RTSBuilding can extract fluid from some buckets or fluid containers and store it in the current RTS fluid storage.

Common entry points:

- Right-click a bucket or fluid container in the main item area.
- Right-click one in the player hotbar.
- Right-click one bound to an RTS quick slot.

Each action attempts to transfer one bucket. Whether it works depends on the item, the bound storage, and fluid capacity limits under survival balancing.

Modded fluid tanks can also be bound like chests. After binding, RTSBuilding can read and use fluids from that fluid network.

## Shift Store

Shift Store quickly sends items from inventories or containers into the current RTS storage.

After enabling it in the side floating window or settings, hold `Shift` and right-click an item in an inventory, chest, or machine screen. The item will be stored into RTS storage.

In the RTSBuilding bottom bar, the player hotbar also responds to Shift Store: hold `Shift` and click a hotbar slot to store that item into the current RTS storage.

This is useful for bulk AE transfers. After connecting an AE system, enable Shift Store and use `Shift + right click` on container items to send them into the network.

Turn off Shift Store in settings or the side floating window when you want normal item movement.
