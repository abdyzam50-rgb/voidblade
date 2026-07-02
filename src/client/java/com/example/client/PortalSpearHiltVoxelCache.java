package com.example.client;

import com.google.common.base.Suppliers;
import com.mojang.math.Quadrant;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.SimpleUnbakedGeometry;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.renderer.item.BlockModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Hilt voxel bake for spear sprites (16×16 inventory + 32×32 in-hand).
 * Uses scaled model UVs so high-res in-hand textures map into the 0–16 item cube correctly.
 */
final class PortalSpearHiltVoxelCache {

    private static final float ITEM_MODEL_SIZE = 16f;
    private static final float Z_BACK = 7.5f;
    private static final float Z_FRONT = 8.5f;
    private static final float UV_SHRINK = 0.1f;

    private static final Map<Identifier, PortalHiltVoxelCache.HiltBakeResult> BY_TEXTURE = new HashMap<>();

    private PortalSpearHiltVoxelCache() {}

    static void bake(ItemModel.BakingContext ctx, Identifier modelId) {
        ModelBaker baker = ctx.blockModelBaker();
        ResolvedModel resolved = baker.getModel(modelId);
        TextureSlots textureSlots = resolved.getTopTextureSlots();
        Material material = textureSlots.getMaterial("layer0");
        if (material == null) {
            return;
        }

        Identifier textureId = material.texture();
        ModelDebugName debugName = () -> modelId.toString();
        SpriteContents sprite = baker.sprites().get(material, debugName).contents();
        TextureData data = buildTextureData(textureId, sprite);

        List<BlockElement> baseElements = new ArrayList<>();
        List<BlockElement> foilElements = new ArrayList<>();
        buildFlatElements(data, baseElements, foilElements);
        buildSideElements(data, baseElements, foilElements);

        List<BakedQuad> baseQuads = bakeElements(baseElements, textureSlots, baker, debugName);
        List<BakedQuad> foilQuads = bakeElements(foilElements, textureSlots, baker, debugName);
        ModelRenderProperties properties = ModelRenderProperties.fromResolvedModel(baker, resolved, textureSlots);
        Function<ItemStack, RenderType> renderType = stack -> Sheets.translucentItemSheet();

        BY_TEXTURE.put(textureId, new PortalHiltVoxelCache.HiltBakeResult(
                baseQuads,
                foilQuads,
                properties,
                renderType,
                Suppliers.memoize(() -> BlockModelWrapper.computeExtents(baseQuads)),
                Suppliers.memoize(() -> BlockModelWrapper.computeExtents(foilQuads))
        ));
    }

    static PortalHiltVoxelCache.HiltBakeResult forTexture(Identifier texture) {
        return BY_TEXTURE.get(texture);
    }

    private static List<BakedQuad> bakeElements(
            List<BlockElement> elements,
            TextureSlots textureSlots,
            ModelBaker baker,
            ModelDebugName debugName
    ) {
        if (elements.isEmpty()) {
            return List.of();
        }
        return List.copyOf(SimpleUnbakedGeometry.bake(
                elements,
                textureSlots,
                baker,
                BlockModelRotation.IDENTITY,
                debugName
        ).getAll());
    }

    private static final class TextureData {
        final Identifier texture;
        final int width;
        final int height;
        final boolean[][] portalHole;
        final boolean[][] opaque;
        final boolean[][] suppressSideExtrusion;

        TextureData(
                Identifier texture,
                int width,
                int height,
                boolean[][] portalHole,
                boolean[][] opaque,
                boolean[][] suppressSideExtrusion
        ) {
            this.texture = texture;
            this.width = width;
            this.height = height;
            this.portalHole = portalHole;
            this.opaque = opaque;
            this.suppressSideExtrusion = suppressSideExtrusion;
        }
    }

    private static TextureData buildTextureData(Identifier texture, SpriteContents sprite) {
        int width = sprite.width();
        int height = sprite.height();
        boolean[][] portalHole = new boolean[width][height];
        boolean[][] opaque = new boolean[width][height];

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                opaque[col][row] = !sprite.isTransparent(0, col, row);
                portalHole[col][row] = PortalBladeGeometry.isPortalHoleTexel(texture, sprite, col, row, width, height);
            }
        }

        return new TextureData(
                texture,
                width,
                height,
                portalHole,
                opaque,
                PortalBladeGeometry.spearShaftExtrusionMask(texture, sprite, width, height)
        );
    }

    private static void buildFlatElements(TextureData data, List<BlockElement> baseElements, List<BlockElement> foilElements) {
        float xScale = ITEM_MODEL_SIZE / data.width;
        float yScale = ITEM_MODEL_SIZE / data.height;

        for (int row = 0; row < data.height; row++) {
            for (int col = 0; col < data.width; col++) {
                if (!data.opaque[col][row] || data.portalHole[col][row]) {
                    continue;
                }

                float x0 = col * xScale;
                float x1 = (col + 1) * xScale;
                float y0 = (data.height - 1 - row) * yScale;
                float y1 = (data.height - row) * yScale;
                BlockElementFace.UVs pixelUv = PortalItemModelUv.pixelUvs(col, row, data.width, data.height);
                Map<Direction, BlockElementFace> faces = Map.of(
                        Direction.SOUTH, face(pixelUv),
                        Direction.NORTH, face(pixelUv)
                );
                BlockElement element = new BlockElement(
                        new Vector3f(x0, y0, Z_BACK),
                        new Vector3f(x1, y1, Z_FRONT),
                        faces
                );
                baseElements.add(element);
                foilElements.add(element);
            }
        }
    }

    private static void buildSideElements(TextureData data, List<BlockElement> baseElements, List<BlockElement> foilElements) {
        float xScale = ITEM_MODEL_SIZE / data.width;
        float yScale = ITEM_MODEL_SIZE / data.height;
        Set<SideFace> sideFaces = new HashSet<>();

        for (int row = 0; row < data.height; row++) {
            for (int col = 0; col < data.width; col++) {
                if (!data.opaque[col][row]) {
                    continue;
                }
                checkSideTransition(SideDirection.UP, sideFaces, data, col, row);
                checkSideTransition(SideDirection.DOWN, sideFaces, data, col, row);
                checkSideTransition(SideDirection.LEFT, sideFaces, data, col, row);
                checkSideTransition(SideDirection.RIGHT, sideFaces, data, col, row);
            }
        }

        for (SideFace sideFace : sideFaces) {
            BlockElement side = createSideElement(data, sideFace, xScale, yScale);
            baseElements.add(side);
            if (sideFace.foil()) {
                foilElements.add(side);
            }
        }
    }

    private static void checkSideTransition(
            SideDirection sideDirection,
            Set<SideFace> sideFaces,
            TextureData data,
            int col,
            int row
    ) {
        int neighborCol = col - sideDirection.direction.getStepX();
        int neighborRow = row - sideDirection.direction.getStepY();
        if (!shouldExtrudeSide(data, neighborCol, neighborRow)) {
            return;
        }
        sideFaces.add(new SideFace(sideDirection, col, row, shouldFoilSide(data, neighborCol, neighborRow)));
    }

    /** Match item/generated: extrude toward any transparent neighbor, including portal holes. */
    private static boolean shouldExtrudeSide(TextureData data, int neighborCol, int neighborRow) {
        if (neighborCol < 0 || neighborRow < 0 || neighborCol >= data.width || neighborRow >= data.height) {
            return true;
        }
        return !data.opaque[neighborCol][neighborRow];
    }

    /** Foil only side faces that do not open into the portal void. */
    private static boolean shouldFoilSide(TextureData data, int neighborCol, int neighborRow) {
        if (neighborCol < 0 || neighborRow < 0 || neighborCol >= data.width || neighborRow >= data.height) {
            return true;
        }
        return !data.portalHole[neighborCol][neighborRow];
    }

    private static BlockElement createSideElement(TextureData data, SideFace sideFace, float xScale, float yScale) {
        float col = sideFace.x();
        float row = sideFace.y();
        SideDirection sideDirection = sideFace.facing();
        float u0 = col + UV_SHRINK;
        float u1 = col + 1.0f - UV_SHRINK;
        float v0;
        float v1;
        if (sideDirection.isHorizontal()) {
            v0 = row + UV_SHRINK;
            v1 = row + 1.0f - UV_SHRINK;
        } else {
            v0 = row + 1.0f - UV_SHRINK;
            v1 = row + UV_SHRINK;
        }

        float o = col;
        float p = row;
        float q = col;
        float r = row;
        switch (sideDirection) {
            case UP -> q++;
            case DOWN -> {
                q++;
                p++;
                r++;
            }
            case LEFT -> r++;
            case RIGHT -> {
                o++;
                q++;
                r++;
            }
        }

        o *= xScale;
        q *= xScale;
        p *= yScale;
        r *= yScale;
        p = ITEM_MODEL_SIZE - p;
        r = ITEM_MODEL_SIZE - r;

        Map<Direction, BlockElementFace> faces = Map.of(
                sideDirection.getDirection(),
                face(new BlockElementFace.UVs(u0 * xScale, v0 * yScale, u1 * xScale, v1 * yScale))
        );

        return switch (sideDirection) {
            case UP -> new BlockElement(new Vector3f(o, p, Z_BACK), new Vector3f(q, p, Z_FRONT), faces);
            case DOWN -> new BlockElement(new Vector3f(o, r, Z_BACK), new Vector3f(q, r, Z_FRONT), faces);
            case LEFT -> new BlockElement(new Vector3f(o, p, Z_BACK), new Vector3f(o, r, Z_FRONT), faces);
            case RIGHT -> new BlockElement(new Vector3f(q, p, Z_BACK), new Vector3f(q, r, Z_FRONT), faces);
        };
    }

    private static BlockElementFace face(BlockElementFace.UVs uvs) {
        return new BlockElementFace(null, 0, "layer0", uvs, Quadrant.R0);
    }

    private enum SideDirection {
        UP(Direction.UP),
        DOWN(Direction.DOWN),
        LEFT(Direction.EAST),
        RIGHT(Direction.WEST);

        final Direction direction;

        SideDirection(Direction direction) {
            this.direction = direction;
        }

        Direction getDirection() {
            return direction;
        }

        boolean isHorizontal() {
            return this == DOWN || this == UP;
        }
    }

    private record SideFace(SideDirection facing, int x, int y, boolean foil) {}
}
