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
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.storage.LevelResource;

public class ShareEnderChest implements ModInitializer, ServerStopping, ServerStarted, EndTick {

    private static SharedInventory sharedInventory;

    private long ticksUntilSave;
    private static Config config;

    public void onServerStarted(MinecraftServer server) {
        File inventoryFile = getFile(server);
        if (inventoryFile.exists()) {
            try (FileInputStream inventoryFileInputStream = new FileInputStream(inventoryFile);
                 DataInputStream inventoryFileDataInput = new DataInputStream(inventoryFileInputStream)) {
                CompoundTag nbt = NbtIo.readCompressed(inventoryFileDataInput, NbtAccounter.unlimitedHeap());
                NonNullList<ItemStack> inventoryItemStacks = NonNullList.withSize(config.inventoryRows * 9, ItemStack.EMPTY);
                ContainerHelper.loadAllItems(nbt, inventoryItemStacks, server.registryAccess());
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
        CompoundTag nbt = new CompoundTag();
        NonNullList<ItemStack> inventoryItemStacks = NonNullList.withSize(config.inventoryRows * 9, ItemStack.EMPTY);
        ContainerHelper.saveAllItems(nbt, sharedInventory.getList(inventoryItemStacks), server.registryAccess());
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
                if ((!config.requireSneak || player.isShiftKeyDown()) && !player.isSpectator()) {
                    if (world.isClientSide()) return InteractionResult.SUCCESS;
                    playEnderChestOpenSound(world, hitResult.getBlockPos());
                    openSharedEnderChest(player);
                    return InteractionResult.SUCCESS;

                    //EnderChestBlockEntity blockEntity = (EnderChestBlockEntity) world.getBlockEntity(hitResult.getBlockPos());
                    //sharedInventory.setBlockEntity(player, blockEntity);
                }
            }
            return InteractionResult.PASS;
        };

        if (config.openFromHand) {
            UseItemCallback.EVENT.register((player, world, hand) -> {
                if (world.isClientSide()) return InteractionResult.PASS;
                
                ItemStack stack = player.getMainHandItem();
                if (isEnderChest(stack) && world.getServer() != null) {
                    if ( /*player.isSneaking() &&*/ !player.isSpectator()) {
                        playEnderChestOpenSound(world, player.blockPosition());
                        openSharedEnderChest(player);
                        return InteractionResult.SUCCESS;
                    }
                }

                return InteractionResult.PASS;
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
                if (context.player().containerMenu != context.player().inventoryMenu) {
                    context.player().connection.send(new ClientboundContainerClosePacket(context.player().containerMenu.containerId));
                    context.player().closeContainer();
                }
                openSharedEnderChest(context.player());
            });
        }
    }

    public static void openSharedEnderChest(Player player) {
        player.openMenu(new SimpleMenuProvider((int_1, playerInventory, playerEntity) ->
                new ChestMenu(config.screenHandlerType(), int_1, playerInventory, sharedInventory, config.inventoryRows), Component.nullToEmpty(config.inventoryName)));
    }

    public static void playEnderChestOpenSound(Level world, BlockPos pos) {
        if (config.playOpenSound)
            world.playSound(null, pos, SoundEvents.ENDER_CHEST_OPEN, SoundSource.BLOCKS, 0.5F, world.random.nextFloat() * 0.1F + 0.9F);
    }

    public static boolean isEnderChest(ItemStack stack) {
        Item item = stack.getItem();
        if (!(item instanceof BlockItem)) return false;
        return ((BlockItem) item).getBlock() instanceof EnderChestBlock;
    }

    private static File getFile(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("shareenderchest.sav").toFile();
    }
}
