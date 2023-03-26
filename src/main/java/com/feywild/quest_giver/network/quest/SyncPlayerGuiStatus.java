package com.feywild.quest_giver.network.quest;

import com.feywild.quest_giver.util.ServerPlayerGuiStatus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class SyncPlayerGuiStatus {

    private final UUID playerUuid;
    private final boolean guiStatus;

    public SyncPlayerGuiStatus(FriendlyByteBuf buf) {
        this.playerUuid = buf.readUUID();
        this.guiStatus = buf.readBoolean();
    }

    public SyncPlayerGuiStatus(UUID playerUuid, boolean guiStatus) {
        this.playerUuid = playerUuid;
        this.guiStatus = guiStatus;
    }

    public static void encode(SyncPlayerGuiStatus packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.playerUuid);
        buf.writeBoolean(packet.guiStatus);
    }

    public static void handle(final SyncPlayerGuiStatus packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {});
        if(packet.guiStatus) ServerPlayerGuiStatus.playersNotInteractingWithAQuestGui.add(packet.playerUuid);
        else ServerPlayerGuiStatus.playersNotInteractingWithAQuestGui.remove(packet.playerUuid);
        ctx.setPacketHandled(true);
    }
}
