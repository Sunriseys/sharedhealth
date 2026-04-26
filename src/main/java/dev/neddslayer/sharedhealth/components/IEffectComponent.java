package dev.neddslayer.sharedhealth.components;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.entry.RegistryEntry;
import org.ladysnake.cca.api.v3.component.ComponentV3;

public interface IEffectComponent extends ComponentV3 {
    RegistryEntry<StatusEffect> getEffect();
    void setEffect(RegistryEntry<StatusEffect> effect);
    void removeEffect(RegistryEntry<StatusEffect> effect);
}
