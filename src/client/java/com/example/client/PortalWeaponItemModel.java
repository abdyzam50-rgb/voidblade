package com.example.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.item.BlockModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class PortalWeaponItemModel implements ItemModel {

    private final PortalToolType toolType;
    private final ItemModel vanillaModel;

    public PortalWeaponItemModel(PortalToolType toolType, ItemModel vanillaModel) {
        this.toolType = toolType;
        this.vanillaModel = vanillaModel;
    }

    @Override
    public void update(ItemStackRenderState state, ItemStack stack, ItemModelResolver resolver,
                       ItemDisplayContext displayContext, ClientLevel level, ItemOwner owner, int seed) {
        if (!PortalVoidGate.shouldUsePortalRender(stack, owner, displayContext)) {
            vanillaModel.update(state, stack, resolver, displayContext, level, owner, seed);
            return;
        }

        Identifier texture = toolType.textureForContext(displayContext);
        PortalHiltVoxelCache.HiltBakeResult hilt = PortalHiltVoxelCache.forTexture(toolType, texture);
        if (hilt == null || hilt.baseQuads().isEmpty()) {
            vanillaModel.update(state, stack, resolver, displayContext, level, owner, seed);
            return;
        }

        state.appendModelIdentityElement(this);
        state.appendModelIdentityElement(texture);

        ItemStackRenderState.LayerRenderState baseLayer = state.newLayer();
        hilt.properties().applyToLayer(baseLayer, displayContext);
        baseLayer.setExtents(hilt.baseExtents());
        baseLayer.setRenderType(hilt.renderType().apply(stack));
        baseLayer.setFoilType(ItemStackRenderState.FoilType.NONE);
        baseLayer.prepareTintLayers(0);
        baseLayer.prepareQuadList().addAll(hilt.baseQuads());

        if (stack.hasFoil() && !hilt.foilQuads().isEmpty()) {
            state.setAnimated();
            ItemStackRenderState.LayerRenderState foilLayer = state.newLayer();
            hilt.properties().applyToLayer(foilLayer, displayContext);
            foilLayer.setExtents(hilt.foilExtents());
            foilLayer.setRenderType(hilt.renderType().apply(stack));
            foilLayer.setFoilType(ItemStackRenderState.FoilType.STANDARD);
            foilLayer.prepareTintLayers(0);
            foilLayer.prepareQuadList().addAll(hilt.foilQuads());
        }

        List<BakedQuad> portalQuads = PortalBladeVoxelCache.quadsFor(texture);
        if (!portalQuads.isEmpty()) {
            ItemStackRenderState.LayerRenderState portalLayer = state.newLayer();
            portalLayer.setFoilType(ItemStackRenderState.FoilType.NONE);
            hilt.properties().applyToLayer(portalLayer, displayContext);
            portalLayer.setRenderType(RenderTypes.endPortal());
            portalLayer.setExtents(() -> BlockModelWrapper.computeExtents(portalQuads));
            portalLayer.prepareTintLayers(0);
            portalLayer.prepareQuadList().addAll(portalQuads);
        }
    }
}
