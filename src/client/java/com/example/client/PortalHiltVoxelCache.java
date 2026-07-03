package com.example.client;

import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3fc;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/** Dispatches hilt voxel baking to per-weapon implementations. */
public final class PortalHiltVoxelCache {

    public record HiltBakeResult(
            List<net.minecraft.client.renderer.block.model.BakedQuad> baseQuads,
            List<net.minecraft.client.renderer.block.model.BakedQuad> foilQuads,
            ModelRenderProperties properties,
            Function<ItemStack, RenderType> renderType,
            Supplier<Vector3fc[]> baseExtents,
            Supplier<Vector3fc[]> foilExtents
    ) {}

    private PortalHiltVoxelCache() {}

    public static void bake(PortalToolType toolType, ItemModel.BakingContext ctx, Identifier modelId) {
        switch (toolType) {
            case SWORD, MACE -> PortalSwordHiltVoxelCache.bake(ctx, modelId);
            case SPEAR -> PortalSpearHiltVoxelCache.bake(ctx, modelId);
        }
    }

    public static HiltBakeResult forTexture(PortalToolType toolType, Identifier texture) {
        return switch (toolType) {
            case SWORD, MACE -> PortalSwordHiltVoxelCache.forTexture(texture);
            case SPEAR -> PortalSpearHiltVoxelCache.forTexture(texture);
        };
    }
}
