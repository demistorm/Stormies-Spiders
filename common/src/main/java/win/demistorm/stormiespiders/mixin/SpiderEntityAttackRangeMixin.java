package win.demistorm.stormiespiders.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Spider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import win.demistorm.stormiespiders.config.Config;

@Mixin(Mob.class)
public abstract class SpiderEntityAttackRangeMixin {

    @WrapOperation(method = "isWithinMeleeAttackRange", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;getMeleeAttackRangeSqr(Lnet/minecraft/world/entity/LivingEntity;)D"))
    private double stormiespiders$reduceAttackRange(Mob instance, LivingEntity entity, Operation<Double> original) {
        double attackRange = original.call(instance, entity);
        if (instance instanceof Spider && Config.COMMON.reducedAttackRange()) {
            double reduction = -0.4;
            attackRange += 2.0 * Math.sqrt(attackRange) * reduction + reduction * reduction;
        }
        return attackRange;
    }
}
