package com.rtsbuilding.rtsbuilding.client;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

public final class RtsClientBootstrap {
    private RtsClientBootstrap() {
    }

    public static void registerConfigUi(ModContainer modContainer) {
        modContainer.registerExtensionPoint(IConfigScreenFactory.class,
                (container, parent) -> new ConfigurationScreen(modContainer, parent));
    }
}
