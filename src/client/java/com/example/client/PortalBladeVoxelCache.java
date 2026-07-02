package com.example.client;

import com.mojang.math.Quadrant;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.SimpleUnbakedGeometry;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pre-bakes portal-blade voxels from transparent texels inside the blade map.
 * Uses BlockElement baking (same path as item/generated) so side-face winding is correct.
 */
public final class PortalBladeVoxelCache {

    private static final float ITEM_MODEL_SIZE = 16f;
    private static final float Z_BACK = 7.5f;
    private static final float Z_FRONT = 8.5f;

    private static final class TextureData {
        final int width;
        final int height;
        final boolean[][] portalHole;
        final boolean[][] opaque;
        final boolean[][] suppressSideExtrusion;
        final float portalMinU;
        final float portalMaxU;
        final float portalMinV;
        final float portalMaxV;
        final boolean hasPortalBounds;

        TextureData(
                int width,
                int height,
                boolean[][] portalHole,
                boolean[][] opaque,
                boolean[][] suppressSideExtrusion,
                float portalMinU,
                float portalMaxU,
                float portalMinV,
                float portalMaxV,
                boolean hasPortalBounds
        ) {
            this.width = width;
            this.height = height;
            this.portalHole = portalHole;
            this.opaque = opaque;
            this.suppressSideExtrusion = suppressSideExtrusion;
            this.portalMinU = portalMinU;
            this.portalMaxU = portalMaxU;
            this.portalMinV = portalMinV;
            this.portalMaxV = portalMaxV;
            this.hasPortalBounds = hasPortalBounds;
        }
    }

    private static final Map<Identifier, List<BakedQuad>> PORTAL_QUADS_BY_TEXTURE = new HashMap<>();
    private static final Map<Identifier, TextureData> DATA_BY_TEXTURE = new HashMap<>();

    private PortalBladeVoxelCache() {}

    public static void bake(PortalToolType toolType, ItemModel.BakingContext ctx, Identifier modelId) {
        ModelBaker baker = ctx.blockModelBaker();
        ResolvedModel resolved = baker.getModel(modelId);
        TextureSlots textureSlots = resolved.getTopTextureSlots();
        Material material = textureSlots.getMaterial("layer0");
        if (material == null) return;

        Identifier textureId = material.texture();
        ModelDebugName debugName = () -> modelId.toString();
        SpriteContents sprite = baker.sprites().get(material, debugName).contents();
        TextureData data = buildTextureData(textureId, sprite);
        DATA_BY_TEXTURE.put(textureId, data);

        List<BlockElement> elements = buildElements(toolType, data);
        if (elements.isEmpty()) {
            PORTAL_QUADS_BY_TEXTURE.put(textureId, List.of());
            return;
        }

        List<BakedQuad> quads = SimpleUnbakedGeometry.bake(
                elements,
                textureSlots,
                baker,
                BlockModelRotation.IDENTITY,
                debugName
        ).getAll();

        PORTAL_QUADS_BY_TEXTURE.put(textureId, List.copyOf(quads));
    }

    public static List<BakedQuad> quadsFor(Identifier texture) {
        return PORTAL_QUADS_BY_TEXTURE.getOrDefault(texture, List.of());
    }

    public static boolean isPortalHole(Identifier texture, float u, float v) {
        TextureData data = DATA_BY_TEXTURE.get(texture);
        if (data == null) return false;
        int col = Math.min(data.width - 1, Math.max(0, (int) (u * data.width)));
        int row = Math.min(data.height - 1, Math.max(0, (int) ((1f - v) * data.height)));
        return data.portalHole[col][row];
    }

    public static boolean overlapsPortalHoleRegion(
            Identifier texture,
            float minU,
            float maxU,
            float minV,
            float maxV
    ) {
        TextureData data = DATA_BY_TEXTURE.get(texture);
        if (data == null || !data.hasPortalBounds) {
            return false;
        }
        return minU < data.portalMaxU && maxU > data.portalMinU
                && minV < data.portalMaxV && maxV > data.portalMinV;
    }

    public static boolean isSideExtrusionSuppressed(Identifier texture, float u, float v) {
        TextureData data = DATA_BY_TEXTURE.get(texture);
        if (data == null) return false;
        int col = Math.min(data.width - 1, Math.max(0, (int) (u * data.width)));
        int row = Math.min(data.height - 1, Math.max(0, (int) ((1f - v) * data.height)));
        return data.suppressSideExtrusion[col][row];
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

        float[] portalBounds = portalHoleUvBounds(portalHole, width, height);
        return new TextureData(
                width,
                height,
                portalHole,
                opaque,
                PortalBladeGeometry.spearShaftExtrusionMask(texture, sprite, width, height),
                portalBounds != null ? portalBounds[0] : 0f,
                portalBounds != null ? portalBounds[1] : 0f,
                portalBounds != null ? portalBounds[2] : 0f,
                portalBounds != null ? portalBounds[3] : 0f,
                portalBounds != null
        );
    }

    private static float[] portalHoleUvBounds(boolean[][] portalHole, int width, int height) {
        int minCol = width;
        int maxCol = -1;
        int minRow = height;
        int maxRow = -1;

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                if (portalHole[col][row]) {
                    minCol = Math.min(minCol, col);
                    maxCol = Math.max(maxCol, col);
                    minRow = Math.min(minRow, row);
                    maxRow = Math.max(maxRow, row);
                }
            }
        }
        if (maxCol < 0) {
            return null;
        }

        return new float[]{
                minCol / (float) width,
                (maxCol + 1) / (float) width,
                (height - maxRow - 1) / (float) height,
                (height - minRow) / (float) height
        };
    }

    private static List<BlockElement> buildElements(PortalToolType toolType, TextureData data) {
        boolean[][] portal = data.portalHole;
        boolean[][] opaque = data.opaque;
        int width = data.width;
        int height = data.height;
        float xScale = ITEM_MODEL_SIZE / width;
        float yScale = ITEM_MODEL_SIZE / height;
        List<BlockElement> elements = new ArrayList<>();

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                if (!portal[col][row]) {
                    continue;
                }

                float x0 = col * xScale;
                float x1 = (col + 1) * xScale;
                float y0 = (height - 1 - row) * yScale;
                float y1 = (height - row) * yScale;

                Map<Direction, BlockElementFace> faces = new HashMap<>(6);
                BlockElementFace.UVs pixelUv = pixelUvFor(toolType, col, row, width, height);

                faces.put(Direction.SOUTH, face(pixelUv));
                faces.put(Direction.NORTH, face(pixelUv));

                if (exposeSide(portal, opaque, width, height, col - 1, row)) {
                    faces.put(Direction.WEST, face(pixelUv));
                }
                if (exposeSide(portal, opaque, width, height, col + 1, row)) {
                    faces.put(Direction.EAST, face(pixelUv));
                }
                if (exposeSide(portal, opaque, width, height, col, row - 1)) {
                    faces.put(Direction.UP, face(pixelUv));
                }
                if (exposeSide(portal, opaque, width, height, col, row + 1)) {
                    faces.put(Direction.DOWN, face(pixelUv));
                }

                elements.add(new BlockElement(
                        new Vector3f(x0, y0, Z_BACK),
                        new Vector3f(x1, y1, Z_FRONT),
                        faces
                ));
            }
        }

        return elements;
    }

    private static BlockElementFace.UVs pixelUvFor(PortalToolType toolType, int col, int row, int width, int height) {
        if (toolType == PortalToolType.SPEAR) {
            return PortalItemModelUv.pixelUvs(col, row, width, height);
        }
        return new BlockElementFace.UVs(col, row, col + 1, row + 1);
    }

    private static BlockElementFace face(BlockElementFace.UVs uvs) {
        return new BlockElementFace(null, 0, "layer0", uvs, Quadrant.R0);
    }

    private static boolean exposeSide(boolean[][] portal, boolean[][] opaque, int width, int height, int col, int row) {
        if (col < 0 || row < 0 || col >= width || row >= height) {
            return true;
        }
        if (portal[col][row]) {
            return false;
        }
        // Match item/generated: side faces only border transparent space, not opaque hilt pixels.
        return !opaque[col][row];
    }
}
