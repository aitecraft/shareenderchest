package me.glitch.aitecraft.shareenderchest.mixin;

import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.glitch.aitecraft.shareenderchest.ShareEnderChestClient;
import me.glitch.aitecraft.shareenderchest.ShareEnderChest;

import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

@Mixin(ClientPlayerInteractionManager.class)
public class ContainerMixin {

    @Unique
    private static boolean isPressed(int keyCode, long handle) {
        return GLFW.glfwGetKey(handle, keyCode) == GLFW.GLFW_PRESS;
    }

    @Unique
    private static boolean keysActive() {
        final long handle = MinecraftClient.getInstance().getWindow().getHandle();
        return 
            isPressed(GLFW.GLFW_KEY_LEFT_CONTROL, handle) ||
            isPressed(GLFW.GLFW_KEY_RIGHT_CONTROL, handle) || 
            isPressed(GLFW.GLFW_KEY_LEFT_ALT, handle) ||
            isPressed(GLFW.GLFW_KEY_RIGHT_ALT, handle); 
    }

    @Inject(at = @At("HEAD"), method = "clickSlot", cancellable = true)
    public void onclick(int syncId, int slotId, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci) {
        if (actionType == SlotActionType.PICKUP && keysActive()) {
            
            Slot selectedSlot = player.currentScreenHandler.getSlot(slotId);
            if (selectedSlot != null && selectedSlot.inventory instanceof PlayerInventory) {

                if (player.currentScreenHandler.getCursorStack().isEmpty()) {
                    if (ShareEnderChest.isEnderChest(selectedSlot.getStack())) {
                        ci.cancel();
                        ShareEnderChestClient.sendOpenPacket();
                    }
                }
            }
        }
    }
}
