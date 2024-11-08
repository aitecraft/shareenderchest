package me.glitch.aitecraft.shareenderchest;

import java.util.UUID;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class ShareEnderChestClient implements ClientModInitializer {
  private static OpenSharedInventory openSharedInventory;
  
  @Override
  public void onInitializeClient() {
    openSharedInventory = new OpenSharedInventory(UUID.randomUUID());
  }

  public static void sendOpenPacket() {
    ClientPlayNetworking.send(openSharedInventory);
  }
}
