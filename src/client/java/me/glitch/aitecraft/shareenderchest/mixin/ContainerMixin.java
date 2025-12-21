package me.glitch.aitecraft.shareenderchest.mixin;

import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.glitch.aitecraft.shareenderchest.ShareEnderChestClient;
import me.glitch.aitecraft.shareenderchest.ShareEnderChest;

import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ClickType;

@Mixin(MultiPlayerGameMode.class)
public class ContainerMixin {

    @Unique
    private static boolean isPressed(int keyCode, long handle) {
        return GLFW.glfwGetKey(handle, keyCode) == GLFW.GLFW_PRESS;
    }

    @Unique
    private static boolean keysActive() {
        final long handle = Minecraft.getInstance().getWindow().getWindow();
        return
            isPressed(GLFW.GLFW_KEY_LEFT_CONTROL, handle) ||
            isPressed(GLFW.GLFW_KEY_RIGHT_CONTROL, handle) || 
            isPressed(GLFW.GLFW_KEY_LEFT_ALT, handle) ||
            isPressed(GLFW.GLFW_KEY_RIGHT_ALT, handle);
    }

    @Inject(at = @At("HEAD"), method = "handleInventoryMouseClick", cancellable = true)
    public void onclick(int syncId, int slotId, int button, ClickType actionType, Player player, CallbackInfo ci) {
        if (actionType == ClickType.PICKUP && keysActive()) {

            Slot selectedSlot = player.containerMenu.getSlot(slotId);
            if (selectedSlot != null && selectedSlot.container instanceof Inventory) {

                if (player.containerMenu.getCarried().isEmpty()) {
                    if (ShareEnderChest.isEnderChest(selectedSlot.getItem())) {
                        ci.cancel();
                        ShareEnderChestClient.sendOpenPacket();
                    }
                }
            }
        }
    }
}
