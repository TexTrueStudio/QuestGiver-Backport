package com.feywild.quest_giver.quest.player;

import com.feywild.quest_giver.EventListener;
import com.feywild.quest_giver.QuestGiverMod;
import com.feywild.quest_giver.events.QuestCompletionEvent;
import com.feywild.quest_giver.quest.*;
import com.feywild.quest_giver.quest.task.TaskType;
import com.feywild.quest_giver.quest.util.SelectableQuest;
import com.feywild.quest_giver.util.RenderEnum;
import com.google.common.collect.ImmutableList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class QuestLineData {

    private final List<ResourceLocation> pendingCompletion = new ArrayList<>();
    private final Set<ResourceLocation> completedQuests = new HashSet<>();
    private final Map<ResourceLocation, QuestProgress> activeQuests = new HashMap<>();
    @Nullable
    private ServerPlayer player;
    public final QuestNumber questNumber;
    private int reputation;
    private boolean approved = false;

    private RenderEnum icon;

   // public boolean finished = false;

    public QuestLineData(QuestNumber questNumber) {
        this.questNumber = questNumber;
        this.icon = RenderEnum.EXCLAMATION;
    }

    public void attach(ServerPlayer player) {
        this.player = player;
        this.startNextQuests();
        this.icon = RenderEnum.EXCLAMATION;
    }

    public RenderEnum getRender() {
        return this.icon;
    }

    public void setRender(String id) {
        this.icon = RenderEnum.getRender(id);
    }

    public boolean checkForEnd() {
        QuestLine quests = this.getQuestLine();
        boolean hasEmptyQuests = false;

        if (quests != null) {
            System.out.println(this.getCompletedQuests().size());
            System.out.println(quests.getQuestCount());
            if(this.getCompletedQuests().size() == quests.getQuestCount()){
                hasEmptyQuests = true;
            }
        }
        return hasEmptyQuests;
    }

    public void approve() {
        this.approved = true;
        this.reputation = 0;
        this.pendingCompletion.clear();
        this.completedQuests.clear();
        this.activeQuests.clear();
        QuestLine quests = QuestManager.getQuests(this.questNumber);
        Quest rootQuest = quests == null ? null : quests.getQuest(new ResourceLocation(QuestGiverMod.getInstance().modid, "root"));
        if (rootQuest != null && rootQuest.tasks.isEmpty()) {
            for (QuestReward reward : rootQuest.rewards) {
                reward.grantReward(this.player);
            }
            this.reputation += rootQuest.reputation;
            this.completedQuests.add(rootQuest.id);
        }
        this.startNextQuests();
    }
    
    @Nullable
    public QuestLine getQuestLine() {
        return this.questNumber == null ? null : QuestManager.getQuests(this.questNumber);
    }

    public Map<ResourceLocation, QuestProgress> getActiveQuests(){
        return this.activeQuests;
    }

    public Set<ResourceLocation> getCompletedQuests() {
        return this.completedQuests;
    }

    public int getReputation() {return this.questNumber == null ? 0 : this.reputation;}

    @Nullable
    public QuestDisplay getActiveQuestDisplay(ResourceLocation id) {
        QuestLine quests = this.getQuestLine();
        if (quests != null && this.player != null && this.activeQuests.containsKey(id)) {
            Quest quest = quests.getQuest(id);
            if (quest != null) {
                return quest.start;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public List<SelectableQuest> getQuests() {
        QuestLine quests = this.getQuestLine();
        if (quests != null && this.player != null) {
            ImmutableList.Builder<SelectableQuest> list = ImmutableList.builder();
            for (QuestProgress progress : this.activeQuests.values().stream().sorted(Comparator.comparing(q -> q.quest)).collect(Collectors.toList())) {
                Quest quest = quests.getQuest(progress.quest);
                if (quest != null) {
                    list.add(new SelectableQuest(quest.id, quest.icon, quest.start));
                }
            }
            return list.build();
        } else {
            return ImmutableList.of();
        }
    }

    // if there are quests pending for completion, picks the first one, grants
    // rewards and returns a quest display for the user
    // If there's non, returns null.
    @Nullable
    public QuestDisplay completePendingQuest() {
        QuestLine quests = this.getQuestLine();
        if (quests != null && this.player != null && !this.pendingCompletion.isEmpty()) {
            while (!this.pendingCompletion.isEmpty()) {
                ResourceLocation id = this.pendingCompletion.remove(0);
                QuestDisplay display = this.tryComplete(this.player, quests, id);
                if (display != null) {
                    return display;
                }
            }
        }
        return null;
    }

    @Nullable
    private QuestDisplay tryComplete(ServerPlayer player, QuestLine quests, ResourceLocation id) {
        Quest quest = quests.getQuest(id);
        if (quest != null) {
            QuestDisplay display = quest.tasks.isEmpty() ? quest.start : quest.complete;
            if (display != null) {
                for (QuestReward reward : quest.rewards) {
                    reward.grantReward(player);
                }
                this.reputation += quest.reputation;
                MinecraftForge.EVENT_BUS.post(new QuestCompletionEvent(player, quest, this.reputation));
                return display;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }


    public boolean hasCompleted(Quest quest) {
        return this.completedQuests.contains(quest.id);
    }

    // True if the task was completed
    // Can be used so fey entities only accept gifts for quests
    public <T> boolean checkComplete(TaskType<?, T> type, T element) {
        boolean success = false;
        QuestLine quests = this.getQuestLine();
        if (quests != null && this.player != null) {
            String msgToDisplay = null;
            // Check each active quest if the task can be completed somewhere
            for (QuestProgress progress : this.activeQuests.values()) {
                String progressMsg = progress.checkComplete(this.player, quests, type, element);
                if (progressMsg != null) {
                    // Something was completed. Set the success flag
                    success = true;
                    if (msgToDisplay == null) msgToDisplay = progressMsg;
                }
            }
            if (success) {
                triggerAfterComplete(quests, msgToDisplay);
            }
        }
        return success;
    }

    public <T, X> List<CompletableTaskInfo<T, X>> getAllCurrentTasks(TaskType<T, X> type) {
        ImmutableList.Builder<CompletableTaskInfo<T, X>> list = ImmutableList.builder();
        QuestLine quests = this.getQuestLine();
        if (quests != null && this.player != null) {
            for (QuestProgress progress : this.activeQuests.values()) {
                return progress.getQuestElements(player, quests, type)
                        .map(elem -> new CompletableTaskInfo<>(type, elem, element -> {
                            String progressMsg = progress.checkComplete(this.player, quests, type, element);
                            if (progressMsg != null) {
                                // Something was completed.
                                triggerAfterComplete(quests, progressMsg);
                            }
                        })).collect(Collectors.toList());
            }
        }
        return list.build();
    }

    private void triggerAfterComplete(QuestLine quests, String msgToDisplay) {
        // Something was completed. Move all completed quests into the
        // completed quests set and unlock new quests
        boolean shouldNotify = false;
        Iterator<QuestProgress> itr = this.activeQuests.values().iterator();
        while (itr.hasNext()) {
            QuestProgress progress = itr.next();
            if (progress.shouldBeComplete(quests)) {
                // grant rewards and remove quest from active quests
                this.pendingCompletion.add(progress.quest);
                this.completedQuests.add(progress.quest);
                shouldNotify = true;
                itr.remove();
            }
        }
        if (this.player != null) {
            if (shouldNotify) {
                this.player.displayClientMessage(new TranslatableComponent("message.quest_giver.quest_completion"), true);
            } else {
                this.player.displayClientMessage(new TextComponent(msgToDisplay), true);
            }
        }
        this.startNextQuests();
    }

    public void startNextQuests() {
        QuestLine quests = this.getQuestLine();
        boolean hasEmptyQuests = false;
        if (quests != null) {
            for (Quest newQuest : quests.getNextQuests(this.activeQuests.keySet(), this.completedQuests)) {
                if (newQuest.tasks.isEmpty()) {
                    // Empty quest will never be active but always pending for completion
                    if (!this.pendingCompletion.contains(newQuest.id)) {
                        this.pendingCompletion.add(newQuest.id);
                        this.completedQuests.add(newQuest.id);
                        hasEmptyQuests = true;
                      //  this.setRender("none");

                    }
                } else {
                    if (!this.activeQuests.containsKey(newQuest.id)) {
                        QuestProgress progress = new QuestProgress(newQuest.id);
                        this.activeQuests.put(newQuest.id, progress);
                    }
                }
            }
        }
        if (hasEmptyQuests) {
            // We have quests that instantly got pending for completion
            // so we need to start their children quests now.
            startNextQuests();
        }
        //noinspection ConstantConditions
        if(this.player.connection!=null) EventListener.syncPlayerRenders(this.player);
    }

    public CompoundTag write() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("QuestNumber", QuestNumber.optionId(this.questNumber));
        nbt.putInt("Reputation", reputation);
        nbt.putBoolean("Approved", approved);
    //    nbt.putBoolean("Finished", this.finished);

        ListTag pending = new ListTag();
        for (ResourceLocation quest : this.pendingCompletion) {
            pending.add(StringTag.valueOf(quest.toString()));
        }
        nbt.put("Pending", pending);
        ListTag completed = new ListTag();
        for (ResourceLocation quest : this.completedQuests) {
            completed.add(StringTag.valueOf(quest.toString()));
        }
        nbt.put("Completed", completed);
        CompoundTag active = new CompoundTag();
        for (Map.Entry<ResourceLocation, QuestProgress> entry : this.activeQuests.entrySet()) {
            active.put(entry.getKey().toString(), entry.getValue().write());
        }
        nbt.put("Active", active);
        return nbt;
    }

    public void read(CompoundTag nbt) {
        this.reputation = nbt.getInt("Reputation");
        this.approved = nbt.getBoolean("Approved");
     //   this.finished = nbt.getBoolean("Finished");

        ListTag pending = nbt.getList("Pending", Tag.TAG_STRING);
        this.pendingCompletion.clear();
        for (int i = 0; i < pending.size(); i++) {
            ResourceLocation id = ResourceLocation.tryParse(pending.getString(i));
            if (id != null) this.pendingCompletion.add(id);
        }

        ListTag completed = nbt.getList("Completed", Tag.TAG_STRING);
        this.completedQuests.clear();
        for (int i = 0; i < completed.size(); i++) {
            ResourceLocation id = ResourceLocation.tryParse(completed.getString(i));
            if (id != null) this.completedQuests.add(id);
        }
        CompoundTag active = nbt.getCompound("Active");

        this.activeQuests.clear();
        for (String key : active.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            if (id != null) {
                QuestProgress progress = new QuestProgress(id);
                progress.read(active.getCompound(key));
                this.activeQuests.put(id, progress);
            }
        }
    }
}
