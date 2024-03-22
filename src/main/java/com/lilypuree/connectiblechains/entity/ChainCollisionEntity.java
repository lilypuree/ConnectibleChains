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

import com.lilypuree.connectiblechains.chain.ChainLink;
import com.lilypuree.connectiblechains.chain.ChainType;
import com.lilypuree.connectiblechains.chain.ChainTypesRegistry;
import com.lilypuree.connectiblechains.client.ClientInitializer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * ChainCollisionEntity is an Entity that is invisible but has a collision.
 * It is used to create a collision for links.
 *
 * @author legoatoom, Quendolin
 */
public class ChainCollisionEntity extends Entity implements IEntityAdditionalSpawnData, ChainLinkEntity {


    /**
     * The link that this collider is a part of.
     */
    @Nullable
    private ChainLink link;

    /**
     * On the client only the chainType information is present, for the pick item action mostly
     */
    private ChainType chainType;


    public ChainCollisionEntity(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public ChainCollisionEntity(Level world, double x, double y, double z, @NotNull ChainLink link) {
        this(ModEntityTypes.CHAIN_COLLISION.get(), world);
        this.link = link;
        this.setPos(x, y, z);
    }

    public @Nullable ChainLink getLink() {
        return link;
    }

    public ChainType getChainType() {
        return chainType;
    }

    public void setChainType(ChainType chainType) {
        this.chainType = chainType;
    }

    @Override
    protected void defineSynchedData() {
        // Required by Entity
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }


    @Override
    public boolean shouldRenderAtSqrDistance(double pDistance) {
        if (ClientInitializer.checkCollisionEntityWithinRenderDistance(this, pDistance)) {
            return super.shouldRenderAtSqrDistance(pDistance);
        } else {
            return false;
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag pCompound) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag pCompound) {
    }


    /**
     * @see ChainKnotEntity#skipAttackInteraction(Entity)
     */
    @Override
    public boolean skipAttackInteraction(Entity attacker) {
        if (attacker instanceof Player playerEntity) {
            return this.hurt(DamageSource.playerAttack(playerEntity), 0.0F);
        } else {
            playSound(SoundEvents.CHAIN_HIT, 0.5F, 1.0F);
        }
        return true;
    }

    /**
     * @see ChainKnotEntity#hurt(DamageSource, float)
     */
    @Override
    public boolean hurt(DamageSource source, float pAmount) {
        InteractionResult result = ChainLinkEntity.onDamageFrom(this, source);

        if (result.consumesAction()) {
            destroyLinks(result == InteractionResult.SUCCESS);
            return true;
        }
        return false;
    }

    @Override
    public void destroyLinks(boolean mayDrop) {
        if (link != null) link.destroy(mayDrop);
    }

    @Override
    public InteractionResult interact(Player pPlayer, InteractionHand pHand) {
        if (ChainLinkEntity.canDestroyWith(pPlayer.getItemInHand(pHand))) {
            destroyLinks(!pPlayer.isCreative());
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        ChainType chainType = link == null ? ChainTypesRegistry.DEFAULT_CHAIN_TYPE: link.chainType;
        buffer.writeResourceLocation(ChainTypesRegistry.getKey(chainType));
    }

    @Override
    public void readSpawnData(FriendlyByteBuf additionalData) {
        this.setChainType(ChainTypesRegistry.getValue(additionalData.readResourceLocation()));
    }

    @Override
    public void tick() {
        if (level.isClientSide) return;

        // Condition can be met when the knots were removed with commands
        // but the collider still exists
        if (link != null && link.needsBeDestroyed()) link.destroy(true);

        // Collider removes itself when the link is dead
        if (link == null || link.isDead()) {
            remove(Entity.RemovalReason.DISCARDED);
        }
    }

    @Override
    public ItemStack getPickedResult(HitResult target) {
        return new ItemStack(chainType.item());
    }
}
