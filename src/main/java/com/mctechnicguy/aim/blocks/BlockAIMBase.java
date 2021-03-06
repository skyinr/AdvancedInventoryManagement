package com.mctechnicguy.aim.blocks;

import com.mctechnicguy.aim.AdvancedInventoryManagement;
import com.mctechnicguy.aim.gui.IManualEntry;
import com.mctechnicguy.aim.items.ItemAIMInfoProvider;
import com.mctechnicguy.aim.items.ItemAIMManual;
import com.mctechnicguy.aim.tileentity.TileEntityAIMDevice;
import com.mctechnicguy.aim.util.AIMUtils;
import com.mctechnicguy.aim.util.IWrenchDestroyable;
import com.mctechnicguy.aim.util.NetworkUtils;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BlockAIMBase extends Block implements ITileEntityProvider, IWrenchDestroyable, IManualEntry {
	
	public BlockAIMBase(@Nonnull String bname) {
		super(Material.IRON);
		this.setHardness(3.5F);
		this.setSoundType(SoundType.STONE);
		this.setResistance(2000F);
		this.setCreativeTab(AdvancedInventoryManagement.AIMTab);
		this.setHarvestLevel("pickaxe", 0);
		this.setUnlocalizedName(bname);
		this.setRegistryName(bname);
	}

	@Override
	public final boolean onBlockActivated(@Nonnull World world, BlockPos pos, IBlockState state, @Nullable EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ)
    {
		return onBlockActivated(world, pos, state, player, hand, side, world.getTileEntity(pos), player.getHeldItem(hand)) == EnumRightClickResult.ACTION_DONE;
    }

    protected EnumRightClickResult onBlockActivated(@Nonnull World world, BlockPos pos, IBlockState state, @Nullable EntityPlayer player, EnumHand hand, EnumFacing side, @Nullable TileEntity tileEntity, ItemStack heldItem) {
	    if (player == null) return EnumRightClickResult.ACTION_DISABLED;
	    if (heldItem.getItem() instanceof ItemAIMInfoProvider) return EnumRightClickResult.ACTION_DISABLED;
	    if (heldItem.getItem() instanceof ItemAIMManual) return EnumRightClickResult.ACTION_DISABLED;
        if (player.isSneaking() && AIMUtils.isWrench(heldItem)) {
            if (!world.isRemote) this.destroyWithWrench(player, world, pos, heldItem);
            return EnumRightClickResult.ACTION_DONE;
        }
	    return EnumRightClickResult.ACTION_PASS;
    }


	@Override
	public void onBlockPlacedBy(@Nonnull World worldIn, @Nonnull BlockPos pos, @Nonnull IBlockState state, EntityLivingBase placer, @Nonnull ItemStack stack)
    {
		if (!NetworkUtils.canPlaceAIMBlock(placer, worldIn, pos)) {
			this.breakBlock(worldIn, pos, state);
			if (placer instanceof EntityPlayer) {
				this.harvestBlock(worldIn, (EntityPlayer) placer, pos, state, worldIn.getTileEntity(pos), stack);
				this.removedByPlayer(state, worldIn, pos, (EntityPlayer)placer, true);
			}
		}
	}


	@Override
	public EnumBlockRenderType getRenderType(IBlockState state) {
		return EnumBlockRenderType.MODEL;
	}

	@Nullable
	@Override
	public TileEntity createNewTileEntity(World worldIn, int meta) {
		return null;
	}

	@Override
	public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player, boolean willHarvest) {
		if (((TileEntityAIMDevice) world.getTileEntity(pos)).isPlayerAccessAllowed(player))
			return super.removedByPlayer(state, world, pos, player, willHarvest);
		else {
			world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
			return false;
		}

	}

	boolean removedByPlayerSupercall(@Nonnull IBlockState state, @Nonnull World world, @Nonnull BlockPos pos, @Nonnull EntityPlayer player, boolean willHarvest) {
		return super.removedByPlayer(state, world, pos, player, willHarvest);
	}

	public void destroyWithWrench(@Nonnull EntityPlayer player, @Nonnull World world, @Nonnull BlockPos pos, @Nonnull ItemStack item) {
		if (world.getTileEntity(pos) instanceof TileEntityAIMDevice) {
			if (((TileEntityAIMDevice) world.getTileEntity(pos)).isPlayerAccessAllowed(player)) {
				this.breakBlock(world, pos, world.getBlockState(pos));
				this.harvestBlock(world, player, pos, world.getBlockState(pos), world.getTileEntity(pos), item);
				this.removedByPlayer(world.getBlockState(pos), world, pos, player, true);
			}	
		}
	}
	
	@Override
	 public void breakBlock(World world, BlockPos pos, IBlockState state)
	{
		if (!world.isRemote && world.getTileEntity(pos) instanceof TileEntityAIMDevice && ((TileEntityAIMDevice)world.getTileEntity(pos)).hasServerCore())
			((TileEntityAIMDevice)world.getTileEntity(pos)).getCore().forceNetworkUpdate(5);
		super.breakBlock(world, pos, state);
	}

	@Nonnull
	@Override
	public String getManualName() {
		return getRegistryName().getResourcePath();
	}

	@Override
	public int getPageCount() {
		return 1;
	}

	@Nonnull
	@Override
	public Object[] getParams(int page) {
		return new Object[0];
	}

	protected enum EnumRightClickResult {
	    ACTION_DONE,
        ACTION_DISABLED,
        ACTION_PASS;
    }

}
