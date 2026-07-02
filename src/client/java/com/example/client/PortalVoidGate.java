package com.example.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * Controls when the portal blade render is active on this client.
 * <p>
 * Local player (mod installed): portal skin on their own weapons without a name when
 * {@link #REQUIRE_VOID_NAME} is false; otherwise their weapons need the void name too.
 * Other players' weapons only use the portal skin here when the item has the void name.
 */
public final class PortalVoidGate {

    /** When true, the local player's weapons also need a void custom name. */
    public static final boolean REQUIRE_VOID_NAME = false;

    public static final String VOID_NAME_TOKEN = "void";

    private PortalVoidGate() {}

    public static boolean shouldUsePortalRender(
            ItemStack stack,
            @Nullable ItemOwner owner,
            ItemDisplayContext displayContext
    ) {
        if (isLocalPlayerOwned(owner, displayContext)) {
            if (!REQUIRE_VOID_NAME) {
                return true;
            }
            return hasVoidName(stack);
        }
        return hasVoidName(stack);
    }

    private static boolean isLocalPlayerOwned(@Nullable ItemOwner owner, ItemDisplayContext displayContext) {
        if (displayContext == ItemDisplayContext.GUI) {
            return true;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || owner == null) {
            return false;
        }

        LivingEntity living = owner.asLivingEntity();
        return living != null && living.getUUID().equals(player.getUUID());
    }

    private static boolean hasVoidName(ItemStack stack) {
        var name = stack.getCustomName();
        return name != null && name.getString().toLowerCase(Locale.ROOT).contains(VOID_NAME_TOKEN);
    }
}
