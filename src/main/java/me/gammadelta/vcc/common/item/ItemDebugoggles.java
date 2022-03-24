package me.gammadelta.vcc.common.item;

import me.gammadelta.vcc.client.model.DebugogglesModel;
import me.gammadelta.vcc.common.VCCConfig;
import me.gammadelta.vcc.common.block.BlockMotherboard;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.IArmorMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

public class ItemDebugoggles extends ArmorItem {
    public static final String NAME = "debugoggles";

    public static final String MOTHERBOARD_POS_KEY = "motherboard_pos";
    public static final String CPU_POS_KEY = "cpu_pos";

    public ItemDebugoggles() {
        super(new Material(), EquipmentSlotType.HEAD, new Properties().maxStackSize(1).group(VCCItems.VCC_ITEM_GROUP));
    }

    @Override
    public void onArmorTick(ItemStack stack, World world, PlayerEntity player) {
        CompoundNBT tag = stack.getOrCreateTag();
        if (tag.contains(MOTHERBOARD_POS_KEY)) {
            // Check if out of bounds
            BlockPos motherboardPos = NBTUtil.readBlockPos(tag.getCompound(MOTHERBOARD_POS_KEY));
            double radius = VCCConfig.DEBUGOGGLES_DEACTIVATE_RADIUS.get();
            if (motherboardPos.distanceSq(
                    new Vector3i(player.getPosX(), player.getPosY(), player.getPosZ())) >= radius * radius) {
                tag.remove(MOTHERBOARD_POS_KEY);
                tag.remove(CPU_POS_KEY);
            }
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected) {
        super.inventoryTick(stack, worldIn, entityIn, itemSlot, isSelected);

        if (!(entityIn instanceof PlayerEntity) || itemSlot != 103) {
            // This is not being worn by a player or it's not in their head slot.
            CompoundNBT tag = stack.getTag();
            if (tag != null) {
                tag.remove(MOTHERBOARD_POS_KEY);
                tag.remove(CPU_POS_KEY);
            }
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip,
            ITooltipFlag flagIn) {
        CompoundNBT tag = stack.getOrCreateTag();
        if (tag.contains(MOTHERBOARD_POS_KEY)) {
            BlockPos pos = NBTUtil.readBlockPos(tag.getCompound(MOTHERBOARD_POS_KEY));
            tooltip.add(new TranslationTextComponent("item.vcc.debugoggles.motherboard", pos.getX(), pos.getY(),
                    pos.getZ()));
            if (tag.contains(CPU_POS_KEY)) {
                BlockPos cpuPos = NBTUtil.readBlockPos(tag.getCompound(MOTHERBOARD_POS_KEY));
                tooltip.add(new TranslationTextComponent("item.vcc.debugoggles.cpu", cpuPos.getX(), cpuPos.getY(),
                        cpuPos.getZ()));
            }
        } else {
            tooltip.add(new TranslationTextComponent("item.vcc.debugoggles.motherboardnt"));
        }
    }

    @Override
    public boolean canEquip(ItemStack stack, EquipmentSlotType armorType, Entity entity) {
        return armorType == EquipmentSlotType.HEAD;
    }

    @Nullable
    @Override
    @OnlyIn(Dist.CLIENT)
    public BipedModel getArmorModel(LivingEntity entityLiving, ItemStack itemStack,
            EquipmentSlotType armorSlot, BipedModel _default) {
        return DebugogglesModel.getInstance();
    }

    @Nullable
    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlotType slot, String type) {
        return "vcc:textures/armor/debugoggles.png";
    }

    public static class Material implements IArmorMaterial {
        @Override
        public int getDurability(EquipmentSlotType slotIn) {
            return 200;
        }

        @Override
        public int getDamageReductionAmount(EquipmentSlotType slotIn) {
            return 1;
        }

        @Override
        public int getEnchantability() {
            return 0;
        }

        @Override
        public SoundEvent getSoundEvent() {
            return null;
        }

        @Override
        public Ingredient getRepairMaterial() {
            return Ingredient.fromItems(Items.LEATHER);
        }

        @Override
        public String getName() {
            return "debugoggles";
        }

        @Override
        public float getToughness() {
            return 0;
        }

        @Override
        public float getKnockbackResistance() {
            return 0;
        }
    }
}
