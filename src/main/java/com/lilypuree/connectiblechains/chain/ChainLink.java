package com.lilypuree.connectiblechains.chain;

import com.lilypuree.connectiblechains.ConnectibleChains;
import com.lilypuree.connectiblechains.entity.ChainCollisionEntity;
import com.lilypuree.connectiblechains.entity.ChainKnotEntity;
import com.lilypuree.connectiblechains.entity.ModEntityTypes;
import com.lilypuree.connectiblechains.network.ModPacketHandler;
import com.lilypuree.connectiblechains.network.S2CChainAttachPacket;
import com.lilypuree.connectiblechains.network.S2CChainDetachPacket;
import com.lilypuree.connectiblechains.util.Helper;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
    public final ChainKnotEntity primary;
    /**
     * The de facto target of this link. Mostly used to calculate positions.
     */
    @NotNull
    public final Entity secondary;
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
        if (!primary.level.isClientSide) {
            link.sendAttachChainPacket(primary.level);
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
        if (primary.level.isClientSide) return;

        double distance = primary.distanceTo(secondary);
        // step = spacing * ?(width^2 + width^2) / distance
        double step = COLLIDER_SPACING * Math.sqrt(Math.pow(ModEntityTypes.CHAIN_COLLISION.get().getWidth(), 2) * 2) / distance;
        double v = step;
        // reserve space for the center collider
        double centerHoldout = ModEntityTypes.CHAIN_COLLISION.get().getWidth() / distance;

        while (v < 0.5 - centerHoldout) {
            Entity collider1 = spawnCollision(false, primary, secondary, v);
            if (collider1 != null) collisionStorage.add(collider1.getId());
            Entity collider2 = spawnCollision(true, primary, secondary, v);
            if (collider2 != null) collisionStorage.add(collider2.getId());

            v += step;
        }

        Entity centerCollider = spawnCollision(false, primary, secondary, 0.5);
        if (centerCollider != null) collisionStorage.add(centerCollider.getId());
    }

    /**
     * Send a package to all the clients around this entity that notifies them of this link's creation.
     */
    private void sendAttachChainPacket(Level world) {
        assert world instanceof ServerLevel;

        Set<ServerPlayer> trackingPlayers = getTrackingPlayers(world);

        S2CChainAttachPacket packet = new S2CChainAttachPacket(primary.getId(), secondary.getId(), ForgeRegistries.ITEMS.getKey(sourceItem));

        for (ServerPlayer player : trackingPlayers) {
            ModPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
        }
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
        assert primary.level instanceof ServerLevel;
        Vec3 startPos = start.position().add(start.getLeashOffset(0));
        Vec3 endPos = end.position().add(end.getLeashOffset(0));

        Vec3 tmp = endPos;
        if (reverse) {
            endPos = startPos;
            startPos = tmp;
        }

        Vector3f offset = Helper.getChainOffset(startPos, endPos);
        startPos = startPos.add(offset.x(), 0, offset.z());
        endPos = endPos.add(-offset.x(), 0, -offset.z());

        double distance = startPos.distanceTo(endPos);

        double x = Mth.lerp(v, startPos.x(), endPos.x());
        double y = startPos.y() + Helper.drip2((v * distance), distance, endPos.y() - startPos.y());
        double z = Mth.lerp(v, startPos.z(), endPos.z());

        y += -ModEntityTypes.CHAIN_COLLISION.get().getHeight() + 2 / 16f;

        ChainCollisionEntity c = new ChainCollisionEntity(primary.level, x, y, z, this);
        if (primary.level.addFreshEntity(c)) {
            return c;
        } else {
            ConnectibleChains.LOGGER.warn("Tried to summon collision entity for a chain, failed to do so");
            return null;
        }
    }

    /**
     * Finds all players that are in {@code world} and tracking either the primary or secondary.
     *
     * @param world the world to search in
     * @return A set of all players that track the primary or secondary.
     */
    private Set<ServerPlayer> getTrackingPlayers(Level world) {
        assert world instanceof ServerLevel;
        Set<ServerPlayer> trackingPlayers = new HashSet<>(
                around((ServerLevel) world, primary.blockPosition(), ChainKnotEntity.VISIBLE_RANGE));
        trackingPlayers.addAll(
                around((ServerLevel) world, secondary.blockPosition(), ChainKnotEntity.VISIBLE_RANGE));
        return trackingPlayers;
    }

    private Collection<ServerPlayer> around(ServerLevel level, Vec3i pos, double radius) {
        double radiusSq = radius * radius;
        Objects.requireNonNull(level, "The world cannot be null");

        return level.players().stream().filter(p -> p.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) <= radiusSq)
                .collect(Collectors.toList());
    }

    public boolean isDead() {
        return !alive;
    }

    /**
     * Returns the squared distance between the primary and secondary.
     */
    public double getSquaredDistance() {
        return this.primary.distanceToSqr(secondary);
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

        boolean partnersEqual = primary.equals(link.primary) && secondary.equals(link.secondary) ||
                primary.equals(link.secondary) && secondary.equals(link.primary);
        return alive == link.alive && partnersEqual;
    }

    @Override
    public int hashCode() {
        return Objects.hash(primary, secondary, alive);
    }

    /**
     * If due to some error, or unforeseeable causes such as commands
     * the link still exists but needs to be destroyed.
     *
     * @return true when {@link #destroy(boolean)} needs to be called
     */
    public boolean needsBeDestroyed() {
        return primary.isRemoved() || secondary.isRemoved();
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
        Level world = primary.level;
        this.alive = false;

        if (world.isClientSide) return;

        if (secondary instanceof Player player && player.isCreative()) drop = false;
        // I think DO_TILE_DROPS makes more sense than DO_ENTITY_DROPS in this case
        if (!world.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)) drop = false;

        if (drop) {
            ItemStack stack = new ItemStack(sourceItem);
            if (secondary instanceof Player player) {
                player.addItem(stack);
            } else {
                Vec3 middle = Helper.middleOf(primary.position(), secondary.position());
                ItemEntity itemEntity = new ItemEntity(world, middle.x, middle.y, middle.z, stack);
                itemEntity.setDefaultPickUpDelay();
                world.addFreshEntity(itemEntity);
            }
        }

        destroyCollision();
        if (!primary.isRemoved() && !secondary.isRemoved())
            sendDetachChainPacket(world);
    }

    /**
     * Removes the collision entities associated with this link.
     */
    private void destroyCollision() {
        for (Integer entityId : collisionStorage) {
            Entity e = primary.level.getEntity(entityId);
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
    private void sendDetachChainPacket(Level world) {
        assert world instanceof ServerLevel;

        Set<ServerPlayer> trackingPlayers = getTrackingPlayers(world);

        // Write both ids so that the client can identify the link
        S2CChainDetachPacket packet = new S2CChainDetachPacket(primary.getId(), secondary.getId());

        for (ServerPlayer player : trackingPlayers) {
            ModPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
        }
    }
}
