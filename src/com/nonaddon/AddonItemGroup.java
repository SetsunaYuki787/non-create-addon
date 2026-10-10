package com.nonaddon;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;

/**
 * The addon's own creative inventory tab.
 *
 * <p>Fabric's item group API is the supported way to put a modded item into a tab: vanilla builds
 * each tab's contents long after mod init, so an item that is never added to any group is missing
 * from the creative inventory <em>and</em> from creative search, which only walks the groups. The
 * andesite plug is therefore collected here instead of being registered bare.
 *
 * <p>The tab holds items only: {@code non-create-addon:redstone_pulse} is a transient technical
 * block that removes itself after a couple of ticks, so it is deliberately not offered.
 */
final class AddonItemGroup {
    private static final RegistryKey<ItemGroup> KEY =
            RegistryKey.of(RegistryKeys.ITEM_GROUP, NoNAddon.id("main"));

    private AddonItemGroup() {
    }

    static void register() {
        Registry.register(
                Registries.ITEM_GROUP,
                KEY,
                FabricItemGroup.builder()
                        .displayName(Text.translatable("itemGroup.non-create-addon.main"))
                        .icon(() -> new ItemStack(AndesitePlug.ITEM))
                        .entries((context, entries) -> entries.add(AndesitePlug.ITEM))
                        .build());
    }
}
