package dev.neddslayer.sharedhealth.components;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.Identifier;

public class SharedEffectComponent implements IEffectComponent {

    // On stocke l'entrée de registre de l'effet actuel
    RegistryEntry<StatusEffect> currentEffect = null;

    Scoreboard scoreboard;
    MinecraftServer server;

    public SharedEffectComponent(Scoreboard scoreboard, MinecraftServer server) {
        this.scoreboard = scoreboard;
        this.server = server;
    }

    @Override
    public RegistryEntry<StatusEffect> getEffect() {
        return this.currentEffect;
    }

    @Override
    public void setEffect(RegistryEntry<StatusEffect> effect) {
        this.currentEffect = effect;
    }

    @Override
    public void removeEffect(RegistryEntry<StatusEffect> effect) {
        this.currentEffect = null;
    }
    /**
     * Lecture : On récupère l'ID (String), on le transforme en Identifier,
     * puis on va chercher l'effet correspondant dans le registre.
     */
    @Override
    public void readData(ReadView readView) {
        if (readView.contains("sharedEffectId")) {
            String idString = readView.getString("sharedEffectId","");
            Identifier id = Identifier.of(idString);

            // Récupération de l'effet dans le registre de la 1.21.1
            Registries.STATUS_EFFECT.getEntry(id).ifPresent(entry -> {
                this.currentEffect = entry;
            });
        }
    }

    /**
     * Écriture : On transforme l'effet en son ID textuel pour le NBT.
     */
    @Override
    public void writeData(WriteView writeView) {
        if (this.currentEffect != null) {
            Identifier id = Registries.STATUS_EFFECT.getId(this.currentEffect.value());
            if (id != null) {
                writeView.putString("sharedEffectId", id.toString());
            }
        }
    }
}