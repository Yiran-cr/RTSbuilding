# Creative Item Picker

The Creative Item Picker is a Creative-mode-only material picker for RTSBuilding. It reads vanilla and modded creative tabs so you can choose blocks and items directly inside the RTS interface without moving them into your inventory first.

This page appears only when the player is in Creative mode. Survival players still use Storage Space to pull materials from bound chests, machines, AE systems, or the player inventory.

## How It Differs from Storage Space

- The Creative Item Picker does not require bound storage.
- Clicking an item only makes it the current RTS placement item. It does not add the item to your inventory.
- Creative-mode remote placement does not consume bound storage or player inventory items.
- Survival mode does not show this tab and cannot use it as a limitless material source.

In short: the Creative Item Picker is for map making, build testing, and creative prototyping. Storage Space is still the resource source for survival play.

## Open the Creative Item Picker

1. Enter Creative mode.
2. Open the RTSBuilding interface.
3. Click the Creative tab above the bottom bar.

If you cannot see the Creative tab in Survival mode, that is expected. Switch to Creative mode and reopen RTSBuilding.

## Find Items

The Creative Item Picker follows creative-tab categories.

- Click a category on the left to switch to a vanilla or modded creative tab.
- Click All to show all currently available creative items.
- Use the top search box to search by item display name, item ID, mod namespace, or registry path.
- Use the page buttons or mouse wheel to page through the current filtered results.

Some modpack creative tabs are generated dynamically by other mods. If a modded creative tab fails while being built, RTSBuilding skips that tab so the RTS screen stays usable instead of crashing.

## Select and Place

Click an item in the Creative Item Picker to make it the current RTS placement item. The top bar will show the selected item.

Then place it normally:

1. Stay in Interact mode.
2. Aim at the target block or position in the world.
3. Right-click to place the currently selected item.

Quick building, shape placement, and other RTS placement tools also use the currently selected item. In Creative mode, materials are not consumed.

## Recent Items

The Recent area still appears on the right side of the Creative Item Picker. Items you just placed are kept there, so you can switch back without searching again.

This is useful when alternating between a few materials such as stone bricks, glass, stairs, lamps, and decorative blocks.

## Clear Selection

Click the **Empty Hand** button in the bottom hotbar to clear the selected item.

After clearing selection, right-click returns to normal block interaction such as opening doors, chests, and machines.

## Common Questions

### Why can't I see the Creative tab?

The tab appears only in Creative mode. Survival and Adventure mode do not show the Creative Item Picker.

### Why can't I find a modded item?

Clear the search box first, then switch to the All category. If the item still does not appear, the mod may not add it to a creative tab, or its creative tab may have failed while RTSBuilding was reading it.

### Why did selecting an item not add it to my inventory?

That is expected. The Creative Item Picker is an RTS placement source, not a give-item button. It does not modify your inventory.

### Does this affect survival balance on servers?

No. Only Creative-mode players can see and use this entry point. Survival mode still pulls materials from bound storage, the player inventory, or other server-allowed resource sources.
