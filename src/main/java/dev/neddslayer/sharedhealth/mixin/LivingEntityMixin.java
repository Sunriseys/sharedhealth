package dev.neddslayer.sharedhealth.mixin;

import dev.neddslayer.sharedhealth.components.SharedEffectComponent;
import dev.neddslayer.sharedhealth.components.SharedHealthComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.List;

import static dev.neddslayer.sharedhealth.components.SharedComponentsInitializer.SHARED_EFFECT;
import static dev.neddslayer.sharedhealth.components.SharedComponentsInitializer.SHARED_HEALTH;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    @Shadow public abstract boolean isAlive();
    @Shadow public abstract boolean hasStatusEffect(RegistryEntry<StatusEffect> effect);
    @Shadow public abstract StatusEffectInstance getStatusEffect(RegistryEntry<StatusEffect> effect);
    @Shadow public abstract float getHealth();

    @Unique
    private boolean isSyncingEffect = false;

    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }



    @Inject(method = "heal", at=@At("HEAD"))
    public void healListener(float amount, CallbackInfo ci) {
        if ((LivingEntity) (Object) this instanceof ServerPlayerEntity player && this.isAlive()) {
            float currentHealth = player.getHealth();
            SharedHealthComponent component = SHARED_HEALTH.get(player.getEntityWorld().getScoreboard());
            float knownHealth = component.getHealth();
            if (currentHealth == knownHealth) {
                component.setHealth(knownHealth + amount);
            }
        }
    }

    @Inject(method = "onStatusEffectApplied", at = @At("RETURN"))
    protected void onEffectApplied(StatusEffectInstance effect, net.minecraft.entity.Entity source, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayerEntity player && this.hasStatusEffect(effect.getEffectType())) {
            RegistryEntry<StatusEffect> effectType = effect.getEffectType();
            SharedEffectComponent component = SHARED_EFFECT.get(this.getEntityWorld().getScoreboard());
            component.setEffect(effectType);
            String playerName = this.getName().getString();
            this.getEntityWorld().getServer().getPlayerManager().broadcast(Text.of(playerName + " a recu l'effet " + effect.getEffectType().getIdAsString() + " pendant " + effect.getDuration()/60 + " secondes"), false);
            for (ServerPlayerEntity otherPlayer : player.getEntityWorld().getPlayers()) {
                if (otherPlayer != player) {
                    StatusEffectInstance otherInstance = otherPlayer.getStatusEffect(effectType);
                    if (otherInstance == null || Math.abs(otherInstance.getDuration() - effect.getDuration()) > 20) {
                        otherPlayer.addStatusEffect(new StatusEffectInstance(effect));
                    }
                }
            }
        }
    }

    @Inject(method = "onStatusEffectsRemoved", at = @At("RETURN"))
    protected void onEffectsRemoved(Collection<StatusEffectInstance> effects, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayerEntity player && this.isAlive()) {
            SharedEffectComponent component = SHARED_EFFECT.get(this.getEntityWorld().getScoreboard());
            for (StatusEffectInstance effectInstance : effects) {
                RegistryEntry<StatusEffect> effectType = effectInstance.getEffectType();
                component.setEffect(null);
                for (ServerPlayerEntity otherPlayer : player.getEntityWorld().getPlayers()) {
                    if (otherPlayer != player && otherPlayer.hasStatusEffect(effectType)) {
                        otherPlayer.removeStatusEffect(effectType);
                    }
                }
            }
        }
    }
}
