package dev.neddslayer.sharedhealth.mixin;

import com.mojang.authlib.GameProfile;
import dev.neddslayer.sharedhealth.SharedHealth;
import dev.neddslayer.sharedhealth.components.SharedHealthComponent;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

import static dev.neddslayer.sharedhealth.components.SharedComponentsInitializer.*;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends PlayerEntity {
    @Shadow public abstract ServerWorld getEntityWorld();

    public ServerPlayerEntityMixin(World world, GameProfile profile) {
        super(world, profile);
    }

    @Inject(method = "damage", at = @At("RETURN"))
    public void damageListener(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() && this.isAlive()) {
            float currentHealth = this.getHealth();
            SharedHealthComponent component = SHARED_HEALTH.get(this.getEntityWorld().getScoreboard());
            float knownHealth = component.getHealth();
            if (currentHealth != knownHealth) {
                component.setHealth(currentHealth);
            }

            if(SharedHealth.randomTeleport && !SharedHealth.isSyncingHealth) {
                List<ServerPlayerEntity> players = new ArrayList<>(world.getServer().getPlayerManager().getPlayerList());

                if (players.size() < 2) {
                    return;
                }

                Collections.shuffle(players);

                List<Vec3d> positions = new ArrayList<>();
                List<ServerWorld> worlds = new ArrayList<>();

                for (ServerPlayerEntity p : players) {
                    positions.add(p.getEntityPos());
                    worlds.add(p.getEntityWorld());
                }

                for (int i = 0; i < players.size(); i++) {
                    ServerPlayerEntity currentPlayer = players.get(i);

                    int nextIndex = (i + 1) % players.size();
                    Vec3d nextPos = positions.get(nextIndex);
                    ServerWorld nextWorld = worlds.get(nextIndex);

                    currentPlayer.teleport(nextWorld, nextPos.x, nextPos.y, nextPos.z, java.util.Collections.emptySet(), currentPlayer.getYaw(), currentPlayer.getPitch(), true);
                }
            }

        }
    }

    @Inject(method = "onDeath", at = @At("TAIL"))
    public void killEveryoneOnDeath(DamageSource damageSource, CallbackInfo ci) {
        // 1. SÉCURITÉ : Empêcher la boucle infinie de morts
        if (SharedHealth.isCollateralDeath) {
            return;
        }

        SharedHealth.isCollateralDeath = true;

        ServerPlayerEntity originalPlayer = (ServerPlayerEntity) (Object) this;
        ServerWorld world = this.getEntityWorld();
        Scoreboard scoreboard = world.getScoreboard();

        for (ServerPlayerEntity p : world.getServer().getPlayerManager().getPlayerList()) {
            if (p != originalPlayer && p.isAlive()) {

                Map<ScoreboardObjective, Integer> savedScores = new HashMap<>();
                for (ScoreboardObjective objective : scoreboard.getObjectives()) {
                    if (objective.getCriterion() == ScoreboardCriterion.DEATH_COUNT) {
                        savedScores.put(objective, scoreboard.getOrCreateScore(p, objective).getScore());
                    }
                }
                p.kill(world);
                for (Map.Entry<ScoreboardObjective, Integer> entry : savedScores.entrySet()) {
                    scoreboard.getOrCreateScore(p, entry.getKey()).setScore(entry.getValue());
                }
            }
        }
        SHARED_HEALTH.get(scoreboard).setHealth(20.0f);
        SHARED_HUNGER.get(scoreboard).setHunger(20);
        SHARED_SATURATION.get(scoreboard).setSaturation(20.0f);

        SharedHealth.isCollateralDeath = false;
    }
}