package com.pocketring;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(PocketRing.MOD_ID)
public class PocketRing {
    public static final String MOD_ID = "pocketring";

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);

    public static final DeferredItem<Item> POCKET_RING = ITEMS.register("pocket_ring",
            () -> new PocketRingItem(new Item.Properties()
                    .stacksTo(1)
                    .rarity(Rarity.EPIC)
                    .fireResistant()));

    public PocketRing(IEventBus modBus) {
        ITEMS.register(modBus);
        modBus.addListener(this::addToCreativeTab);
    }

    private void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(POCKET_RING);
        }
    }
}
