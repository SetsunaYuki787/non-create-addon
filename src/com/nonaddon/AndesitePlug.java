package com.nonaddon;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;

/**
 * Feature 5, the item half: the andesite plug.
 *
 * <p>It is an ordinary item on purpose. NoN's own data driven accessories ({@code
 * assets/needsofnature/non_accessory_items/*.json}) can only be contributed by a file placed in
 * NoN's own jar roots or in the {@code needsofnature/} content pack folder - {@code
 * NonAccessoryItemRegistry.loadBundledDefinitions} reads exactly one mod container and the external
 * catalogue reads a directory, so a classpath resource inside this addon would be ignored. The item
 * therefore registers itself, which also keeps the id in this addon's own namespace.
 *
 * <p>Wearing it is {@link PlugGoggles}' job: Trinkets decides slot eligibility from the {@code
 * trinkets:legs/v} and {@code trinkets:legs/a} item tags, which are shipped as data files, so
 * nothing here has to know about Trinkets at all and the item keeps working without it.
 */
public final class AndesitePlug {
    /** Registry key and translation key of the plug. */
    public static final RegistryKey<Item> KEY = RegistryKey.of(RegistryKeys.ITEM, NoNAddon.id("andesite_plug"));

    /** The plug itself: a single, unstackable wearable. */
    public static final Item ITEM = new Item(new Item.Settings().registryKey(KEY).maxCount(1));

    private AndesitePlug() {
    }

    public static void register() {
        Registry.register(Registries.ITEM, KEY, ITEM);
    }
}
