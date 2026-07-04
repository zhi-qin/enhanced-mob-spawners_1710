package com.branders.spawnermod.render;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.branders.spawnermod.registry.ModRegistry;

public class RenderDebugHarmPotion extends Render {

    private static final ResourceLocation TEXTURE_MAP = TextureMap.locationItemsTexture;

    public RenderDebugHarmPotion() {}

    @Override
    public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTick) {
        ItemStack stack = new ItemStack(ModRegistry.debugHarmPotion);

        IIcon bottleIcon = stack.getItem()
            .getIconFromDamageForRenderPass(stack.getItemDamage(), 0);
        if (bottleIcon == null) return;

        IIcon overlayIcon = stack.getItem()
            .getIconFromDamageForRenderPass(stack.getItemDamage(), 1);

        GL11.glPushMatrix();
        GL11.glTranslatef((float) x, (float) y, (float) z);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        GL11.glScalef(0.5F, 0.5F, 0.5F);

        this.bindTexture(TEXTURE_MAP);
        Tessellator tessellator = Tessellator.instance;

        // Pass 1: Render liquid overlay with potion color (rendered first, behind bottle)
        if (overlayIcon != null) {
            int color = stack.getItem()
                .getColorFromItemStack(stack, 1);
            float r = (color >> 16 & 0xFF) / 255.0F;
            float g = (color >> 8 & 0xFF) / 255.0F;
            float b = (color & 0xFF) / 255.0F;
            GL11.glColor3f(r, g, b);
            renderQuad(tessellator, overlayIcon);
            GL11.glColor3f(1.0F, 1.0F, 1.0F);
        }

        // Pass 0: Render bottle icon in white (on top)
        renderQuad(tessellator, bottleIcon);

        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        GL11.glPopMatrix();
    }

    private void renderQuad(Tessellator tessellator, IIcon icon) {
        float minU = icon.getMinU();
        float maxU = icon.getMaxU();
        float minV = icon.getMinV();
        float maxV = icon.getMaxV();

        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(-0.5D, -0.5D, 0.0D, minU, maxV);
        tessellator.addVertexWithUV(0.5D, -0.5D, 0.0D, maxU, maxV);
        tessellator.addVertexWithUV(0.5D, 0.5D, 0.0D, maxU, minV);
        tessellator.addVertexWithUV(-0.5D, 0.5D, 0.0D, minU, minV);
        tessellator.draw();
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return TEXTURE_MAP;
    }
}
