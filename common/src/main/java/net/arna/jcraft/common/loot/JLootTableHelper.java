package net.arna.jcraft.common.loot;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import net.arna.jcraft.common.enchantments.CinderellasKissEnchantment;
import net.arna.jcraft.api.registry.JItemRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetEnchantmentsFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.BinomialDistributionGenerator;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import java.util.function.Consumer;

public class JLootTableHelper {
    public static final Multimap<ResourceLocation, Consumer<LootTable.Builder>> modifications = MultimapBuilder.hashKeys().linkedHashSetValues().build();

    public static void registerLootTables() {
        registerModification(JLootTableHelper::addMaskPool,
                new ResourceLocation("chests/abandoned_mineshaft"),
                new ResourceLocation("chests/buried_treasure"),
                new ResourceLocation("chests/end_city_treasure"),
                new ResourceLocation("chests/pillager_outpost"),
                new ResourceLocation("chests/simple_dungeon"),
                new ResourceLocation("chests/spawn_bonus_chest"),
                new ResourceLocation("chests/stronghold_library"),
                new ResourceLocation("chests/woodland_mansion")
        );
    }

    public static void registerModification(Consumer<LootTable.Builder> modifier, ResourceLocation... lootTables) {
        for (ResourceLocation lootTable : lootTables) {
            modifications.put(lootTable, modifier);
        }
    }

    private static void addMaskPool(LootTable.Builder builder) {
        builder.withPool(LootPool.lootPool()
                .add(LootItem.lootTableItem(JItemRegistry.CINDERELLA_MASK.get())
                        .setWeight(1) // 33% chance
                        .apply(new SetEnchantmentsFunction.Builder()
                                // Binomial distribution with n = 3, p = 0.4, plotter here:
                                // https://homepage.divms.uiowa.edu/~mbognar/applets/bin.html
                                // P(0) = 21.6%; P(1) = 43.2%; P(2) = 28.8%; P(3) = 6.4%
                                .withEnchantment(CinderellasKissEnchantment.INSTANCE, BinomialDistributionGenerator.binomial(3, 0.4f))))
                .add(LootItem.lootTableItem(Items.BOOK)
                        .setWeight(2) // 67% chance
                        .apply(new SetEnchantmentsFunction.Builder()
                                // Enchant with at least level 1
                                .withEnchantment(CinderellasKissEnchantment.INSTANCE, ConstantValue.exactly(1)))
                        .apply(new SetEnchantmentsFunction.Builder(true)
                                // Add up to 2 levels to the kiss enchantment
                                // n = 2, p = 0.25
                                // P(0) = 56.25%; P(1) = 37.5%; P(2) = 6.25%
                                .withEnchantment(CinderellasKissEnchantment.INSTANCE, BinomialDistributionGenerator.binomial(2, 0.25f))))
                .when(LootItemRandomChanceCondition.randomChance(0.08f)));

        builder.withPool(LootPool.lootPool()
                .add(LootItem.lootTableItem(JItemRegistry.STONE_MASK.get()))
                .when(LootItemRandomChanceCondition.randomChance(0.04f)));
    }
}