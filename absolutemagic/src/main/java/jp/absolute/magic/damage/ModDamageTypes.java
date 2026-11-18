package jp.absolute.magic.damage;

import jp.absolute.magic.AbsoluteMagic;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public final class ModDamageTypes {
    /** The extra true-damage type shared by both Transcend spells. */
    public static final ResourceKey<DamageType> TRANSCEND = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(AbsoluteMagic.MODID, "transcend"));

    public static DamageSource source(Level level, Entity causing, Entity direct) {
        Holder<DamageType> holder = level.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(TRANSCEND);
        return new DamageSource(holder, direct, causing);
    }

    private ModDamageTypes() {
    }
}
