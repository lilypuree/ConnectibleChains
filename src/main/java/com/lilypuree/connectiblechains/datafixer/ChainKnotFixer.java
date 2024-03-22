package com.lilypuree.connectiblechains.datafixer;

import com.lilypuree.connectiblechains.entity.ChainKnotEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;

/**
 * Upgrades the NBT data of {@link com.lilypuree.connectiblechains.entity.ChainKnotEntity Chain Knots}
 * to newer versions.
 */
public class ChainKnotFixer extends NbtFixer {
    public static final ChainKnotFixer INSTANCE = new ChainKnotFixer();

    /**
     * Not strictly the same as the mod version.
     */
    @Override
    protected int getVersion() {
        return 3_01_00;
    }

    /**
     * The numbers in the function names represent the version for which the fix was created.
     */
    @Override
    public void registerFixers() {
        addFix(2_01_00, "Make Chains position relative", this::fixChainPos201);
        addFix(3_01_00, "Use new SourceItem instead of ChainType", this::fixSourceItemInsteadOfChainType);
    }


    /**
     * Since the 1.19 version a chain knot entity doesn't use the custom ChainType to store its type.
     * It instead uses a tag named 'SourceItem' to store the item from which the chain is made.
     * This fix adds that tag to Chain Knots that were created with the old format (prior to 1.19).
     * Important: The fix doesn't check what type the old chain knot had and just uses minecraft:chain as
     * the new SourceItem. But as far as I can tell, compatibility with other mods was not really active in this
     * port anyway, so it shouldn't really be a problem. Further I think it's much better to replace all existing
     * connected chains with the default minecraft variant, than having them break and just display missing textures.
     */
    private CompoundTag fixSourceItemInsteadOfChainType(CompoundTag nbt) {
        if (isNotChainKnot201(nbt)) return nbt;

        // if there's the old tag present, delete it
        if (nbt.contains("ChainType")) {
            nbt.remove("ChainType");
        }
        nbt.putString(ChainKnotEntity.SOURCE_ITEM_KEY, "minecraft:chain");

        for (Tag linkElem : nbt.getList("Chains", Tag.TAG_COMPOUND)) {
            if (linkElem instanceof CompoundTag link) {
                if (link.contains("ChainType")) {
                    link.remove("ChainType");
                }
                link.putString(ChainKnotEntity.SOURCE_ITEM_KEY, "minecraft:chain");
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
