# Modpack and Server Config

This page is for modpack authors, server admins, and players who want to tune RTSBuilding survival rules.

If you are only playing normally, you usually only need Skill Tree and Unlocks and RTS Home and Range.

## Key Rule Options

Press `G` to open the RTS interface, then open the gear settings panel. Important options include:

- Survival Balancing: whether skill tree unlocks are enabled.
- Team Sharing: whether multiplayer teams share unlock progress and home anchors.
- Maximum Operation Radius: how far late-game RTS actions can reach.
- Blueprints: whether blueprint features are enabled.
- Maximum Blueprint Blocks: the maximum size for imported, selected, saved, or placed blueprints.
- Skill Material Cost: custom materials required by each skill node.

In multiplayer, server configuration overrides local client settings.

## Custom Skill Material Cost

In the Skill Material Cost section, find the material input for the target skill node and enter the material list.

The format is item ID plus amount. Separate multiple materials with commas:

```text
minecraft:chest:2,minecraft:redstone:8
minecraft:copper_ingot:16
minecraft:diamond_pickaxe:1,minecraft:redstone_block:1
```

Click Reset to restore the default material cost for that skill node.

Save settings to apply the rule. In multiplayer, the server configuration decides the final rule.

## Tune Progression Pace

To let RTSBuilding reach base-building earlier, reduce the material cost of early nodes such as Camera Core, Storage Binding, and Remote Placement.

To make RTSBuilding feel more like late-game technology, raise the cost of key nodes, or place Remote Mining, Crafting Terminal, and Blueprint-related abilities behind later material gates.

Prefer changing material cost before disabling many features. That way players can still see the full ability tree while unlocking it at the modpack's intended pace.
