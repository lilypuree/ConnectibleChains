package com.lilypuree.connectiblechains;

import net.minecraft.client.Minecraft;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.ModConfigSpec;

public class CCConfig {
    private static final boolean IS_DEBUG_ENV = FMLEnvironment.production;

    public ModConfigSpec.DoubleValue chainHangAmount;
    public ModConfigSpec.IntValue maxChainRange;
    public ModConfigSpec.IntValue quality;

    public ModConfigSpec.BooleanValue showToolTip;

    public static ModConfigSpec COMMON_CONFIG;
    public static ModConfigSpec CLIENT_CONFIG;

    public CCConfig() {
        ModConfigSpec.Builder COMMON_BUILDER = new ModConfigSpec.Builder();
        ModConfigSpec.Builder CLIENT_BUILDER = new ModConfigSpec.Builder();
        COMMON_BUILDER.comment("Connectible Chains Config\n\n");

        chainHangAmount = COMMON_BUILDER
                .comment("\r\n Chain Hang Amount\n"
                        + "\r\n Effects how much the chain hangs."
                        + "\r\n Collision will update on new chains or world loading."
                        + "\r\n  Has no effect in multiplayer."
                ).defineInRange("chainHangAmount", 9.0f, 0.0f, Double.MAX_VALUE);
        maxChainRange = COMMON_BUILDER
                .comment("\r\n  Max Chain Distance\n"
                        + "\r\n Warning: Long chains can sometimes become invisible!"
                        + "\r\n Has no effect in multiplayer.")
                .defineInRange("maxChainRange", 7, 0, 32);


        CLIENT_BUILDER.comment("Configurable Chains Client Config");
        quality = CLIENT_BUILDER
                .comment("\r\n  Chain Quality\n"
                        + "\r\n Effects the visual quality of the chain.")
                .defineInRange("quality", 4, 1, 9);

        showToolTip = CLIENT_BUILDER
                .comment("\r\n  ToolTip\n"
                        + "\r\n Displays a ToolTip under compatible chain items")
                .define("showToolTip", true);

        COMMON_CONFIG = COMMON_BUILDER.build();
        CLIENT_CONFIG = CLIENT_BUILDER.build();
    }

    public float getChainHangAmount() {
        return chainHangAmount.get().floatValue();
    }

    public int getMaxChainRange() {
        return maxChainRange.get();
    }

    public int getQuality() {
        return quality.get();
    }

    public Boolean doShowToolTip() {
        return showToolTip.get();
    }

    public boolean doDebugDraw() {
        return IS_DEBUG_ENV && Minecraft.getInstance().getDebugOverlay().showDebugScreen();
    }
}
