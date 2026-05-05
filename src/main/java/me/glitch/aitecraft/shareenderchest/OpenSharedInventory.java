package me.glitch.aitecraft.shareenderchest;

import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record OpenSharedInventory(UUID opened) implements CustomPacketPayload {
    public static final Type<OpenSharedInventory> PACKET_ID = new Type<>(Identifier.fromNamespaceAndPath("shareenderchest", "open_shared_inventory"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenSharedInventory> PACKET_CODEC = UUIDUtil.STREAM_CODEC.map(OpenSharedInventory::new, OpenSharedInventory::opened).cast();

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PACKET_ID;
    }

}
