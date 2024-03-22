package com.lilypuree.connectiblechains.mixin.server;

import com.lilypuree.connectiblechains.datafixer.ChainKnotFixer;
import com.mojang.datafixers.DataFixer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.datafix.DataFixTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NbtUtils.class)
public class NBTUtilsMixin {

    @Inject(at = @At("RETURN"), method = "update(Lcom/mojang/datafixers/DataFixer;Lnet/minecraft/util/datafix/DataFixTypes;Lnet/minecraft/nbt/CompoundTag;II)Lnet/minecraft/nbt/CompoundTag;", cancellable = true)
    private static void updateDataWithFixers(DataFixer fixer, DataFixTypes fixTypes, CompoundTag compound, int oldVersion, int targetVersion, CallbackInfoReturnable<CompoundTag> cir) {
        CompoundTag original = cir.getReturnValue();
        if (fixTypes == DataFixTypes.ENTITY_CHUNK) {
            CompoundTag finalTag = ChainKnotFixer.INSTANCE.update(original);
            cir.setReturnValue(finalTag);
        }
    }
}
