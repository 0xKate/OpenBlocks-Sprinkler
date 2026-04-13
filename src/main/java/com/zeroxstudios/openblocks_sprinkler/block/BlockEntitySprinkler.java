package com.zeroxstudios.openblocks_sprinkler.block;

import com.zeroxstudios.openblocks_sprinkler.fx.FXLiquidSprayData;
import com.zeroxstudios.openblocks_sprinkler.network.ModNetwork;
import com.zeroxstudios.openblocks_sprinkler.network.PacketSprayParticle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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

import static com.zeroxstudios.openblocks_sprinkler.block.BlockSprinkler.ENABLED;

public class BlockEntitySprinkler extends BlockEntity implements MenuProvider {

    // ---- Config constants (mirror original OpenBlocks Config) ----
    public static final int TANK_CAPACITY      = 1000;   // mB
    public static final int WATER_CONSUME_RATE = 20;   // ticks per mB
    public static final int WATER_CONSUME_AMOUNT = 100;
    public static final int BONEMEAL_RATE      = 200;  // ticks per item consumed
    public static final int BONEMEAL_ATTEMPTS = 4;
    public static final int EFFECTIVE_RANGE    = 4;    // blocks in each cardinal direction
    public static final float ANGLE_RICHNESS     = 0.01f;
    private static final double SPRAY_SIDE_SCATTER = Math.toRadians(25);
    private static final float SPRAY_Y_VELOCITY = 0.40f;

    // ---- Inventory: 9 slots for bone meal ----
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

    // ---- Fluid tank: accepts water only ----
    private final FluidTank tank = new FluidTank(TANK_CAPACITY) {
        @Override
        public boolean isFluidValid(FluidStack stack) {
            return !stack.isEmpty();
        }
        @Override
        protected void onContentsChanged() {
            setChanged();
            syncEnabled();
        }
    };
    private final LazyOptional<IFluidHandler> tankCap = LazyOptional.of(() -> tank);

    // ---- State ----
    private int ticks = 0;

    public BlockEntitySprinkler(BlockPos pos, BlockState state) {
        super(ModBlocks.SPRINKLER_BE.get(), pos, state);
    }

    // ---- Tick (server-side only) ----
    public static void serverTick(Level level,
                                  BlockPos pos, BlockState state,
                                  BlockEntitySprinkler be) {
        be.tickServer((ServerLevel) level, pos, state);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, BlockEntitySprinkler be) {
        be.tickClient((ClientLevel)level, pos ,state);
    }

    private void tickClient(ClientLevel level, BlockPos pos, BlockState state) {
        if (state.getValue(ENABLED)) {
            spawnSprayParticles(level, pos);
            ticks++;
        }
    }

    private void tickServer(ServerLevel level, BlockPos pos, BlockState state) {
        tryFillFromBelow(level, pos);

        if (ticks % WATER_CONSUME_RATE == 0) {
            boolean wasEnabled = isEnabled();
            boolean processing = false;

            if (tank.getFluidAmount() > 0) {
                var drained = tank.drain(WATER_CONSUME_AMOUNT, IFluidHandler.FluidAction.EXECUTE);
                if (drained.getAmount() >= WATER_CONSUME_AMOUNT) {
                    processing = true;
                    hydrateFarmland(level, pos);
                }
            }

            if (!processing) {
                HydrationManager.clearAreaForSprinkler(level, worldPosition);
            }

            if (wasEnabled != processing) {
                level.setBlock(pos, state.setValue(ENABLED, processing), Block.UPDATE_CLIENTS);
            }
        }

        if (isEnabled()) {
            spawnSprayParticles(level, pos);

            if (ticks % BONEMEAL_RATE == 0 && consumeBonemeal()) {
                for (int i = 0; i < BONEMEAL_ATTEMPTS; i++) {
                    attemptFertilize(level, pos);
                }
            }
        }

        ticks++;
    }

    private void tryFillFromBelow(Level level, BlockPos pos) {
        if (tank.getFluidAmount() >= TANK_CAPACITY) return;
        BlockPos below = pos.below();
        var belowBE = level.getBlockEntity(below);
        if (belowBE == null) return;
        belowBE.getCapability(ForgeCapabilities.FLUID_HANDLER).ifPresent(handler -> {
            FluidStack drained = handler.drain(new FluidStack(Fluids.WATER, TANK_CAPACITY - tank.getFluidAmount()),
                    IFluidHandler.FluidAction.SIMULATE);
            if (!drained.isEmpty() && drained.getFluid() == Fluids.WATER) {
                FluidStack actual = handler.drain(drained, IFluidHandler.FluidAction.EXECUTE);
                tank.fill(actual, IFluidHandler.FluidAction.EXECUTE);
            }
        });
    }


    private void hydrateFarmland(ServerLevel level, BlockPos pos) {
        for (int dx = -EFFECTIVE_RANGE; dx <= EFFECTIVE_RANGE; dx++) {
            for (int dz = -EFFECTIVE_RANGE; dz <= EFFECTIVE_RANGE; dz++) {
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

    private boolean consumeBonemeal() {
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.is(Items.BONE_MEAL)) {
                inventory.extractItem(i, 1, false);
                return true;
            }
        }
        return false;
    }

    private void attemptFertilize(ServerLevel level, BlockPos pos) {
        int ox = level.random.nextInt(2 * EFFECTIVE_RANGE + 1) - EFFECTIVE_RANGE;
        int oz = level.random.nextInt(2 * EFFECTIVE_RANGE + 1) - EFFECTIVE_RANGE;

        BlockPos target = pos.offset(ox, 0, oz);
        BlockState targetState = level.getBlockState(target);
        Block block = targetState.getBlock();

        if (block instanceof BonemealableBlock bonemealable
                && bonemealable.isValidBonemealTarget(level, target, targetState, false)) {

            // only fertilize if we have bonemeal to spend
            bonemealable.performBonemeal(level, level.random, target, targetState);

            double x = target.getX() + 0.5;
            double y = target.getY() + 0.55;
            double z = target.getZ() + 0.5;

            level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    x, y, z, 8, 0.3, 0.35, 0.3, 0.02);

            level.sendParticles(ParticleTypes.COMPOSTER,
                    x, y, z, 4, 0.2, 0.2, 0.2, 0.03);
        }
    }


    private void spawnSprayParticles(Level level, BlockPos pos) {
        Direction facing = getBlockState().getValue(BlockSprinkler.FACING);

        final double nozzleAngle = getSprayDirection();
        final double sprayForwardVelocity = Math.sin(Math.toRadians(nozzleAngle * 25));

        final int offsetX = facing.getStepX();
        final int offsetZ = facing.getStepZ();

        final double forwardVelocityX = sprayForwardVelocity * offsetZ / -2.0;
        final double forwardVelocityZ = sprayForwardVelocity * offsetX / 2.0;

        double outletPosition = -0.5;
        while (outletPosition <= 0.5) {
            final double spraySideVelocity = Math.sin(SPRAY_SIDE_SCATTER * (level.random.nextDouble() - 0.5));

            double velX = forwardVelocityX + spraySideVelocity * offsetX;
            double velZ = forwardVelocityZ + spraySideVelocity * offsetZ;

            var x = pos.getX() + 0.5 + (outletPosition * 0.6 * offsetX);
            var y = pos.getY() + 0.10;
            var z = pos.getZ() + 0.5 + (outletPosition * 0.6 * offsetZ);

            if (level instanceof ClientLevel client) {
                var engine = Minecraft.getInstance().particleEngine;
                engine.createParticle(
                        new FXLiquidSprayData("minecraft:water", 0.2f, 0.8f),
                        x, y, z,
                        velX, SPRAY_Y_VELOCITY, velZ
                );
            }


//            ModNetwork.sendToNearby(
//                    new PacketSprayParticle(
//                            pos.getX() + 0.5 + (outletPosition * 0.6 * offsetX),
//                            pos.getY() + 0.10,
//                            pos.getZ() + 0.5 + (outletPosition * 0.6 * offsetZ),
//                            velX, SPRAY_Y_VELOCITY, velZ
//                    ),
//                    level, pos
//            );

            outletPosition += 0.2;
        }
    }

    private boolean isEnabled() {
        return getBlockState().getValue(ENABLED);
    }

    private void syncEnabled() {
        // Tank changed; will be picked up on next WATER_CONSUME_RATE tick
    }

    // ---- MenuProvider ----
    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("block.openblocks_sprinkler.sprinkler");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, @NotNull Inventory inv, @NotNull Player player) {
        return new ContainerSprinkler(windowId, inv, this);
    }

    // ---- Capabilities ----
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

    // ---- NBT ----
    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inventory", inventory.serializeNBT());
        tag.put("tank", tank.writeToNBT(new CompoundTag()));
        tag.putInt("ticks", ticks);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        inventory.deserializeNBT(tag.getCompound("inventory"));
        tank.readFromNBT(tag.getCompound("tank"));
        ticks = tag.getInt("ticks");
    }

    // ---- Getters for GUI / renderer ----
    public ItemStackHandler getInventory() { return inventory; }
    public FluidTank getTank() { return tank; }
    public int getTicks() { return ticks; }

    /** Spray direction: sine wave -1..1, used by client particle system */
    public float getSprayDirection() {
        return isEnabled() ? (float) Math.sin(ticks * ANGLE_RICHNESS) : 0f;
    }
}
