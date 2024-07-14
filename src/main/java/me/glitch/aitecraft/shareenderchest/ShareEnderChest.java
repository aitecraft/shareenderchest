package me.glitch.aitecraft.shareenderchest;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.UUID;

import me.glitch.aitecraft.shareenderchest.util.SharedInvAccessType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.ServerStarted;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.ServerStopping;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.EndTick;

import net.minecraft.block.EnderChestBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.collection.DefaultedList;

public class ShareEnderChest implements ModInitializer, ServerStopping, ServerStarted, EndTick {

    private static SharedInventory sharedInventory;

    private final long AUTOSAVE_TICKS = 20L * 60L * 5L;  // Autosave Every 5 minutes
    private long ticksUntilSave = AUTOSAVE_TICKS;

    private static OpenSharedInventory openSharedInventory;

    public void onServerStarted(MinecraftServer server) {
        File inventoryFile = getFile(server);
        if (inventoryFile.exists()) {
            try (FileInputStream inventoryFileInputStream = new FileInputStream(inventoryFile);
                 DataInputStream inventoryFileDataInput = new DataInputStream(inventoryFileInputStream)) {
                NbtCompound nbt = NbtIo.readCompressed(inventoryFileDataInput, NbtSizeTracker.ofUnlimitedBytes());
                DefaultedList<ItemStack> inventoryItemStacks = DefaultedList.ofSize(54, ItemStack.EMPTY);
                Inventories.readNbt(nbt, inventoryItemStacks, server.getRegistryManager());
                sharedInventory = new SharedInventory(inventoryItemStacks);
            } catch (Exception e) {
                System.out.println("[ShareEnderChest] Error while loading inventory: " + e);
                sharedInventory = new SharedInventory();
            }
        } else {
            sharedInventory = new SharedInventory();
        }
    }

    public static void saveInventory(MinecraftServer server) {
        File inventoryFile = getFile(server);
        NbtCompound nbt = new NbtCompound();
        DefaultedList<ItemStack> inventoryItemStacks = DefaultedList.ofSize(54, ItemStack.EMPTY);
        Inventories.writeNbt(nbt, sharedInventory.getList(inventoryItemStacks), server.getRegistryManager());
        try (FileOutputStream inventoryFileOutputStream = new FileOutputStream(inventoryFile);
             DataOutputStream inventoryFileDataOutput = new DataOutputStream(inventoryFileOutputStream)) {
            inventoryFile.createNewFile();
            NbtIo.writeCompressed(nbt, inventoryFileDataOutput);
        } catch (Exception e) {
            System.out.println("[ShareEnderChest] Error while saving inventory: " + e);
        }
    }

    public void onServerStopping(MinecraftServer server) {
        saveInventory(server);
    }

    public void onEndTick(MinecraftServer server) {
        if (--ticksUntilSave <= 0L) {
            saveInventory(server);
            ticksUntilSave = AUTOSAVE_TICKS;
        }
    }

    public void onInitialize() {
        System.out.println("ShareEnderChest (Fabric) loaded");
        openSharedInventory = new OpenSharedInventory(UUID.randomUUID());

        UseBlockCallback listenerUseBlock = (player, world, hand, hitResult) -> {
            if (!(world.getBlockState(hitResult.getBlockPos()).getBlock() instanceof EnderChestBlock)) {
                return ActionResult.PASS;
            }

            if (world.isClient()) return ActionResult.SUCCESS;
            if (openSharedEnderChest(player, SharedInvAccessType.BLOCK_USE)) {
                return ActionResult.SUCCESS;
            }
            //EnderChestBlockEntity blockEntity = (EnderChestBlockEntity) world.getBlockEntity(hitResult.getBlockPos());
            //sharedInventory.setBlockEntity(player, blockEntity);
            return ActionResult.PASS;
        };

        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getMainHandStack();
            if (world.isClient()) return TypedActionResult.pass(stack);
            if (!isEnderChest(stack) || world.getServer() == null) {
                return TypedActionResult.pass(stack);
            }
            if (openSharedEnderChest(player, SharedInvAccessType.ITEM_USE)) {
                return TypedActionResult.success(stack);
            }
            return TypedActionResult.pass(stack);
        });

        UseBlockCallback.EVENT.register(listenerUseBlock);
        ServerLifecycleEvents.SERVER_STARTED.register(this);
        ServerLifecycleEvents.SERVER_STOPPING.register(this);
        ServerTickEvents.END_SERVER_TICK.register(this);

        PayloadTypeRegistry.playC2S().register(OpenSharedInventory.PACKET_ID, OpenSharedInventory.PACKET_CODEC);

        // Packet Receiver
        ServerPlayNetworking.registerGlobalReceiver(OpenSharedInventory.PACKET_ID, (payload, context) -> {
            if (context.player().currentScreenHandler != context.player().playerScreenHandler) {
                context.player().networkHandler.sendPacket(new CloseScreenS2CPacket(context.player().currentScreenHandler.syncId));
                context.player().closeHandledScreen();
            }
            openSharedEnderChest(context.player(), SharedInvAccessType.SLOT_USE);
        });
    }

    // Packet Sender
    @Environment(EnvType.CLIENT)
    public static void sendOpenPacket() {
        ClientPlayNetworking.send(openSharedInventory);
    }

    public static boolean openSharedEnderChest(PlayerEntity player, SharedInvAccessType sharedInvAccessType) {
        if (!sharedInvAccessType.shouldPlayerUseItem(player)) {
            return false;
        }
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory((int_1, playerInventory, playerEntity) -> new GenericContainerScreenHandler(ScreenHandlerType.GENERIC_9X6, int_1, playerInventory, sharedInventory,
                6), Text.of("Shared Ender Chest")));
        return true;
    }

    public static boolean isEnderChest(ItemStack stack) {
        Item item = stack.getItem();
        if (!(item instanceof BlockItem)) return false;
        return ((BlockItem) item).getBlock() instanceof EnderChestBlock;
    }

    private static File getFile(MinecraftServer server) {
        return server.getSavePath(WorldSavePath.ROOT).resolve("shareenderchest.sav").toFile();
    }
}
