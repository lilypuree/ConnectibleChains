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

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.Tags;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;

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

    public void setStartOwnerId(int startOwnerId) {
        this.startOwnerId = startOwnerId;
    }

    public void setEndOwnerId(int endOwnerId) {
        this.endOwnerId = endOwnerId;
    }

    public ChainCollisionEntity(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public ChainCollisionEntity(Level world, double x, double y, double z, int startOwnerId, int endOwnerId) {
        this(ModEntityTypes.CHAIN_COLLISION.get(), world);
        this.startOwnerId = startOwnerId;
        this.endOwnerId = endOwnerId;
        this.setPos(x, y, z);
    }

    /**
     * When this entity is attacked by a player with a item that has Tag: {@link net.minecraftforge.common.Tags.Items#SHEARS},
     * it calls the {@link ChainKnotEntity#damageLink(boolean, ChainKnotEntity)} method
     * to destroy the link between the {@link #startOwnerId} and {@link #endOwnerId}
     */
    @Override
    public boolean hurt(DamageSource source, float pAmount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        } else if (!this.level.isClientSide) {
            Entity startOwner = this.level.getEntity(startOwnerId);
            Entity endOwner = this.level.getEntity(endOwnerId);
            Entity sourceEntity = source.getEntity();
            if (source.getDirectEntity() instanceof AbstractArrow) {
                return false;
            } else if (sourceEntity instanceof Player player
                    && startOwner instanceof ChainKnotEntity && endOwner instanceof ChainKnotEntity) {
                boolean isCreative = player.isCreative();
                if (!player.getMainHandItem().isEmpty() && player.getMainHandItem().is(Tags.Items.SHEARS)) {
                    ((ChainKnotEntity) startOwner).damageLink(isCreative, (ChainKnotEntity) endOwner);
                }
            }
            return true;
        } else {
            return !(source.getDirectEntity() instanceof AbstractArrow);
        }
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
    protected void defineSynchedData() {
        // Required by Entity
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag pCompound) {
        // Required by Entity, but does nothing.
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag pCompound) {
        // Required by Entity, but does nothing.
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        buffer.writeVarInt(startOwnerId);
        buffer.writeVarInt(endOwnerId);
    }

    @Override
    public void readSpawnData(FriendlyByteBuf additionalData) {
        this.startOwnerId = additionalData.readVarInt();
        this.endOwnerId = additionalData.readVarInt();
    }

    @Override
    public ItemStack getPickedResult(HitResult target) {
        return new ItemStack(Items.CHAIN);
    }
}
