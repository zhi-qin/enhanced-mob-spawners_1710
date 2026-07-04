package com.branders.spawnermod.proxy;

import com.branders.spawnermod.entity.EntityDebugHarmPotion;
import com.branders.spawnermod.render.RenderDebugHarmPotion;

import cpw.mods.fml.client.registry.RenderingRegistry;

public class ClientProxy extends CommonProxy {

    @Override
    public void registerRenderers() {
        RenderingRegistry.registerEntityRenderingHandler(EntityDebugHarmPotion.class, new RenderDebugHarmPotion());
    }
}
