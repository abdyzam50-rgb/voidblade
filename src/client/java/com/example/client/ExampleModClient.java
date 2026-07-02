package com.example.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.UnbakedExtraModel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.resources.Identifier;

public class ExampleModClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		ModelLoadingPlugin.register(pluginCtx -> {
			for (PortalToolType toolType : PortalToolType.values()) {
				Identifier hiltModelId = toolType.hiltModelId();
				Identifier hiltInHandModelId = toolType.hiltInHandModelId();

				pluginCtx.addModel(ExtraModelKey.create(), new UnbakedExtraModel<Object>() {
					@Override public void resolveDependencies(ResolvableModel.Resolver resolver) {
						resolver.markDependency(hiltModelId);
						if (hiltInHandModelId != null) {
							resolver.markDependency(hiltInHandModelId);
						}
					}
					@Override public Object bake(ModelBaker baker) { return null; }
				});
			}

			pluginCtx.modifyItemModelAfterBake().register((original, ctx) -> {
				PortalToolType toolType = PortalToolType.fromItemId(ctx.itemId());
				if (toolType == null) return original;

				ItemModel.BakingContext bakeContext = ctx.bakeContext();

				PortalBladeVoxelCache.bake(toolType, bakeContext, toolType.hiltModelId());
				PortalHiltVoxelCache.bake(toolType, bakeContext, toolType.hiltModelId());

				if (toolType.hiltInHandModelId() != null) {
					PortalBladeVoxelCache.bake(toolType, bakeContext, toolType.hiltInHandModelId());
					PortalHiltVoxelCache.bake(toolType, bakeContext, toolType.hiltInHandModelId());
				}

				return new PortalWeaponItemModel(toolType, original);
			});
		});
	}
}
