package com.lilypuree.connectiblechains.entity;

import com.lilypuree.connectiblechains.ConnectibleChains;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = ConnectibleChains.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEntityTypes {

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITIES, ConnectibleChains.MODID);

    //    public static EntityType<ChainKnotEntity> CHAIN_KNOT;
    public static RegistryObject<EntityType<ChainKnotEntity>> CHAIN_KNOT = ENTITIES.register("chain_knot", () -> EntityType.Builder.<ChainKnotEntity>of(ChainKnotEntity::new, EntityClassification.MISC)
            .clientTrackingRange(64).setShouldReceiveVelocityUpdates(false)
            .sized(0.5f, 0.5f).build("chain_knot"));
    public static RegistryObject<EntityType<ChainCollisionEntity>> CHAIN_COLLISION = ENTITIES.register("chain_collision", ()->EntityType.Builder.<ChainCollisionEntity>of(ChainCollisionEntity::new, EntityClassification.MISC)
            .clientTrackingRange(10).updateInterval(Integer.MAX_VALUE).setShouldReceiveVelocityUpdates(false)
            .sized(0.3F, 0.3F)
            .noSave()
            .noSummon()
            .fireImmune()
            .build("chain_collision"));


    public static void register() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ENTITIES.register(modEventBus);
    }
}
