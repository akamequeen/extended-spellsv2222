package jp.absolute.magic.damage;

import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import jp.absolute.magic.config.MagicConfig;
import jp.absolute.magic.registry.ModEffects;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Fixed Damage   : every qualifying hit by the buffed caster queues an extra hit of a flat amount.
 * Percent Damage : every qualifying hit queues an extra hit of (ratio x target HP).
 *
 * Qualifying hits: melee (tools / weapons), optionally projectiles, and Iron's Spells spell damage. They are
 * detected in LivingDamageEvent at MONITOR priority, i.e. after every calculation has finished and only if
 * the hit really dealt damage. The extra amount never depends on the original hit, so enchantments, relics,
 * skill tree buffs and spell power cannot change it.
 *
 * The extra hit is applied at the end of the server tick with the damage type "absolutemagic:transcend"
 * (bypass tags skip armor, resistance, shields and enchantments). To stop other mods from scaling it
 * (damage-buff relics, L2Hostility traits, ...), the exact amount is re-imposed in LivingHurtEvent and
 * LivingDamageEvent at LOWEST priority, i.e. after all other listeners.
 */
public final class TranscendHandler {
    private static final class Pending {
        final LivingEntity attacker;
        final LivingEntity target;
        final Entity direct;
        final boolean fixed;
        final int level;

        Pending(LivingEntity attacker, LivingEntity target, Entity direct, boolean fixed, int level) {
            this.attacker = attacker;
            this.target = target;
            this.direct = direct;
            this.fixed = fixed;
            this.level = level;
        }
    }

    private record CooldownKey(UUID attacker, UUID target, boolean fixed) {
    }

    private static final List<Pending> QUEUE = new ArrayList<>();
    private static final Map<CooldownKey, Long> LAST_TRIGGER = new HashMap<>();
    /** Extra-hit damage sources currently being applied, mapped to the exact amount they must deal. */
    private static final Map<DamageSource, Float> EXACT = new IdentityHashMap<>();

    // ------------------------------------------------------------------ trigger

    @SubscribeEvent(priority = EventPriority.MONITOR)
    public static void onDamageDone(LivingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide || event.getAmount() <= 0.0F) {
            return;
        }
        DamageSource source = event.getSource();
        if (source.is(ModDamageTypes.TRANSCEND) || !(source.getEntity() instanceof LivingEntity attacker)) {
            return;
        }
        if (!isTrigger(source, attacker) || !canTarget(attacker, target)) {
            return;
        }
        tryQueue(attacker, target, source, true);
        tryQueue(attacker, target, source, false);
    }

    private static void tryQueue(LivingEntity attacker, LivingEntity target, DamageSource source, boolean fixed) {
        MobEffectInstance effect = attacker.getEffect(fixed
                ? ModEffects.FIXED_DAMAGE.get()
                : ModEffects.PERCENT_DAMAGE.get());
        if (effect == null) {
            return;
        }
        int cooldown = MagicConfig.INTERNAL_COOLDOWN_TICKS.get();
        long now = target.level().getGameTime();
        CooldownKey key = new CooldownKey(attacker.getUUID(), target.getUUID(), fixed);
        if (cooldown > 0) {
            Long last = LAST_TRIGGER.get(key);
            if (last != null && now - last < cooldown) {
                return;
            }
        }
        LAST_TRIGGER.put(key, now);
        Entity direct = source.getDirectEntity() != null ? source.getDirectEntity() : attacker;
        QUEUE.add(new Pending(attacker, target, direct, fixed, effect.getAmplifier() + 1));
    }

    private static boolean isTrigger(DamageSource source, LivingEntity attacker) {
        if (source instanceof SpellDamageSource) {
            return MagicConfig.TRIGGER_SPELLS.get();
        }
        Entity direct = source.getDirectEntity();
        if (direct == attacker
                && (source.is(DamageTypes.PLAYER_ATTACK)
                || source.is(DamageTypes.MOB_ATTACK)
                || source.is(DamageTypes.MOB_ATTACK_NO_AGGRO))) {
            return MagicConfig.TRIGGER_MELEE.get();
        }
        return MagicConfig.TRIGGER_PROJECTILES.get()
                && direct instanceof Projectile
                && source.is(DamageTypeTags.IS_PROJECTILE);
    }

    private static boolean canTarget(LivingEntity attacker, LivingEntity target) {
        if (target == attacker || attacker.isAlliedTo(target)) {
            return false;
        }
        return !(target instanceof Player) || MagicConfig.AFFECT_PLAYERS.get();
    }

    // ------------------------------------------------------------------ apply

    private static float amountFor(Pending p) {
        if (p.fixed) {
            return (float) (MagicConfig.FIXED_DAMAGE_PER_LEVEL.get() * p.level);
        }
        float hp = MagicConfig.PERCENT_BASIS.get() == MagicConfig.HpBasis.MAX
                ? p.target.getMaxHealth()
                : p.target.getHealth();
        double amount = hp * MagicConfig.PERCENT_PER_LEVEL.get() * p.level;
        double cap = MagicConfig.PERCENT_MAX_DAMAGE.get();
        if (cap > 0.0D) {
            amount = Math.min(amount, cap);
        }
        return (float) amount;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (QUEUE.isEmpty()) {
            return;
        }
        List<Pending> batch = new ArrayList<>(QUEUE);
        QUEUE.clear();
        for (Pending p : batch) {
            if (!p.target.isAlive()) {
                continue;
            }
            float amount = amountFor(p);
            if (amount <= 0.0F) {
                continue;
            }
            DamageSource source = ModDamageTypes.source(p.target.level(), p.attacker, p.direct);
            EXACT.put(source, amount);
            try {
                p.target.hurt(source, amount);
            } finally {
                EXACT.remove(source);
            }
        }
        // Drop stale cooldown entries now and then.
        if (!LAST_TRIGGER.isEmpty() && event.getServer().getTickCount() % 1200 == 0) {
            long now = event.getServer().overworld().getGameTime();
            LAST_TRIGGER.values().removeIf(t -> now - t > 1200L);
        }
    }

    // ------------------------------------------------------------------ keep the amount exact

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void pinHurt(LivingHurtEvent event) {
        Float exact = EXACT.get(event.getSource());
        if (exact != null) {
            event.setAmount(exact);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void pinDamage(LivingDamageEvent event) {
        Float exact = EXACT.get(event.getSource());
        if (exact != null) {
            event.setAmount(exact);
        }
    }

    private TranscendHandler() {
    }
}
