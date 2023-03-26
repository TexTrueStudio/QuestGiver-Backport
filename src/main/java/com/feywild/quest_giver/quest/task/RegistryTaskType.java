package com.feywild.quest_giver.quest.task;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;

public abstract class RegistryTaskType<T extends IForgeRegistryEntry<T>, X> implements TaskType<T, X> {

    private final String key;
    private final IForgeRegistry<T> registry;

    protected RegistryTaskType(String key, IForgeRegistry<T> registry) {
        this.key = key;
        this.registry = registry;
    }

    @Override
    public Class<T> element() {
        return this.registry.getRegistrySuperType();
    }

    @Override
    public T fromJson(JsonObject json) {
        ResourceLocation rl = ResourceLocation.tryParse(json.get(this.key).getAsString());
        T value = rl == null ? null : this.registry.getValue(rl);
        if (value == null)
            throw new IllegalStateException("Can't load Quest Giver quest task: " + this.element().getSimpleName() + " not found: " + rl);
        return value;
    }

    @Override
    public JsonObject toJson(T element) {
        JsonObject json = new JsonObject();
        ResourceLocation rl = this.registry.getKey(element);
        if (rl == null) throw new IllegalStateException(this.element().getSimpleName() + " not registered: " + element);
        json.addProperty(this.key, rl.toString());
        return json;
    }
}

