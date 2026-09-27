package com.tenko.titlescrolls.registry;

import com.tenko.titlescrolls.TitleScrolls;
import com.tenko.titlescrolls.item.TitleScrollItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items REGISTER = DeferredRegister.createItems(TitleScrolls.MOD_ID);

    // One reusable item for every title. Which title a given stack grants is
    // decided entirely by the ModDataComponents.GRANTS_TITLE component on that
    // stack, set per Lootr loot table entry — never by registering a new item.
    public static final DeferredItem<TitleScrollItem> TITLE_SCROLL = REGISTER.register("title_scroll",
            () -> new TitleScrollItem(new Item.Properties().stacksTo(1)));

    private ModItems() {}
}
