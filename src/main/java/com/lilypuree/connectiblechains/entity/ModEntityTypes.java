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

import com.lilypuree.connectiblechains.ConnectibleChains;
import com.lilypuree.connectiblechains.util.Helper;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * This class keeps track of all entities that this mod has.
 * It also registers them.
 *
 * @author legoatoom
 */
public class ModEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, ConnectibleChains.MODID);


        public static DeferredHolder<EntityType<?>, EntityType<ChainKnotEntity>> CHAIN_KNOT = ENTITIES.register("chain_knot", () -> EntityType.Builder.<ChainKnotEntity>of(ChainKnotEntity::new, MobCategory.MISC)
                .clientTrackingRange(10).updateInterval(Integer.MAX_VALUE).setShouldReceiveVelocityUpdates(false)
                .sized(6 / 16f, 0.5f).canSpawnFarFromPlayer().fireImmune().build("chain_knot"));

        public static DeferredHolder<EntityType<?>, EntityType<ChainCollisionEntity>> CHAIN_COLLISION = ENTITIES.register("chain_collision", () -> EntityType.Builder.<ChainCollisionEntity>of(ChainCollisionEntity::new, MobCategory.MISC)
                .clientTrackingRange(10).updateInterval(Integer.MAX_VALUE).setShouldReceiveVelocityUpdates(false)
                .sized(4 / 16f, 6 / 16f).fireImmune().noSave().noSummon().build("chain_collision"));


    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }
}
