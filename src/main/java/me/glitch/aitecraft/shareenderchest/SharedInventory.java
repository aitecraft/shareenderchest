package me.glitch.aitecraft.shareenderchest;

import java.util.Iterator;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class SharedInventory implements Container {
    private final NonNullList<ItemStack> stacks;
    //private static final HashMap<PlayerEntity, EnderChestBlockEntity> enderChests = new HashMap<PlayerEntity, EnderChestBlockEntity>();

    public SharedInventory(int inventoryRows) {
        this.stacks = NonNullList.withSize(inventoryRows * 9, ItemStack.EMPTY);
    }

    public SharedInventory(NonNullList<ItemStack> dl) {
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
    
    public NonNullList<ItemStack> getList(NonNullList<ItemStack> dl) {
        dl = stacks;
        return dl;
    }

    @Override
    public int getContainerSize() {
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
    public ItemStack getItem(int i) {
        return i >= stacks.size() ? ItemStack.EMPTY : stacks.get(i);
    }

    @Override
    public ItemStack removeItem(int int_1, int int_2) {
        ItemStack itemStack_1 = ContainerHelper.removeItem(this.stacks, int_1, int_2);
        if (!itemStack_1.isEmpty()) {
            //this.container.onContentChanged(this);
        }

        return itemStack_1;
    }

    @Override
    public ItemStack removeItemNoUpdate(int i) {
        return ContainerHelper.takeItem(this.stacks, i);
    }

    @Override
    public void setItem(int i, ItemStack itemStack) {
        this.stacks.set(i, itemStack);
        //this.container.onContentChanged(this);
    }

    @Override
    public void setChanged() {

    }

    @Override
    public boolean stillValid(Player playerEntity) {
        return true;
    }

    @Override
    public void clearContent() {
        stacks.clear();
    }
}