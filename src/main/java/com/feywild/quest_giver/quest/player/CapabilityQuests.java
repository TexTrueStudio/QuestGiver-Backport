package com.feywild.quest_giver.quest.player;

import com.feywild.quest_giver.QuestGiverMod;
import com.feywild.quest_giver.quest.QuestLine;
import io.github.noeppi_noeppi.libx.util.LazyValue;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.LazyLoadedValue;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CapabilityQuests {

    public static final ResourceLocation KEY = new ResourceLocation(QuestGiverMod.getInstance().modid, "player_quests");

    public static Capability<QuestData> QUESTS = CapabilityManager.get(new CapabilityToken<>() {});

    public static void register(RegisterCapabilitiesEvent event) {
        event.register(QuestData.class);
    }

    public static void attachPlayerCaps(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof ServerPlayer player) {
            event.addCapability(KEY, new Provider(QUESTS, new LazyLoadedValue<>(() -> {
                QuestData data = new QuestData();
                data.attach(player);
                return data;
            })));
        }
    }


    @SuppressWarnings("CodeBlock2Expr")
    public static void playerCopy(PlayerEvent.Clone event) {
        // Keep the data on death/copy
            event.getOriginal().reviveCaps();
            event.getOriginal().getCapability(QUESTS).ifPresent(oldData -> {
                event.getPlayer().getCapability(QUESTS).ifPresent(newData -> {
                    newData.read(oldData.write());
                });
            });
            event.getOriginal().invalidateCaps();
    }

    private record Provider(Capability<QuestData> capability, LazyLoadedValue<QuestData> value) implements ICapabilityProvider, INBTSerializable<Tag> {

        @Nonnull
        @Override
        public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
            //noinspection NullableProblems
            return cap == this.capability() ? LazyOptional.of(this.value()::get).cast() : LazyOptional.empty();
        }

        @Override
        public Tag serializeNBT() {
            return this.value().get().write();
        }

        @Override
        public void deserializeNBT(Tag nbt) {
            if (nbt instanceof CompoundTag tag) {
                this.value().get().read(tag);
            }
        }
    }
}
