package me.glitch.aitecraft.shareenderchest;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

import java.util.Iterator;

public class SharedInventory implements Inventory {
    public static final int SECTION_SIZE = 54;
    private final DefaultedList<ItemStack> stacks;
    //private static final HashMap<PlayerEntity, EnderChestBlockEntity> enderChests = new HashMap<PlayerEntity, EnderChestBlockEntity>();

    public SharedInventory() {
        this.stacks = DefaultedList.ofSize(SECTION_SIZE, ItemStack.EMPTY);
    }

    public SharedInventory(DefaultedList<ItemStack> dl) {
        this.stacks = dl;
    }

    /*
    @Override
    public void onClose(PlayerEntity player) {
        Inventory.super.onClose(player);
        EnderChestBlockEntity blockEntity = enderChests.remove(player);
        if (blockEntity != null)
            blockEntity.onClose(player);
    }

    @Override
    public void onOpen(PlayerEntity player) {
        Inventory.super.onOpen(player);
        EnderChestBlockEntity blockEntity = enderChests.get(player);
        if (blockEntity != null)
            blockEntity.onOpen(player);
    }
    
    public void setBlockEntity(PlayerEntity player, EnderChestBlockEntity be) {
        enderChests.put(player, be);
    }
    */
    
    public DefaultedList<ItemStack> getList(DefaultedList<ItemStack> dl) {
        dl = stacks;
        return dl;
    }

    @Override
    public int size() {
        return stacks.size();
    }

    @Override
    public boolean isEmpty() {
        Iterator<ItemStack> var1 = this.stacks.iterator();

        ItemStack itemStack_1;
        do {
            if (!var1.hasNext()) {
                return true;
            }

            itemStack_1 = (ItemStack)var1.next();
        } while(itemStack_1.isEmpty());

        return false;
    }

    @Override
    public ItemStack getStack(int i) {
        return i >= stacks.size() ? ItemStack.EMPTY : stacks.get(i);
    }

    @Override
    public ItemStack removeStack(int int_1, int int_2) {
        ItemStack itemStack_1 = Inventories.splitStack(this.stacks, int_1, int_2);
        if (!itemStack_1.isEmpty()) {
            //this.container.onContentChanged(this);
        }

        return itemStack_1;
    }

    @Override
    public ItemStack removeStack(int i) {
        return Inventories.removeStack(this.stacks, i);
    }

    @Override
    public void setStack(int i, ItemStack itemStack) {
        this.stacks.set(i, itemStack);
        //this.container.onContentChanged(this);
    }

    @Override
    public void markDirty() {

    }

    @Override
    public boolean canPlayerUse(PlayerEntity playerEntity) {
        return true;
    }

    @Override
    public void clear() {
        stacks.clear();
    }
}