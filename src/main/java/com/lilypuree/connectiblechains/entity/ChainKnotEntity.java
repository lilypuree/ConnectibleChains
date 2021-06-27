package com.lilypuree.connectiblechains.entity;

import com.lilypuree.connectiblechains.network.ModPacketHandler;
import com.lilypuree.connectiblechains.network.S2CChainAttachPacket;
import com.lilypuree.connectiblechains.network.S2CChainDetachPacket;
import com.lilypuree.connectiblechains.util.Helper;
import net.minecraft.block.Block;
import net.minecraft.entity.*;
import net.minecraft.entity.item.HangingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.Tags;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The ChainKnotEntity is the main entity of this mod.
 * It has connections between others of it's kind, and is a combination of {@link MobEntity}
 * and {@link net.minecraft.entity.item.LeashKnotEntity}.
 *
 * @author legoatoom
 */
public class ChainKnotEntity extends HangingEntity implements IEntityAdditionalSpawnData {

    /**
     * The max range of the chain.
     */
    private static final double MAX_RANGE = 7d;

    /**
     * The distance when it is visible.
     */
    public static final double VISIBLE_RANGE = 2048.0D;

    /**
     * A map that holds a list of entity ids. These entities should be {@link ChainCollisionEntity ChainCollisionEntities}
     * The key is the entity id of the ChainKnot that this is connected to.
     */
    private final Map<Integer, ArrayList<Integer>> COLLISION_STORAGE;

    /**
     * A map of entities that this chain is connected to.
     * The entity can be a {@link PlayerEntity} or a {@link ChainKnotEntity}.
     */
    private final Map<Integer, Entity> holdingEntities = new HashMap<>();

    /**
     * A counter of how many other chainsKnots connect to this.
     */
    private int holdersCount = 0;

    /**
     * The Tag that stores everything
     */
    private ListNBT chainTags;


    /**
     * A timer integer for destroying this entity if it isn't connected anything.
     */
    private int obstructionCheckCounter;

    public ChainKnotEntity(EntityType<? extends ChainKnotEntity> entityType, World world) {
        super(entityType, world);
        this.COLLISION_STORAGE = new HashMap<>();
    }

    public ChainKnotEntity(World world, BlockPos pos) {
        super(ModEntityTypes.CHAIN_KNOT.get(), world, pos);
        this.absMoveTo((double) pos.getX() + 0.5D, (double) pos.getY() + 0.5D, (double) pos.getZ() + 0.5D);
        this.setBoundingBox(new AxisAlignedBB(this.getX() - 0.1875D, this.getY() - 0.25D + 0.125D, this.getZ() - 0.1875D, this.getX() + 0.1875D, this.getY() + 0.25D + 0.125D, this.getZ() + 0.1875D));
        this.forcedLoading = true;
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
    public static Boolean tryAttachHeldChainsToBlock(PlayerEntity playerEntity, World world, BlockPos pos, @Nullable ChainKnotEntity chain) {
        boolean hasMadeConnection = false;
        double i = pos.getX();
        double j = pos.getY();
        double k = pos.getZ();
        List<ChainKnotEntity> list = world.getEntitiesOfClass(ChainKnotEntity.class,
                new AxisAlignedBB(i - MAX_RANGE, j - MAX_RANGE, k - MAX_RANGE,
                        i + MAX_RANGE, j + MAX_RANGE, k + MAX_RANGE));
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

    @Override
    protected void setDirection(Direction facing) {
    }

    /**
     * Update the position of this chain to the position of the block this is attached too.
     */
    @Override
    protected void recalculateBoundingBox() {
        this.setPosRaw((double) this.pos.getX() + 0.5D, (double) this.pos.getY() + 0.5D, (double) this.pos.getZ() + 0.5D);
        this.setBoundingBox(new AxisAlignedBB(this.getX() - 0.1875D, this.getY() - 0.25D + 0.125D, this.getZ() - 0.1875D, this.getX() + 0.1875D, this.getY() + 0.25D + 0.125D, this.getZ() + 0.1875D));
    }

    /**
     * This happens every tick.
     * It deletes the chainEntity if it is in the void.
     * It updates the chains, see {@link #updateChains()}
     * It checks if it is still connected to a block every 100 ticks.
     */
    @Override
    public void tick() {
        if (!this.level.isClientSide) {
            if (this.getY() < -64.0D) {
                this.remove();
            }
            this.updateChains();

            if (this.obstructionCheckCounter++ == 100) {
                this.obstructionCheckCounter = 0;
                if (!this.removed && !this.canStayAttached()) {
                    ArrayList<Entity> list = this.getHoldingEntities();
                    for (Entity entity : list) {
                        if (entity instanceof ChainKnotEntity) {
                            damageLink(false, (ChainKnotEntity) entity);
                        }
                    }
                    this.remove();
                    this.dropItem(null);
                }
            }
        }
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
        return block.is(BlockTags.WALLS) || block.is(BlockTags.FENCES);
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
        } else if (!this.level.isClientSide && !this.removed) {
            Entity sourceEntity = source.getEntity();
            if (sourceEntity instanceof PlayerEntity) {
                boolean isCreative = ((PlayerEntity) sourceEntity).isCreative();
                if (!((PlayerEntity) sourceEntity).getMainHandItem().isEmpty()
                        && ((PlayerEntity) sourceEntity).getMainHandItem().getItem().is(Tags.Items.SHEARS)) {
                    breakChain(isCreative);
                }
            }
            return true;
        } else {
            return true;
        }
    }

    public void breakChain(boolean doNotDrop) {
        ArrayList<Entity> list = this.getHoldingEntities();
        for (Entity entity : list) {
            if (entity instanceof ChainKnotEntity) {
                damageLink(doNotDrop, (ChainKnotEntity) entity);
            }
        }

        double i = pos.getX();
        double j = pos.getY();
        double k = pos.getZ();
        List<ChainKnotEntity> chainList = level.getEntitiesOfClass(ChainKnotEntity.class,
                new AxisAlignedBB(i - MAX_RANGE, j - MAX_RANGE, k - MAX_RANGE,
                        i + MAX_RANGE, j + MAX_RANGE, k + MAX_RANGE));
        for (ChainKnotEntity otherKnots : chainList) {
            if (otherKnots.getHoldingEntities().contains(this)) {
                otherKnots.damageLink(doNotDrop, this);
            }
        }

        this.dropItem(null);
        this.remove();
    }

    /**
     * Method to write all connections in a {@link CompoundNBT} when we save the game.
     * It doesn't store the {@link #holdersCount} or {@link #COLLISION_STORAGE} since
     * they will be updated when connection are being remade when we read it.
     *
     * @param tag the tag to write info in.
     */
    @Override
    public void addAdditionalSaveData(CompoundNBT tag) {
        boolean b = false;
        ListNBT listTag = new ListNBT();
        for (Entity entity : this.holdingEntities.values()) {
            if (entity != null) {
                CompoundNBT compoundTag = new CompoundNBT();
                if (entity instanceof PlayerEntity) {
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
    public void readAdditionalSaveData(CompoundNBT tag) {
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
    public void dropItem(@Nullable Entity entity) {
        this.playSound(SoundEvents.CHAIN_BREAK, 1.0f, 1.0f);
    }

    @Override
    public void playPlacementSound() {
        this.playSound(SoundEvents.CHAIN_PLACE, 1.0f, 1.0f);
    }

    @Override
    public void absMoveTo(double x, double y, double z) {
        super.absMoveTo((double) MathHelper.floor(x) + 0.5D, (double) MathHelper.floor(y) + 0.5D, (double) MathHelper.floor(z) + 0.5D);
    }

    /**
     * This method will call {@link #deserializeChainTag(CompoundNBT)} if the {@link #chainTags} has any tags.
     * It will also break all connections that are larger than the {@link #MAX_RANGE}
     */
    protected void updateChains() {
        if (chainTags != null) {
            ListNBT copy = chainTags.copy();
            for (INBT tag : copy) {
                assert tag instanceof CompoundNBT;
                this.deserializeChainTag(((CompoundNBT) tag));
            }
        }

        Entity[] entitySet = holdingEntities.values().toArray(new Entity[0]).clone();
        for (Entity entity : entitySet) {
            if (entity != null) {
                if (!this.isAlive() || !entity.isAlive() || entity.position().distanceToSqr(this.position()) > MAX_RANGE * MAX_RANGE) {
                    this.detachChain(entity, true, !(entity instanceof PlayerEntity && ((PlayerEntity) entity).isCreative()));
                    dropItem(null);
                }
            }
        }
    }

    /**
     * Get method for all the entities that we are connected to.
     *
     * @return ArrayList with Entities - These entities are {@link PlayerEntity PlayerEntities} or {@link ChainKnotEntity ChainKnotEntities}
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
    public void damageLink(boolean doNotDrop, ChainKnotEntity endChain) {
        if (!this.getHoldingEntities().contains(endChain))
            return; // We cannot destroy a connection that does not exist.
        if (endChain.holdersCount <= 1 && endChain.getHoldingEntities().isEmpty()) {
            endChain.remove();
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
    private void deserializeChainTag(CompoundNBT tag) {
        if (tag != null && this.level instanceof ServerWorld) {
            if (tag.contains("UUID")) {
                UUID uuid = tag.getUUID("UUID");
                Entity entity = ((ServerWorld) this.level).getEntity(uuid);
                if (entity != null) {
                    this.attachChain(entity, true, 0);
                    this.chainTags.remove(tag);
                    return;
                }
            } else if (tag.contains("X")) {
                BlockPos blockPos = new BlockPos(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z"));
                ChainKnotEntity entity = ChainKnotEntity.getOrCreate(this.level, blockPos, true);
                if (entity != null) {
                    this.attachChain(ChainKnotEntity.getOrCreate(this.level, blockPos, false), true, 0);
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
     * @param fromPlayerEntityId the entityID of the player that this connects to. 0 if it a chainKnot.
     */
    public void attachChain(Entity entity, boolean sendPacket, int fromPlayerEntityId) {
        this.holdingEntities.put(entity.getId(), entity);
        this.forcedLoading = true;
        if (!(entity instanceof PlayerEntity)) {
            entity.forcedLoading = true;
        }

        if (fromPlayerEntityId != 0) {
            removePlayerWithId(fromPlayerEntityId);
        }

        if (!this.level.isClientSide && sendPacket && this.level instanceof ServerWorld) {
            if (entity instanceof ChainKnotEntity) {
                ((ChainKnotEntity) entity).holdersCount++;
                createCollision(entity);
            }
            sendAttachChainPacket(entity.getId(), fromPlayerEntityId);
        }
    }

    /**
     * Remove a link between this chainKnot and a player or other chainKnot.
     *
     * @param entity     the entity it is connected to.
     * @param sendPacket should we send a packet to the client?
     * @param dropItem   should we drop an item?
     */
    public void detachChain(Entity entity, boolean sendPacket, boolean dropItem) {
        if (entity != null) {
            if (this.holdingEntities.size() <= 1) {
                this.forcedLoading = false;
            }
            if (entity instanceof ChainKnotEntity) {
                if (((ChainKnotEntity) entity).holdingEntities.isEmpty()) {
                    entity.forcedLoading = false;
                }
            }

            this.holdingEntities.remove(entity.getId());
            if (!this.level.isClientSide && dropItem && this.level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
                Vector3d middle = Helper.middleOf(position(), entity.position());
                ItemEntity entity1 = new ItemEntity(level, middle.x, middle.y, middle.z, new ItemStack(Items.CHAIN));
                entity1.setDefaultPickUpDelay();
                this.level.addFreshEntity(entity1);
            }

            if (!this.level.isClientSide && sendPacket && this.level instanceof ServerWorld) {
                if (entity instanceof ChainKnotEntity) {
                    ((ChainKnotEntity) entity).holdersCount--;
                    if (this.holdersCount <= 0 && getHoldingEntities().isEmpty()) {
                        this.remove();
                    }
                }
                deleteCollision(entity);
                sendDetachChainPacket(entity.getId());
            }
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
        double distance = this.getPos().distManhattan(entity.blockPosition());
        double a = .5 / distance;
        double v = a;

        ArrayList<Integer> entityIdList = new ArrayList<>();
        double x, y, z;
        double offset = 0.2D;
        while (v <= 1 - (a / 2)) {
            x = MathHelper.lerp(v, this.getX(), entity.getX());
            y = MathHelper.lerp(v, this.getY(), entity.getY()) + Helper.drip(v * distance, distance) + offset;
            z = MathHelper.lerp(v, this.getZ(), entity.getZ());
            ChainCollisionEntity c = new ChainCollisionEntity(this.level, x - .15, y, z - .15, this.getId(), entity.getId());

            if (level.addFreshEntity(c)) {
                entityIdList.add(c.getId());
            } else {
                LOGGER.warn("Tried to summon collision entity for a chain, failed to do so");
            }
            v = v + a;
        }
        this.COLLISION_STORAGE.put(entity.getId(), entityIdList);
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
                    e.remove();
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
    public static ChainKnotEntity getOrCreate(World world, BlockPos pos, Boolean hasToExist) {
        int posX = pos.getX();
        int posY = pos.getY();
        int posZ = pos.getZ();
        final List<ChainKnotEntity> list = world.getEntitiesOfClass(ChainKnotEntity.class,
                new AxisAlignedBB((double) posX - 1.0D, (double) posY - 1.0D, (double) posZ - 1.0D,
                        (double) posX + 1.0D, (double) posY + 1.0D, (double) posZ + 1.0D));
        Iterator<ChainKnotEntity> var6 = list.iterator();

        ChainKnotEntity surroundingChains;
        do {
            if (!var6.hasNext()) {
                if (hasToExist) {
                    // If it has to exist and it doesn't, we return null.
                    return null;
                }
                ChainKnotEntity newChain = new ChainKnotEntity(world, pos);
                world.addFreshEntity(newChain);
                newChain.playPlacementSound();
                return newChain;
            }

            surroundingChains = var6.next();
        } while (surroundingChains == null || !surroundingChains.getPos().equals(pos));

        return surroundingChains;
    }

    /**
     * Send to all players around that this chain wants to attach to another entity.
     *
     * @param entityId           the entity to connect to.
     * @param fromPlayerEntityId the {@link PlayerEntity} id that made the connection.
     */
    public void sendAttachChainPacket(int entityId, int fromPlayerEntityId) {
        Stream<ServerPlayerEntity> watchingPlayers =
                around((ServerWorld) level, blockPosition(), VISIBLE_RANGE).stream();

        //Write our id and the id of the one we connect to.
        S2CChainAttachPacket packet = new S2CChainAttachPacket(new int[]{this.getId(), entityId}, fromPlayerEntityId);

        watchingPlayers.forEach(playerEntity ->
                ModPacketHandler.sendToClient(packet, playerEntity));
    }


    /**
     * Send a package to all the clients around this entity that specifies it want's to detach.
     *
     * @param entityId the entity id that it wants to connect to.
     */
    private void sendDetachChainPacket(int entityId) {
        if (this.level.isClientSide) return;

        Stream<ServerPlayerEntity> watchingPlayers =
                around((ServerWorld) level, blockPosition(), VISIBLE_RANGE).stream();

        //Write our id and the id of the one we connect to.
        S2CChainDetachPacket packet = new S2CChainDetachPacket(new int[]{this.getId(), entityId});

        watchingPlayers.forEach(playerEntity ->
                ModPacketHandler.sendToClient(packet, playerEntity));
    }


    /**
     * Remove a player id from the {@link #holdingEntities list}
     *
     * @param playerId the id of the player.
     */
    private void removePlayerWithId(int playerId) {
        this.holdingEntities.remove(playerId);
    }

    /**
     * Should we render this?
     *
     * @param distance the distance from the chainKnot.
     * @return boolean, yes or no.
     */
    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < VISIBLE_RANGE;
    }

    /**
     * Interaction of a player and this entity.
     * It will try to make new connections to the player or allow other chains that are connected to the player to
     * be made to this.
     *
     * @param player the player that interacted.
     * @param hand   the hand of the player.
     * @return ActionResult
     */
    @Override
    public ActionResultType interact(PlayerEntity player, Hand hand) {
        if (this.level.isClientSide) {
            return ActionResultType.SUCCESS;
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

            return ActionResultType.CONSUME;
        }
    }

    @Override
    protected float getEyeHeight(Pose pose, EntitySize dimensions) {
        return -0.0625F;
    }

    @Override
    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public Vector3d getRopeHoldPosition(float f) {
        return super.getRopeHoldPosition(f).add(0.0D, 0.2D, 0.0D);
    }

    /**
     * Client method to keep track of all the entities that it holds.
     * Adds an id that it holds, removes the player id if applicable.
     *
     * @param id           the id that we connect to.
     * @param fromPlayerId the id from the player it was from, 0 if this was not applicable.
     */
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
     */
    public void removeHoldingEntityId(int id) {
        this.holdingEntities.remove(id);
    }


    /**
     * Multiple version of {@link #addHoldingEntityId(int id, int playerFromId)}
     * This version does not have a playerFromId, since this only is called when the world is loaded and
     * all previous connection need to be remade.
     *
     * @param ids array of ids to connect to.
     * @see com.lilypuree.connectiblechains.mixin.server.world.ChunkManagerMixin
     */
    public void addHoldingEntityIds(int[] ids) {
        for (int id : ids) this.holdingEntities.put(id, null);
    }

    @Override
    public void writeSpawnData(PacketBuffer buffer) {

    }

    @Override
    public void readSpawnData(PacketBuffer additionalData) {
        Vector3d pos = this.position();
        setBoundingBox(new AxisAlignedBB(pos.x() - 0.1875D, pos.y() - 0.25D + 0.125D, pos.z() - 0.1875D,
                pos.x() + 0.1875D, pos.y() + 0.25D + 0.125D, pos.z() + 0.1875D));
        this.forcedLoading = true;
    }


    @Override
    public ItemStack getPickedResult(RayTraceResult target) {
        return new ItemStack(Items.CHAIN);
    }

    //fabric api methods

    public static Collection<ServerPlayerEntity> around(ServerWorld world, Vector3i pos, double radius) {
        double radiusSq = radius * radius;
        return (Collection) world(world).stream().filter((p) -> {
            return p.distanceToSqr((double) pos.getX(), (double) pos.getY(), (double) pos.getZ()) <= radiusSq;
        }).collect(Collectors.toList());
    }

    public static Collection<ServerPlayerEntity> world(ServerWorld world) {
        Objects.requireNonNull(world, "The world cannot be null");
        return Collections.unmodifiableCollection(world.players());
    }

}
