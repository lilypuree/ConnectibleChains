package com.lilypuree.connectiblechains.mixin.network;

import com.lilypuree.connectiblechains.entity.ChainKnotEntity;
import com.lilypuree.connectiblechains.entity.ModEntityTypes;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.network.play.server.SSpawnObjectPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ClientPlayNetHandler.class)
public class ClientPlayNetHandlerMixin {

    @Shadow
    private ClientWorld level;

    @Inject(
            method = "handleAddEntity(Lnet/minecraft/network/play/server/SSpawnObjectPacket;)V",
            at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/network/play/server/SSpawnObjectPacket;getType()Lnet/minecraft/entity/EntityType;"),
            cancellable = true,
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void onEntitySpawn(SSpawnObjectPacket packet, CallbackInfo ci, double x, double y, double z, EntityType<?> type) {
        Entity entity = null;
        if (type == ModEntityTypes.CHAIN_KNOT.get()) {
            entity = new ChainKnotEntity(this.level, new BlockPos(x, y, z));
        } // we can replicate this one here for all our other entities
        // entity would be null here when the type was not one for us
        if (entity != null) {
            int entityId = packet.getId();
            entity.setDeltaMovement(Vector3d.ZERO); // entities always spawn standing still. We may change this later
            entity.setPos(x, y, z);
            entity.setPacketCoordinates(x, y, z);
            entity.xRot = (float) (packet.getxRot() * 360) / 256.0F;
            entity.yRot = (float) (packet.getyRot() * 360) / 256.0F;
            entity.setId(entityId);
            entity.setUUID(packet.getUUID());
            this.level.putNonPlayerEntity(entityId, entity);
            ci.cancel(); // cancel stops the rest of the method to run (so no spawning code from mc runs)
        }
    }
}
