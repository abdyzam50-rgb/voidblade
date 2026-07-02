package com.example.client;

import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

import java.util.List;

public final class PortalBladeGeometry {

    private PortalBladeGeometry() {}

    public static boolean isPortalHiltSprite(TextureAtlasSprite sprite) {
        Identifier name = sprite.contents().name();
        for (PortalToolType type : PortalToolType.values()) {
            if (type.hiltTexture().equals(name) || name.equals(type.hiltInHandTexture())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isBladePoint(Identifier texture, float u, float v) {
        float mappedU = mapBladeU(texture, u);
        for (PortalToolType type : PortalToolType.values()) {
            if (type.hiltTexture().equals(texture) || texture.equals(type.hiltInHandTexture())) {
                for (float[] row : type.bladeRows()) {
                    if (mappedU >= row[0] && mappedU < row[1] && v >= row[2] && v < row[3]) return true;
                }
                return false;
            }
        }
        return false;
    }

    private static float mapBladeU(Identifier texture, float u) {
        for (PortalToolType type : PortalToolType.values()) {
            if (type.hiltTexture().equals(texture) && type.inventoryFlipBladeU()) {
                return 1f - u;
            }
        }
        return u;
    }

    private static boolean inventoryFlipsBladeU(Identifier texture) {
        for (PortalToolType type : PortalToolType.values()) {
            if (type.hiltTexture().equals(texture) && type.inventoryFlipBladeU()) {
                return true;
            }
        }
        return false;
    }

    public static boolean isBladeTexel(Identifier texture, int column, int rowFromTop, int width, int height) {
        float u = (column + 0.5f) / width;
        float v = 1f - (rowFromTop + 0.5f) / height;
        return isBladePoint(texture, u, v);
    }

    private static final String[] INVENTORY_SPEAR_PORTAL_MASK = {
            ".............xxx",
            "...........xxxxx",
            ".........xxxxxxx",
            ".......xxxxxxxx.",
            ".......xxxxxxxx.",
            "........xxxxxx..",
            ".........xxxxx..",
            "..........xxx...",
            ".......x...xx...",
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
    };

    /**
     * Transparent texels that receive the end-portal layer. The inventory spear sprite places
     * the head on the top-right, so mirrored vanilla blade rows miss most of the void area.
     */
    public static boolean isPortalHoleTexel(
            Identifier texture,
            SpriteContents sprite,
            int column,
            int rowFromTop,
            int width,
            int height
    ) {
        if (!sprite.isTransparent(0, column, rowFromTop)) {
            return false;
        }
        if (texture.equals(PortalToolType.SPEAR.hiltTexture())) {
            return isInventorySpearPortalMasked(column, rowFromTop);
        }
        return isBladeTexel(texture, column, rowFromTop, width, height);
    }

    private static boolean isInventorySpearPortalMasked(int column, int rowFromTop) {
        if (rowFromTop < 0 || rowFromTop >= INVENTORY_SPEAR_PORTAL_MASK.length) {
            return false;
        }
        String row = INVENTORY_SPEAR_PORTAL_MASK[rowFromTop];
        if (column < 0 || column >= row.length()) {
            return false;
        }
        return row.charAt(column) == 'x';
    }

    private static boolean bordersInventorySpearPortal(int column, int rowFromTop) {
        return isInventorySpearPortalMasked(column, rowFromTop)
                || isInventorySpearPortalMasked(column - 1, rowFromTop)
                || isInventorySpearPortalMasked(column + 1, rowFromTop)
                || isInventorySpearPortalMasked(column, rowFromTop - 1)
                || isInventorySpearPortalMasked(column, rowFromTop + 1);
    }

    public static boolean isInventoryPortalBorderSuppressed(
            Identifier texture,
            SpriteContents sprite,
            int column,
            int rowFromTop
    ) {
        if (!texture.equals(PortalToolType.SPEAR.hiltTexture())) {
            return false;
        }
        if (column < 0 || rowFromTop < 0 || column >= sprite.width() || rowFromTop >= sprite.height()) {
            return false;
        }
        if (sprite.isTransparent(0, column, rowFromTop)) {
            return false;
        }
        return bordersInventorySpearPortal(column, rowFromTop);
    }

    public static boolean isFlatItemFace(BakedQuad quad) {
        Direction direction = quad.direction();
        return direction == Direction.SOUTH || direction == Direction.NORTH;
    }

    /**
     * True when this quad belongs to the visible hilt pass (no portal side extrusions).
     */
    public static boolean isHiltRenderQuad(BakedQuad quad, Identifier texture) {
        if (!isPortalHiltSprite(quad.sprite())) {
            return true;
        }
        // Flat faces only exist on opaque texels; portal holes are transparent and have no quad.
        if (isFlatItemFace(quad)) {
            return true;
        }
        if (touchesSpearShaftSuppress(quad, texture)) {
            return false;
        }
        if (!overlapsBladeRegion(quad, texture)) {
            return !sideQuadFacesPortalHole(quad, texture);
        }
        for (int i = 0; i < 4; i++) {
            float[] uv = spriteUv(quad, i, quad.sprite());
            if (PortalBladeVoxelCache.isPortalHole(texture, uv[0], uv[1])) {
                return false;
            }
        }
        return false;
    }

    private static boolean sideQuadFacesPortalHole(BakedQuad quad, Identifier texture) {
        Direction direction = quad.direction();
        if (direction == Direction.SOUTH || direction == Direction.NORTH) {
            return false;
        }

        TextureAtlasSprite sprite = quad.sprite();
        SpriteContents contents = sprite.contents();
        int width = contents.width();
        int height = contents.height();

        float uSum = 0f;
        float vSum = 0f;
        for (int i = 0; i < 4; i++) {
            float[] uv = spriteUv(quad, i, sprite);
            uSum += uv[0];
            vSum += uv[1];
        }
        int col = (int) ((uSum / 4f) * width);
        int row = (int) (((1f - vSum / 4f)) * height);

        int neighborCol = col;
        int neighborRow = row;
        switch (direction) {
            case WEST -> neighborCol--;
            case EAST -> neighborCol++;
            case UP -> neighborRow--;
            case DOWN -> neighborRow++;
            default -> {
                return false;
            }
        }

        float neighborU = (neighborCol + 0.5f) / width;
        float neighborV = 1f - (neighborRow + 0.5f) / height;
        return PortalBladeVoxelCache.isPortalHole(texture, neighborU, neighborV);
    }

    public static float[] spriteUv(BakedQuad quad, int vertex, TextureAtlasSprite sprite) {
        long packed = quad.packedUV(vertex);
        float uScale = sprite.getU1() - sprite.getU0();
        float vScale = sprite.getV1() - sprite.getV0();
        float u = (UVPair.unpackU(packed) - sprite.getU0()) / uScale;
        float v = (UVPair.unpackV(packed) - sprite.getV0()) / vScale;
        return new float[]{ u, v };
    }

    private static boolean rangesOverlap(float a0, float a1, float b0, float b1) {
        return a0 < b1 && b0 < a1;
    }

    public static boolean overlapsBladeRegion(BakedQuad quad, Identifier texture) {
        TextureAtlasSprite sprite = quad.sprite();
        float minU = 1f, maxU = 0f, minV = 1f, maxV = 0f;
        for (int i = 0; i < 4; i++) {
            float[] uv = spriteUv(quad, i, sprite);
            minU = Math.min(minU, uv[0]);
            maxU = Math.max(maxU, uv[0]);
            minV = Math.min(minV, uv[1]);
            maxV = Math.max(maxV, uv[1]);
        }
        if (inventoryFlipsBladeU(texture)) {
            float flippedMinU = 1f - maxU;
            float flippedMaxU = 1f - minU;
            minU = flippedMinU;
            maxU = flippedMaxU;
        }
        for (PortalToolType type : PortalToolType.values()) {
            if (type.hiltTexture().equals(texture) || texture.equals(type.hiltInHandTexture())) {
                for (float[] row : type.bladeRows()) {
                    if (rangesOverlap(minU, maxU, row[0], row[1]) && rangesOverlap(minV, maxV, row[2], row[3])) {
                        return true;
                    }
                }
                if (texture.equals(PortalToolType.SPEAR.hiltTexture())) {
                    return PortalBladeVoxelCache.overlapsPortalHoleRegion(texture, minU, maxU, minV, maxV);
                }
                return false;
            }
        }
        return false;
    }

    public static boolean usesPortalHiltSprite(List<BakedQuad> quads) {
        for (BakedQuad quad : quads) {
            if (isPortalHiltSprite(quad.sprite())) return true;
        }
        return false;
    }

    /**
     * Diagonal spear shafts get item/generated side cubes on every open neighbor. Those
     * extrusions are not in the sprite but render as a ghost line parallel to the shaft.
     */
    public static boolean[][] spearShaftExtrusionMask(
            Identifier texture,
            SpriteContents sprite,
            int width,
            int height
    ) {
        boolean[][] suppress = new boolean[width][height];
        if (texture.equals(PortalToolType.SPEAR.hiltTexture())) {
            boolean[][] opaque = new boolean[width][height];
            for (int row = 0; row < height; row++) {
                for (int col = 0; col < width; col++) {
                    opaque[col][row] = !sprite.isTransparent(0, col, row);
                    if (opaque[col][row] && bordersInventorySpearPortal(col, row)) {
                        suppress[col][row] = true;
                    }
                }
            }
            return suppress;
        }
        if (!texture.equals(PortalToolType.SPEAR.hiltInHandTexture())) {
            return suppress;
        }

        boolean[][] portalHole = new boolean[width][height];
        boolean[][] opaque = new boolean[width][height];
        int maxPortalRow = -1;

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                opaque[col][row] = !sprite.isTransparent(0, col, row);
                portalHole[col][row] = isBladeTexel(texture, col, row, width, height) && !opaque[col][row];
                if (portalHole[col][row]) {
                    maxPortalRow = Math.max(maxPortalRow, row);
                }
            }
        }
        if (maxPortalRow < 0) {
            return suppress;
        }

        for (int row = maxPortalRow + 1; row < height; row++) {
            for (int col = 0; col < width; col++) {
                if (!opaque[col][row]) {
                    continue;
                }
                // Keep the long shaft depth; only trim the junction rows under the portal head.
                if (row >= 10) {
                    continue;
                }
                if (row == maxPortalRow + 1 && col >= 3 && col <= 6) {
                    continue;
                }
                suppress[col][row] = true;
            }
        }
        return suppress;
    }

    public static boolean isSpearShaftExtrusionSuppressed(
            Identifier texture,
            SpriteContents sprite,
            int col,
            int rowFromTop
    ) {
        int width = sprite.width();
        int height = sprite.height();
        if (col < 0 || rowFromTop < 0 || col >= width || rowFromTop >= height) {
            return false;
        }
        boolean[][] suppress = spearShaftExtrusionMask(texture, sprite, width, height);
        return suppress[col][rowFromTop];
    }

    public static boolean touchesSpearShaftSuppress(BakedQuad quad, Identifier texture) {
        SpriteContents sprite = quad.sprite().contents();
        for (int i = 0; i < 4; i++) {
            float[] uv = spriteUv(quad, i, quad.sprite());
            int col = (int) (uv[0] * sprite.width());
            int row = (int) ((1f - uv[1]) * sprite.height());
            if (texture.equals(PortalToolType.SPEAR.hiltInHandTexture())
                    && isSpearShaftExtrusionSuppressed(texture, sprite, col, row)) {
                return true;
            }
            if (isInventoryPortalBorderSuppressed(texture, sprite, col, row)) {
                return true;
            }
        }
        return false;
    }
}
