package com.branders.spawnermod.item;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import com.branders.spawnermod.entity.EntityDebugHarmPotion;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ItemDebugHarmPotion extends Item {

    @SideOnly(Side.CLIENT)
    private IIcon bottleIcon;
    @SideOnly(Side.CLIENT)
    private IIcon overlayIcon;

    public ItemDebugHarmPotion() {
        super();
        this.setUnlocalizedName("debug_harm_potion");
        // Texture set via registerIcons, no need for setTextureName
        this.setMaxStackSize(16);
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (!player.capabilities.isCreativeMode) {
            --stack.stackSize;
        }

        world.playSoundAtEntity(player, "random.bow", 0.5F, 0.4F / (itemRand.nextFloat() * 0.4F + 0.8F));

        if (!world.isRemote) {
            world.spawnEntityInWorld(new EntityDebugHarmPotion(world, player));
        }

        return stack;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister register) {
        this.bottleIcon = register.registerIcon("minecraft:potion_bottle_splash");
        this.overlayIcon = register.registerIcon("minecraft:potion_overlay");
        this.itemIcon = this.bottleIcon;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamageForRenderPass(int damage, int pass) {
        return pass == 0 ? this.bottleIcon : this.overlayIcon;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getColorFromItemStack(ItemStack stack, int pass) {
        return pass == 0 ? 0xFFFFFF : 0xDC143C;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean requiresMultipleRenderPasses() {
        return true;
    }
}
