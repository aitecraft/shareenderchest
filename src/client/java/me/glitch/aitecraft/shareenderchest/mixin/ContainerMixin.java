package me.glitch.aitecraft.shareenderchest.mixin;

import java.nio.ByteBuffer;

import org.lwjgl.sdl.SDLKeyboard;
import org.lwjgl.sdl.SDLScancode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ContainerInput;

import me.glitch.aitecraft.shareenderchest.ShareEnderChestClient;
import me.glitch.aitecraft.shareenderchest.ShareEnderChest;

@Mixin(MultiPlayerGameMode.class)
public class ContainerMixin {
    @Unique
    private static boolean keysActive() {
        ByteBuffer state = SDLKeyboard.SDL_GetKeyboardState();
        return
            state.get(SDLScancode.SDL_SCANCODE_RCTRL) > 0 ||
            state.get(SDLScancode.SDL_SCANCODE_LCTRL) > 0 ||
            state.get(SDLScancode.SDL_SCANCODE_LALT)  > 0 ||
            state.get(SDLScancode.SDL_SCANCODE_RALT)  > 0 ;
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
