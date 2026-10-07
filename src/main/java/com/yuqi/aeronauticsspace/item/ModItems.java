package com.yuqi.aeronauticsspace.item;

import com.yuqi.aeronauticsspace.AeronauticsSpace;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 模组物品注册。 */
public final class ModItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(AeronauticsSpace.MODID);

    /** 星际传送器：右键打开星球选择界面 */
    public static final DeferredItem<Item> PLANET_TELEPORTER =
            ITEMS.register("planet_teleporter",
                    () -> new PlanetTeleporterItem(new Item.Properties().stacksTo(1)));

    private ModItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
