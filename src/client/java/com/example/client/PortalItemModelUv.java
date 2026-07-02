package com.example.client;

import net.minecraft.client.renderer.block.model.BlockElementFace;

/**
 * Maps sprite texel indices into the 0–16 item-model UV space used by {@code ItemModelGenerator}.
 * Sword sprites (16×16) are unchanged; spear in-hand sprites (32×32) must be halved.
 */
final class PortalItemModelUv {

    private static final float ITEM_MODEL_SIZE = 16f;

    private PortalItemModelUv() {}

    static BlockElementFace.UVs pixelUvs(int col, int row, int spriteWidth, int spriteHeight) {
        float uScale = ITEM_MODEL_SIZE / spriteWidth;
        float vScale = ITEM_MODEL_SIZE / spriteHeight;
        return new BlockElementFace.UVs(
                col * uScale,
                row * vScale,
                (col + 1) * uScale,
                (row + 1) * vScale
        );
    }
}
