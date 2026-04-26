package dev.neddslayer.sharedhealth;

import dev.neddslayer.sharedhealth.components.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.rule.GameRule;
import net.minecraft.world.rule.GameRuleCategory;

import static dev.neddslayer.sharedhealth.components.SharedComponentsInitializer.*;

public class SharedHealth implements ModInitializer {
    public static final GameRule<Boolean> SYNC_HEALTH = GameRuleBuilder
            .forBoolean(true).category(GameRuleCategory.PLAYER).buildAndRegister(Identifier.of("sharedhealth", "share_health"));
    public static final GameRule<Boolean> SYNC_HUNGER = GameRuleBuilder
            .forBoolean(true).category(GameRuleCategory.PLAYER).buildAndRegister(Identifier.of("sharedhealth", "share_hunger"));
    public static final GameRule<Boolean> SYNC_EFFECT = GameRuleBuilder
            .forBoolean(true).category(GameRuleCategory.PLAYER).buildAndRegister(Identifier.of("sharedhealth", "share_effect"));
    public static final GameRule<Boolean> LIMIT_HEALTH = GameRuleBuilder
            .forBoolean(true).category(GameRuleCategory.PLAYER).buildAndRegister(Identifier.of("sharedhealth", "limit_health"));

    public static final GameRule<Boolean> RANDOM_TELEPORT = GameRuleBuilder
            .forBoolean(false).category(GameRuleCategory.PLAYER).buildAndRegister(Identifier.of("sharedhealth", "random_teleport"));
    private static boolean lastHealthValue = true;
    private static boolean lastHungerValue = true;
    private static boolean lastEffectValue = true;

    public static boolean isCollateralDeath = false;
    public static boolean randomTeleport = false;
    public static boolean isSyncingHealth = false;

    /**
     * Runs the mod initializer.
     */
    @Override
    public void onInitialize() {
        ServerTickEvents.END_WORLD_TICK.register((world -> {
            boolean currentHealthValue = world.getGameRules().getValue(SYNC_HEALTH);
            boolean currentHungerValue = world.getGameRules().getValue(SYNC_HUNGER);
            boolean currentEffectValue = world.getGameRules().getValue(SYNC_EFFECT);
            boolean limitHealthValue = world.getGameRules().getValue(LIMIT_HEALTH);
            randomTeleport = world.getGameRules().getValue(RANDOM_TELEPORT);
            if (currentHealthValue != lastHealthValue && currentHealthValue) {
                world.getServer().getPlayerManager().getPlayerList().forEach(player -> player.sendMessageToClient(Text.translatable("gamerule.sharedhealth.share_health.enabled").formatted(Formatting.GREEN, Formatting.BOLD), false));
                lastHealthValue = true;
            }
            else if (currentHealthValue != lastHealthValue) {
                world.getServer().getPlayerManager().getPlayerList().forEach(player -> player.sendMessageToClient(Text.translatable("gamerule.sharedhealth.share_health.disabled").formatted(Formatting.RED, Formatting.BOLD), false));
                lastHealthValue = false;
            }
            if (currentHungerValue != lastHungerValue && currentHungerValue) {
                world.getServer().getPlayerManager().getPlayerList().forEach(player -> player.sendMessageToClient(Text.translatable("gamerule.sharedhealth.share_hunger.enabled").formatted(Formatting.GREEN, Formatting.BOLD), false));
                lastHungerValue = true;
            }
            else if (currentHungerValue != lastHungerValue) {
                world.getServer().getPlayerManager().getPlayerList().forEach(player -> player.sendMessageToClient(Text.translatable("gamerule.sharedhealth.share_hunger.disabled").formatted(Formatting.RED, Formatting.BOLD), false));
                lastHungerValue = false;
            }
            if (currentEffectValue != lastEffectValue && currentEffectValue) {
                world.getServer().getPlayerManager().getPlayerList().forEach(player -> player.sendMessageToClient(Text.translatable("gamerule.sharedhealth.share_effect.enabled").formatted(Formatting.GREEN, Formatting.BOLD), false));
                lastEffectValue = true;
            }
            else if (currentEffectValue != lastEffectValue) {
                world.getServer().getPlayerManager().getPlayerList().forEach(player -> player.sendMessageToClient(Text.translatable("gamerule.sharedhealth.share_effect.disabled").formatted(Formatting.RED, Formatting.BOLD), false));
                lastEffectValue = false;
            }
            if (world.getGameRules().getValue(SYNC_HEALTH)) {
                SharedHealthComponent component = SHARED_HEALTH.get(world.getScoreboard());
                if (component.getHealth() > 20 && limitHealthValue) component.setHealth(20);
                float finalKnownHealth = component.getHealth();
                isSyncingHealth = true;
                world.getPlayers().forEach(playerEntity -> {
                    try {
                        float currentHealth = playerEntity.getHealth();

                        if (currentHealth > finalKnownHealth) {
                            playerEntity.damage(world, world.getDamageSources().genericKill(), currentHealth - finalKnownHealth);
                        } else if (currentHealth < finalKnownHealth) {
                            playerEntity.heal(finalKnownHealth - currentHealth);
                        }
                    } catch (Exception e) {
                        System.err.println(e.getMessage());
                    }
                });
                isSyncingHealth = false;
            }
            if (world.getGameRules().getValue(SYNC_HUNGER)) {
                SharedHungerComponent component = SHARED_HUNGER.get(world.getScoreboard());
	            SharedSaturationComponent saturationComponent = SHARED_SATURATION.get(world.getScoreboard());
	            SharedExhaustionComponent exhaustionComponent = SHARED_EXHAUSTION.get(world.getScoreboard());
                if (component.getHunger() > 20) component.setHunger(20);
				if (saturationComponent.getSaturation() > 20) saturationComponent.setSaturation(20.0f);
                int finalKnownHunger = component.getHunger();
				float finalKnownSaturation = saturationComponent.getSaturation();
				float finalKnownExhaustion = exhaustionComponent.getExhaustion();
                world.getPlayers().forEach(playerEntity -> {
                    try {
                        float currentHunger = playerEntity.getHungerManager().getFoodLevel();
						float currentSaturation = playerEntity.getHungerManager().getSaturationLevel();
						float currentExhaustion = playerEntity.getHungerManager().exhaustion;

                        if (currentHunger != finalKnownHunger) {
                            playerEntity.getHungerManager().setFoodLevel(finalKnownHunger);
                        }
						if (currentSaturation != finalKnownSaturation) {
							playerEntity.getHungerManager().setSaturationLevel(finalKnownSaturation);
						}
						if (currentExhaustion != finalKnownExhaustion) {
							playerEntity.getHungerManager().exhaustion = finalKnownExhaustion;
						}
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                });
            }
            if (world.getGameRules().getValue(SYNC_EFFECT)) {
                SharedEffectComponent effectComponent = SHARED_EFFECT.get(world.getScoreboard());
                RegistryEntry<StatusEffect> sharedEffectType = effectComponent.getEffect();

                if (sharedEffectType != null) {
                    StatusEffectInstance masterInstance = null;
                    for (ServerPlayerEntity p : world.getPlayers()) {
                        if (p.hasStatusEffect(sharedEffectType)) {
                            masterInstance = p.getStatusEffect(sharedEffectType);
                            break;
                        }
                    }
                    if (masterInstance != null) {
                        final StatusEffectInstance reference = masterInstance;
                        world.getPlayers().forEach(player -> {
                            StatusEffectInstance current = player.getStatusEffect(sharedEffectType);
                            if (current == null || Math.abs(current.getDuration() - reference.getDuration()) > 20) {
                                player.addStatusEffect(new StatusEffectInstance(reference));
                            }
                        });
                    }
                } else {
                    //Si on veut gerer qu'un groupe ou tout le monde
                }
            }
        }));

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> handler.player.setHealth(SHARED_HEALTH.get(handler.player.getEntityWorld().getScoreboard()).getHealth()));

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> newPlayer.setHealth(SHARED_HEALTH.get(newPlayer.getEntityWorld().getScoreboard()).getHealth()));
    }
}
