package win.demistorm.stormiespiders.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import win.demistorm.stormiespiders.config.Config;

@Mixin(Mob.class)
public abstract class SpiderEntityAttackRangeMixin {

    @WrapOperation(method = "isWithinMeleeAttackRange", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;getAttackBoundingBox(D)Lnet/minecraft/world/phys/AABB;"))
    private AABB stormiespiders$reduceAttackRange(Mob instance, double horizontalExpansion, Operation<AABB> original) {
        AABB attackBox = original.call(instance, horizontalExpansion);
        if (instance instanceof Spider && Config.COMMON.reducedAttackRange()) {
            attackBox = attackBox.inflate(-0.4, 0, -0.4);
        }
        return attackBox;
    }
}
