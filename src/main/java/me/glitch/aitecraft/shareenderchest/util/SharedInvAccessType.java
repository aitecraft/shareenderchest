package me.glitch.aitecraft.shareenderchest.util;

import net.minecraft.entity.player.PlayerEntity;

public enum SharedInvAccessType {
    ITEM_USE,
    BLOCK_USE,
    SLOT_USE;

    public boolean shouldPlayerUseItem(PlayerEntity player) {
        boolean isInCreativeDim = player.getWorld().getRegistryKey().equals(Compat.CREATIVE_SUPERFLAT_KEY);
        if (isInCreativeDim) {
            return false;
        }
        return switch (this) {
            case ITEM_USE -> !player.isSpectator();
            case BLOCK_USE -> !player.isSpectator() && player.isSneaking();
            case SLOT_USE -> true;
        };

    }

}
