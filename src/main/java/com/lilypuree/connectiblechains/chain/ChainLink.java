/*
 * Copyright (C) 2024 legoatoom.
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.lilypuree.connectiblechains.chain;

import com.lilypuree.connectiblechains.ConnectibleChains;
import com.lilypuree.connectiblechains.entity.ChainCollisionEntity;
import com.lilypuree.connectiblechains.entity.ChainKnotEntity;
import com.lilypuree.connectiblechains.entity.ModEntityTypes;
import com.lilypuree.connectiblechains.networking.packet.ChainAttachPayload;
import com.lilypuree.connectiblechains.util.Helper;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.LeadItem;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * A logical representation of the link between a knot and another entity.
 * It also serves as a single source of truth which prevents state mismatches in the code.
 *
 * @author Qendolin
 */
public class ChainLink {
    /**
     * The x/z distance between {@link ChainCollisionEntity ChainCollisionEntities}.
     * A value of 1 means they are "shoulder to shoulder"
     */
    private static final float COLLIDER_SPACING = 1.5f;

    /**
     * The de facto owner of this link. It is responsive for managing the link and keeping track of it across saves.
     */
    @NotNull
    private final ChainKnotEntity primary;
    /**
     * The de facto target of this link. Mostly used to calculate positions.
     */
    @NotNull
    private final Entity secondary;
    /**
     * The type of the link
     */
    @NotNull
    public final Item sourceItem;
    /**
     * Holds the entity ids of associated {@link ChainCollisionEntity collision entities}.
     */
    private final IntList collisionStorage = new IntArrayList(16);
    /**
     * Indicates that no sound should be played when the link is destroyed.
     */
    public boolean removeSilently = false;
    /**
     * Whether the link exists and is active
     */
    private boolean alive = true;

    private ChainLink(@NotNull ChainKnotEntity primary, @NotNull Entity secondary, @NotNull Item sourceItem) {
        if (primary.equals(secondary))
            throw new IllegalStateException("Tried to create a link between a knot and itself");
        this.primary = Objects.requireNonNull(primary);
        this.secondary = Objects.requireNonNull(secondary);
        this.sourceItem = Objects.requireNonNull(sourceItem);
    }

    /**
     * Create a chain link between primary and secondary,
     * adds it to their lists. Also spawns {@link ChainCollisionEntity collision entities}
     * when the link is created between two knots.
     *
     * @param primary    The source knot
     * @param secondary  A different chain knot or player
     * @param sourceItem The type of the link
     * @return A new chain link or null if it already exists
     */
    @Nullable
    public static ChainLink create(@NotNull ChainKnotEntity primary, @NotNull Entity secondary, @NotNull Item sourceItem) {
        ChainLink link = new ChainLink(primary, secondary, sourceItem);
        // Prevent multiple links between same targets.
        // Checking on the secondary is not required as the link always exists on both sides.
        if (primary.getLinks().contains(link)) return null;

        primary.addLink(link);
        if (secondary instanceof ChainKnotEntity secondaryKnot) {
            secondaryKnot.addLink(link);
            link.createCollision();
        }
        if (!primary.level().isClientSide()) {
            link.sendAttachChainPacket(primary.level());
        }
        return link;
    }

    /**
     * Create a collision between this and an entity.
     * It spawns multiple {@link ChainCollisionEntity ChainCollisionEntities} that are equal distance from each other.
     * Position is the same no matter what if the connection is from A -> B or A <- B.
     */
    private void createCollision() {
        if (!collisionStorage.isEmpty()) return;
        if (getPrimary().level().isClientSide()) return;

        double distance = getPrimary().distanceTo(getSecondary());
        // step = spacing * √(width^2 + width^2) / distance
        double step = COLLIDER_SPACING * Math.sqrt(Math.pow(ModEntityTypes.CHAIN_COLLISION.get().getWidth(), 2) * 2) / distance;
        double v = step;
        // reserve space for the center collider
        double centerHoldout = ModEntityTypes.CHAIN_COLLISION.get().getWidth() / distance;

        while (v < 0.5 - centerHoldout) {
            Entity collider1 = spawnCollision(false, getPrimary(), getSecondary(), v);
            if (collider1 != null) collisionStorage.add(collider1.getId());
            Entity collider2 = spawnCollision(true, getPrimary(), getSecondary(), v);
            if (collider2 != null) collisionStorage.add(collider2.getId());

            v += step;
        }

        Entity centerCollider = spawnCollision(false, getPrimary(), getSecondary(), 0.5);
        if (centerCollider != null) collisionStorage.add(centerCollider.getId());
    }

    /**
     * Send a package to all the clients around this entity that notifies them of this link's creation.
     */
    private void sendAttachChainPacket(Level level) {
        assert level instanceof ServerLevel;

        PacketDistributor.sendToPlayersTrackingEntity(this.primary, new ChainAttachPayload(this, true));
        PacketDistributor.sendToPlayersTrackingEntity(this.secondary, new ChainAttachPayload(this, true));
    }

    /**
     * Spawns a collider at {@code v} percent between {@code start} and {@code end}
     *
     * @param reverse Reverse start and end
     * @param start   the entity at {@code v} = 0
     * @param end     the entity at {@code v} = 1
     * @param v       percent of the distance
     * @return {@link ChainCollisionEntity} or null
     */
    @Nullable
    private Entity spawnCollision(boolean reverse, Entity start, Entity end, double v) {
        assert getPrimary().level() instanceof ServerLevel;
        Vec3 startPos = start.position().add(start.getLeashOffset(0));
        Vec3 endPos = end.position().add(end.getLeashOffset(0));

        Vec3 tmp = endPos;
        if (reverse) {
            endPos = startPos;
            startPos = tmp;
        }


        Vec3 offset = Helper.getChainOffset(startPos, endPos);
        startPos = startPos.add(offset.x(), 0, offset.z());
        endPos = endPos.add(-offset.x(), 0, -offset.z());

        double distance = startPos.distanceTo(endPos);

        double x = Mth.lerp(v, startPos.x(), endPos.x());
        double y = startPos.y() + Helper.drip2((v * distance), distance, endPos.y() - startPos.y());
        double z = Mth.lerp(v, startPos.z(), endPos.z());

        y += -ModEntityTypes.CHAIN_COLLISION.get().getHeight() + 2 / 16f;

        ChainCollisionEntity c = new ChainCollisionEntity(getPrimary().level(), x, y, z, this);
        if (getPrimary().level().addFreshEntity(c)) {
            return c;
        } else {
            ConnectibleChains.LOGGER.warn("Tried to summon collision entity for a chain, failed to do so");
            return null;
        }
    }

    public boolean isDead() {
        return !alive;
    }

    /**
     * Returns the squared distance between the primary and secondary.
     */
    public double getSquaredDistance() {
        return this.getPrimary().distanceToSqr(getSecondary());
    }

    /**
     * Two links are considered equal when the involved entities are the same, regardless of their designation
     * and the links have the same living status.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChainLink link = (ChainLink) o;

        boolean partnersEqual = getPrimary().equals(link.getPrimary()) && getSecondary().equals(link.getSecondary()) ||
                getPrimary().equals(link.getSecondary()) && getSecondary().equals(link.getPrimary());
        return alive == link.alive && partnersEqual;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPrimary(), getSecondary(), alive);
    }

    /**
     * If due to some error, or unforeseeable causes such as commands
     * the link still exists but needs to be destroyed.
     *
     * @return true when {@link #destroy(boolean)} needs to be called
     */
    public boolean needsBeDestroyed() {
        return getPrimary().isRemoved() || getSecondary().isRemoved();
    }

    /**
     * Destroys the link including all collision entities and drops an item in its center when the conditions allow it. <br/>
     * This method is idempotent.
     *
     * @param mayDrop if an item may drop.
     */
    public void destroy(boolean mayDrop) {
        if (!alive) return;

        boolean drop = mayDrop;
        Level level = getPrimary().level();
        this.alive = false;

        if (level.isClientSide()) {
            return;
        }

        if (getSecondary() instanceof Player player && player.isCreative()) drop = false;
        // I think DO_TILE_DROPS makes more sense than DO_ENTITY_DROPS in this case
        if (!level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) drop = false;

        if (drop) {
            ItemStack stack = new ItemStack(getSourceItem());
            if (getSecondary() instanceof Player player) {
                player.addItem(stack);
            } else {
                Vec3 middle = Helper.middleOf(getPrimary().position(), getSecondary().position());
                ItemEntity itemEntity = new ItemEntity(level, middle.x, middle.y, middle.z, stack);
                itemEntity.setDefaultPickUpDelay();
                level.addFreshEntity(itemEntity);
            }
        }

        destroyCollision();
        if (!primary.isRemoved() && !secondary.isRemoved()) {
            sendDetachChainPacket(level);
        }
    }

    /**
     * Removes the collision entities associated with this link.
     */
    private void destroyCollision() {
        for (Integer entityId : collisionStorage) {
            Entity e = getPrimary().level().getEntity(entityId);
            if (e instanceof ChainCollisionEntity) {
                e.remove(Entity.RemovalReason.DISCARDED);
            } else {
                ConnectibleChains.LOGGER.warn("Collision storage contained reference to {} (#{}) which is not a collision entity.", e, entityId);
            }
        }
        collisionStorage.clear();
    }

    /**
     * Send a package to all the clients around this entity that notifies them of this link's destruction.
     */
    private void sendDetachChainPacket(Level level) {
        assert level instanceof ServerLevel;

        PacketDistributor.sendToPlayersTrackingEntity(primary, new ChainAttachPayload(this, false));
        PacketDistributor.sendToPlayersTrackingEntity(secondary, new ChainAttachPayload(this, false));
    }

    /**
     * Get the sound used for the source item, this way the sound is consistent.
     */
    public static SoundType getSoundGroup(@Nullable Item sourceItem) {
        if (sourceItem instanceof BlockItem blockItem) {
            return blockItem.getBlock().defaultBlockState().getSoundType();
        }
        if (sourceItem instanceof LeadItem) {
            return new SoundType(1.0f,
                    1.0f,
                    SoundEvents.LEASH_KNOT_BREAK,
                    SoundType.WOOL.getStepSound(),
                    SoundEvents.LEASH_KNOT_PLACE,
                    SoundType.WOOL.getHitSound(),
                    SoundType.WOOL.getFallSound()
            );
        }
        return SoundType.CHAIN;
    }

    /**
     * The de facto owner of this link. It is responsive for managing the link and keeping track of it across saves.
     */
    @NotNull
    public ChainKnotEntity getPrimary() {
        return primary;
    }

    /**
     * The de facto target of this link. Mostly used to calculate positions.
     */
    @NotNull
    public Entity getSecondary() {
        return secondary;
    }

    /**
     * The type of the link
     */
    @NotNull
    public Item getSourceItem() {
        return sourceItem;
    }
}
