package me.glitch.aitecraft.shareenderchest.config;

import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;

public class Config {
    public long autosaveSeconds = 300;
    public boolean requireSneak = true;
    public int inventoryRows = 6;
    public boolean openFromHand = true;
    public boolean openFromInventory = true;
    public boolean playOpenSound = true;
    public String inventoryName = "Shared Ender Chest";

    public MenuType<ChestMenu> screenHandlerType() {
        return switch (inventoryRows) {
            case 1 -> MenuType.GENERIC_9x1;
            case 2 -> MenuType.GENERIC_9x2;
            case 3 -> MenuType.GENERIC_9x3;
            case 4 -> MenuType.GENERIC_9x4;
            case 5 -> MenuType.GENERIC_9x5;
            case 6 -> MenuType.GENERIC_9x6;
            default -> MenuType.GENERIC_9x3;
        };
    }
}
