package com.branders.spawnermod.entity;

import java.util.List;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class EntityDebugHarmPotion extends EntityThrowable {

    public EntityDebugHarmPotion(World world) {
        super(world);
    }

    public EntityDebugHarmPotion(World world, EntityLivingBase thrower) {
        super(world, thrower);
    }

    public EntityDebugHarmPotion(World world, double x, double y, double z) {
        super(world, x, y, z);
    }

    @Override
    protected void onImpact(MovingObjectPosition mop) {
        if (this.worldObj.isRemote) return;

        // 20x vanilla splash radius = 80 blocks
        double radius = 80.0;
        AxisAlignedBB aabb = AxisAlignedBB.getBoundingBox(
            this.posX - radius,
            this.posY - radius,
            this.posZ - radius,
            this.posX + radius,
            this.posY + radius,
            this.posZ + radius);

        List<EntityLivingBase> entities = this.worldObj.getEntitiesWithinAABB(EntityLivingBase.class, aabb);

        for (EntityLivingBase entity : entities) {
            double distSq = this.getDistanceSqToEntity(entity);
            if (distSq < radius * radius) {
                // Scale damage by distance (closer = more damage)
                double factor = 1.0 - Math.sqrt(distSq) / radius;
                int damage = (int) (300.0 * factor); // 3 * 100 = 300 base
                if (damage > 0) {
                    entity.attackEntityFrom(DamageSource.magic, (float) damage);
                }
            }
        }

        // Splash potion particles (harming potion color: purple)
        this.worldObj.playAuxSFX(
            2002,
            (int) Math.round(this.posX),
            (int) Math.round(this.posY),
            (int) Math.round(this.posZ),
            0x9C27B0);
        this.worldObj.playSoundEffect(this.posX, this.posY, this.posZ, "random.glass", 1.0F, 1.0F);

        this.setDead();
    }
}
