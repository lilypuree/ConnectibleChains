package com.lilypuree.connectiblechains.mixin.server.network;

import com.lilypuree.connectiblechains.entity.ChainKnotEntity;
import net.minecraft.entity.Entity;
import net.minecraft.network.IPacket;
import net.minecraft.world.TrackedEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.function.Consumer;

@Mixin(TrackedEntity.class)
public class TrackedEntityMixin {
    @Shadow
    @Final
    private Entity entity;

    @Inject(method = "sendPairingData", at = @At("TAIL"))
    private void sendPackets(Consumer<IPacket<?>> sender, CallbackInfo ci) {
        if (this.entity instanceof ChainKnotEntity) {
            ChainKnotEntity chainKnotEntity = (ChainKnotEntity) this.entity;
            ArrayList<Entity> list = chainKnotEntity.getHoldingEntities();
            for (Entity e : list) {
                chainKnotEntity.init();
//                chainKnotEntity.sendAttachChainPacket(e.getId(), 0);
            }
        }
    }
}
