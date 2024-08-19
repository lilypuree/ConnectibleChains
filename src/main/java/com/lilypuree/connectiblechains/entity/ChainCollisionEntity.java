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

package com.lilypuree.connectiblechains.entity;

import com.lilypuree.connectiblechains.chain.ChainLink;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * ChainCollisionEntity is an Entity that is invisible but has a collision.
 * It is used to create a collision for links.
 *
 * @author legoatoom, Qendolin
 */
public class ChainCollisionEntity extends Entity implements ChainLinkEntity {

    /**
     * The link that this collider is a part of.
     */
    @Nullable
    private ChainLink link;

    @NotNull
    private Item linkSourceItem;


    public ChainCollisionEntity(Level level, double x, double y, double z, @NotNull ChainLink link) {
        this(ModEntityTypes.CHAIN_COLLISION.get(), level);
        this.link = link;
        this.setPos(x, y, z);
        this.linkSourceItem = link.sourceItem;
    }

    public ChainCollisionEntity(EntityType<? extends ChainCollisionEntity> entityType, Level level) {
        super(entityType, level);
    }

    @SuppressWarnings("unused")
    public @Nullable ChainLink getLink() {
        // Only available in the server. In the client, it is null.
        return link;
    }

    public @NotNull Item getLinkSourceItem() {
        // Always available.
        return linkSourceItem;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
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
     * We only allow the collision box to be rendered if a player is holding a shears type item.
     * This might be helpful when using F3+B to see the boxes of the chain.
     *
     * @param distance the camera distance from the collider.
     * @return true when it should be rendered
     */


    @OnlyIn(Dist.CLIENT)
    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.isHolding(itemStack -> itemStack.is(Tags.Items.TOOLS_SHEAR))) {
            return super.shouldRenderAtSqrDistance(distance);
        } else {
            return false;
        }
    }



    @Override
    public boolean fireImmune() {
        return super.fireImmune();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    /**
     * Makes sure that nothing can walk through it.
     *
     * @return true
     */


    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    /**
     * @see ChainKnotEntity#skipAttackInteraction(Entity)
     */
    @Override
    public boolean skipAttackInteraction(Entity attacker) {
        if (attacker instanceof Player player) {
            this.hurt(this.damageSources().playerAttack(player), 0.0F);
        } else {
            playSound(getHitSound(), 0.5F, 1.0F);
        }
        return true;
    }


    /**
     * @see ChainKnotEntity#hurt(DamageSource, float)
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        InteractionResult result = ChainLinkEntity.onDamageFrom(this, source, getHitSound());

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

    private SoundEvent getHitSound() {
        if (link != null) {
            return ChainLink.getSoundGroup(link.sourceItem).getHitSound();
        } else {
            return ChainLink.getSoundGroup(null).getHitSound();
        }
    }

    /**
     * Interaction (attack or use) of a player and this entity.
     * Tries to destroy the link with the item in the players hand.
     *
     * @param player The player that interacted.
     * @param hand   The hand that interacted.
     * @return {@link InteractionResult#SUCCESS} when the interaction was successful.
     */
    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (ChainLinkEntity.canDestroyWith(player.getItemInHand(hand))) {
            destroyLinks(!player.isCreative());
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {

    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity serverEntity) {
        int id = BuiltInRegistries.ITEM.getId(linkSourceItem);
        return new ClientboundAddEntityPacket(this, serverEntity, id);
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        int rawChainItemSourceId = packet.getData();
        linkSourceItem = BuiltInRegistries.ITEM.getHolder(rawChainItemSourceId).get().value();
    }

    /**
     * Destroys broken links and removes itself when there is no alive link.
     */
    @Override
    public void tick() {
        if (level().isClientSide) return;
        // Condition can be met when the knots were removed with commands
        // but the collider still exists
        if (link != null && link.needsBeDestroyed()) link.destroy(true);

        // Collider removes itself when the link is dead
        if (link == null || link.isDead()) {
            remove(Entity.RemovalReason.DISCARDED);
        }
    }
}
