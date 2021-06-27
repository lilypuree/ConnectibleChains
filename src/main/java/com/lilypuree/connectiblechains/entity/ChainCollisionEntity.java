package com.lilypuree.connectiblechains.entity;


import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.common.Tags;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.network.NetworkHooks;

/**
 * ChainCollisionEntity is an Entity that is invisible but has a collision.
 * It is used to create a collision for connections between chains.
 *
 * @author legoatoom
 */
public class ChainCollisionEntity extends Entity implements IEntityAdditionalSpawnData {

    /**
     * The chainKnot entity id that has a connection to another chainKnot with id {@link #endOwnerId}.
     */
    private int startOwnerId;
    /**
     * The chainKnot entity id that has a connection from another chainKnot with id {@link #startOwnerId}.
     */
    private int endOwnerId;


    public ChainCollisionEntity(EntityType<? extends ChainCollisionEntity> entityType, World world) {
        super(entityType, world);
        this.forcedLoading = true;
    }

    public ChainCollisionEntity(World world, double x, double y, double z, int startOwnerId, int endOwnerId) {
        this(ModEntityTypes.CHAIN_COLLISION.get(), world);
        this.startOwnerId = startOwnerId;
        this.endOwnerId = endOwnerId;
        this.setPos(x, y, z);
//        this.setBoundingBox(new Box(x, y, z, x, y, z).expand(.01d, 0, .01d));
    }

    @Override
    protected void defineSynchedData() {
        // Required by Entity
    }

    @Override
    protected boolean isMovementNoisy() {
        //canClimb in yarn?
        return false;
    }

    /**
     * When this entity is attacked by a player with a item that has Tag: {@link net.minecraftforge.common.Tags.Items#SHEARS},
     * it calls the {@link ChainKnotEntity#damageLink(boolean, ChainKnotEntity)} method
     * to destroy the link between the {@link #startOwnerId} and {@link #endOwnerId}
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        } else if (!this.level.isClientSide && !this.removed) {
            Entity startOwner = this.level.getEntity(startOwnerId);
            Entity endOwner = this.level.getEntity(endOwnerId);
            Entity sourceEntity = source.getEntity();
            if (sourceEntity instanceof PlayerEntity
                    && startOwner instanceof ChainKnotEntity && endOwner instanceof ChainKnotEntity) {
                boolean isCreative = ((PlayerEntity) sourceEntity).isCreative();
                if (!((PlayerEntity) sourceEntity).getMainHandItem().isEmpty() && ((PlayerEntity) sourceEntity).getMainHandItem().getItem().is(Tags.Items.SHEARS)) {
                    ((ChainKnotEntity) startOwner).damageLink(isCreative, (ChainKnotEntity) endOwner);
                }
            }
            return true;
        } else {
            return true;
        }
    }

    /**
     * If this entity can even be collided with.
     * Different from {@link #canBeCollidedWith()} ()} as this tells if something can collide with this.
     *
     * @return true
     */
    @Override
    public boolean isPickable() {
        //collides in Yarn
        return !this.removed;
    }

    /**
     * We don't want to be able to push the collision box of the chain.
     *
     * @return false
     */
    @Override
    public boolean isPushable() {
        return false;
    }

    /**
     * We only allow the collision box to be rendered if a player is holding a item that has tag {@link net.minecraftforge.common.Tags.Items#SHEARS}.
     * This might be helpful when using F3+B to see the boxes of the chain.
     *
     * @return boolean - should the collision box be rendered.
     */
    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        ClientPlayerEntity player = Minecraft.getInstance().player;
        if (player != null && !player.getMainHandItem().isEmpty() && player.getMainHandItem().getItem().is(Tags.Items.SHEARS)) {
            return super.shouldRenderAtSqrDistance(distance);
        } else {
            return false;
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundNBT tag) {

    }

    @Override
    protected void addAdditionalSaveData(CompoundNBT tag) {

    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    //handle attack here

    /**
     * What happens when this is attacked?
     * This method is called by {@link PlayerEntity#attack(Entity)} to allow an entity to choose what happens when
     * it is attacked. We don't want to play sounds when we attack it without shears, so that is why we override this.
     */
    @Override
    public boolean skipAttackInteraction(Entity attacker) {
        playSound(SoundEvents.CHAIN_HIT, 0.5f, 1.0f);
        if (attacker instanceof PlayerEntity) {
            PlayerEntity playerEntity = (PlayerEntity) attacker;
            return this.hurt(DamageSource.playerAttack(playerEntity), 0.0f);
        } else {
            return false;
        }
    }

    @Override
    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void writeSpawnData(PacketBuffer buffer) {
        buffer.writeVarInt(startOwnerId);
        buffer.writeVarInt(endOwnerId);
    }

    @Override
    public void readSpawnData(PacketBuffer additionalData) {
        setStartOwnerId(additionalData.readVarInt());
        setEndOwnerId(additionalData.readVarInt());
        Vector3d pos = this.position();
        setBoundingBox(new AxisAlignedBB(pos, pos).expandTowards(.01d, 0, .01d));
    }

    @Override
    public ItemStack getPickedResult(RayTraceResult target) {
        return new ItemStack(Items.CHAIN);
    }

    public void setStartOwnerId(int startOwnerId) {
        this.startOwnerId = startOwnerId;
    }

    public void setEndOwnerId(int endOwnerId) {
        this.endOwnerId = endOwnerId;
    }


}
