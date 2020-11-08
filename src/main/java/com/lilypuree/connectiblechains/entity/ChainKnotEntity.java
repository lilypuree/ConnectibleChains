/*
 *     Copyright (C) 2020 legoatoom
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.lilypuree.connectiblechains.entity;

import com.google.common.collect.Lists;
import com.lilypuree.connectiblechains.network.ModPacketHandler;
import com.lilypuree.connectiblechains.network.S2CChainAttachPacket;
import com.lilypuree.connectiblechains.network.S2CChainDetachPacket;
import io.netty.buffer.Unpooled;
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
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.network.datasync.IDataSerializer;
import net.minecraft.network.play.server.SSpawnObjectPacket;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ChainKnotEntity extends HangingEntity {
    public static final double MAX_RANGE = 7d;

    public static final IDataSerializer<List<Integer>> INTEGER_LIST = new IDataSerializer<List<Integer>>() {
        public void write(PacketBuffer buffer, List<Integer> list) {
            buffer.writeVarIntArray(list.stream().mapToInt(i -> i).toArray());
        }

        public List<Integer> read(PacketBuffer buffer) {
            return Arrays.stream(buffer.readVarIntArray()).boxed().collect(Collectors.toList());
        }

        public List<Integer> copy(List<Integer> list) {
            return new ArrayList<>(list);
        }
    };


    public static final DataParameter<List<Integer>> ATTACHED = EntityDataManager.defineId(ChainKnotEntity.class, INTEGER_LIST);
//    public static final DataParameter<Integer> ADJUSTMENT = EntityDataManager.defineId(ChainKnotEntity.class, DataSerializers.INT);


    private final Map<Integer, Entity> holdingEntities = new HashMap<>();
    public int holdersCount = 0;
    private int syncTicks = 0;
    private ListNBT chainTags;


    public ChainKnotEntity(World world) {
        super(ModEntityTypes.CHAIN_KNOT.get(), world);
    }

    public ChainKnotEntity(EntityType<? extends HangingEntity> entityType, World level) {
        super(entityType, level);
    }

    public ChainKnotEntity(World world, BlockPos pos) {
        super(ModEntityTypes.CHAIN_KNOT.get(), world, pos);
        this.setPos((double) pos.getX() + 0.5D, (double) pos.getY() + 0.5D, (double) pos.getZ() + 0.5D);
        this.setBoundingBox(new AxisAlignedBB(this.getX() - 0.1875D, this.getY() - 0.25D + 0.125D, this.getZ() - 0.1875D, this.getX() + 0.1875D, this.getY() + 0.25D + 0.125D, this.getZ() + 0.1875D));
        this.forcedLoading = true;
    }

    @Override
    public void setPos(double x, double y, double z) {
        super.setPos((double) MathHelper.floor(x) + 0.5D, (double) MathHelper.floor(y) + 0.5D, (double) MathHelper.floor(z) + 0.5D);
    }

    @Override
    protected void recalculateBoundingBox() {
        this.setPosRaw((double) this.pos.getX() + 0.5D, (double) this.pos.getY() + 0.5D, (double) this.pos.getZ() + 0.5D);
        this.setBoundingBox(new AxisAlignedBB(this.getX() - 0.1875D, this.getY() - 0.25D + 0.125D, this.getZ() - 0.1875D, this.getX() + 0.1875D, this.getY() + 0.25D + 0.125D, this.getZ() + 0.1875D));
    }

    @Override
    protected void setDirection(Direction p_174859_1_) {
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
    protected float getEyeHeight(Pose pose, EntitySize entitySize) {
        return -0.0625F;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 1024.0D;
    }

    @Override
    public void playPlacementSound() {
        this.playSound(SoundEvents.CHAIN_PLACE, 1.0F, 1.0F);
    }

    @Override
    public void dropItem(@Nullable Entity entity) {
        this.playSound(SoundEvents.CHAIN_BREAK, 1.0F, 1.0F);
    }

    @Override
    public void addAdditionalSaveData(CompoundNBT tag) {
        boolean holding = false;
        ListNBT listTag = new ListNBT();
        for (Entity entity : this.holdingEntities.values()) {
            if (entity != null) {
                CompoundNBT compoundTag = new CompoundNBT();
                if (entity instanceof PlayerEntity) {
                    UUID uuid = entity.getUUID();
                    compoundTag.putUUID("UUID", uuid);
                    holding = true;
                } else if (entity instanceof HangingEntity) {
                    BlockPos blockPos = ((HangingEntity) entity).getPos();
                    compoundTag.putInt("X", blockPos.getX());
                    compoundTag.putInt("Y", blockPos.getY());
                    compoundTag.putInt("Z", blockPos.getZ());
                    holding = true;
                }
                listTag.add(compoundTag);
            }
        }
        tag.putInt("holdersCount", holdersCount);
        if (holding) {
            tag.put("Chains", listTag);
        } else if (chainTags != null && !chainTags.isEmpty()) {
            tag.put("Chains", chainTags.copy());
        }
//        if (!this.holdingEntities.isEmpty()){
//            CompoundTag compoundTag = new CompoundTag();
//            if (this.holdingEntity instanceof PlayerEntity){
//                UUID uuid = this.holdingEntity.getUuid();
//                compoundTag.putUuid("UUID", uuid);
//            } else if (this.holdingEntity instanceof AbstractDecorationEntity) {
//                BlockPos blockPos = ((AbstractDecorationEntity) this.holdingEntity).getDecorationBlockPos();
//                compoundTag.putInt("X", blockPos.getX());
//                compoundTag.putInt("Y", blockPos.getY());
//                compoundTag.putInt("Z", blockPos.getZ());
//            }
//
//            tag.put("Chain", compoundTag);
//        } else if (this.chainTag != null){
//            tag.put("Chain", this.chainTag.copy());
//        }    }
    }

    @Override
    public void readAdditionalSaveData(CompoundNBT tag) {
        if (tag.contains("Chains")) {
            this.chainTags = tag.getList("Chains", 10);
        }
        holdersCount = tag.getInt("holdersCount");
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level.isClientSide()) {
            this.updateChains();
//            if (syncTicks > 0) {
//                this.entityData.set(ATTACHED, this.entityData.get(ATTACHED));
//                syncTicks--;
//            }
        }

    }

    @Override
    public ActionResultType interact(PlayerEntity player, Hand hand) {
        if (this.level.isClientSide()) {
            return ActionResultType.SUCCESS;
        } else {
            boolean attachedPlayerChain = false;
            List<ChainKnotEntity> nearbyChainKnots = this.level.getEntitiesOfClass(ChainKnotEntity.class, new AxisAlignedBB(this.getX() - MAX_RANGE, this.getY() - MAX_RANGE, this.getZ() - MAX_RANGE, this.getX() + MAX_RANGE, this.getY() + MAX_RANGE, this.getZ() + MAX_RANGE));
            Iterator<ChainKnotEntity> iterator = nearbyChainKnots.iterator();

            ChainKnotEntity nextKnot;
            while (iterator.hasNext()) {
                nextKnot = iterator.next();
                ArrayList<Entity> holdings = nextKnot.getHoldingEntities();
                if (holdings.contains(player) && !holdings.contains(this) && !this.holdingEntities.containsKey(nextKnot.getId()) && !nextKnot.equals(this)) {
                    nextKnot.attachChain(this, player.getId());
                    attachedPlayerChain = true;
                    break;
                }
            }

            if (!attachedPlayerChain) {
                if (this.getHoldingEntities().contains(player)) {
                    dropItem(null);
                    detachChain(player, false);
                    if (!player.isCreative()) {
                        player.getItemInHand(hand).grow(1);
                    }
                } else if (player.getItemInHand(hand).getItem().equals(Items.CHAIN)) {
                    playPlacementSound();
                    attachChain(player, 0);
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
    public boolean hurt(DamageSource source, float amount) {
        boolean bool = super.hurt(source, amount);
        if (!this.level.isClientSide()) {
            ArrayList<Entity> list = this.getHoldingEntities();
            for (Entity entity : list) {
                if (entity instanceof ChainKnotEntity && ((ChainKnotEntity) entity).holdersCount <= 1 && ((ChainKnotEntity) entity).getHoldingEntities().isEmpty()) {
                    entity.remove();
                }
                Vector3d middle = middleOf(position(), entity.position());
                ItemEntity item = new ItemEntity(level, middle.x, middle.y, middle.z, new ItemStack(Items.CHAIN));
                item.setDefaultPickUpDelay();
                this.level.addFreshEntity(item);
            }
        }
        return bool;
    }

    @Override
    public boolean survives() {
        return this.level.getBlockState(this.pos).getBlock().is(BlockTags.FENCES);
    }

    @SuppressWarnings("ConstantConditions")
    public static ChainKnotEntity getOrCreate(World world, BlockPos pos) {
        return getOrCreate(world, pos, false);
    }

    @Nullable
    public static ChainKnotEntity getOrCreate(World world, BlockPos pos, Boolean hasToExist) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        List<ChainKnotEntity> list = world.getEntitiesOfClass(ChainKnotEntity.class, new AxisAlignedBB((double) x - 1.0D, (double) y - 1.0D, (double) z - 1.0D, (double) x + 1.0D, (double) y + 1.0D, (double) z + 1.0D));
        Iterator<ChainKnotEntity> iter = list.iterator();

        ChainKnotEntity leashKnotEntity;
        do {
            if (!iter.hasNext()) {
                if (hasToExist) {
                    return null;
                }
                ChainKnotEntity newLeashKnotEntity = new ChainKnotEntity(world, pos);
                world.addFreshEntity(newLeashKnotEntity);
                newLeashKnotEntity.playPlacementSound();
                return newLeashKnotEntity;
            }

            leashKnotEntity = iter.next();
        } while (leashKnotEntity == null || !leashKnotEntity.getPos().equals(pos));

        return leashKnotEntity;
    }

    @Override
    public IPacket<?> getAddEntityPacket() {
//        return NetworkHooks.getEntitySpawningPacket(this);
        return new SSpawnObjectPacket(this, this.getType(), 0, this.getPos());
    }


    @OnlyIn(Dist.CLIENT)
    @Override
    public Vector3d getRopeHoldPosition(float f) {
        return this.getPosition(f).add(0.0D, 0.2D, 0.0D);
    }

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
                    this.detachChain(entity, true);
                    dropItem(null);
                }
            }
        }

    }

    private void removeEntity(int id) {
        this.holdingEntities.remove(id);
        List<Integer> newList = new ArrayList<>(entityData.get(ATTACHED));
        newList.remove(new Integer(id));
        entityData.set(ATTACHED, newList);
    }

    private void addEntity(Entity entity) {
        this.holdingEntities.put(entity.getId(), entity);
        List<Integer> newList = new ArrayList<>(entityData.get(ATTACHED));
        newList.add(entity.getId());
        entityData.set(ATTACHED, newList);
    }

    public void init() {
        entityData.set(ATTACHED, new ArrayList<>(this.holdingEntities.keySet()));
    }

    public void detachChain(Entity entity, boolean dropItem) {
        if (entity != null) {
            if (!this.level.isClientSide()) {
                if (this.holdingEntities.size() <= 1) {
                    this.forcedLoading = false;
                }
                removeEntity(entity.getId());

                if (entity instanceof ChainKnotEntity) {
                    if (((ChainKnotEntity) entity).holdingEntities.isEmpty()) {
                        entity.forcedLoading = false;
                    }
                    ((ChainKnotEntity) entity).holdersCount--;
                }

                if (this.holdersCount <= 0 && getHoldingEntities().isEmpty()) {
                    this.remove();
                }

                if (dropItem) {
                    Vector3d middle = middleOf(position(), entity.position());
                    ItemEntity entity1 = new ItemEntity(level, middle.x, middle.y, middle.z, new ItemStack(Items.CHAIN));
                    entity1.setDefaultPickUpDelay();
                    this.level.addFreshEntity(entity1);
                }
            }

//            if (!this.level.isClientSide() && sendPacket && this.level instanceof ServerWorld) {
//                if (entity instanceof ChainKnotEntity) {
//                    ((ChainKnotEntity) entity).holdersCount--;
//                    if (this.holdersCount <= 0 && getHoldingEntities().isEmpty()) {
//                        this.remove();
//                    }
//                }
//                sendDetachChainPacket(entity.getId());
//            }

        }
    }

    public void attachChain(Entity entity, int fromPlayerEntityId) {
        if (!this.level.isClientSide()) {
            this.forcedLoading = true;
            addEntity(entity);

            if (!(entity instanceof PlayerEntity)) {
                entity.forcedLoading = true;
                if (entity instanceof ChainKnotEntity) {
                    ((ChainKnotEntity) entity).holdersCount++;
                }
            }

            if (fromPlayerEntityId != 0) {
                removeEntity(fromPlayerEntityId);
//            removePlayerWithId(fromPlayerEntityId);
            }
        }


//        if (!this.level.isClientSide() && sendPacket && this.level instanceof ServerWorld) {
//            if (entity instanceof ChainKnotEntity) {
//                ((ChainKnotEntity) entity).holdersCount++;
//            }
//            sendAttachChainPacket(entity.getId(), fromPlayerEntityId);
//        }
    }

//    public void sendDetachChainPacket(int entityId) {
//        Stream<ServerPlayerEntity> watchingPlayers = getPlayersAround(level, blockPosition(), 1024d);
//        PacketBuffer passedData = new PacketBuffer(Unpooled.buffer());
//
//        //Write our id and the id of the one we connect to.
//        passedData.writeVarIntArray(new int[]{this.getId(), entityId});
//
//        watchingPlayers.forEach(playerEntity ->
//                ModPacketHandler.sendToClient(new S2CChainDetachPacket(passedData), playerEntity));
//    }
//
//    public void sendAttachChainPacket(int entityId, int fromPlayerEntityId) {
//        Stream<ServerPlayerEntity> watchingPlayers = getPlayersAround(level, blockPosition(), 1024d);
//        PacketBuffer passedData = new PacketBuffer(Unpooled.buffer());
//
//        //Write our id and the id of the one we connect to.
//        passedData.writeVarIntArray(new int[]{this.getId(), entityId});
//        passedData.writeInt(fromPlayerEntityId);
//
//        watchingPlayers.forEach(playerEntity ->
//                ModPacketHandler.sendToClient(new S2CChainAttachPacket(passedData), playerEntity));
//    }

    @OnlyIn(Dist.CLIENT)
    public void addHoldingEntityId(int id, int fromPlayerId) {
        if (fromPlayerId != 0) {
            this.holdingEntities.remove(fromPlayerId);
        }
        this.holdingEntities.put(id, null);
    }

    @OnlyIn(Dist.CLIENT)
    public void removeHoldingEntityId(int id) {
        this.holdingEntities.remove(id);
    }

    @OnlyIn(Dist.CLIENT)
    public void addHoldingEntityIds(int[] ids) {
        for (int id : ids) this.holdingEntities.put(id, null);
    }

    @OnlyIn(Dist.CLIENT)
    public void removeHoldingEntityIds(int[] ids) {
        for (int id : ids) this.holdingEntities.remove(id);
    }

    public void removePlayerWithId(int entityId) {
        this.holdingEntities.remove(entityId);
    }

    private void deserializeChainTag(CompoundNBT tag) {
        if (tag != null && this.level instanceof ServerWorld) {
            if (tag.contains("UUID")) {
                UUID uuid = tag.getUUID("UUID");
                Entity entity = ((ServerWorld) this.level).getEntity(uuid);
                if (entity != null) {
                    this.attachChain(entity, 0);
                    this.chainTags.remove(tag);
                    return;
                }
            } else if (tag.contains("X")) {
                BlockPos blockPos = new BlockPos(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z"));
                ChainKnotEntity entity = ChainKnotEntity.getOrCreate(this.level, blockPos, true);
                if (entity != null) {
                    this.attachChain(ChainKnotEntity.getOrCreate(this.level, blockPos), 0);
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

    public static Vector3d middleOf(Vector3d a, Vector3d b) {
        double x = (a.x() - b.x()) / 2d + b.x();
        double y = (a.y() - b.y()) / 2d + b.y();
        double z = (a.z() - b.z()) / 2d + b.z();
        return new Vector3d(x, y, z);
    }

//    private static Stream<ServerPlayerEntity> getPlayersAround(World world, BlockPos pos, double radius) {
//        double radiusSq = radius * radius;
//        if (world instanceof ServerWorld) {
//            return ((ServerWorld) world).players().stream().filter(p -> {
//                return p.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) <= radiusSq;
//            });
//        } else {
//            throw new RuntimeException("unexpected clientside getplayers");
//        }
//    }

    @Override
    public ItemStack getPickedResult(RayTraceResult target) {
        return new ItemStack(Items.CHAIN, 1);
    }


    @Override
    public void onSyncedDataUpdated(DataParameter<?> param) {
        if (ATTACHED.equals(param)) {
            if (this.level.isClientSide()) {
                this.holdingEntities.clear();
                for (int id : entityData.get(ATTACHED)) {
                    this.holdingEntities.put(id, this.level.getEntity(id));
                }
            }
        }
//        } else if (ADJUSTMENT.equals(param)) {
//            if (this.level.isClientSide()) {
//                int id = this.entityData.get(ADJUSTMENT);
//                if (id < 0) {
//                    this.holdingEntities.remove(-id);
//                } else if (id > 0) {
//                    this.holdingEntities.put(id, this.level.getEntity(id));
//                }
//            }
//        }
        super.onSyncedDataUpdated(param);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ATTACHED, new ArrayList<>());
//        this.entityData.define(ADJUSTMENT, 0);
    }
}
