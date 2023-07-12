package com.lilypuree.connectiblechains.entity;

import com.lilypuree.connectiblechains.ConnectibleChains;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = ConnectibleChains.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEntityTypes {

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ConnectibleChains.MODID);

    //    public static EntityType<ChainKnotEntity> CHAIN_KNOT;
    public static RegistryObject<EntityType<ChainKnotEntity>> CHAIN_KNOT = ENTITIES.register("chain_knot", () -> EntityType.Builder.<ChainKnotEntity>of(ChainKnotEntity::new, MobCategory.MISC)
            .clientTrackingRange(10).updateInterval(Integer.MAX_VALUE).setShouldReceiveVelocityUpdates(false)
            .sized(6/16f, 0.5f).canSpawnFarFromPlayer().fireImmune().build("chain_knot"));

    public static RegistryObject<EntityType<ChainCollisionEntity>> CHAIN_COLLISION = ENTITIES.register("chain_collision", () -> EntityType.Builder.<ChainCollisionEntity>of(ChainCollisionEntity::new, MobCategory.MISC)
            .clientTrackingRange(10).updateInterval(Integer.MAX_VALUE).setShouldReceiveVelocityUpdates(false)
            .sized(4 / 16f, 6 / 16f).fireImmune().noSave().noSummon().build("chain_collision"));

    public static void register() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ENTITIES.register(modEventBus);
    }
}
