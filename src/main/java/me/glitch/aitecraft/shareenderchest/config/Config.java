package me.glitch.aitecraft.shareenderchest.config;

import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;

public class Config {
    public long autosaveSeconds = 300;
    public boolean requireSneak = true;
    public int inventoryRows = 6;
    public boolean openFromHand = true;
    public boolean playOpenSound = false;
    public String inventoryName = "Shared Ender Chest";

    public ScreenHandlerType<GenericContainerScreenHandler> screenHandlerType() {
        return switch (inventoryRows) {
            case 1 -> ScreenHandlerType.GENERIC_9X1;
            case 2 -> ScreenHandlerType.GENERIC_9X2;
            case 3 -> ScreenHandlerType.GENERIC_9X3;
            case 4 -> ScreenHandlerType.GENERIC_9X4;
            case 5 -> ScreenHandlerType.GENERIC_9X5;
            case 6 -> ScreenHandlerType.GENERIC_9X6;
            default -> ScreenHandlerType.GENERIC_9X3;
        };
    }
}
