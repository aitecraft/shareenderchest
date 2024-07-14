package me.glitch.aitecraft.shareenderchest.util;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public final class Compat {

    private Compat() { throw new AssertionError(); }

    public static final RegistryKey<World> CREATIVE_SUPERFLAT_KEY = RegistryKey.of(RegistryKeys.WORLD, Identifier.of("pk_cr_di", "creative_superflat"));

}
