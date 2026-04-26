package dev.neddslayer.sharedhealth.mixin;

import com.mojang.authlib.GameProfile;
import dev.neddslayer.sharedhealth.components.SharedHealthComponent;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.message.MessageType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

import static dev.neddslayer.sharedhealth.components.SharedComponentsInitializer.*;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends PlayerEntity {
    @Shadow public abstract ServerWorld getEntityWorld();

    public ServerPlayerEntityMixin(World world, GameProfile profile) {
        super(world, profile);
    }

    @Inject(method = "damage", at = @At("RETURN"))
    public void damageListener(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
		// ensure that damage is only taken if the damage listener is handled; you shouldn't be able to punch invulnerable players, etc.
		if (cir.getReturnValue() && this.isAlive()) {
			float currentHealth = this.getHealth();
			SharedHealthComponent component = SHARED_HEALTH.get(this.getEntityWorld().getScoreboard());
			float knownHealth = component.getHealth();
            if ((knownHealth - currentHealth) > 0.0) {
                String playerName = this.getName().getString();
                if ((knownHealth - currentHealth) > 6.0) {
                    this.getEntityWorld().getServer().getPlayerManager().broadcast(Text.of(playerName + " A PRIT " + amount/2 + " COEURS, WTF"), false);
                }
                else {
                    this.getEntityWorld().getServer().getPlayerManager().broadcast(Text.of(playerName + " a prit " + amount/2 + " coeurs"), true);
                }
            }
			if (currentHealth != knownHealth) {
				component.setHealth(currentHealth);
			}
		}
    }

    @Inject(method = "onDeath", at = @At("TAIL"))
    public void killEveryoneOnDeath(DamageSource damageSource, CallbackInfo ci) {
        this.getEntityWorld().getServer().getPlayerManager().getPlayerList().forEach(p -> p.kill(this.getEntityWorld()));
        SHARED_HEALTH.get(this.getEntityWorld().getScoreboard()).setHealth(20.0f);
        SHARED_HUNGER.get(this.getEntityWorld().getScoreboard()).setHunger(20);
		SHARED_SATURATION.get(this.getEntityWorld().getScoreboard()).setSaturation(20.0f);
    }
}
