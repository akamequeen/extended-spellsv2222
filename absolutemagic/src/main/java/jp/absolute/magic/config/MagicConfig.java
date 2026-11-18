package jp.absolute.magic.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class MagicConfig {
    public static final ForgeConfigSpec SPEC;

    // ---- Fixed Damage ----
    public static final ForgeConfigSpec.DoubleValue FIXED_DAMAGE_PER_LEVEL;
    public static final ForgeConfigSpec.IntValue FIXED_DURATION_SECONDS;

    // ---- Percent Damage ----
    public static final ForgeConfigSpec.DoubleValue PERCENT_PER_LEVEL;
    public static final ForgeConfigSpec.EnumValue<HpBasis> PERCENT_BASIS;
    public static final ForgeConfigSpec.DoubleValue PERCENT_MAX_DAMAGE;
    public static final ForgeConfigSpec.IntValue PERCENT_DURATION_SECONDS;

    // ---- Common ----
    public static final ForgeConfigSpec.BooleanValue TRIGGER_MELEE;
    public static final ForgeConfigSpec.BooleanValue TRIGGER_PROJECTILES;
    public static final ForgeConfigSpec.BooleanValue TRIGGER_SPELLS;
    public static final ForgeConfigSpec.IntValue INTERNAL_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.BooleanValue AFFECT_PLAYERS;

    public enum HpBasis { MAX, CURRENT }

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.push("fixed_damage");
        FIXED_DAMAGE_PER_LEVEL = b
                .comment("Flat extra damage per spell level (level 5 = 5x this value).",
                        "Not affected by enchantments, relics, skill tree or spell power.")
                .defineInRange("damagePerLevel", 3.0D, 0.0D, 1.0E6D);
        FIXED_DURATION_SECONDS = b
                .comment("Buff duration in seconds.")
                .defineInRange("durationSeconds", 30, 1, 3600);
        b.pop();

        b.push("percent_damage");
        PERCENT_PER_LEVEL = b
                .comment("Extra damage per spell level as a ratio of the target's HP. 0.0004 = 0.04% per level (level 5 = 0.2%).")
                .defineInRange("percentPerLevel", 0.0004D, 0.0D, 1.0D);
        PERCENT_BASIS = b
                .comment("MAX: ratio of the target's max HP. CURRENT: ratio of the target's current HP (weakens as it gets hurt).")
                .defineEnum("basis", HpBasis.MAX);
        PERCENT_MAX_DAMAGE = b
                .comment("Cap for one percent-damage hit. 0 means no limit.")
                .defineInRange("maxDamagePerHit", 0.0D, 0.0D, 1.0E9D);
        PERCENT_DURATION_SECONDS = b
                .comment("Buff duration in seconds.")
                .defineInRange("durationSeconds", 30, 1, 3600);
        b.pop();

        b.push("common");
        TRIGGER_MELEE = b
                .comment("Melee attacks (tools / weapons) trigger the extra hit.")
                .define("triggerMelee", true);
        TRIGGER_PROJECTILES = b
                .comment("Projectiles (arrows, tridents) fired by the caster trigger the extra hit.")
                .define("triggerProjectiles", false);
        TRIGGER_SPELLS = b
                .comment("Iron's Spells 'n Spellbooks spell damage triggers the extra hit.")
                .define("triggerSpells", true);
        INTERNAL_COOLDOWN_TICKS = b
                .comment("Minimum ticks between extra hits from the same attacker on the same target.",
                        "Prevents multi-hit / damage-over-time spells from stacking the extra damage every tick. 0 = none.")
                .defineInRange("internalCooldownTicks", 20, 0, 1200);
        AFFECT_PLAYERS = b
                .comment("If true, the extra damage can also hurt players (PvP).")
                .define("affectPlayers", false);
        b.pop();

        SPEC = b.build();
    }

    private MagicConfig() {
    }
}
