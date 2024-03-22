package com.lilypuree.connectiblechains.mixin.server;

import com.lilypuree.connectiblechains.datafixer.ChainKnotFixer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.chunk.storage.EntityStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityStorage.class)
public class NBTUtilsMixin {

    // No clue if that's the correct way of doing things, but it seems like it's working :)
    @Inject(at = @At("RETURN"), method = "upgradeChunkTag(Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/nbt/CompoundTag;", cancellable = true)
    private void updateDataWithFixers(CompoundTag pTag, CallbackInfoReturnable<CompoundTag> cir) {
        CompoundTag original = cir.getReturnValue();
        CompoundTag finalTag = ChainKnotFixer.INSTANCE.update(original);
        cir.setReturnValue(finalTag);
    }
}