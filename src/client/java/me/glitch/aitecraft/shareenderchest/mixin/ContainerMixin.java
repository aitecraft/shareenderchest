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
import net.minecraft.world.inventory.ContainerInput;

@Mixin(MultiPlayerGameMode.class)
public class ContainerMixin {

    @Unique
    private static boolean isPressed(int keyCode, long handle) {
        return GLFW.glfwGetKey(handle, keyCode) == GLFW.GLFW_PRESS;
    }

    @Unique
    private static boolean keysActive() {
        final long handle = Minecraft.getInstance().getWindow().handle();
        return
            isPressed(GLFW.GLFW_KEY_LEFT_CONTROL, handle) ||
            isPressed(GLFW.GLFW_KEY_RIGHT_CONTROL, handle) || 
            isPressed(GLFW.GLFW_KEY_LEFT_ALT, handle) ||
            isPressed(GLFW.GLFW_KEY_RIGHT_ALT, handle);
    }

    @Inject(at = @At("HEAD"), method = "handleContainerInput", cancellable = true)
    public void onclick(int containerId, int slotNum, int buttonNum, ContainerInput containerInput, Player player, CallbackInfo ci) {
        if (containerInput == ContainerInput.PICKUP && keysActive()) {

            Slot selectedSlot = player.containerMenu.getSlot(slotNum);
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
