package com.zeroxstudios.openblocks_sprinkler;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {

    public static final ForgeConfigSpec SPEC;

    // ---- Tank ----
    public static final ForgeConfigSpec.IntValue TANK_CAPACITY;

    // ---- Water Consumption ----
    public static final ForgeConfigSpec.IntValue WATER_CONSUME_RATE;
    public static final ForgeConfigSpec.IntValue WATER_CONSUME_AMOUNT;

    // ---- Bonemeal ----
    public static final ForgeConfigSpec.BooleanValue BONEMEAL_ENABLED;
    public static final ForgeConfigSpec.IntValue BONEMEAL_RATE;
    public static final ForgeConfigSpec.IntValue BONEMEAL_ATTEMPTS;
    public static final ForgeConfigSpec.DoubleValue BONEMEAL_CONSUME_CHANCE;

    // ---- Range ----
    public static final ForgeConfigSpec.IntValue EFFECTIVE_RANGE;

    // ---- Particles ----
    public static final ForgeConfigSpec.DoubleValue ANGLE_RICHNESS;
    public static final ForgeConfigSpec.IntValue VIEW_DISTANCE;
    public static final ForgeConfigSpec.IntValue PARTICLE_TICK_DELAY;

    // ---- Cheats ----
    public static final ForgeConfigSpec.BooleanValue INFINITE_WATER;
    public static final ForgeConfigSpec.BooleanValue INFINITE_BONEMEAL;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Sprinkler Tank Settings").push("tank");
        TANK_CAPACITY = builder
                .comment("Fluid tank capacity in mB. Default: 1000")
                .defineInRange("tankCapacity", 1000, 100, 64000);
        builder.pop();

        builder.comment("Sprinkler Fluid Consumption Settings").push("water");
        WATER_CONSUME_RATE = builder
                .comment("Ticks between each fluid consumption attempt. Default: 20")
                .defineInRange("waterConsumeRate", 20, 1, 72000);
        WATER_CONSUME_AMOUNT = builder
                .comment("Amount of fluid consumed per attempt in mB. Default: 100")
                .defineInRange("waterConsumeAmount", 100, 1, 10000);
        builder.pop();

        builder.comment("Sprinkler Bonemeal Settings").push("bonemeal");
        BONEMEAL_ENABLED = builder
                .comment("If false, the sprinkler will never apply bonemeal effects. Default: true")
                .define("bonemealEnabled", true);
        BONEMEAL_RATE = builder
                .comment("Ticks between bonemeal attempts. Default: 200")
                .defineInRange("bonemealRate", 200, 1, 72000);
        BONEMEAL_ATTEMPTS = builder
                .comment("Number of plants the sprinkler attempts to fertilize per bonemeal use. Default: 4")
                .defineInRange("bonemealAttempts", 4, 1, 64);
        BONEMEAL_CONSUME_CHANCE = builder
                .comment("Chance (0.0 - 1.0) that bonemeal is consumed after a successful fertilize attempt. Default: 1.0 (always consumed)")
                .defineInRange("bonemealConsumeChance", 1.0, 0.0, 1.0);
        builder.pop();

        builder.comment("Sprinkler Range Settings").push("range");
        EFFECTIVE_RANGE = builder
                .comment("WARNING: (Visuals May not Line Up) Radius in blocks the sprinkler affects in each cardinal direction. Default: 4")
                .defineInRange("effectiveRange", 4, 1, 32);
        builder.pop();

        builder.comment("Sprinkler Particle Settings").push("particles");
        ANGLE_RICHNESS = builder
                .comment("Controls the speed of the spray nozzle sine wave. Lower = slower sweep. Default: 0.01")
                .defineInRange("angleRichness", 0.01, 0.001, 1.0);
        VIEW_DISTANCE = builder
                .comment("Distance in blocks at which particles are rendered. Uses squared distance internally. Default: 64")
                .defineInRange("viewDistance", 64, 8, 256);
        PARTICLE_TICK_DELAY = builder
                .comment("Ticks between each particle spawn attempt on the client. 1 = every tick. Default: 1")
                .defineInRange("particleTickDelay", 1, 1, 20);
        builder.pop();

        builder.comment("Cheat Settings - for creative modpacks or standalone use").push("cheats");
        INFINITE_WATER = builder
                .comment("If true, the sprinkler operates without needing any fluid. Default: false")
                .define("infiniteWater", false);
        INFINITE_BONEMEAL = builder
                .comment("If true, the sprinkler fertilizes without consuming bonemeal. Default: false")
                .define("infiniteBonemeal", false);
        builder.pop();

        SPEC = builder.build();
    }
}
