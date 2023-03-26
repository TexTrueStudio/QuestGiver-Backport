package com.feywild.quest_giver.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.KeybindComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.level.Level;

import java.util.Arrays;
import java.util.List;

public class TooltipHelper {

    public static void addTooltip(List<Component> tooltip,Level level, Component... lines) {
        if (level.isClientSide && KeyboardHelper.isHoldingShift()) {
            tooltip.addAll(Arrays.asList(lines));
        } else {
            TranslatableComponent textComponent = (TranslatableComponent) new TranslatableComponent("message.quest_giver.itemmessage", new KeybindComponent("key.sneak")).withStyle(ChatFormatting.GRAY);
            if (!tooltip.contains(textComponent))
                tooltip.add(textComponent);
        }
    }

}
