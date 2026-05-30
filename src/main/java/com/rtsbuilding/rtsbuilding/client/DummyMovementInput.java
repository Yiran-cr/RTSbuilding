package com.rtsbuilding.rtsbuilding.client;

import net.minecraft.client.player.Input;

public final class DummyMovementInput extends Input {
    @Override
    public void tick(boolean slowDown, float speedModifier) {
        this.forwardImpulse = 0.0F;
        this.leftImpulse = 0.0F;
        this.jumping = false;
        this.shiftKeyDown = false;
        this.down = false;
        this.up = false;
        this.left = false;
        this.right = false;
    }
}
