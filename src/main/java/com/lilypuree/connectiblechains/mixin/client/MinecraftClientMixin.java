///*
// *     Copyright (C) 2020 legoatoom
// *
// *     This program is free software: you can redistribute it and/or modify
// *     it under the terms of the GNU General Public License as published by
// *     the Free Software Foundation, either version 3 of the License, or
// *     (at your option) any later version.
// *
// *     This program is distributed in the hope that it will be useful,
// *     but WITHOUT ANY WARRANTY; without even the implied warranty of
// *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// *     GNU General Public License for more details.
// *
// *     You should have received a copy of the GNU General Public License
// *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
// */
//
//package com.lilypuree.connectiblechains.mixin.client;
//
//import com.github.legoatoom.connectiblechains.enitity.ChainKnotEntity;
//import com.lilypuree.connectiblechains.entity.ChainKnotEntity;
//import net.minecraft.client.Minecraft;
//import net.minecraft.client.MinecraftClient;
//import net.minecraft.client.entity.player.ClientPlayerEntity;
//import net.minecraft.client.multiplayer.PlayerController;
//import net.minecraft.client.network.ClientPlayerEntity;
//import net.minecraft.client.network.ClientPlayerInteractionManager;
//import net.minecraft.client.network.play.ClientPlayNetHandler;
//import net.minecraft.entity.Entity;
//import net.minecraft.entity.player.PlayerInventory;
//import net.minecraft.item.ItemStack;
//import net.minecraft.item.Items;
//import net.minecraft.util.Hand;
//import net.minecraft.util.hit.EntityHitResult;
//import net.minecraft.util.hit.HitResult;
//import net.minecraft.util.math.EntityRayTraceResult;
//import net.minecraft.util.math.RayTraceResult;
//import org.jetbrains.annotations.Nullable;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.Shadow;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.Slice;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//
//import javax.annotation.Nullable;
//
//@Mixin(Minecraft.class)
//public class MinecraftClientMixin {
//
//    @Shadow
//    @Nullable public RayTraceResult hitResult;
//
//    @Shadow
//    @Nullable
//    public ClientPlayerEntity player;
//
//    @Shadow
//    @Nullable public PlayerController gameMode;
//
//    @Inject(
//            method = "pickBlock",
//            at = @At(value = "INVOKE",
//                    target = "Lnet/minecraft/util/hit/EntityHitResult;getEntity()Lnet/minecraft/entity/Entity;",
//                    shift = At.Shift.AFTER),
//            slice = @Slice(
//                    from = @At("HEAD"),
//                    to = @At(value = "INVOKE",
//                            target = "Lnet/minecraft/item/SpawnEggItem;forEntity(Lnet/minecraft/entity/EntityType;)Lnet/minecraft/item/SpawnEggItem;")
//            ),
//            cancellable = true
//    )
//    private void givePlayerChain(CallbackInfo ci){
//        if (this.hitResult != null && this.gameMode != null && this.player != null) {
//            Entity entity = ((EntityRayTraceResult) this.hitResult).getEntity();
//            if (entity instanceof ChainKnotEntity){
//                ItemStack itemStack12 = new ItemStack(Items.CHAIN);
//                PlayerInventory playerInventory = this.player.inventory;
//                playerInventory.setPickedItem(itemStack12);
//                this.gameMode.handleCreativeModeItemAdd(this.player.getItemInHand(Hand.MAIN_HAND), 36 + playerInventory.selected);
//                ci.cancel();
//            }
//        }
//    }
//
//}
