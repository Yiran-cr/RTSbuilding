# Blueprint Space

Blueprint Space imports, selects, previews, and places building blueprints.

You can use existing blueprints, or select a structure in the world and save it as a new blueprint.

## Open Blueprint Space

After opening RTSBuilding, look at the tabs above the bottom bar.

Click Blueprint Space to switch the bottom bar to the blueprint interface.

If a server or modpack disables blueprints, the interface will say that blueprints are unavailable.

## Import Blueprints

Click Upload and choose a blueprint file.

Supported formats include:

- `.nbt`
- `.schem`
- `.schematic`
- `.litematic`

After a successful import, the blueprint appears in the list. Click its name to select it.

If import fails, check whether the format is supported, the file is too large, or the blueprint contains blocks that do not exist in the current game.

## Sync Create Schematics

If you use Create schematics, click Sync Create Schematics.

RTSBuilding reads existing Create schematic files from the current game folder and copies them into the RTSBuilding blueprint folder.

After syncing, refresh the blueprint list and select them there.

## Select and Preview a Blueprint

Click a blueprint in the list. A preview appears in the world.

Use the preview to check position, direction, and rough shape. Move the camera and find a suitable placement position.

If no preview appears, make sure a blueprint is selected and your cursor points to a valid world position.

After selecting a blueprint, press `X` to clear the current blueprint preview. This key can be changed in Minecraft's key settings.

## Pin the Preview

Aim at a block in the world and right-click to pin the blueprint preview at that position.

After pinning, the blueprint no longer follows the mouse. Check the position before placing it.

If the preview is in the wrong place, clear it and pin again, or use movement buttons to adjust it.

## Move, Raise, Lower, and Rotate

After the blueprint is pinned, use the movement and rotation buttons in Blueprint Space to adjust the preview.

Common controls:

- Click direction buttons on screen to move forward, backward, left, or right.
- Use raise/lower buttons to adjust height.
- Use rotation buttons to change facing.
- Use arrow keys for horizontal nudging.
- Use `Page Up` / `Page Down` for vertical nudging.

After adjusting, check whether the world preview is aligned. Place only after confirming.

## Place the Blueprint

After confirming position, direction, and materials, click Place to start building.

In survival mode, RTSBuilding checks whether enough materials are available. If materials are missing, Blueprint Space lists what is missing.

Creative mode does not consume materials.

## Material Check

Blueprint Space shows required materials and how much can currently be built.

If materials are missing, read the missing block names and amounts. Add the materials, then return to Blueprint Space and refresh or reselect the blueprint.

If the blueprint contains blocks that do not exist in the current game, the interface marks them. Placeable parts can still be used.

## Save a Structure as a Blueprint

Click Create New Blueprint to enter selection mode.

Basic flow:

1. Right-click the first corner in the world.
2. Right-click the opposite corner.
3. Check the selected area.
4. Exclude blocks you do not want to save if prompted.
5. Click Save and name the blueprint.

Press `X` during selection mode to cancel the current selection.

After saving, the new blueprint appears in the blueprint list.

## Rename, Delete, and Save As

After selecting a blueprint, you can rename or delete it.

To share a blueprint, use Save As and save it to a location you choose.

Confirm before deleting. Deleted blueprints are removed from the blueprint folder.
