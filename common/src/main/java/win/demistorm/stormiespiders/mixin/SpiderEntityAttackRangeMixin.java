package win.demistorm.stormiespiders.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import win.demistorm.stormiespiders.config.Config;
import win.demistorm.stormiespiders.config.RotationOverrideConfig;

@Mixin(Mob.class)
public abstract class SpiderEntityAttackRangeMixin {

    @WrapOperation(method = "isWithinMeleeAttackRange", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;getAttackBoundingBox()Lnet/minecraft/world/phys/AABB;"))
    private AABB stormiespiders$reduceAttackRange(Mob instance, Operation<AABB> original) {
        AABB attackBox = original.call(instance);
        if (instance instanceof Spider && Config.COMMON.reducedAttackRange() && !RotationOverrideConfig.isClimberDisabled(instance.getType())) {
            attackBox = attackBox.inflate(-0.4, 0, -0.4);
        }
        return attackBox;
    }
}
