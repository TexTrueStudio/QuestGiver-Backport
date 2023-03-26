package com.feywild.quest_giver.network.quest;

import com.feywild.quest_giver.QuestGiverMod;
import com.feywild.quest_giver.quest.QuestDisplay;
import com.feywild.quest_giver.quest.player.QuestData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.PacketDistributor;

import java.util.function.Supplier;

public class SelectQuestHandler {

    public static void handle(SelectQuestSerializer.Message msg, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player != null) {
                QuestDisplay display = QuestData.get(player).getQuestLine(msg.questNumber).getActiveQuestDisplay(msg.quest); //CHANGED
                if (display != null) {
                    QuestGiverMod.getNetwork().instance.send(PacketDistributor.PLAYER.with(() -> player),
                            new OpenQuestDisplaySerializer.Message(display, false, msg.title, msg.questNumber, msg.pos, msg.id));
                }
            }
        });
        context.get().setPacketHandled(true);
    }
}
