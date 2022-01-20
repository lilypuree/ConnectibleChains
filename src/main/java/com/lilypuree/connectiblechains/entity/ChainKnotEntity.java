
/*
 * Copyright (C) 2022 legoatoom
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.lilypuree.connectiblechains.entity;

import com.lilypuree.connectiblechains.ConnectibleChains;
import com.lilypuree.connectiblechains.network.ModPacketHandler;
import com.lilypuree.connectiblechains.network.S2CChainAttachPacket;
import com.lilypuree.connectiblechains.network.S2CChainDetachPacket;
import com.lilypuree.connectiblechains.util.Helper;
import com.mojang.math.Vector3f;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.Tags;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.*;

/**
 * The ChainKnotEntity is the main entity of this mod.
 * It has connections between others of its kind, and is a combination of {@link net.minecraft.world.entity.Mob}
 * and {@link net.minecraft.world.entity.decoration.LeashFenceKnotEntity}.
 *
 * @author legoatoom
 */
public class ChainKnotEntity extends HangingEntity {
    /**
     * The distance when it is visible.
     */
    private static final double VISIBLE_RANGE = 2048.0D;

    /**
     * The x/z distance between {@link ChainCollisionEntity ChainCollisionEntities}.
     * A value of 1 means they are "shoulder to shoulder"
     */
    private static final float COLLIDER_SPACING = 1.5f;

    /**
     * A map that holds a list of entity ids. These entities should be {@link ChainCollisionEntity ChainCollisionEntities}
     * The key is the entity id of the ChainKnot that this is connected to.
     */
    public final Map<Integer, ArrayList<Integer>> COLLISION_STORAGE;

    /**
     * A map of entities that this chain is connected to.
     * The entity can be a {@link Player} or a {@link ChainKnotEntity}.
     */
    private final Map<Integer, Entity> holdingEntities = new HashMap<>();

    /**
     * A counter of how many other chainsKnots connect to this.
     */
    private int holdersCount = 0;

    /**
     * The Tag that stores everything
     */
    private ListTag chainTags;

    /**
     * A timer integer for destroying this entity if it isn't connected anything.
     */
    private int obstructionCheckCounter;

    protected ChainKnotEntity(EntityType<? extends HangingEntity> entityType, Level level) {
        super(entityType, level);
        this.COLLISION_STORAGE = new HashMap<>();
    }

    public ChainKnotEntity(Level world, BlockPos pos) {
        super(ModEntityTypes.CHAIN_KNOT.get(), world, pos);
        this.setPos((double) pos.getX() + 0.5D, (double) pos.getY() + 0.5D, (double) pos.getZ() + 0.5D);
        this.COLLISION_STORAGE = new HashMap<>();
    }

    /**
     * This method tries to check if around the target there are other {@link ChainKnotEntity ChainKnotEntities} that
     * have a connection to this player, if so we make a chainKnot at the location and connect the chain to it and remove
     * the connection to the player.
     *
     * @param playerEntity the player wo tries to make a connection.
     * @param world        The current world.
     * @param pos          the position where we want to make a chainKnot.
     * @param chain        nullable chainKnot if one already has one, if null, we will make one only if we have to make a connection.
     * @return boolean, if it has made a connection.
     */
    public static Boolean tryAttachHeldChainsToBlock(Player playerEntity, Level world, BlockPos pos, @Nullable ChainKnotEntity chain) {
        boolean hasMadeConnection = false;
        double i = pos.getX();
        double j = pos.getY();
        double k = pos.getZ();
        List<ChainKnotEntity> list = world.getEntitiesOfClass(ChainKnotEntity.class,
                new AABB(i - getMaxRange(), j - getMaxRange(), k - getMaxRange(),
                        i + getMaxRange(), j + getMaxRange(), k + getMaxRange()));

        for (ChainKnotEntity otherKnots : list) {
            if (otherKnots.getHoldingEntities().contains(playerEntity)) {
                if (!otherKnots.equals(chain)) {
                    // We found a knot that is connected to the player and therefore needs to connect to the chain.
                    if (chain == null) {
                        chain = new ChainKnotEntity(world, pos);
                        world.addFreshEntity(chain);
                        chain.playPlacementSound();
                    }

                    otherKnots.attachChain(chain, true, playerEntity.getId());
                    hasMadeConnection = true;
                }
            }
        }
        return hasMadeConnection;
    }

    /**
     * The max range of the chain.
     */
    public static double getMaxRange() {
        return ConnectibleChains.runtimeConfig.getMaxChainRange();
    }

    /**
     * This entity does not want to set a facing.
     */
    @Override
    protected void setDirection(Direction pFacingDirection) {
    }

    /**
     * Update the position of this chain to the position of the block this is attached too.
     */
    @Override
    protected void recalculateBoundingBox() {
        this.setPosRaw((double) this.pos.getX() + 0.5D, (double) this.pos.getY() + 0.5D, (double) this.pos.getZ() + 0.5D);
        double w = this.getType().getWidth() / 2.0;
        double h = this.getType().getHeight();
        this.setBoundingBox(new AABB(this.getX() - w, this.getY(), this.getZ() - w, this.getX() + w, this.getY() + h, this.getZ() + w));
    }

    /**
     * This happens every tick.
     * It deletes the chainEntity if it is in the void.
     * It updates the chains, see {@link #updateChains()}
     * It checks if it is still connected to a block every 100 ticks.
     */
    @Override
    public void tick() {
        if (this.level.isClientSide) {
            return;
        }
        if (this.getY() < -64.0D) {
            this.outOfWorld();
        }
        this.updateChains();

        if (this.obstructionCheckCounter++ == 100) {
            this.obstructionCheckCounter = 0;
            if (!isRemoved() && !this.canStayAttached()) {
                ArrayList<Entity> list = this.getHoldingEntities();
                for (Entity entity : list) {
                    if (entity instanceof ChainKnotEntity) {
                        damageLink(false, (ChainKnotEntity) entity);
                    }
                }
                this.remove(RemovalReason.KILLED);
                this.dropItem(null);
            }
        }
    }

    public void setObstructionCheckCounter() {
        this.obstructionCheckCounter = 100;
    }

    /**
     * Simple checker to see if the block is connected to a fence or a wall.
     *
     * @return boolean - if it can stay attached.
     */
    public boolean canStayAttached() {
        Block block = this.level.getBlockState(this.pos).getBlock();
        return canConnectTo(block);
    }

    /**
     * Is this block acceptable to connect too?
     *
     * @param block the block in question.
     * @return boolean if is allowed or not.
     */
    public static boolean canConnectTo(Block block) {
        return BlockTags.WALLS.contains(block) || BlockTags.FENCES.contains(block);
    }

    @Override
    public boolean skipAttackInteraction(Entity attacker) {
        playSound(SoundEvents.CHAIN_HIT, 0.5F, 1.0F);
        if (attacker instanceof Player playerEntity) {
            return this.hurt(DamageSource.playerAttack(playerEntity), 0.0F);
        } else {
            return false;
        }
    }

    /**
     * When this entity is being attacked, we remove all connections and then remove this entity.
     *
     * @return if it is successfully attacked.
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        } else if (!this.level.isClientSide && !isRemoved()) {
            Entity sourceEntity = source.getEntity();
            if (source.getDirectEntity() instanceof AbstractArrow) {
                return false;
            }
            if (sourceEntity instanceof Player player) {
                boolean isCreative = ((Player) sourceEntity).isCreative();
                if (!player.getMainHandItem().isEmpty()
                        && Tags.Items.SHEARS.contains(player.getMainHandItem().getItem())) {
                    ArrayList<Entity> list = this.getHoldingEntities();
                    for (Entity entity : list) {
                        if (entity instanceof ChainKnotEntity) {
                            damageLink(isCreative, (ChainKnotEntity) entity);
                        }
                    }
                    this.dropItem(null);
                    this.remove(RemovalReason.KILLED);
                }
            }
            return true;
        } else {
            return !(source.getDirectEntity() instanceof AbstractArrow);
        }
    }

    /**
     * Method to write all connections in a {@link Tag} when we save the game.
     * It doesn't store the {@link #holdersCount} or {@link #COLLISION_STORAGE} since
     * they will be updated when connection are being remade when we read it.
     *
     * @param tag the tag to write info in.
     */
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        boolean b = false;
        ListTag listTag = new ListTag();
        for (Entity entity : this.holdingEntities.values()) {
            if (entity != null) {
                CompoundTag compoundTag = new CompoundTag();
                if (entity instanceof Player) {
                    UUID uuid = entity.getUUID();
                    compoundTag.putUUID("UUID", uuid);
                    b = true;
                } else if (entity instanceof HangingEntity) {
                    BlockPos blockPos = ((HangingEntity) entity).getPos();
                    compoundTag.putInt("X", blockPos.getX());
                    compoundTag.putInt("Y", blockPos.getY());
                    compoundTag.putInt("Z", blockPos.getZ());
                    b = true;
                }
                listTag.add(compoundTag);
            }
        }
        if (b) {
            tag.put("Chains", listTag);
        } else if (chainTags != null && !chainTags.isEmpty()) {
            tag.put("Chains", chainTags.copy());
        }
    }

    /**
     * Read all the info into the {@link #chainTags}
     * We do not make connections here because not all entities might be loaded yet.
     *
     * @param tag the tag to read from.
     */
    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("Chains")) {
            this.chainTags = tag.getList("Chains", 10);
        }
    }

    @Override
    public int getWidth() {
        return 9;
    }

    @Override
    public int getHeight() {
        return 9;
    }

    @Override
    public void dropItem(Entity entity) {
        this.playSound(SoundEvents.CHAIN_BREAK, 1.0F, 1.0F);
    }

    @Override
    public void playPlacementSound() {
        this.playSound(SoundEvents.CHAIN_PLACE, 1.0F, 1.0F);
    }

    /**
     * This method will call {@link #deserializeChainTag(CompoundTag)} if the {@link #chainTags} has any tags.
     * It will also break all connections that are larger than the {@link #getMaxRange()}
     */
    private void updateChains() {
        if (chainTags != null) {
            ListTag copy = chainTags.copy();
            for (Tag tag : copy) {
                assert tag instanceof CompoundTag;
                this.deserializeChainTag(((CompoundTag) tag));
            }
        }

        Entity[] entitySet = holdingEntities.values().toArray(new Entity[0]).clone();
        for (Entity entity : entitySet) {
            if (entity == null) continue;
            if (!this.isAlive() || !entity.isAlive() || entity.position().distanceToSqr(this.position()) > getMaxRange() * getMaxRange()) {
                if (entity instanceof ChainKnotEntity knot) {
                    damageLink(false, knot);
                    continue;
                }

                boolean drop = true;
                if (entity instanceof Player player) {
                    drop = !player.isCreative();
                }
                this.detachChain(entity, true, drop);
                dropItem(null);
            }
        }
    }

    /**
     * Get method for all the entities that we are connected to.
     *
     * @return ArrayList with Entities - These entities are {@link Player PlayerEntities} or {@link ChainKnotEntity ChainKnotEntities}
     */
    public ArrayList<Entity> getHoldingEntities() {
        if (this.level.isClientSide) {
            for (Integer id : holdingEntities.keySet()) {
                if (id != 0 && holdingEntities.get(id) == null) {
                    holdingEntities.put(id, this.level.getEntity(id));
                }
            }
        }
        return new ArrayList<>(holdingEntities.values());
    }

    /**
     * Destroy the collisions between two chains, and delete the endpoint if it doesn't have any other connection.
     *
     * @param doNotDrop if we should not drop an item.
     * @param endChain  the entity that this is connected to.
     */
    void damageLink(boolean doNotDrop, ChainKnotEntity endChain) {
        if (!this.getHoldingEntities().contains(endChain))
            return; // We cannot destroy a connection that does not exist.
        if (endChain.holdersCount <= 1 && endChain.getHoldingEntities().isEmpty()) {
            endChain.remove(RemovalReason.KILLED);
        }
        this.deleteCollision(endChain);
        this.detachChain(endChain, true, !doNotDrop);
        dropItem(null);
    }

    /**
     * This method tries to connect to an entity that is in the {@link #chainTags}.
     * If they do not exist yet, we skip them. If they do, make a connection and remove it from the tag.
     * <p>
     * If when the {@link #tickCount} of this entity is bigger than 100, we remove the tag from the {@link #chainTags}
     * meaning that we cannot find the connection anymore and we assume that it will not be loaded in the future.
     *
     * @param tag the tag that contains a single connection.
     * @see #updateChains()
     */
    private void deserializeChainTag(CompoundTag tag) {
        if (tag != null && this.level instanceof ServerLevel) {
            if (tag.contains("UUID")) {
                UUID uuid = tag.getUUID("UUID");
                Entity entity = ((ServerLevel) this.level).getEntity(uuid);
                if (entity != null) {
                    this.attachChain(entity, true, 0);
                    this.chainTags.remove(tag);
                    return;
                }
            } else if (tag.contains("X")) {
                BlockPos blockPos = new BlockPos(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z"));
                ChainKnotEntity entity = ChainKnotEntity.getOrCreate(this.level, blockPos, true);
                if (entity != null) {
                    this.attachChain(Objects.requireNonNull(ChainKnotEntity.getOrCreate(this.level, blockPos, false)), true, 0);
                    this.chainTags.remove(tag);
                }
                return;
            }

            // At the start the server and client need to tell each other the info.
            // So we need to check if the object is old enough for these things to exist before we delete them.
            if (this.tickCount > 100) {
                this.spawnAtLocation(Items.CHAIN);
                this.chainTags.remove(tag);
            }
        }
    }

    /**
     * Attach this chain to an entity.
     *
     * @param entity             The entity to connect to.
     * @param sendPacket         Whether we send a packet to the client.
     * @param fromPlayerEntityId the entityID of the player that this connects to. 0 if it is a chainKnot.
     * @return Returns false if the entity already has a connection with us or vice versa.
     */
    public boolean attachChain(Entity entity, boolean sendPacket, int fromPlayerEntityId) {
        if (this.holdingEntities.containsKey((entity.getId()))) {
            return false;
        }
        if (entity instanceof ChainKnotEntity knot && knot.holdingEntities.containsKey(this.getId())) {
            return false;
        }

        this.holdingEntities.put(entity.getId(), entity);

        if (fromPlayerEntityId != 0) {
            removePlayerWithId(fromPlayerEntityId);
        }

        if (!this.level.isClientSide && sendPacket && this.level instanceof ServerLevel) {
            if (entity instanceof ChainKnotEntity) {
                ((ChainKnotEntity) entity).holdersCount++;
                createCollision(entity);
            }
            sendAttachChainPacket(entity.getId(), fromPlayerEntityId);
        }
        return true;
    }

    /**
     * Remove a link between this chainKnot and a player or other chainKnot.
     *
     * @param entity     the entity it is connected to.
     * @param sendPacket should we send a packet to the client?
     * @param dropItem   should we drop an item?
     */
    public void detachChain(Entity entity, boolean sendPacket, boolean dropItem) {
        if (entity == null) return;

        this.holdingEntities.remove(entity.getId());
        if (!this.level.isClientSide && dropItem && this.level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            Vec3 middle = Helper.middleOf(position(), entity.position());
            ItemEntity entity1 = new ItemEntity(level, middle.x, middle.y, middle.z, new ItemStack(Items.CHAIN));
            entity1.setDefaultPickUpDelay();
            this.level.addFreshEntity(entity1);
        }

        if (!this.level.isClientSide() && sendPacket && this.level instanceof ServerLevel) {
            if (entity instanceof ChainKnotEntity) {
                ((ChainKnotEntity) entity).holdersCount--;
            }
            if (this.holdersCount <= 0 && getHoldingEntities().isEmpty()) {
                this.remove(RemovalReason.DISCARDED);
            }
            deleteCollision(entity);
            sendDetachChainPacket(entity.getId());
        }
    }

    /**
     * Create a collision between this and an entity.
     * It spawns multiple {@link ChainCollisionEntity ChainCollisionEntities} that are equal distance from each other.
     * Position is the same no matter what if the connection is from A -> B or A <- B.
     *
     * @param entity the entity to create collisions too.
     * @see ChainCollisionEntity
     */
    private void createCollision(Entity entity) {
        //Safety check!
        if (COLLISION_STORAGE.containsKey(entity.getId())) return;

        double distance = this.distanceTo(entity);
        double step = COLLIDER_SPACING * Math.sqrt(Math.pow(ModEntityTypes.CHAIN_COLLISION.get().getWidth(), 2) * 2) / distance;
        double v = step;
        double centerHoldout = ModEntityTypes.CHAIN_COLLISION.get().getWidth() / distance;

        ArrayList<Integer> entityIdList = new ArrayList<>();
        while (v < 0.5 - centerHoldout) {
            Entity collider1 = spawnCollision(false, this, entity, v);
            if (collider1 != null) entityIdList.add(collider1.getId());
            Entity collider2 = spawnCollision(true, this, entity, v);
            if (collider2 != null) entityIdList.add(collider2.getId());

            v += step;
        }

        Entity centerCollider = spawnCollision(false, this, entity, 0.5);
        if (centerCollider != null) entityIdList.add(centerCollider.getId());

        this.COLLISION_STORAGE.put(entity.getId(), entityIdList);
    }

    /**
     * Spawns a collider at v percent between entity1 and entity2
     *
     * @param reverse Reverse start and end
     * @param start   the entity at v=0
     * @param end     the entity at v=1
     * @param v       percent of the distance
     * @return {@link ChainCollisionEntity} or null
     */
    @Nullable
    private Entity spawnCollision(boolean reverse, Entity start, Entity end, double v) {
        Vec3 startPos = start.position().add(start.getLeashOffset());
        Vec3 endPos = end.position().add(end.getLeashOffset());

        Vec3 tmp = endPos;
        if (reverse) {
            endPos = startPos;
            startPos = tmp;
        }

        Vector3f offset = Helper.getChainOffset(startPos, endPos);
        startPos = startPos.add(offset.x(), 0, offset.z());
        endPos = endPos.add(-offset.x(), 0, -offset.z());

        double distance = startPos.distanceTo(endPos);

        double x = Mth.lerp(v, startPos.x, endPos.x);
        double y = startPos.y + Helper.drip2((v * distance), distance, endPos.y - startPos.y);
        double z = Mth.lerp(v, startPos.z, endPos.z);

        y += -ModEntityTypes.CHAIN_COLLISION.get().getHeight() + 1 / 16f;

        ChainCollisionEntity c = new ChainCollisionEntity(this.level, x, y, z, start.getId(), end.getId());
        if (level.addFreshEntity(c)) {
            return c;
        } else {
            LOGGER.warn("Tried to summon collision entity for a chain, failed to do so");
            return null;
        }
    }

    /**
     * Remove a collision between this and an entity.
     *
     * @param entity the entity in question.
     */
    private void deleteCollision(Entity entity) {
        int entityId = entity.getId();
        ArrayList<Integer> entityIdList = this.COLLISION_STORAGE.get(entityId);
        if (entityIdList != null) {
            entityIdList.forEach(id -> {
                Entity e = level.getEntity(id);
                if (e instanceof ChainCollisionEntity) {
                    e.remove(RemovalReason.DISCARDED);
                }
            });
        }
        this.COLLISION_STORAGE.remove(entityId);
    }

    /**
     * Get or create a chainKnot in a location.
     *
     * @param world      the world.
     * @param pos        the location to check.
     * @param hasToExist boolean that specifies if the chainKnot has to exist, if this is true and we cannot find
     *                   a knot, it will return null.
     * @return {@link ChainKnotEntity} or null
     */
    @Nullable
    public static ChainKnotEntity getOrCreate(Level world, BlockPos pos, Boolean hasToExist) {
        int posX = pos.getX();
        int posY = pos.getY();
        int posZ = pos.getZ();
        final List<ChainKnotEntity> list = world.getEntitiesOfClass(ChainKnotEntity.class,
                new AABB((double) posX - 1.0D, (double) posY - 1.0D, (double) posZ - 1.0D,
                        (double) posX + 1.0D, (double) posY + 1.0D, (double) posZ + 1.0D));
        Iterator<ChainKnotEntity> iterator = list.iterator();

        ChainKnotEntity surroundingChains;
        do {
            if (!iterator.hasNext()) {
                if (hasToExist) {
                    // If it has to exist and it doesn't, we return null.
                    return null;
                }
                ChainKnotEntity newChain = new ChainKnotEntity(world, pos);
                world.addFreshEntity(newChain);
                newChain.playPlacementSound();
                return newChain;
            }

            surroundingChains = iterator.next();
        } while (surroundingChains == null || !surroundingChains.getPos().equals(pos));

        return surroundingChains;
    }

    /**
     * Send to all players around that this chain wants to attach to another entity.
     *
     * @param entityId           the entity to connect to.
     * @param fromPlayerEntityId the {@link Player} id that made the connection.
     */
    private void sendAttachChainPacket(int entityId, int fromPlayerEntityId) {
        S2CChainAttachPacket packet = new S2CChainAttachPacket(fromPlayerEntityId, new int[]{this.getId(), entityId});
        ModPacketHandler.INSTANCE.send(PacketDistributor.TRACKING_ENTITY.with(() -> this), packet);
    }

    /**
     * Send a package to all the clients around this entity that specifies it want's to detach.
     *
     * @param entityId the entity id that it wants to connect to.
     */
    private void sendDetachChainPacket(int entityId) {
        assert !this.level.isClientSide();
        S2CChainDetachPacket packet = new S2CChainDetachPacket(new int[]{this.getId(), entityId});
        ModPacketHandler.INSTANCE.send(PacketDistributor.TRACKING_ENTITY.with(() -> this), packet);
    }

    /**
     * Remove a player id from the {@link #holdingEntities list}
     *
     * @param playerId the id of the player.
     */
    private void removePlayerWithId(int playerId) {
        this.holdingEntities.remove(playerId);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < VISIBLE_RANGE;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (this.level.isClientSide) {
            return InteractionResult.SUCCESS;
        } else {
            boolean madeConnection = tryAttachHeldChainsToBlock(player, level, getPos(), this);

            if (!madeConnection) {
                if (this.getHoldingEntities().contains(player)) {
                    dropItem(null);
                    detachChain(player, true, false);
                    if (!player.isCreative()) {
                        player.addItem(new ItemStack(Items.CHAIN));
                    }
                } else if (player.getItemInHand(hand).getItem().equals(Items.CHAIN)) {
                    playPlacementSound();
                    attachChain(player, true, 0);
                    if (!player.isCreative()) {
                        player.getItemInHand(hand).shrink(1);
                    }
                } else {
                    hurt(DamageSource.playerAttack(player), 0);
                }
            } else {
                playPlacementSound();
            }

            return InteractionResult.CONSUME;
        }
    }

    @Override
    protected float getEyeHeight(Pose pPose, EntityDimensions pSize) {
        return -0.0625F;
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public Vec3 getLeashOffset() {
        return new Vec3(0, 5 / 16f, 0);
    }

    @Override
    public Vec3 getRopeHoldPosition(float pPartialTicks) {
        return this.getPosition(pPartialTicks).add(0.0D, 5 / 16f, 0.0D);
    }

    /**
     * Client method to keep track of all the entities that it holds.
     * Adds an id that it holds, removes the player id if applicable.
     *
     * @param id           the id that we connect to.
     * @param fromPlayerId the id from the player it was from, 0 if this was not applicable.
     * @see S2CChainAttachPacket
     */
    @OnlyIn(Dist.CLIENT)
    public void addHoldingEntityId(int id, int fromPlayerId) {
        if (fromPlayerId != 0) {
            this.holdingEntities.remove(fromPlayerId);
        }
        this.holdingEntities.put(id, null);
    }

    /**
     * Client method to keep track of all the entities that it holds.
     * Removes a id that it holds.
     *
     * @param id the id that we do not connect to anymore.
     * @see S2CChainDetachPacket
     */
    @OnlyIn(Dist.CLIENT)
    public void removeHoldingEntityId(int id) {
        this.holdingEntities.remove(id);
    }

    /**
     * Multiple version of {@link #addHoldingEntityId(int id, int playerFromId)}
     * This version does not have a playerFromId, since this only is called when the world is loaded and
     * all previous connection need to be remade.
     *
     * @param ids array of ids to connect to.
     * @see com.lilypuree.connectiblechains.mixin.server.world.ChunkMapMixin
     */
    @OnlyIn(Dist.CLIENT)
    public void addHoldingEntityIds(int[] ids) {
        for (int id : ids) this.holdingEntities.put(id, null);
    }

    @Override
    public ItemStack getPickedResult(HitResult target) {
        return new ItemStack(Items.CHAIN);
    }
}
