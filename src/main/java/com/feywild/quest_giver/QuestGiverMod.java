package com.feywild.quest_giver;

import com.feywild.quest_giver.config.QuestConfig;
import com.feywild.quest_giver.config.SpawnConfig;
import com.feywild.quest_giver.entity.*;
import com.feywild.quest_giver.network.QuestGiverNetwork;
import com.feywild.quest_giver.quest.QuestManager;
import com.feywild.quest_giver.quest.player.CapabilityQuests;
import com.feywild.quest_giver.quest.reward.CommandReward;
import com.feywild.quest_giver.quest.reward.ItemReward;
import com.feywild.quest_giver.quest.reward.ReputationReward;
import com.feywild.quest_giver.quest.reward.RewardTypes;
import com.feywild.quest_giver.quest.task.*;
import com.feywild.quest_giver.events.RenderEvents;
import com.feywild.quest_giver.events.ClientEvents;
import com.feywild.quest_giver.util.QuestGiverJigsawHelper;
import com.feywild.quest_giver.worldgen.feature.structures.ModStructures;
import com.feywild.quest_giver.worldgen.feature.structures.load.ModStructurePieces;
import io.github.noeppi_noeppi.libx.config.ConfigManager;
import io.github.noeppi_noeppi.libx.mod.registration.ModXRegistration;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import tallestegg.guardvillagers.client.renderer.GuardRenderer;

import javax.annotation.Nonnull;

@Mod(QuestGiverMod.MODID)
public final class QuestGiverMod extends ModXRegistration {

    public static final String MODID = "quest_giver";
    private static QuestGiverMod instance;
    private static QuestGiverNetwork network;

    public QuestGiverMod(String MODID, CreativeModeTab tab) {
        super(MODID, tab);
        instance = this;
        network = new QuestGiverNetwork(this);

        ConfigManager.registerConfig(new ResourceLocation(this.modid, "quest_numbers"), QuestConfig.class, false);
        ConfigManager.registerConfig(new ResourceLocation(this.modid, "spawn_rates"), SpawnConfig.class, false);

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        IEventBus forgeEventBus = MinecraftForge.EVENT_BUS;

        modEventBus.addListener(CapabilityQuests::register);
        modEventBus.addListener(this::entityAttributes);

        forgeEventBus.addListener(this::reloadData);

        forgeEventBus.addGenericListener(Entity.class, CapabilityQuests::attachPlayerCaps);
        forgeEventBus.addListener(CapabilityQuests::playerCopy);

        forgeEventBus.register(EventListener.class);
        GuildMasterProfession.PROFESSION.register(modEventBus);
        ModPoiTypes.POI_TYPES.register(modEventBus);
        ModStructures.register(modEventBus);


        // Quest task & reward types. Not in setup as they are required for datagen.
        TaskTypes.register(new ResourceLocation(this.modid, "craft"), CraftTask.INSTANCE);
        TaskTypes.register(new ResourceLocation(this.modid, "gift"), GiftTask.INSTANCE);
        TaskTypes.register(new ResourceLocation(this.modid, "item_stack"), ItemStackTask.INSTANCE);
        TaskTypes.register(new ResourceLocation(this.modid, "item_pickup"), ItemPickupTask.INSTANCE);
        TaskTypes.register(new ResourceLocation(this.modid, "kill"), KillTask.INSTANCE);
        TaskTypes.register(new ResourceLocation(this.modid, "biome"), BiomeTask.INSTANCE);
        TaskTypes.register(new ResourceLocation(this.modid, "structure"), StructureTask.INSTANCE);
        TaskTypes.register(new ResourceLocation(this.modid, "special"), SpecialTask.INSTANCE);

        RewardTypes.register(new ResourceLocation(this.modid, "item"), ItemReward.INSTANCE);
        RewardTypes.register(new ResourceLocation(this.modid, "command"), CommandReward.INSTANCE);
        RewardTypes.register(new ResourceLocation(this.modid, "reputation"), ReputationReward.INSTANCE);

        forgeEventBus.register(this);
    }

    @Nonnull
    public static QuestGiverMod getInstance(){
        return instance;
    }

    @Nonnull
    public static QuestGiverNetwork getNetwork() {
        return network;
    }

    //@Override
    //protected void initRegistration(RegistrationBuilder builder) {
    //    builder.setVersion(1);
    //}

    @Override
    protected void setup(final FMLCommonSetupEvent event)
    {
        event.enqueueWork(() -> {
            ModStructurePieces.setup();

            SpawnPlacements.register(ModEntityTypes.questVillager, SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, QuestVillager::canSpawn);
            SpawnPlacements.register(ModEntityTypes.questGuardVillager, SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, QuestGuardVillager::canSpawn);

            ModPoiTypes.register();

        });
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    protected void clientSetup(FMLClientSetupEvent event) {
        EntityRenderDispatcher.registerRenderers(ModEntityTypes.questVillager, VillagerRenderer::new);
        EntityRenderDispatcher.registerRenderers(ModEntityTypes.questGuardVillager, GuardRenderer::new);
        MinecraftForge.EVENT_BUS.register(RenderEvents.class);
        MinecraftForge.EVENT_BUS.register(ClientEvents.class);
    }

    @SubscribeEvent
    public void reloadData(AddReloadListenerEvent event) {
        event.addListener(QuestManager.createReloadListener());
    }

    private void entityAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntityTypes.questVillager, QuestVillager.createAttributes().build());
        event.put(ModEntityTypes.questGuardVillager, QuestGuardVillager.createAttributes().build());
    }

    @SubscribeEvent
    public void onServerStartEvent(FMLServerAboutToStartEvent event){
        //TODO Add Guards

        QuestGiverJigsawHelper.registerJigsaw(event.getServer(), new ResourceLocation("minecraft:village/desert/houses"),
                new ResourceLocation("quest_giver:village/desert/houses/guild_house"), SpawnConfig.guild_house_spawn_rate);
        QuestGiverJigsawHelper.registerJigsaw(event.getServer(), new ResourceLocation("minecraft:village/plains/houses"),
                new ResourceLocation("quest_giver:village/plains/houses/guild_house"), SpawnConfig.guild_house_spawn_rate);
        QuestGiverJigsawHelper.registerJigsaw(event.getServer(), new ResourceLocation("minecraft:village/savanna/houses"),
                new ResourceLocation("quest_giver:village/savanna/houses/guild_house"), SpawnConfig.guild_house_spawn_rate);
        QuestGiverJigsawHelper.registerJigsaw(event.getServer(), new ResourceLocation("minecraft:village/snowy/houses"),
                new ResourceLocation("quest_giver:village/snowy/houses/guild_house"), SpawnConfig.guild_house_spawn_rate);
        QuestGiverJigsawHelper.registerJigsaw(event.getServer(), new ResourceLocation("minecraft:village/taiga/houses"),
                new ResourceLocation("quest_giver:village/taiga/houses/guild_house"), SpawnConfig.guild_house_spawn_rate);

        QuestGiverJigsawHelper.registerJigsaw(event.getServer(), new ResourceLocation("minecraft:village/desert/houses"),
                new ResourceLocation("quest_giver:village/desert/houses/stall"), SpawnConfig.guild_stall_spawn_rate);
        QuestGiverJigsawHelper.registerJigsaw(event.getServer(), new ResourceLocation("minecraft:village/plains/houses"),
                new ResourceLocation("quest_giver:village/plains/houses/stall"), SpawnConfig.guild_stall_spawn_rate);
        QuestGiverJigsawHelper.registerJigsaw(event.getServer(), new ResourceLocation("minecraft:village/savanna/houses"),
                new ResourceLocation("quest_giver:village/savanna/houses/stall"), SpawnConfig.guild_stall_spawn_rate);
        QuestGiverJigsawHelper.registerJigsaw(event.getServer(), new ResourceLocation("minecraft:village/snowy/houses"),
                new ResourceLocation("quest_giver:village/snowy/houses/stall"), SpawnConfig.guild_stall_spawn_rate);
        QuestGiverJigsawHelper.registerJigsaw(event.getServer(), new ResourceLocation("minecraft:village/taiga/houses"),
                new ResourceLocation("quest_giver:village/taiga/houses/stall"), SpawnConfig.guild_stall_spawn_rate);

        QuestGiverJigsawHelper.registerJigsaw(event.getServer(), new ResourceLocation("minecraft:village/desert/villagers"),
                new ResourceLocation("quest_giver:village/desert/villagers/quest_villager_desert"), SpawnConfig.quest_villager_weight);
        QuestGiverJigsawHelper.registerJigsaw(event.getServer(), new ResourceLocation("minecraft:village/plains/villagers"),
                new ResourceLocation("quest_giver:village/plains/villagers/quest_villager"), SpawnConfig.quest_villager_weight);
        QuestGiverJigsawHelper.registerJigsaw(event.getServer(), new ResourceLocation("minecraft:village/savanna/villagers"),
                new ResourceLocation("quest_giver:village/savanna/villagers/quest_villager_savanna"), SpawnConfig.quest_villager_weight);
        QuestGiverJigsawHelper.registerJigsaw(event.getServer(), new ResourceLocation("minecraft:village/snowy/villagers"),
                new ResourceLocation("quest_giver:village/snowy/villagers/quest_villager_snow"), SpawnConfig.quest_villager_weight);
        QuestGiverJigsawHelper.registerJigsaw(event.getServer(), new ResourceLocation("minecraft:village/taiga/villagers"),
                new ResourceLocation("quest_giver:village/taiga/villagers/quest_villager_taiga"), SpawnConfig.quest_villager_weight);

        QuestGiverJigsawHelper.registerJigsaw(event.getServer(), new ResourceLocation("minecraft:village/desert/villagers"),
                new ResourceLocation("quest_giver:village/desert/villagers/quest_guard_villager_desert"), SpawnConfig.quest_guard_weight);
        QuestGiverJigsawHelper.registerJigsaw(event.getServer(), new ResourceLocation("minecraft:village/plains/villagers"),
                new ResourceLocation("quest_giver:village/plains/villagers/quest_guard_villager"), SpawnConfig.quest_guard_weight);
        QuestGiverJigsawHelper.registerJigsaw(event.getServer(), new ResourceLocation("minecraft:village/savanna/villagers"),
                new ResourceLocation("quest_giver:village/savanna/villagers/quest_guard_villager_savanna"), SpawnConfig.quest_guard_weight);
        QuestGiverJigsawHelper.registerJigsaw(event.getServer(), new ResourceLocation("minecraft:village/snowy/villagers"),
                new ResourceLocation("quest_giver:village/snowy/villagers/quest_guard_villager_snow"), SpawnConfig.quest_guard_weight);
        QuestGiverJigsawHelper.registerJigsaw(event.getServer(), new ResourceLocation("minecraft:village/taiga/villagers"),
                new ResourceLocation("quest_giver:village/taiga/villagers/quest_guard_villager_taiga"), SpawnConfig.quest_guard_weight);

    }
}
