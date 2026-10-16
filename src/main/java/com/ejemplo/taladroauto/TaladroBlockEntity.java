package com.ejemplo.taladroauto;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * El combustible se guarda en la entidad del bloque para sobrevivir a reinicios.
 * Se traslada explícitamente al recrear la cabeza un bloque más adelante.
 */
public final class TaladroBlockEntity extends BlockEntity {
    public static final int MAX_FUEL_STEPS = 4096;
    private int fuelSteps;

    public TaladroBlockEntity(BlockPos pos, BlockState state) {
        super(TaladroAutoMod.TALADRO_BLOCK_ENTITY.get(), pos, state);
    }

    public int getFuelSteps() {
        return fuelSteps;
    }

    public boolean addFuel(int amount) {
        if (amount <= 0 || fuelSteps > MAX_FUEL_STEPS - amount) return false;
        fuelSteps += amount;
        setChanged();
        return true;
    }

    public void setFuelSteps(int amount) {
        fuelSteps = Math.max(0, Math.min(MAX_FUEL_STEPS, amount));
        setChanged();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        fuelSteps = Math.max(0, Math.min(MAX_FUEL_STEPS, input.getIntOr("fuel_steps", 0)));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("fuel_steps", fuelSteps);
    }
}
