package com.corosus.watut;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;

public final class ItemStackNbt {
  private ItemStackNbt() {}

  public static Tag write(ItemStack stack, HolderLookup.Provider registries) {
    return ItemStack.CODEC
        .encodeStart(RegistryOps.create(NbtOps.INSTANCE, registries), stack)
        .getOrThrow();
  }

  public static ItemStack read(CompoundTag data, HolderLookup.Provider registries) {
    return ItemStack.CODEC
        .parse(RegistryOps.create(NbtOps.INSTANCE, registries), data)
        .result()
        .orElse(ItemStack.EMPTY);
  }
}
