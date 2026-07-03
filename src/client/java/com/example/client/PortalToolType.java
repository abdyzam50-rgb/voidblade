package com.example.client;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public enum PortalToolType {
    SWORD(
            Items.NETHERITE_SWORD,
            Identifier.fromNamespaceAndPath("modid", "item/portal_sword_hilt"),
            null,
            Identifier.fromNamespaceAndPath("modid", "item/portal_sword_hilt"),
            null,
            16, 16,
            16, 16,
            false,
            false,
            new float[][] {
                { 13f/16, 16f/16, 15f/16, 16f/16 },
                { 12f/16, 16f/16, 14f/16, 15f/16 },
                { 11f/16, 16f/16, 13f/16, 14f/16 },
                { 10f/16, 15f/16, 12f/16, 13f/16 },
                {  9f/16, 14f/16, 11f/16, 12f/16 },
                {  8f/16, 13f/16, 10f/16, 11f/16 },
                {  7f/16, 12f/16,  9f/16, 10f/16 },
                {  6f/16, 11f/16,  8f/16,  9f/16 },
                {  5f/16, 10f/16,  7f/16,  8f/16 },
                {  7f/16,  9f/16,  6f/16,  7f/16 },
                {  5f/16,  6f/16,  5f/16,  6f/16 },
                {  7f/16,  8f/16,  5f/16,  6f/16 },
            }
    ),
    MACE(
            Items.MACE,
            Identifier.fromNamespaceAndPath("modid", "item/portal_mace_hilt"),
            null,
            Identifier.fromNamespaceAndPath("modid", "item/portal_mace_hilt"),
            null,
            16, 16,
            16, 16,
            false,
            false,
            // Head area: cols 5-15 (u 5/16–1.0), rows 0-10 from top (v 5/16–1.0).
            // Transparent pixels inside this region get the portal effect.
            new float[][] {
                { 5f/16, 16f/16, 5f/16, 16f/16 }
            }
    ),
    SPEAR(
            Items.NETHERITE_SPEAR,
            Identifier.fromNamespaceAndPath("modid", "item/portal_spear_hilt"),
            Identifier.fromNamespaceAndPath("modid", "item/portal_spear_hilt_in_hand"),
            Identifier.fromNamespaceAndPath("modid", "item/portal_spear_hilt"),
            Identifier.fromNamespaceAndPath("modid", "item/portal_spear_hilt_in_hand"),
            16, 16,
            32, 32,
            false,
            true,
            // Blade rows from the vanilla spear head (left frame of the sprite sheet), rows 0-9.
            // Format per row: { col_start/32, col_end/32, v_bottom, v_top }
            new float[][] {
                {  0f/32,  3f/32, 31f/32, 32f/32 },
                {  0f/32,  5f/32, 30f/32, 31f/32 },
                {  0f/32,  7f/32, 29f/32, 30f/32 },
                {  1f/32,  9f/32, 28f/32, 29f/32 },
                {  1f/32,  9f/32, 27f/32, 28f/32 },
                {  2f/32,  8f/32, 26f/32, 27f/32 },
                {  2f/32,  8f/32, 25f/32, 26f/32 },
                {  3f/32,  9f/32, 24f/32, 25f/32 },
                {  3f/32, 10f/32, 23f/32, 24f/32 },
                {  8f/32, 11f/32, 22f/32, 23f/32 },
            }
    );

    private final Item item;
    private final Identifier itemId;
    private final Identifier hiltModelId;
    private final Identifier hiltInHandModelId;
    private final Identifier hiltTexture;
    private final Identifier hiltInHandTexture;
    private final int inventoryTexWidth;
    private final int inventoryTexHeight;
    private final int inHandTexWidth;
    private final int inHandTexHeight;
    /** Mirror blade U for the inventory texture (spear GUI sprite faces the other way). */
    private final boolean inventoryFlipBladeU;
    private final boolean flipY;
    private final float[][] bladeRows;

    PortalToolType(
            Item item,
            Identifier hiltModelId,
            Identifier hiltInHandModelId,
            Identifier hiltTexture,
            Identifier hiltInHandTexture,
            int inventoryTexWidth,
            int inventoryTexHeight,
            int inHandTexWidth,
            int inHandTexHeight,
            boolean flipY,
            boolean inventoryFlipBladeU,
            float[][] bladeRows
    ) {
        this.item = item;
        this.itemId = item.builtInRegistryHolder().key().identifier();
        this.hiltModelId = hiltModelId;
        this.hiltInHandModelId = hiltInHandModelId;
        this.hiltTexture = hiltTexture;
        this.hiltInHandTexture = hiltInHandTexture;
        this.inventoryTexWidth = inventoryTexWidth;
        this.inventoryTexHeight = inventoryTexHeight;
        this.inHandTexWidth = inHandTexWidth;
        this.inHandTexHeight = inHandTexHeight;
        this.flipY = flipY;
        this.inventoryFlipBladeU = inventoryFlipBladeU;
        this.bladeRows = bladeRows;
    }

    public Item item() { return item; }
    public Identifier itemId() { return itemId; }
    public Identifier hiltModelId() { return hiltModelId; }
    public Identifier hiltInHandModelId() { return hiltInHandModelId; }
    public Identifier hiltTexture() { return hiltTexture; }
    public Identifier hiltInHandTexture() { return hiltInHandTexture; }
    public int inventoryTexWidth() { return inventoryTexWidth; }
    public int inventoryTexHeight() { return inventoryTexHeight; }
    public int inHandTexWidth() { return inHandTexWidth; }
    public int inHandTexHeight() { return inHandTexHeight; }
    public boolean flipY() { return flipY; }
    public boolean inventoryFlipBladeU() { return inventoryFlipBladeU; }
    public float[][] bladeRows() { return bladeRows; }

    public static boolean isInventoryContext(ItemDisplayContext ctx) {
        return ctx == ItemDisplayContext.GUI
                || ctx == ItemDisplayContext.GROUND
                || ctx == ItemDisplayContext.FIXED
                || ctx == ItemDisplayContext.ON_SHELF;
    }

    public Identifier textureForContext(ItemDisplayContext ctx) {
        if (hiltInHandTexture != null && !isInventoryContext(ctx)) {
            return hiltInHandTexture;
        }
        return hiltTexture;
    }

    public static PortalToolType fromStack(ItemStack stack) {
        for (PortalToolType type : values()) {
            if (stack.is(type.item)) return type;
        }
        return null;
    }

    public static PortalToolType fromItemId(Identifier itemId) {
        for (PortalToolType type : values()) {
            if (type.itemId.equals(itemId)) return type;
        }
        return null;
    }
}
