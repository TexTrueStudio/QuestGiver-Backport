package com.feywild.quest_giver.network.quest;

import com.feywild.quest_giver.quest.QuestNumber;
import com.feywild.quest_giver.quest.util.SelectableQuest;
import com.google.common.collect.ImmutableList;
import io.github.noeppi_noeppi.libx.network.PacketSerializer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

import java.util.List;

public class OpenQuestSelectionSerializer implements PacketSerializer<OpenQuestSelectionSerializer.Message> {

    @Override
    public Class<Message> messageClass() {
        return Message.class;
    }

    @Override
    public void encode(Message msg, FriendlyByteBuf buffer) {

        buffer.writeComponent(msg.title);
        buffer.writeEnum(msg.questNumber);
        buffer.writeBlockPos(msg.pos);
        buffer.writeVarInt(msg.quests.size());
        for (SelectableQuest quest : msg.quests) {
            quest.toNetwork(buffer);
        }
        buffer.writeInt(msg.id);
    }

    @Override
    public Message decode(FriendlyByteBuf buffer) {

        Component title = buffer.readComponent();
        QuestNumber questNumber = buffer.readEnum(QuestNumber.class);
        BlockPos pos = buffer.readBlockPos();
        int questSize = buffer.readVarInt();
        ImmutableList.Builder<SelectableQuest> quests = ImmutableList.builder();
        for (int i = 0; i < questSize; i++) {
            quests.add(SelectableQuest.fromNetwork(buffer));
        }
        int id = buffer.readInt();
        return new Message(title, questNumber, quests.build(), pos, id);
    }

    public static class Message {

        public final Component title;
        public final QuestNumber questNumber;
        public final List<SelectableQuest> quests;
        public final BlockPos pos;
        public final int id;

        public Message(Component title, QuestNumber questNumber, List<SelectableQuest> quests, BlockPos pos, int id) {
            this.title = title;
            this.questNumber = questNumber;
            this.quests = ImmutableList.copyOf(quests);
            this.pos = pos;
            this.id = id;
        }
    }
}
