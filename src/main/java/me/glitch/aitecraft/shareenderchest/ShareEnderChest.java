package me.glitch.aitecraft.shareenderchest;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import me.glitch.aitecraft.shareenderchest.config.Config;
import me.glitch.aitecraft.shareenderchest.config.ConfigManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
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
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ShareEnderChest implements ModInitializer, ServerStopping, ServerStarted, EndTick {

    private static SharedInventory sharedInventory;

    private long ticksUntilSave;
    private static Config config;

    public void onServerStarted(MinecraftServer server) {
        File inventoryFile = getFile(server);
        if (inventoryFile.exists()) {
            try (FileInputStream inventoryFileInputStream = new FileInputStream(inventoryFile);
                 DataInputStream inventoryFileDataInput = new DataInputStream(inventoryFileInputStream)) {
                NbtCompound nbt = NbtIo.readCompressed(inventoryFileDataInput, NbtSizeTracker.ofUnlimitedBytes());
                DefaultedList<ItemStack> inventoryItemStacks = DefaultedList.ofSize(config.inventoryRows * 9, ItemStack.EMPTY);
                Inventories.readNbt(nbt, inventoryItemStacks, server.getRegistryManager());
                sharedInventory = new SharedInventory(inventoryItemStacks);
            } catch (Exception e) {
                System.out.println("[ShareEnderChest] Error while loading inventory: " + e);
                sharedInventory = new SharedInventory(config.inventoryRows);
            }
        } else {
            sharedInventory = new SharedInventory(config.inventoryRows);
        }
    }

    public static void saveInventory(MinecraftServer server) {
        File inventoryFile = getFile(server);
        NbtCompound nbt = new NbtCompound();
        DefaultedList<ItemStack> inventoryItemStacks = DefaultedList.ofSize(config.inventoryRows * 9, ItemStack.EMPTY);
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
        if (config != null && --ticksUntilSave <= 0L) {
            saveInventory(server);
            ticksUntilSave = config.autosaveSeconds * 20L;
        }
    }

    public void onInitialize() {
        config = ConfigManager.load();
        ticksUntilSave = config.autosaveSeconds * 20L;
        System.out.println("ShareEnderChest (Fabric) loaded");

        UseBlockCallback listenerUseBlock = (player, world, hand, hitResult) -> {

            if (world.getBlockState(hitResult.getBlockPos()).getBlock() instanceof EnderChestBlock) {
                // player.isSneaking matters only if requireSneak is true
                if ((!config.requireSneak || player.isSneaking()) && !player.isSpectator()) {
                    if (world.isClient()) return ActionResult.SUCCESS;
                    playEnderChestOpenSound(world, hitResult.getBlockPos());
                    openSharedEnderChest(player);
                    return ActionResult.SUCCESS;

                    //EnderChestBlockEntity blockEntity = (EnderChestBlockEntity) world.getBlockEntity(hitResult.getBlockPos());
                    //sharedInventory.setBlockEntity(player, blockEntity);
                }
            }
            return ActionResult.PASS;
        };

        if (config.openFromHand) {
            UseItemCallback.EVENT.register((player, world, hand) -> {
                if (world.isClient()) return ActionResult.PASS;
                
                ItemStack stack = player.getMainHandStack();
                if (isEnderChest(stack) && world.getServer() != null) {
                    if ( /*player.isSneaking() &&*/ !player.isSpectator()) {
                        playEnderChestOpenSound(world, player.getBlockPos());
                        openSharedEnderChest(player);
                        return ActionResult.SUCCESS;
                    }
                }

                return ActionResult.PASS;
            });
        }

        UseBlockCallback.EVENT.register(listenerUseBlock);
        ServerLifecycleEvents.SERVER_STARTED.register(this);
        ServerLifecycleEvents.SERVER_STOPPING.register(this);
        ServerTickEvents.END_SERVER_TICK.register(this);

        PayloadTypeRegistry.playC2S().register(OpenSharedInventory.PACKET_ID, OpenSharedInventory.PACKET_CODEC);

        if (config.openFromInventory) {
            // Packet Receiver
            ServerPlayNetworking.registerGlobalReceiver(OpenSharedInventory.PACKET_ID, (payload, context) -> {
                if (context.player().currentScreenHandler != context.player().playerScreenHandler) {
                    context.player().networkHandler.sendPacket(new CloseScreenS2CPacket(context.player().currentScreenHandler.syncId));
                    context.player().closeHandledScreen();
                }
                openSharedEnderChest(context.player());
            });
        }
    }

    public static void openSharedEnderChest(PlayerEntity player) {
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory((int_1, playerInventory, playerEntity) ->
                new GenericContainerScreenHandler(config.screenHandlerType(), int_1, playerInventory, sharedInventory, config.inventoryRows), Text.of(config.inventoryName)));
    }

    public static void playEnderChestOpenSound(World world, BlockPos pos) {
        if (config.playOpenSound)
            world.playSound(null, pos, SoundEvents.BLOCK_ENDER_CHEST_OPEN, SoundCategory.BLOCKS, 0.5F, world.random.nextFloat() * 0.1F + 0.9F);
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
