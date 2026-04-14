package com.zeroxstudios.openblocks_sprinkler.block;

import com.zeroxstudios.openblocks_sprinkler.Config;
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

public class BlockEntitySprinkler extends BlockEntity implements MenuProvider {

    // State
    private int serverTicks = 0;

    // Constructor
    public BlockEntitySprinkler(BlockPos pos, BlockState state) {
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
    public FluidTank getTank() { return tank; }

    // Server Ticks
    public static void serverTick(Level level, BlockPos pos, BlockState state, BlockEntitySprinkler be) {
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

                if (!processing) HydrationManager.get().clearAreaForSprinkler(level, worldPosition);
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
                    if (state.getValue(FarmBlock.MOISTURE) < 7) {
                        HydrationManager.get().setWatered(level, target, true);
                        level.setBlock(target, state.setValue(FarmBlock.MOISTURE, 7), Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
    }

    public void onBroken() {
        if (level instanceof ServerLevel serverLevel) {
            HydrationManager.get().clearAreaForSprinkler(serverLevel, worldPosition);
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
