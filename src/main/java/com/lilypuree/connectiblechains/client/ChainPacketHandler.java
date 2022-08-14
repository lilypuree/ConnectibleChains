package com.lilypuree.connectiblechains.client;

import com.lilypuree.connectiblechains.ConnectibleChains;
import com.lilypuree.connectiblechains.chain.ChainLink;
import com.lilypuree.connectiblechains.chain.ChainType;
import com.lilypuree.connectiblechains.chain.ChainTypesRegistry;
import com.lilypuree.connectiblechains.chain.IncompleteChainLink;
import com.lilypuree.connectiblechains.entity.ChainKnotEntity;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.List;

public class ChainPacketHandler {
    /**
     * Links where this is the primary and the secondary doesn't yet exist / hasn't yet loaded.
     * They are kept in a separate list to prevent accidental accesses of the secondary which would
     * result in a NPE. The links will try to be completed each world tick.
     */
    private final ObjectList<IncompleteChainLink> incompleteLinks = new ObjectArrayList<>(256);

    /**
     * Will create links from the entity with the id {@code fromId} to multiple targets.
     *
     * @param fromId Primary entity id
     * @param toIds  Secondary entity ids
     * @param typeIds  Link type raw ids
     */
    public void createLinks(int fromId, int[] toIds, List<ResourceLocation> typeIds) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;
        Entity from = client.level.getEntity(fromId);
        if (from instanceof ChainKnotEntity knot) {
            for (int i = 0; i < toIds.length; i++) {
                Entity to = client.level.getEntity(toIds[i]);
                ChainType chainType = ChainTypesRegistry.getValue(typeIds.get(i));
                if (to == null) {
                    incompleteLinks.add(new IncompleteChainLink(knot, toIds[i], chainType));
                } else {
                    ChainLink.create(knot, to, chainType);
                }
            }
        } else {
            logBadActionTarget("attach from", from, fromId, "chain knot");
        }
    }

    public void removeLink(int fromId, int toId) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return;
        Entity from = level.getEntity(fromId);
        Entity to = level.getEntity(toId);

        if (from instanceof ChainKnotEntity knot) {
            if (to == null) {
                for (IncompleteChainLink link : incompleteLinks) {
                    if (link.primary == from && link.secondaryId == toId) {
                        link.destroy();
                    }
                }
            } else {
                for (ChainLink link : knot.getLinks()) {
                    if (link.secondary == to) {
                        link.destroy(true);
                    }
                }
            }
        } else {
            logBadActionTarget("detach from", from, fromId, "chain knot");
        }
    }

    public void changeKnotType(int knotId, ResourceLocation typeId){
        Entity entity = Minecraft.getInstance().level.getEntity(knotId);
        ChainType chainType = ChainTypesRegistry.getValue(typeId);
        if (entity instanceof ChainKnotEntity knot) {
            knot.updateChainType(chainType);
        } else {
            logBadActionTarget("change type of", entity, knotId, "chain knot");
        }
    }

    private void logBadActionTarget(String action, Entity target, int targetId, String expectedTarget) {
        ConnectibleChains.LOGGER.error(String.format("Tried to %s %s (#%d) which is not %s",
                action, target, targetId, expectedTarget
        ));
    }

    /**
     * Called on every client tick.
     * Tries to complete all links.
     * Completed links or links that are no longer valid because the primary is dead are removed.
     */
    public void tick() {
        incompleteLinks.removeIf(IncompleteChainLink::tryCompleteOrRemove);
    }
}
