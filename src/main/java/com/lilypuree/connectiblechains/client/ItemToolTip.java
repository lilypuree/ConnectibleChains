package com.lilypuree.connectiblechains.client;

import com.lilypuree.connectiblechains.ConnectibleChains;
import com.lilypuree.connectiblechains.tag.CommonTags;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ItemToolTip {
    @SubscribeEvent
    public static void addToolTip(ItemTooltipEvent event) {
        if (CommonTags.isChain(event.getItemStack()) && ConnectibleChains.runtimeConfig.doShowToolTip()) {
            if (Screen.hasShiftDown()) {
                event.getToolTip().add(1, Component.translatable("message.connectiblechains.connectible_chain_detailed")
                        .withStyle(ChatFormatting.AQUA));
            } else {
                event.getToolTip().add(1, Component.translatable("message.connectiblechains.connectible_chain")
                        .withStyle(ChatFormatting.YELLOW));
            }
        }

    }
}
