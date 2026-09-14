package com.example.skyblockusd.mixin;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import java.util.List;
import java.util.Set;

/** Optional targets must not link when their owning mod is absent. */
public final class CompatibilityPlugin implements IMixinConfigPlugin {
    @Override public void onLoad(String mixinPackage) { }
    @Override public String getRefMapperConfig() { return null; }
    @Override public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.contains("CustomScoreboard")) return FabricLoader.getInstance().isModLoaded("customscoreboard");
        if (mixinClassName.contains("SkyHanni")) return FabricLoader.getInstance().isModLoaded("skyhanni");
        return true;
    }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) { }
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String name, ClassNode target, String mixin, IMixinInfo info) { }
    @Override public void postApply(String name, ClassNode target, String mixin, IMixinInfo info) { }
}
