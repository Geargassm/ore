package rearth.oritech.init;

import net.minecraftforge.common.ForgeConfigSpec;

public class OritechConfig {
    
    // Server config
    private static final ForgeConfigSpec.Builder COMMON = new ForgeConfigSpec.Builder();
    
    // Machine settings
    static {
        COMMON.push("machineSettings");
    }
    
    public static final ProcessingMachines processingMachines = new ProcessingMachines(COMMON);
    public static final Generators generators = new Generators(COMMON);
    public static final LaserArmConfig laserArmConfig = new LaserArmConfig(COMMON);
    public static final DeepDrillConfig deepDrillConfig = new DeepDrillConfig(COMMON);
    public static final MachineFrameConfig destroyerConfig = new MachineFrameConfig(COMMON, "destroyerConfig", 15, 40, 8, 128);
    public static final FertilizerConfig fertilizerConfig = new FertilizerConfig(COMMON);
    public static final MachineFrameConfig placerConfig = new MachineFrameConfig(COMMON, "placerConfig", 10, 5, 8, 64);
    public static final AddonConfig addonConfig = new AddonConfig(COMMON);
    
    public static final ForgeConfigSpec.BooleanValue additiveAddons = COMMON
                                                                      .comment("Whether addon bonuses are added additive or multiplicative")
                                                                      .worldRestart()
                                                                      .define("additiveAddons", true);
    
    public static final ForgeConfigSpec.BooleanValue layeredExtenders = COMMON
                                                                        .comment("When enabled, machine core quality determines how many layers of machine extenders can be used. If false, it's the direct amount.")
                                                                        .define("layeredExtenders", false);
    
    public static final ForgeConfigSpec.DoubleValue blockBreakHardnessExponentialFactor = COMMON
                                                                                          .comment("Applies to the destroyer block and enderic laser. Lower = hardness has a lower effect on speed.")
                                                                                          .defineInRange("blockBreakHardnessExponentialFactor", 0.5, 0.0, 10.0);
    
    static {
        COMMON.pop();
    }
    
    // Storage blocks
    static {
        COMMON.push("storageBlocks");
    }
    
    public static final BasicMachineConfig smallEnergyStorage = new BasicMachineConfig(COMMON, "smallEnergyStorage", 1_000_000, 5_000, 5_000, 0);
    public static final BasicMachineConfig largeEnergyStorage = new BasicMachineConfig(COMMON, "largeEnergyStorage", 20_000_000, 10_000, 10_000, 0);
    
    public static final ForgeConfigSpec.IntValue portableTankCapacityBuckets = COMMON
                                                                               .comment("Portable tank fluid capacity in buckets")
                                                                               .defineInRange("portableTankCapacityBuckets", 256, 1, Integer.MAX_VALUE);
    
    public static final ForgeConfigSpec.IntValue overchargedCrystalChargeRate = COMMON
                                                                                .comment("Overcharged crystal energy generation rate in RF/t")
                                                                                .defineInRange("overchargedCrystalChargeRate", 10, 1, Integer.MAX_VALUE);
    
    static {
        COMMON.pop();
    }
    
    // Logistics
    static {
        COMMON.push("logistics");
    }
    
    public static final ForgeConfigSpec.IntValue itemPipeTransferAmount = COMMON
                                                                          .comment("Items transferred per pipe cycle")
                                                                          .worldRestart()
                                                                          .defineInRange("itemPipeTransferAmount", 8, 1, Integer.MAX_VALUE);
    
    public static final ForgeConfigSpec.IntValue itemPipeIntervalDuration = COMMON
                                                                            .comment("Ticks between item pipe transfers")
                                                                            .worldRestart()
                                                                            .defineInRange("itemPipeIntervalDuration", 5, 1, Integer.MAX_VALUE);
    
    public static final ForgeConfigSpec.DoubleValue fluidPipeExtractAmountBuckets = COMMON
                                                                                    .comment("Fluid extracted per pipe cycle in buckets")
                                                                                    .worldRestart()
                                                                                    .defineInRange("fluidPipeExtractAmountBuckets", 0.5, 0.0, 1000.0);
    
    public static final ForgeConfigSpec.IntValue fluidPipeExtractIntervalDuration = COMMON
                                                                                    .comment("Ticks between fluid pipe extractions")
                                                                                    .worldRestart()
                                                                                    .defineInRange("fluidPipeExtractIntervalDuration", 3, 1, Integer.MAX_VALUE);
    
    public static final ForgeConfigSpec.DoubleValue fluidPipeInternalStorageBuckets = COMMON
                                                                                      .comment("Fluid pipe internal buffer size in buckets")
                                                                                      .defineInRange("fluidPipeInternalStorageBuckets", 2.0, 0.0, 1000.0);
    
    public static final ForgeConfigSpec.LongValue energyPipeTransferRate = COMMON
                                                                           .comment("Energy pipe transfer rate in RF/t")
                                                                           .defineInRange("energyPipeTransferRate", 10_000L, 0L, Long.MAX_VALUE);
    
    public static final ForgeConfigSpec.LongValue superConductorTransferRate = COMMON
                                                                               .comment("Superconductor cable transfer rate in RF/t")
                                                                               .defineInRange("superConductorTransferRate", 4_194_304L, 0L, Long.MAX_VALUE);
    
    public static final PowerPoleConfig poleConfig = new PowerPoleConfig(COMMON);
    
    static {
        COMMON.pop();
    }
    
    // Equipment
    static {
        COMMON.push("equipment");
    }
    
    public static final BasicMachineConfig charger = new BasicMachineConfig(COMMON, "charger", 500_000, 10_000, 5_000, 0);
    
    public static final ForgeConfigSpec.BooleanValue chainsawTreeCutting = COMMON
                                                                           .comment("Enable tree cutting for the chainsaw")
                                                                           .define("chainsawTreeCutting", true);
    
    static {
        COMMON.pop();
    }
    
    // World generation
    static {
        COMMON.push("worldGeneration");
    }
    
    public static final ForgeConfigSpec.BooleanValue easyFindFeatures = COMMON
                                                                        .comment("Makes oil wells and bedrock ore nodes easier to find. Oil wells show a small fountain, ore nodes a boulder on the surface.")
                                                                        .define("easyFindFeatures", true);
    
    static {
        COMMON.pop();
    }
    
    // Reactor
    static {
        COMMON.push("reactor");
    }
    
    public static final ForgeConfigSpec.BooleanValue safeMode = COMMON
                                                                .comment("With safe mode enabled, the reactor enters a cooldown period instead of exploding when overheated")
                                                                .define("safeMode", false);
    
    public static final ForgeConfigSpec.IntValue safeModeCooldown = COMMON
                                                                    .comment("Reactor safe mode cooldown duration in ticks")
                                                                    .defineInRange("safeModeCooldown", 2400, 0, Integer.MAX_VALUE);
    
    public static final ForgeConfigSpec.IntValue maxSize = COMMON
                                                           .comment("Maximum reactor multiblock size")
                                                           .worldRestart()
                                                           .defineInRange("maxSize", 64, 1, Integer.MAX_VALUE);
    
    public static final ForgeConfigSpec.IntValue reactorMaxEnergyStored = COMMON
                                                                          .comment("Maximum energy stored in the reactor in RF")
                                                                          .worldRestart()
                                                                          .defineInRange("reactorMaxEnergyStored", 50_000_000, 0, Integer.MAX_VALUE);
    
    public static final ForgeConfigSpec.IntValue reactorMaxEnergyOutput = COMMON
                                                                          .comment("Maximum energy output in RF/t per energy port")
                                                                          .worldRestart()
                                                                          .defineInRange("reactorMaxEnergyOutput", 25_000, 0, Integer.MAX_VALUE);
    
    public static final ForgeConfigSpec.IntValue rfPerPulse = COMMON
                                                              .comment("RF generated per reactor pulse")
                                                              .worldRestart()
                                                              .defineInRange("rfPerPulse", 64, 0, Integer.MAX_VALUE);
    
    public static final ForgeConfigSpec.IntValue absorberRate = COMMON
                                                                .comment("Neutron absorber heat reduction rate")
                                                                .worldRestart()
                                                                .defineInRange("absorberRate", 16, 0, Integer.MAX_VALUE);
    
    public static final ForgeConfigSpec.IntValue ventBaseRate = COMMON
                                                                .comment("Vent base cooling rate")
                                                                .worldRestart()
                                                                .defineInRange("ventBaseRate", 4, 0, Integer.MAX_VALUE);
    
    public static final ForgeConfigSpec.IntValue ventRelativeRate = COMMON
                                                                    .comment("Vent proportional cooling rate")
                                                                    .worldRestart()
                                                                    .defineInRange("ventRelativeRate", 100, 0, Integer.MAX_VALUE);
    
    public static final ForgeConfigSpec.IntValue maxHeat = COMMON
                                                           .comment("Maximum heat before reactor meltdown")
                                                           .worldRestart()
                                                           .defineInRange("maxHeat", 2000, 0, Integer.MAX_VALUE);
    
    public static final ForgeConfigSpec.IntValue maxUnstableTicks = COMMON
                                                                    .comment("Ticks before an overheated reactor explodes")
                                                                    .worldRestart()
                                                                    .defineInRange("maxUnstableTicks", 600, 0, Integer.MAX_VALUE);
    
    public static final ForgeConfigSpec.BooleanValue boringNukes = COMMON
                                                                   .comment("Disable fancy reactor explosion effects")
                                                                   .define("boringNukes", false);
    
    static {
        COMMON.pop();
    }
    
    // Arcane
    static {
        COMMON.push("arcane");
    }
    
    public static final ForgeConfigSpec.IntValue enchanterCostMultiplier = COMMON
                                                                           .comment("Enchanter soul cost multiplier")
                                                                           .defineInRange("enchanterCostMultiplier", 5, 0, Integer.MAX_VALUE);
    
    public static final ForgeConfigSpec.IntValue catalystBaseSouls = COMMON
                                                                     .comment("Catalyst base soul capacity")
                                                                     .defineInRange("catalystBaseSouls", 50, 0, Integer.MAX_VALUE);
    
    public static final ForgeConfigSpec.IntValue catalystRFPerSoul = COMMON
                                                                     .comment("RF/t per catalyst soul capacity level increase")
                                                                     .defineInRange("catalystRFPerSoul", 20, 0, Integer.MAX_VALUE);
    
    public static final ForgeConfigSpec.IntValue catalystCostMultiplier = COMMON
                                                                          .comment("Catalyst soul cost multiplier")
                                                                          .defineInRange("catalystCostMultiplier", 2, 0, Integer.MAX_VALUE);
    
    public static final ForgeConfigSpec.IntValue catalystHyperMultiplier = COMMON
                                                                           .comment("Additional hyper enchanting cost multiplier")
                                                                           .defineInRange("catalystHyperMultiplier", 2, 0, Integer.MAX_VALUE);
    
    public static final ForgeConfigSpec.DoubleValue catalystHyperExpFactor = COMMON
                                                                             .comment("Exponential factor for hyper enchanting cost. Increase carefully, makes higher levels MUCH more expensive.")
                                                                             .defineInRange("catalystHyperExpFactor", 1.15, 0.0, 100.0);
    
    public static final ForgeConfigSpec.IntValue spawnerCostMultiplier = COMMON
                                                                         .comment("Spawner soul cost multiplier")
                                                                         .defineInRange("spawnerCostMultiplier", 1, 0, Integer.MAX_VALUE);
    
    static {
        COMMON.pop();
    }
    
    // Particle accelerator
    static {
        COMMON.push("particleAccelerator");
    }
    
    public static final ForgeConfigSpec.IntValue maxGateDist = COMMON
                                                               .comment("Maximum particle gate distance")
                                                               .defineInRange("maxGateDist", 10, 1, Integer.MAX_VALUE);
    
    public static final ForgeConfigSpec.DoubleValue bendFactor = COMMON
                                                                 .comment("Particle curve requirement factor")
                                                                 .defineInRange("bendFactor", 2.5, 0.0, 100.0);
    
    public static final ForgeConfigSpec.IntValue accelerationRFCost = COMMON
                                                                      .comment("Base RF cost per particle acceleration step")
                                                                      .defineInRange("accelerationRFCost", 10, 0, Integer.MAX_VALUE);
    
    public static final ForgeConfigSpec.LongValue acceleratorMotorRFCapacity = COMMON
                                                                               .comment("Accelerator motor RF storage capacity")
                                                                               .defineInRange("acceleratorMotorRFCapacity", 5_000_000L, 0L, Long.MAX_VALUE);
    
    public static final ForgeConfigSpec.IntValue endPortalRequiredSpeed = COMMON
                                                                          .comment("Minimum collision energy to activate an end portal")
                                                                          .defineInRange("endPortalRequiredSpeed", 10_000, 0, Integer.MAX_VALUE);
    
    public static final ForgeConfigSpec.IntValue netherPortalRequiredSpeed = COMMON
                                                                             .comment("Minimum collision energy to activate a nether portal")
                                                                             .defineInRange("netherPortalRequiredSpeed", 5_000, 0, Integer.MAX_VALUE);
    
    public static final ForgeConfigSpec.IntValue blackHoleRequiredSpeed = COMMON
                                                                          .comment("Minimum collision energy to create a black hole")
                                                                          .defineInRange("blackHoleRequiredSpeed", 15_000, 0, Integer.MAX_VALUE);
    
    public static final ForgeConfigSpec.IntValue collectorEnergyStorage = COMMON
                                                                          .comment("Tachyon collector energy capacity in RF")
                                                                          .defineInRange("collectorEnergyStorage", 1_000_000, 0, Integer.MAX_VALUE);
    
    public static final ForgeConfigSpec.DoubleValue tachyonCollisionEnergyFactor = COMMON
                                                                                   .comment("Particle collision tachyon RF multiplier")
                                                                                   .defineInRange("tachyonCollisionEnergyFactor", 1.0, 0.0, 100.0);
    
    static {
        COMMON.pop();
    }
    
    // Black hole
    static {
        COMMON.push("blackHole");
    }
    
    public static final ForgeConfigSpec.IntValue pullTimeMultiplier = COMMON
                                                                      .comment("Black hole pull time multiplier")
                                                                      .defineInRange("pullTimeMultiplier", 8, 0, Integer.MAX_VALUE);
    
    public static final ForgeConfigSpec.IntValue pullRange = COMMON
                                                             .comment("Black hole pull range in blocks")
                                                             .defineInRange("pullRange", 16, 0, Integer.MAX_VALUE);
    
    public static final ForgeConfigSpec.IntValue idleWaitTicks = COMMON
                                                                 .comment("Black hole idle wait ticks between operations")
                                                                 .defineInRange("idleWaitTicks", 200, 0, Integer.MAX_VALUE);
    
    public static final ForgeConfigSpec.IntValue blackHoleTachyonEnergy = COMMON
                                                                          .comment("Energy of tachyons produced by black holes")
                                                                          .defineInRange("blackHoleTachyonEnergy", 50_000, 0, Integer.MAX_VALUE);
    
    public static final ForgeConfigSpec.LongValue unstableContainerBaseCapacity = COMMON
                                                                                  .comment("Unstable container base RF storage capacity")
                                                                                  .worldRestart()
                                                                                  .defineInRange("unstableContainerBaseCapacity", 20_000_000L, 0L, Long.MAX_VALUE);
    
    static {
        COMMON.pop();
    }
    
    // Augments
    static {
        COMMON.push("augments");
    }
    
    public static final ForgeConfigSpec.LongValue augmenterMaxEnergy = COMMON
                                                                       .comment("Cybernetical station maximum energy storage")
                                                                       .worldRestart()
                                                                       .defineInRange("augmenterMaxEnergy", 500_000_000L, 0L, Long.MAX_VALUE);
    
    static {
        COMMON.pop();
    }
    
    // block / sound interactions
    static {
        COMMON.push("interaction");
    }
    
    public static final ForgeConfigSpec.DoubleValue machineVolumeMultiplier = COMMON
                                                                              .comment("Machine sound volume multiplier")
                                                                              .defineInRange("machineVolumeMultiplier", 1.0, 0.0, 10.0);
    
    static {
        COMMON.pop();
    }
    
    public static final ForgeConfigSpec COMMON_SPEC = COMMON.build();
    
    // Config section classes
    
    public static class ProcessingMachines {
        public final ForgeConfigSpec.IntValue machineFrameMaxLength;
        public final BasicMachineConfig assemblerData;
        public final BasicMachineConfig atomicForgeData;
        public final CentrifugeConfig centrifugeData;
        public final BasicMachineConfig foundryData;
        public final BasicMachineConfig coolerData;
        public final BasicMachineConfig fragmentForgeData;
        public final FurnaceConfig furnaceData;
        public final BasicMachineConfig pulverizerData;
        public final BasicMachineConfig refineryData;
        
        ProcessingMachines(ForgeConfigSpec.Builder b) {
            b.push("processingMachines");
            machineFrameMaxLength = b.comment("Maximum machine frame chain length").worldRestart().defineInRange("machineFrameMaxLength", 64, 1, Integer.MAX_VALUE);
            assemblerData = new BasicMachineConfig(b, "assemblerData", 50_000, 128 * 8, 0, 128);
            atomicForgeData = new BasicMachineConfig(b, "atomicForgeData", 1024, 0, 0, 1024);
            centrifugeData = new CentrifugeConfig(b);
            foundryData = new BasicMachineConfig(b, "foundryData", 50_000, 128 * 8, 0, 128);
            coolerData = new BasicMachineConfig(b, "coolerData", 50_000, 32 * 8, 0, 32);
            fragmentForgeData = new BasicMachineConfig(b, "fragmentForgeData", 50_000, 256 * 8, 0, 256);
            furnaceData = new FurnaceConfig(b);
            pulverizerData = new BasicMachineConfig(b, "pulverizerData", 25_000, 32 * 8, 0, 32);
            refineryData = new BasicMachineConfig(b, "refineryData", 50_000, 64 * 8, 0, 64);
            b.pop();
        }
    }
    
    public static class Generators {
        public final ForgeConfigSpec.DoubleValue animationSpeedMultiplier;
        public final ForgeConfigSpec.ConfigValue<String> steamId;
        public final BasicMachineConfig basicGeneratorData;
        public final BasicMachineConfig bioGeneratorData;
        public final BasicMachineConfig lavaGeneratorData;
        public final BasicMachineConfig fuelGeneratorData;
        public final SteamEngineConfig steamEngineData;
        public final BasicMachineConfig solarGeneratorData;
        
        Generators(ForgeConfigSpec.Builder b) {
            b.push("generators");
            animationSpeedMultiplier = b.comment("Generator animation speed multiplier").defineInRange("animationSpeedMultiplier", 10.0, 0.0, 1000.0);
            steamId = b.comment("Fluid ID for steam produced by the steam boiler. Only this is accepted by steam engines.").define("steamId", "oritech:still_steam");
            basicGeneratorData = new BasicMachineConfig(b, "basicGeneratorData", 50_000, 0, 32 * 8, 32);
            bioGeneratorData = new BasicMachineConfig(b, "bioGeneratorData", 100_000, 0, 64 * 8, 64);
            lavaGeneratorData = new BasicMachineConfig(b, "lavaGeneratorData", 100_000, 0, 64 * 8, 64);
            fuelGeneratorData = new BasicMachineConfig(b, "fuelGeneratorData", 250_000, 0, 256 * 8, 256);
            steamEngineData = new SteamEngineConfig(b);
            solarGeneratorData = new BasicMachineConfig(b, "solarGeneratorData", 100_000, 0, 32 * 8, 32);
            b.pop();
        }
    }
    
    public static class LaserArmConfig {
        public final ForgeConfigSpec.LongValue energyCapacity;
        public final ForgeConfigSpec.LongValue maxEnergyInsertion;
        public final ForgeConfigSpec.LongValue energyPerTick;
        public final ForgeConfigSpec.IntValue blockBreakEnergyBase;
        public final ForgeConfigSpec.DoubleValue damageTickBase;
        public final ForgeConfigSpec.IntValue range;
        
        LaserArmConfig(ForgeConfigSpec.Builder b) {
            b.push("laserArmConfig");
            energyCapacity = b.defineInRange("energyCapacity", 20_000L, 0L, Long.MAX_VALUE);
            maxEnergyInsertion = b.defineInRange("maxEnergyInsertion", 128L * 8, 0L, Long.MAX_VALUE);
            energyPerTick = b.defineInRange("energyPerTick", 128L, 0L, Long.MAX_VALUE);
            blockBreakEnergyBase = b.comment("Base RF cost to break a block").defineInRange("blockBreakEnergyBase", 1024, 0, Integer.MAX_VALUE);
            damageTickBase = b.comment("Base damage per tick to entities").defineInRange("damageTickBase", 2.0, 0.0, 1000.0);
            range = b.comment("Maximum targeting range in blocks").defineInRange("range", 128, 1, Integer.MAX_VALUE);
            b.pop();
        }
    }
    
    public static class DeepDrillConfig {
        public final ForgeConfigSpec.LongValue energyCapacity;
        public final ForgeConfigSpec.IntValue stepsPerOre;
        public final ForgeConfigSpec.IntValue energyPerStep;
        
        DeepDrillConfig(ForgeConfigSpec.Builder b) {
            b.push("deepDrillConfig");
            energyCapacity = b.defineInRange("energyCapacity", 20_000L, 0L, Long.MAX_VALUE);
            stepsPerOre = b.comment("Work steps required per ore output").defineInRange("stepsPerOre", 20, 1, Integer.MAX_VALUE);
            energyPerStep = b.comment("RF consumed per work step").defineInRange("energyPerStep", 1024, 0, Integer.MAX_VALUE);
            b.pop();
        }
    }
    
    public static class AddonConfig {
        public final ForgeConfigSpec.DoubleValue burstAddonSpeedMultiplier;
        public final ForgeConfigSpec.DoubleValue burstAddonThrottleMultiplier;
        public final ForgeConfigSpec.LongValue addonShrinkerRF;
        
        AddonConfig(ForgeConfigSpec.Builder b) {
            b.push("addonConfig");
            burstAddonSpeedMultiplier = b.comment("Burst addon processing speed multiplier").defineInRange("burstAddonSpeedMultiplier", 8.0, 0.0, 1000.0);
            burstAddonThrottleMultiplier = b.comment("Burst addon throttle energy multiplier").defineInRange("burstAddonThrottleMultiplier", 1.2, 0.0, 100.0);
            addonShrinkerRF = b.comment("Addon splicer RF storage capacity").defineInRange("addonShrinkerRF", 50_000_000L, 0L, Long.MAX_VALUE);
            b.pop();
        }
    }
    
    public static class PowerPoleConfig {
        public final ForgeConfigSpec.LongValue energyCapacity;
        public final ForgeConfigSpec.IntValue minRange;
        public final ForgeConfigSpec.IntValue maxRange;
        
        PowerPoleConfig(ForgeConfigSpec.Builder b) {
            b.push("poleConfig");
            energyCapacity = b.comment("Energy transmission rate and capacity in RF/t").defineInRange("energyCapacity", 1_000_000L, 0L, Long.MAX_VALUE);
            minRange = b.comment("Minimum separation distance between poles").defineInRange("minRange", 50, 0, Integer.MAX_VALUE);
            maxRange = b.comment("Maximum separation distance between poles").defineInRange("maxRange", 1000, 0, Integer.MAX_VALUE);
            b.pop();
        }
    }
    
    public static class FertilizerConfig {
        public final ForgeConfigSpec.IntValue moveDuration;
        public final ForgeConfigSpec.IntValue workDuration;
        public final ForgeConfigSpec.IntValue moveEnergyUsage;
        public final ForgeConfigSpec.IntValue workEnergyUsage;
        public final ForgeConfigSpec.DoubleValue liquidPerBlockUsage;
        
        FertilizerConfig(ForgeConfigSpec.Builder b) {
            b.push("fertilizerConfig");
            moveDuration = b.defineInRange("moveDuration", 10, 1, Integer.MAX_VALUE);
            workDuration = b.defineInRange("workDuration", 20, 1, Integer.MAX_VALUE);
            moveEnergyUsage = b.defineInRange("moveEnergyUsage", 8, 0, Integer.MAX_VALUE);
            workEnergyUsage = b.defineInRange("workEnergyUsage", 128, 0, Integer.MAX_VALUE);
            liquidPerBlockUsage = b.comment("Liquid consumed per fertilized block in buckets").worldRestart().defineInRange("liquidPerBlockUsage", 0.25, 0.0, 100.0);
            b.pop();
        }
    }
    
    public static class CentrifugeConfig {
        public final ForgeConfigSpec.LongValue energyCapacity;
        public final ForgeConfigSpec.LongValue maxEnergyInsertion;
        public final ForgeConfigSpec.IntValue energyPerTick;
        public final ForgeConfigSpec.LongValue tankSizeInBuckets;
        
        CentrifugeConfig(ForgeConfigSpec.Builder b) {
            b.push("centrifugeData");
            energyCapacity = b.defineInRange("energyCapacity", 10_000L, 0L, Long.MAX_VALUE);
            maxEnergyInsertion = b.defineInRange("maxEnergyInsertion", 64L * 8, 0L, Long.MAX_VALUE);
            energyPerTick = b.defineInRange("energyPerTick", 64, 0, Integer.MAX_VALUE);
            tankSizeInBuckets = b.comment("Fluid tank size in buckets").defineInRange("tankSizeInBuckets", 8L, 0L, Long.MAX_VALUE);
            b.pop();
        }
    }
    
    public static class FurnaceConfig {
        public final ForgeConfigSpec.LongValue energyCapacity;
        public final ForgeConfigSpec.LongValue maxEnergyInsertion;
        public final ForgeConfigSpec.IntValue energyPerTick;
        public final ForgeConfigSpec.DoubleValue speedMultiplier;
        
        FurnaceConfig(ForgeConfigSpec.Builder b) {
            b.push("furnaceData");
            energyCapacity = b.defineInRange("energyCapacity", 10_000L, 0L, Long.MAX_VALUE);
            maxEnergyInsertion = b.defineInRange("maxEnergyInsertion", 32L * 8, 0L, Long.MAX_VALUE);
            energyPerTick = b.defineInRange("energyPerTick", 32, 0, Integer.MAX_VALUE);
            speedMultiplier = b.comment("Furnace speed multiplier relative to vanilla").defineInRange("speedMultiplier", 0.5, 0.0, 100.0);
            b.pop();
        }
    }
    
    public static class SteamEngineConfig {
        public final ForgeConfigSpec.LongValue energyCapacity;
        public final ForgeConfigSpec.LongValue maxEnergyExtraction;
        public final ForgeConfigSpec.DoubleValue rfToSteamRatio;
        public final ForgeConfigSpec.IntValue steamToRfRatio;
        public final ForgeConfigSpec.BooleanValue stopOnEnergyFull;
        public final ForgeConfigSpec.BooleanValue stopOnWaterFull;
        public final ForgeConfigSpec.DoubleValue steamBoilerCapacityBuckets;
        
        SteamEngineConfig(ForgeConfigSpec.Builder b) {
            b.push("steamEngineData");
            energyCapacity = b.defineInRange("energyCapacity", 100_000L, 0L, Long.MAX_VALUE);
            maxEnergyExtraction = b.defineInRange("maxEnergyExtraction", 50_000L, 0L, Long.MAX_VALUE);
            rfToSteamRatio = b.comment("Applies to generators with the steam addon. Droplets of steam produced per the usual RF.").defineInRange("rfToSteamRatio", 2.0, 0.0, 1000.0);
            steamToRfRatio = b.comment("Energy per steam unit in the steam engine").defineInRange("steamToRfRatio", 1, 0, Integer.MAX_VALUE);
            stopOnEnergyFull = b.comment("When enabled, the steam engine stops when energy storage is full").define("stopOnEnergyFull", false);
            stopOnWaterFull = b.comment("When enabled, the steam engine stops when the water tank is full. Must be pumped out to resume.").define("stopOnWaterFull", true);
            steamBoilerCapacityBuckets = b.comment("Steam capacity of steam-boiler generators and the steam engine, in buckets").defineInRange("steamBoilerCapacityBuckets", 8.0, 0.0, 1000.0);
            b.pop();
        }
    }
    
    // Reusable config types
    
    public static class BasicMachineConfig {
        public final ForgeConfigSpec.LongValue energyCapacity;
        public final ForgeConfigSpec.LongValue maxEnergyInsertion;
        public final ForgeConfigSpec.LongValue maxEnergyExtraction;
        public final ForgeConfigSpec.IntValue energyPerTick;
        
        BasicMachineConfig(ForgeConfigSpec.Builder b, String name, long defCap, long defIns, long defExt, int defTick) {
            b.push(name);
            energyCapacity = b.defineInRange("energyCapacity", defCap, 0L, Long.MAX_VALUE);
            maxEnergyInsertion = b.defineInRange("maxEnergyInsertion", defIns, 0L, Long.MAX_VALUE);
            maxEnergyExtraction = b.defineInRange("maxEnergyExtraction", defExt, 0L, Long.MAX_VALUE);
            energyPerTick = b.defineInRange("energyPerTick", defTick, 0, Integer.MAX_VALUE);
            b.pop();
        }
    }
    
    public static class MachineFrameConfig {
        public final ForgeConfigSpec.IntValue moveDuration;
        public final ForgeConfigSpec.IntValue workDuration;
        public final ForgeConfigSpec.IntValue moveEnergyUsage;
        public final ForgeConfigSpec.IntValue workEnergyUsage;
        
        MachineFrameConfig(ForgeConfigSpec.Builder b, String name, int defMove, int defWork, int defMoveEnergy, int defWorkEnergy) {
            b.push(name);
            moveDuration = b.defineInRange("moveDuration", defMove, 1, Integer.MAX_VALUE);
            workDuration = b.defineInRange("workDuration", defWork, 1, Integer.MAX_VALUE);
            moveEnergyUsage = b.defineInRange("moveEnergyUsage", defMoveEnergy, 0, Integer.MAX_VALUE);
            workEnergyUsage = b.defineInRange("workEnergyUsage", defWorkEnergy, 0, Integer.MAX_VALUE);
            b.pop();
        }
    }
    
}
