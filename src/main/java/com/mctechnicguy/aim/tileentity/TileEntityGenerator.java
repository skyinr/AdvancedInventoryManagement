package com.mctechnicguy.aim.tileentity;

import com.mctechnicguy.aim.AdvancedInventoryManagement;
import com.mctechnicguy.aim.blocks.BlockGenerator;
import com.mctechnicguy.aim.client.render.NetworkInfoOverlayRenderer;
import net.minecraft.item.ItemBucket;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TileEntityGenerator extends TileEntityAIMDevice implements ITickable, IItemHandler{

    public int burnTimeRemaining; //Max. 50000 -> 50 rf/t (default)
    private int lastBurningStage = -1;
    public static final int MAX_BURN_TIME = 50000;


    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    }

    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, EnumFacing facing) {
        try {
            if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return (T) this;
        } catch (ClassCastException x) {
            return super.getCapability(capability, facing);
        }
        return super.getCapability(capability, facing);
    }

    private int getBurningStage() {
        if (burnTimeRemaining > 40000) return 5;
        else if (burnTimeRemaining > 30000) return 4;
        else if (burnTimeRemaining > 20000) return 3;
        else if (burnTimeRemaining > 10000) return 2;
        else if (burnTimeRemaining > 0) return 1;
        else return 0;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        burnTimeRemaining = nbt.getInteger("burnTimeRemaining");
        lastBurningStage = -1;
    }

    @Nonnull
    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        nbt = super.writeToNBT(nbt);
        nbt.setInteger("burnTimeRemaining", burnTimeRemaining);
        return nbt;
    }

    @Override
    public void update() {
        if (hasWorld() && world.isRemote) return;

        if (burnTimeRemaining > 0) {
            outputPower();
            burnTimeRemaining--;
            setNewBlockState();
        }
    }

    public void addBurnTime(int toAdd)  {
        burnTimeRemaining += toAdd;
        if (burnTimeRemaining >= MAX_BURN_TIME) burnTimeRemaining = MAX_BURN_TIME;
    }

    private void setNewBlockState() {
        if (getBurningStage() != lastBurningStage && hasWorld()) {
            world.setBlockState(pos, world.getBlockState(pos).withProperty(BlockGenerator.BURNING_STAGE, getBurningStage()));
            lastBurningStage = getBurningStage();
        }
    }

    private void outputPower() {
        if (this.hasServerCore()) {
            this.getCore().changePower(Math.round(AdvancedInventoryManagement.MAX_GENERATOR_POWER_OUTPUT / 5) * getBurningStage());
        }
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Nonnull
    @Override
    public ItemStack getStackInSlot(int slot) {
        return ItemStack.EMPTY;
    }

    @Nonnull
    @Override
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        if (!TileEntityFurnace.isItemFuel(stack) || stack.getItem() instanceof ItemBucket) return stack;
        int toAccept;
        toAccept = Math.min(stack.getCount(), Math.round(MAX_BURN_TIME - burnTimeRemaining) / TileEntityFurnace.getItemBurnTime(stack));
        if (!simulate) {
            this.addBurnTime(TileEntityFurnace.getItemBurnTime(stack) * toAccept);
        }
        if (toAccept >= stack.getCount()) return ItemStack.EMPTY;
        else stack.shrink(toAccept);
        return stack;
    }

    @Nonnull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 64;
    }

    @Nullable
    @Override
    public NBTTagCompound getTagForOverlayUpdate() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setInteger("burnTime", burnTimeRemaining);
        return nbt;
    }

    @Override
    public void handleTagForOverlayUpdate(NBTTagCompound nbt) {
        super.handleTagForOverlayUpdate(nbt);
        if (nbt.hasKey("burnTime")) {
            this.burnTimeRemaining = nbt.getInteger("burnTime");
            this.hasAccurateServerInfo = true;
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderStatusInformation(NetworkInfoOverlayRenderer renderer) {
        renderer.renderStatusString(hasClientCore() ? getBurningStage() > 0 ? "aimoverlay.generatorstatus.active" : "aimoverlay.generatorstatus.idle" : "aimoverlay.generatorstatus.offline",
                hasClientCore() ? getBurningStage() > 0 ? TextFormatting.GREEN : TextFormatting.YELLOW : TextFormatting.RED);
        renderer.renderTileValues("burntime", TextFormatting.GREEN, !hasAccurateServerInfo, (int)(burnTimeRemaining / 20));
        renderer.renderTileValues("poweroutput", TextFormatting.GREEN, !hasAccurateServerInfo, hasClientCore() ? Math.round(AdvancedInventoryManagement.MAX_GENERATOR_POWER_OUTPUT / 5) * getBurningStage() : 0);
    }
}
