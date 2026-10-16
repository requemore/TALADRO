package com.ejemplo.taladroauto;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

@Mod(TaladroAutoMod.MOD_ID)
public final class TaladroAutoMod {
    public static final String MOD_ID = "taladro_auto";

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID);

    public static final DeferredBlock<TaladroBlock> TALADRO = BLOCKS.registerBlock(
        "taladro", TaladroBlock::new, properties -> properties.strength(5.0f, 12.0f)
    );
    public static final DeferredItem<BlockItem> TALADRO_ITEM = ITEMS.registerSimpleBlockItem(TALADRO);

    public static final Supplier<BlockEntityType<TaladroBlockEntity>> TALADRO_BLOCK_ENTITY =
        BLOCK_ENTITY_TYPES.register("taladro", () -> new BlockEntityType<>(
            TaladroBlockEntity::new, false, TALADRO.get()
        ));

    public TaladroAutoMod(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITY_TYPES.register(modBus);
        modBus.addListener(this::creativeTab);
    }

    private void creativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS) {
            event.accept(TALADRO_ITEM.get());
        }
    }
}
