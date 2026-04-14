package com.zeroxstudios.openblocks_sprinkler.block;

import com.zeroxstudios.openblocks_sprinkler.Config;
import com.zeroxstudios.openblocks_sprinkler.fx.FXLiquidSprayData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TileEntitySprinkler extends BlockEntity implements MenuProvider {

    // State
    private int clientTicks = 0;
    private int serverTicks = 0;
    private boolean playerInRange = false;
    private int waterColor = 0x3F76E4;

    // Constructor
    public TileEntitySprinkler(BlockPos pos, BlockState state) {
        super(ModBlocks.SPRINKLER_BE.get(), pos, state);
    }

    // Inventory
    private final ItemStackHandler inventory = new ItemStackHandler(9) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return stack.is(Items.BONE_MEAL);
        }
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final LazyOptional<IItemHandler> inventoryCap = LazyOptional.of(() -> inventory);

    // Tank
    private final FluidTank tank = new FluidTank(1000) {
        @Override
        public boolean isFluidValid(FluidStack stack) {
            return !stack.isEmpty();
        }
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    @Override
    public void onLoad() {
        super.onLoad();
        tank.setCapacity(Config.TANK_CAPACITY.get());
        if (tank.getFluidAmount() > tank.getCapacity()) {
            tank.drain(tank.getFluidAmount() - tank.getCapacity(), IFluidHandler.FluidAction.EXECUTE);
        }
    }
    private final LazyOptional<IFluidHandler> tankCap = LazyOptional.of(() -> tank);

    // Forge Capabilities
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return inventoryCap.cast();
        if (cap == ForgeCapabilities.FLUID_HANDLER) return tankCap.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        inventoryCap.invalidate();
        tankCap.invalidate();
    }

    // NBT
    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inventory", inventory.serializeNBT());
        tag.put("tank", tank.writeToNBT(new CompoundTag()));
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        inventory.deserializeNBT(tag.getCompound("inventory"));
        tank.readFromNBT(tag.getCompound("tank"));
    }

    // Sync
    @Override
    public @NotNull CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // MenuProvider
    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("block.openblocks_sprinkler.sprinkler");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, @NotNull Inventory inv, @NotNull Player player) {
        return new ContainerSprinkler(windowId, inv, this);
    }

    // Getters
    public ItemStackHandler getInventory() { return inventory; }

    // Spray angle: sine wave -1..1, used by client particle system
    public float getSprayAngle(BlockState state, long gameTime) {
        return state.getValue(BlockSprinkler.ENABLED) ? (float) Math.sin(gameTime * Config.ANGLE_RICHNESS.get()) : 0f;
    }

    // Client Ticks
    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntitySprinkler be) {
        be.tickClient((ClientLevel)level, pos, state);
    }

    private void tickClient(ClientLevel level, BlockPos pos, BlockState state) {
        if (!state.getValue(BlockSprinkler.ENABLED)) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (clientTicks % 20 == 0) {
            if (player == null) {
                playerInRange = false;
            } else {
                double dx = player.getX() - (pos.getX() + 0.5);
                double dy = player.getY() - (pos.getY() + 0.5);
                double dz = player.getZ() - (pos.getZ() + 0.5);
                double maxDistSq = Config.VIEW_DISTANCE.get() * (double) Config.VIEW_DISTANCE.get();
                playerInRange = (dx*dx + dy*dy + dz*dz) <= maxDistSq;
            }

            var stack = tank.getFluid();
            if (!stack.isEmpty())
                waterColor = IClientFluidTypeExtensions.of(stack.getFluid()).getTintColor(stack);
        }

        if (playerInRange)
            if (clientTicks % Config.PARTICLE_TICK_DELAY.get() == 0)
                spawnSprayParticles(level, pos, state, mc);

        clientTicks++;
    }

    // Client Methods
    private void spawnSprayParticles(ClientLevel level, BlockPos pos, BlockState state, Minecraft mc) {
        Direction facing = state.getValue(BlockSprinkler.FACING);

        final double nozzleAngle = getSprayAngle(state, level.getGameTime());

        double rangeScale = Math.sqrt((double) Config.EFFECTIVE_RANGE.get() / Config.EFFECTIVE_RANGE.getDefault());

        double angleGrowth = 25.0;
        double velocityGrowth = 0.6;

        double sweepAngle = 25.0 + (angleGrowth * (rangeScale - 1.0)); // 25° at default, 39° at 2x range
        double velocityScale = Math.pow(rangeScale, velocityGrowth); // gentler velocity growth

        final double sprayForwardVelocity = Math.sin(Math.toRadians(nozzleAngle * sweepAngle)) * velocityScale;
        final double sprayYVelocity = 0.40f * velocityScale;
        double spraySideScatter = Math.toRadians(sweepAngle);

        final int offsetX = facing.getStepX();
        final int offsetZ = facing.getStepZ();

        final double forwardVelocityX = sprayForwardVelocity * offsetZ / -2.0;
        final double forwardVelocityZ = sprayForwardVelocity * offsetX / 2.0;

        double outletPosition = -0.5;
        while (outletPosition <= 0.5) {
            final double spraySideVelocity = Math.sin(spraySideScatter * (level.random.nextDouble() - 0.5));

            double velX = forwardVelocityX + spraySideVelocity * offsetX;
            double velZ = forwardVelocityZ + spraySideVelocity * offsetZ;

            var x = pos.getX() + 0.5 + (outletPosition * 0.6 * offsetX);
            var y = pos.getY() + 0.10;
            var z = pos.getZ() + 0.5 + (outletPosition * 0.6 * offsetZ);

            var engine = mc.particleEngine;
            engine.createParticle(
                    new FXLiquidSprayData(waterColor, 0.2f, 0.8f),
                    x, y, z,
                    velX, sprayYVelocity, velZ
            );

            outletPosition += 0.2;
        }
    }

    // Server Ticks
    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntitySprinkler be) {
        be.tickServer((ServerLevel) level, pos, state);
    }

    private void tickServer(ServerLevel level, BlockPos pos, BlockState state) {
        boolean processing;

        if (Config.INFINITE_WATER.get()) {
            processing = true;
            if (serverTicks % Config.WATER_CONSUME_RATE.get() == 0) {
                hydrateFarmland(level, pos);
            }
        } else {
            tryFillFromBelow(level, pos);
            if (serverTicks % Config.WATER_CONSUME_RATE.get() == 0) {
                boolean wasEnabled = state.getValue(BlockSprinkler.ENABLED);
                processing = false;

                if (tank.getFluidAmount() > 0) {
                    var drained = tank.drain(Config.WATER_CONSUME_AMOUNT.get(), IFluidHandler.FluidAction.EXECUTE);
                    if (drained.getAmount() >= Config.WATER_CONSUME_AMOUNT.get()) {
                        processing = true;
                        hydrateFarmland(level, pos);
                    }
                }

                if (!processing) HydrationManager.clearAreaForSprinkler(level, worldPosition);
                if (wasEnabled != processing) {
                    level.setBlock(pos, state.setValue(BlockSprinkler.ENABLED, processing), Block.UPDATE_CLIENTS);
                }
            } else {
                processing = state.getValue(BlockSprinkler.ENABLED);
            }
        }

        if (Config.BONEMEAL_ENABLED.get()) {
            if (processing && serverTicks % Config.BONEMEAL_RATE.get() == 0) {
                if (Config.INFINITE_BONEMEAL.get() || hasBonemeal()) {
                    boolean anyFertilized = false;
                    for (int i = 0; i < Config.BONEMEAL_ATTEMPTS.get(); i++) {
                        if (attemptFertilize(level, pos)) anyFertilized = true;
                    }
                    if (anyFertilized && !Config.INFINITE_BONEMEAL.get()
                            && level.random.nextDouble() < Config.BONEMEAL_CONSUME_CHANCE.get()) {
                        consumeBonemeal();
                    }
                }
            }
        }

        serverTicks++;
    }

    // Server Methods
    private void tryFillFromBelow(Level level, BlockPos pos) {
        if (tank.getFluidAmount() >= Config.TANK_CAPACITY.get()) return;
        BlockPos below = pos.below();
        var belowBE = level.getBlockEntity(below);
        if (belowBE == null) return;
        belowBE.getCapability(ForgeCapabilities.FLUID_HANDLER).ifPresent(handler -> {
            FluidStack drained = handler.drain(new FluidStack(Fluids.WATER, Config.TANK_CAPACITY.get() - tank.getFluidAmount()),
                    IFluidHandler.FluidAction.SIMULATE);
            if (!drained.isEmpty() && drained.getFluid() == Fluids.WATER) {
                FluidStack actual = handler.drain(drained, IFluidHandler.FluidAction.EXECUTE);
                tank.fill(actual, IFluidHandler.FluidAction.EXECUTE);
            }
        });
    }


    private void hydrateFarmland(ServerLevel level, BlockPos pos) {
        var range = Config.EFFECTIVE_RANGE.get();
        for (int dx = -range; dx <= range; dx++) {
            for (int dz = -range; dz <= range; dz++) {
                BlockPos target = pos.offset(dx, -1, dz);
                BlockState state = level.getBlockState(target);
                if (state.is(Blocks.FARMLAND)) {
                    if (!HydrationManager.isWatered(level, target)) {
                        HydrationManager.setWatered(level, target, true);
                        level.setBlock(target, state.setValue(FarmBlock.MOISTURE, 7), Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
    }

    public void onBroken() {
        if (level instanceof ServerLevel serverLevel) {
            HydrationManager.clearAreaForSprinkler(serverLevel, worldPosition);
        }
    }

    private boolean hasBonemeal() {
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.is(Items.BONE_MEAL)) return true;
        }
        return false;
    }

    private void consumeBonemeal() {
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.is(Items.BONE_MEAL)) {
                inventory.extractItem(i, 1, false);
                return;
            }
        }
    }

    private boolean attemptFertilize(ServerLevel level, BlockPos pos) {
        int ox = level.random.nextInt(2 * Config.EFFECTIVE_RANGE.get() + 1) - Config.EFFECTIVE_RANGE.get();
        int oz = level.random.nextInt(2 * Config.EFFECTIVE_RANGE.get() + 1) - Config.EFFECTIVE_RANGE.get();

        BlockPos target = pos.offset(ox, 0, oz);
        BlockState targetState = level.getBlockState(target);
        Block block = targetState.getBlock();

        if (block instanceof BonemealableBlock bonemealable
                && bonemealable.isValidBonemealTarget(level, target, targetState, false)) {

            bonemealable.performBonemeal(level, level.random, target, targetState);

            double x = target.getX() + 0.5;
            double y = target.getY() + 0.55;
            double z = target.getZ() + 0.5;

            level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    x, y, z, 8, 0.3, 0.35, 0.3, 0.02);

            level.sendParticles(ParticleTypes.COMPOSTER,
                    x, y, z, 4, 0.2, 0.2, 0.2, 0.03);

            return true;
        }

        return false;
    }
    }
