package com.lilypuree.connectiblechains.datafixer;

import com.lilypuree.connectiblechains.chain.ChainTypesRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;

/**
 * Upgrades the NBT data of {@link com.lilypuree.connectiblechains.entity.ChainKnotEntity Chain Knots}
 * to newer versions.
 */
public class ChainKnotFixer extends NbtFixer{
    public static final ChainKnotFixer INSTANCE = new ChainKnotFixer();

    /**
     * Not strictly the same as the mod version.
     */
    @Override
    protected int getVersion() {
        return 2_01_00;
    }

    /**
     * The numbers in the function names represent the version for which the fix was created.
     */
    @Override
    public void registerFixers() {
        addFix(2_01_00, "Add ChainType", this::fixChainType201);
        addFix(2_01_00, "Make Chains position relative", this::fixChainPos201);
    }

    /**
     * Add the chain types
     */
    private CompoundTag fixChainType201(CompoundTag nbt) {
        if (isNotChainKnot201(nbt)) return nbt;
        if (!nbt.contains("ChainType")) {
            nbt.putString("ChainType", ChainTypesRegistry.DEFAULT_CHAIN_TYPE_ID.toString());
        }
        for (Tag linkElem : nbt.getList("Chains", Tag.TAG_COMPOUND)) {
            if (linkElem instanceof CompoundTag link) {
                if (!link.contains("ChainType")) {
                    link.putString("ChainType", ChainTypesRegistry.DEFAULT_CHAIN_TYPE_ID.toString());
                }
            }
        }
        return nbt;
    }

    /**
     * Make chain positions relative
     */
    private CompoundTag fixChainPos201(CompoundTag nbt) {
        if (isNotChainKnot201(nbt)) return nbt;
        ListTag pos = nbt.getList("Pos", Tag.TAG_DOUBLE);
        int sx = Mth.floor(pos.getDouble(0));
        int sy = Mth.floor(pos.getDouble(1));
        int sz = Mth.floor(pos.getDouble(2));
        for (Tag linkElem : nbt.getList("Chains", Tag.TAG_COMPOUND)) {
            if (linkElem instanceof CompoundTag link) {
                if (link.contains("X")) {
                    int dx = link.getInt("X");
                    int dy = link.getInt("Y");
                    int dz = link.getInt("Z");
                    link.remove("X");
                    link.remove("Y");
                    link.remove("Z");
                    link.putInt("RelX", dx - sx);
                    link.putInt("RelY", dy - sy);
                    link.putInt("RelZ", dz - sz);
                }
            }
        }
        return nbt;
    }

    /**
     * @return Returns true when {@code nbt} does not belong to a chain knot.
     */
    private boolean isNotChainKnot201(CompoundTag nbt) {
        // Not using the registry here to avoid breaking when the id changes
        return !nbt.getString("id").equals("connectiblechains:chain_knot");
    }
}
