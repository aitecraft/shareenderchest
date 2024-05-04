package me.glitch.aitecraft.shareenderchest;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

public record OpenSharedInventory(UUID opened) implements CustomPayload {
    public static final Id<OpenSharedInventory> PACKET_ID = new Id<>(new Identifier("shareenderchest", "open_shared_inventory"));
    public static final PacketCodec<RegistryByteBuf, OpenSharedInventory> PACKET_CODEC = Uuids.PACKET_CODEC.xmap(OpenSharedInventory::new, OpenSharedInventory::opened).cast();

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }

}
