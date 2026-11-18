package jp.absolute.magic.registry;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import jp.absolute.magic.AbsoluteMagic;
import jp.absolute.magic.spell.FixedDamageSpell;
import jp.absolute.magic.spell.PercentDamageSpell;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModSpells {
    public static final DeferredRegister<AbstractSpell> SPELLS =
            DeferredRegister.create(SpellRegistry.SPELL_REGISTRY_KEY, AbsoluteMagic.MODID);

    public static final RegistryObject<AbstractSpell> FIXED_DAMAGE = registerSpell(new FixedDamageSpell());
    public static final RegistryObject<AbstractSpell> PERCENT_DAMAGE = registerSpell(new PercentDamageSpell());

    private static RegistryObject<AbstractSpell> registerSpell(AbstractSpell spell) {
        return SPELLS.register(spell.getSpellName(), () -> spell);
    }

    private ModSpells() {
    }
}
