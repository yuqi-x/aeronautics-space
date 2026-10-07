package com.yuqi.aeronauticsspace.mixin;

import com.yuqi.aeronauticsspace.client.SpaceSkyClient;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 让"天空盒底色"随高度切换：太空线以上，天空色被拉向深空黑。
 *
 * ClientLevel#getSkyColor 是天空 clear color 的来源，
 * 只要它在太空时返回深色，整个天空底色就变了。
 */
@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {

    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
    private void aeronauticsSpace$getSkyColor(Vec3 cameraPos, float partialTick,
                                              CallbackInfoReturnable<Vec3> cir) {
        final double f = SpaceSkyClient.clientSpaceFactor();
        if (f <= 0.0D) {
            return;
        }
        final Vec3 orig = cir.getReturnValue();
        if (orig == null) {
            return;
        }
        final double k = 1.0D - f;
        cir.setReturnValue(new Vec3(
                orig.x * k * 0.25D,
                orig.y * k * 0.25D,
                orig.z * k * 0.25D + 0.035D * f
        ));
    }

    @Inject(method = "getCloudColor", at = @At("RETURN"), cancellable = true)
    private void aeronauticsSpace$getCloudColor(float partialTick,
                                                CallbackInfoReturnable<Vec3> cir) {
        final double f = SpaceSkyClient.clientSpaceFactor();
        if (f <= 0.0D) {
            return;
        }
        final Vec3 orig = cir.getReturnValue();
        if (orig == null) {
            return;
        }
        final double k = 1.0D - f * 0.85D;
        cir.setReturnValue(new Vec3(orig.x * k, orig.y * k, orig.z * k + 0.02D * f));
    }
}
