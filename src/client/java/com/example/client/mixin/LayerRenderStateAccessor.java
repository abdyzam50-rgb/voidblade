package com.example.client.mixin;

import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemStackRenderState.LayerRenderState.class)
public interface LayerRenderStateAccessor {
    @Accessor("transform")
    ItemTransform getTransform();

    @Accessor("renderType")
    @Nullable RenderType getRenderType();

    @Accessor("renderType")
    void setRenderType(@Nullable RenderType renderType);

    @Accessor("usesBlockLight")
    boolean getUsesBlockLight();

    @Accessor("usesBlockLight")
    void setUsesBlockLight(boolean usesBlockLight);
}
