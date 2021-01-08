package me.gammadelta.common.block;

import me.gammadelta.common.block.tile.ContainerPuncher;
import me.gammadelta.common.block.tile.TilePuncher;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nullable;

/**
 * a block that *is* a puncher, not something that punches blocks.
 */
public class BlockPuncher extends Block {
    public static final String NAME = "puncher";

    public BlockPuncher() {
        super(Properties.create(Material.WOOD).hardnessAndResistance(1.5f).harvestTool(ToolType.AXE).sound(
                SoundType.WOOD));
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new TilePuncher();
    }

    @SuppressWarnings("deprecation")
    @Override
    public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player,
            Hand handIn, BlockRayTraceResult hit) {
        if (!world.isRemote) {
            TileEntity tileEntity = world.getTileEntity(pos);
            if (tileEntity instanceof TilePuncher) {
                INamedContainerProvider containerProvider = new INamedContainerProvider() {
                    @Override
                    public ITextComponent getDisplayName() {
                        return new TranslationTextComponent(VCCBlocks.PUNCHER_BLOCK.get().getTranslationKey());
                    }

                    @Override
                    public Container createMenu(int i, PlayerInventory playerInventory, PlayerEntity playerEntity) {
                        return new ContainerPuncher(i, world, pos, playerInventory, playerEntity);
                    }
                };
                NetworkHooks.openGui((ServerPlayerEntity) player, containerProvider, tileEntity.getPos());
            } else {
                throw new IllegalStateException("Our named container provider is missing!");
            }
        }
        return ActionResultType.SUCCESS;
    }

    // Make the items inside drop when it is broken
    @Override
    public void onReplaced(BlockState state, World worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.isIn(newState.getBlock())) {
            TileEntity te = worldIn.getTileEntity(pos);
            if (te instanceof TilePuncher) {
                TilePuncher puncher = (TilePuncher) te;
                // Spawn items
                // i think isMoving is if it's moved by a piston?
                // don't want to spawn items on being pushed
                if (!isMoving) {
                    puncher.itemHandler.ifPresent(handler -> {
                        for (int i = 0; i < handler.getSlots(); i++) {
                            InventoryHelper.spawnItemStack(worldIn, pos.getX(), pos.getY(), pos.getZ(),
                                    handler.getStackInSlot(i));
                        }
                    });
                }

                worldIn.updateComparatorOutputLevel(pos, this);
            }

            super.onReplaced(state, worldIn, pos, newState, isMoving);
        }
    }
}
