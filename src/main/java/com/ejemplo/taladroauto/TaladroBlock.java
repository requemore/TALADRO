package com.ejemplo.taladroauto;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Taladro minero autopropulsado. Avanza 1 bloque por segundo, excavando una
 * sección 3x3 perpendicular a FACING, centrada en la cabeza. Lleva consigo
 * dos cofres vanilla normales (54 ranuras). Un avance solo ocurre cuando
 * todos los destinos son válidos y todos los drops caben.
 */
public final class TaladroBlock extends Block implements EntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty RUNNING = BooleanProperty.create("running");
    public static final int STEP_TICKS = 20;
    public static final int COAL_STEPS = 16;
    public static final int LAVA_STEPS = 200;

    public TaladroBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
            .setValue(FACING, Direction.NORTH)
            .setValue(RUNNING, false));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TaladroBlockEntity(pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, RUNNING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    /** Carga combustible sin consumirlo si supera el límite. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        int charge = 0;
        if (stack.is(Items.COAL) || stack.is(Items.CHARCOAL)) charge = COAL_STEPS;
        if (stack.is(Items.LAVA_BUCKET)) charge = LAVA_STEPS;
        if (charge == 0) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!player.getAbilities().mayBuild) return InteractionResult.PASS;
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof TaladroBlockEntity drill) {
            if (!drill.addFuel(charge)) {
                player.displayClientMessage(Component.literal("Depósito de energía lleno; no se gastó combustible."), true);
                return InteractionResult.SUCCESS;
            }
            if (!player.getAbilities().instabuild) {
                if (stack.is(Items.LAVA_BUCKET)) {
                    player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                } else {
                    stack.shrink(1);
                }
            }
            if (state.getValue(RUNNING)) level.scheduleTick(pos, this, STEP_TICKS);
            player.displayClientMessage(Component.literal(
                "Combustible cargado: " + drill.getFuelSteps() + " pasos disponibles."), true);
        }
        return InteractionResult.SUCCESS;
    }

    /** Mano vacía: alternar encendido. Agachado: consultar combustible. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!player.getAbilities().mayBuild || !player.getMainHandItem().isEmpty()) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            if (player.isShiftKeyDown()) {
                int fuel = level.getBlockEntity(pos) instanceof TaladroBlockEntity be ? be.getFuelSteps() : 0;
                player.displayClientMessage(Component.literal("Energía para " + fuel + " avances."), true);
                return InteractionResult.SUCCESS;
            }
            boolean run = !state.getValue(RUNNING);
            level.setBlock(pos, state.setValue(RUNNING, run), Block.UPDATE_ALL);
            if (run) level.scheduleTick(pos, this, STEP_TICKS);
            player.displayClientMessage(Component.literal(run
                ? "Taladro encendido: túnel 3x3. Necesita 2 cofres detrás y combustible."
                : "Taladro apagado."), true);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(RUNNING)) return;
        if (!(level.getBlockEntity(pos) instanceof TaladroBlockEntity be)) return;
        // Si se agota el combustible, se mantiene 'encendido', pero en espera.
        // Una carga nueva vuelve a programar el avance.
        if (be.getFuelSteps() <= 0) return;

        boolean advanced = advance(level, pos, state, be.getFuelSteps());
        if (advanced) {
            level.scheduleTick(pos.relative(state.getValue(FACING)), this, STEP_TICKS);
        } else if (level.getBlockState(pos).is(this)
            && level.getBlockState(pos).getValue(RUNNING)) {
            // Reintenta cuando alguien libere un obstáculo o vacíe el cofre.
            level.scheduleTick(pos, this, STEP_TICKS);
        }
    }

    /** A está detrás de la cabeza. B está a la derecha de A mirando al frente. */
    private boolean advance(ServerLevel level, BlockPos head, BlockState drillState, int fuelBefore) {
        Direction forward = drillState.getValue(FACING);
        Direction right = forward.getClockWise();
        BlockPos oldA = head.relative(forward.getOpposite());
        BlockPos oldB = oldA.relative(right);
        BlockPos newHead = head.relative(forward);
        BlockPos newB = head.relative(right);

        if (!validPos(level, oldA) || !validPos(level, oldB) || !validPos(level, newB)) return false;

        BlockState oldStateA = level.getBlockState(oldA);
        BlockState oldStateB = level.getBlockState(oldB);
        if (!oldStateA.is(Blocks.CHEST) || !oldStateB.is(Blocks.CHEST)
            || !(level.getBlockEntity(oldA) instanceof ChestBlockEntity chestA)
            || !(level.getBlockEntity(oldB) instanceof ChestBlockEntity chestB)) return false;
        // Se exige que realmente sean dos mitades conectadas hacia la derecha.
        if (oldStateA.getValue(ChestBlock.TYPE) != ChestType.LEFT
            || oldStateB.getValue(ChestBlock.TYPE) != ChestType.RIGHT
            || oldStateA.getValue(ChestBlock.FACING) != forward
            || oldStateB.getValue(ChestBlock.FACING) != forward) return false;

        Set<BlockPos> targets = new HashSet<>();
        // El centro del 3x3 es newHead (su misma altura, una posición delante).
        for (int lateral = -1; lateral <= 1; lateral++) {
            for (int vertical = -1; vertical <= 1; vertical++) {
                targets.add(newHead.relative(right, lateral).above(vertical));
            }
        }
        // El segundo cofre ocupa a continuación el lado de la antigua cabeza.
        targets.add(newB);

        for (BlockPos target : targets) {
            if (!validPos(level, target) || !canMine(level, target, level.getBlockState(target))) return false;
        }

        List<ItemStack> prospective = new ArrayList<>(54);
        for (int i = 0; i < 27; i++) prospective.add(chestA.getItem(i).copy());
        for (int i = 0; i < 27; i++) prospective.add(chestB.getItem(i).copy());

        List<ItemStack> loot = new ArrayList<>();
        for (BlockPos target : targets) {
            collectDrops(level, target, level.getBlockState(target), loot);
        }
        for (ItemStack drop : loot) {
            if (!insertAll(prospective, drop)) return false;
        }

        // COMMIT: a partir de aquí solo se realizan los cambios ya comprobados.
        for (BlockPos target : targets) {
            if (!level.getBlockState(target).isAir()) {
                level.setBlock(target, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        // Evita que el juego arroje los objetos de los cofres al retirar los bloques.
        for (int i = 0; i < 27; i++) {
            chestA.setItem(i, ItemStack.EMPTY);
            chestB.setItem(i, ItemStack.EMPTY);
        }
        chestA.setChanged();
        chestB.setChanged();
        level.setBlock(oldA, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(oldB, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(head, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);

        // Mover primero la cabeza y copiar el combustible restante.
        level.setBlock(newHead, drillState, Block.UPDATE_ALL);
        if (level.getBlockEntity(newHead) instanceof TaladroBlockEntity movedDrill) {
            movedDrill.setFuelSteps(fuelBefore - 1);
        } else {
            level.setBlock(newHead, drillState.setValue(RUNNING, false), Block.UPDATE_ALL);
        }

        level.setBlock(head, oldStateA, Block.UPDATE_ALL);
        level.setBlock(newB, oldStateB, Block.UPDATE_ALL);
        if (!(level.getBlockEntity(head) instanceof ChestBlockEntity movedA)
            || !(level.getBlockEntity(newB) instanceof ChestBlockEntity movedB)) {
            // No perder objetos si algún otro mod impide recrear el contenedor.
            for (ItemStack stack : prospective) {
                if (!stack.isEmpty()) Block.popResource(level, head, stack.copy());
            }
            level.setBlock(newHead, drillState.setValue(RUNNING, false), Block.UPDATE_ALL);
            return true;
        }
        for (int i = 0; i < 27; i++) {
            movedA.setItem(i, prospective.get(i));
            movedB.setItem(i, prospective.get(i + 27));
        }
        movedA.setChanged();
        movedB.setChanged();
        return true;
    }

    private static boolean validPos(ServerLevel level, BlockPos pos) {
        return level.getWorldBorder().isWithinBounds(pos) && level.hasChunkAt(pos)
            && pos.getY() >= level.getMinY() && pos.getY() < level.getMaxY();
    }

    private static boolean canMine(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.isAir()) return true;
        if (!state.getFluidState().isEmpty()) return false;
        if (level.getBlockEntity(pos) != null) return false;
        if (state.getDestroySpeed(level, pos) < 0) return false;
        return !state.is(TaladroAutoMod.TALADRO.get());
    }

    private static void collectDrops(ServerLevel level, BlockPos pos, BlockState state,
                                     List<ItemStack> all) {
        if (state.isAir()) return;
        ItemStack tool = new ItemStack(Items.NETHERITE_PICKAXE);
        if (state.is(BlockTags.MINEABLE_WITH_AXE)) tool = new ItemStack(Items.NETHERITE_AXE);
        else if (state.is(BlockTags.MINEABLE_WITH_SHOVEL)) tool = new ItemStack(Items.NETHERITE_SHOVEL);
        else if (state.is(BlockTags.MINEABLE_WITH_HOE)) tool = new ItemStack(Items.NETHERITE_HOE);
        all.addAll(Block.getDrops(state, level, pos, null, null, tool));
    }

    /** Inserción simulada. Si no cabe TODO el botín, no se rompe ningún bloque. */
    private static boolean insertAll(List<ItemStack> slots, ItemStack original) {
        if (original.isEmpty()) return true;
        int left = original.getCount();
        for (ItemStack stack : slots) {
            if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, original)) {
                int add = Math.min(left, stack.getMaxStackSize() - stack.getCount());
                if (add > 0) {
                    stack.grow(add);
                    left -= add;
                    if (left == 0) return true;
                }
            }
        }
        for (int i = 0; i < slots.size(); i++) {
            if (slots.get(i).isEmpty()) {
                int add = Math.min(left, original.getMaxStackSize());
                slots.set(i, original.copyWithCount(add));
                left -= add;
                if (left == 0) return true;
            }
        }
        return false;
    }
}
