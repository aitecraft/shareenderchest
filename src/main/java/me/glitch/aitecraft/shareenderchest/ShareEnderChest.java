package me.glitch.aitecraft.shareenderchest;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
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
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.collection.DefaultedList;

public class ShareEnderChest implements ModInitializer, ServerStopping, ServerStarted, EndTick {

	private static SharedInventory sharedInventory;

	private final long AUTOSAVE_TICKS = 20L * 60L * 5L;  // Autosave Every 5 minutes
	private long ticksUntilSave = AUTOSAVE_TICKS;

	private static final Identifier OPEN_SHARED_INV_PACKET = new Identifier("shareenderchest", "open_shared_inventory");

	public void onServerStarted(MinecraftServer server) {
		File inventoryFile = getFile(server);
		if (inventoryFile.exists()) {
			try {
				FileInputStream inventoryFileInputStream = new FileInputStream(inventoryFile);
				DataInputStream inventoryFileDataInput = new DataInputStream(inventoryFileInputStream);
				NbtCompound nbt = NbtIo.readCompressed(inventoryFileDataInput);
				inventoryFileInputStream.close();
				
				DefaultedList<ItemStack> inventoryItemStacks = DefaultedList.ofSize(54, ItemStack.EMPTY);
				Inventories.readNbt(nbt, inventoryItemStacks);
				
				sharedInventory = new SharedInventory(inventoryItemStacks);
			} catch (Exception e) {
				System.out.println("[ShareEnderChest] Error while loading inventory: " + e);
			}
		} else {
			sharedInventory = new SharedInventory();
		}
	}

	public static void saveInventory(MinecraftServer server) {
		File inventoryFile = getFile(server);
		NbtCompound nbt = new NbtCompound();
		DefaultedList<ItemStack> inventoryItemStacks = DefaultedList.ofSize(54, ItemStack.EMPTY);
		Inventories.writeNbt(nbt, sharedInventory.getList(inventoryItemStacks));
		try {
			inventoryFile.createNewFile();
			FileOutputStream inventoryFileOutputStream = new FileOutputStream(inventoryFile);
			DataOutputStream inventoryFileDataOutput = new DataOutputStream(inventoryFileOutputStream);
			NbtIo.writeCompressed(nbt, inventoryFileDataOutput);
			inventoryFileOutputStream.close();
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

		UseBlockCallback listenerUseBlock = (player, world, hand, hitResult) -> {
			
			if (world.getBlockState(hitResult.getBlockPos()).getBlock() instanceof EnderChestBlock) {
				if (player.isSneaking() && !player.isSpectator()) {
					if (world.isClient()) return ActionResult.SUCCESS;
					openSharedEnderChest(player);
					return ActionResult.SUCCESS;

					//EnderChestBlockEntity blockEntity = (EnderChestBlockEntity) world.getBlockEntity(hitResult.getBlockPos());
					//sharedInventory.setBlockEntity(player, blockEntity);
				}
			}
			return ActionResult.PASS;
		};

		UseItemCallback.EVENT.register((player, world, hand) -> {
			ItemStack stack = player.getMainHandStack();
			if (world.isClient()) return TypedActionResult.pass(stack);

			if (isEnderChest(stack) && world.getServer() != null) {
				if ( /*player.isSneaking() &&*/ !player.isSpectator()) {
					openSharedEnderChest(player);
					return TypedActionResult.success(stack);
				}
			}
			
			return TypedActionResult.pass(stack);
		});
		
		UseBlockCallback.EVENT.register(listenerUseBlock);
		ServerLifecycleEvents.SERVER_STARTED.register(this);
		ServerLifecycleEvents.SERVER_STOPPING.register(this);
		ServerTickEvents.END_SERVER_TICK.register(this);

		// Packet Receiver
		ServerPlayNetworking.registerGlobalReceiver(
			OPEN_SHARED_INV_PACKET,
			(server, player, handler, buf, sender) -> server.execute(() -> {
				if (player.currentScreenHandler != player.playerScreenHandler) {
					player.networkHandler.sendPacket(new CloseScreenS2CPacket(player.currentScreenHandler.syncId));
					player.closeScreenHandler();
				}
				openSharedEnderChest(player);
			})
		);
	}

	// Packet Sender
	@Environment(EnvType.CLIENT)
    public static void sendOpenPacket() {
        ClientPlayNetworking.send(OPEN_SHARED_INV_PACKET, PacketByteBufs.empty());
    }

	public static void openSharedEnderChest(PlayerEntity player) {
		player.openHandledScreen(new SimpleNamedScreenHandlerFactory((int_1, playerInventory, playerEntity) -> {
			return new GenericContainerScreenHandler(ScreenHandlerType.GENERIC_9X6, int_1, playerInventory, sharedInventory,
					6);
		}, Text.of("Shared Ender Chest")));
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
