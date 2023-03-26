package com.feywild.quest_giver.quest.util;

import com.feywild.quest_giver.quest.QuestDisplay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

public class SelectableQuest {

    public final ResourceLocation id;
    public final Item icon;
    public final QuestDisplay display;

    public SelectableQuest(ResourceLocation id, Item icon, QuestDisplay display) {
        this.id = id;
        this.icon = icon;
        this.display = display;
    }

    public void toNetwork(FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(this.id);
        buffer.writeRegistryId(this.icon);
        this.display.toNetwork(buffer);
    }

    public static SelectableQuest fromNetwork(FriendlyByteBuf buffer) {
        ResourceLocation id = buffer.readResourceLocation();
        Item icon = buffer.readRegistryId();
        QuestDisplay display = QuestDisplay.fromNetwork(buffer);
        return new SelectableQuest(id, icon, display);
    }

}
