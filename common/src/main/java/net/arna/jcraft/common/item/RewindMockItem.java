package net.arna.jcraft.common.item;

import net.arna.jcraft.api.registry.JItemRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class RewindMockItem extends Item {
    private static final ItemStack FALLBACK = new ItemStack(Items.DIRT);

    public RewindMockItem() {
        super(new Properties());
    }

    public static boolean isMockItem(ItemStack stack) {
        // Crashes on startup due to FireBlock.bootStrap(), requiring this
        if (!JItemRegistry.REWIND_MOCK_ITEM.isPresent()) return false;
        return stack.getItem() == JItemRegistry.REWIND_MOCK_ITEM.get();
    }

    public static ItemStack getMockedStack(ItemStack mockItemStack) {
        CompoundTag nbt = mockItemStack.getTag();
        if (nbt == null || !nbt.contains("RewindMockItem", Tag.TAG_STRING)) {
            return FALLBACK;
        }

        String mockItemId = nbt.getString("RewindMockItem");
        Item mockItem = BuiltInRegistries.ITEM.get(new ResourceLocation(mockItemId));

        CompoundTag mockData = nbt.contains("MockData", Tag.TAG_COMPOUND) ? nbt.getCompound("MockData") : null;

        ItemStack mockedStack = new ItemStack(mockItem, mockItemStack.getCount());
        mockedStack.setTag(mockData);

        return mockedStack;
    }

    public static ItemStack createMockStack(ItemStack stack) {
        // No need to create a mock stack if it already is one
        if (isMockItem(stack)) {
            return stack;
        }

        ItemStack mockStack = new ItemStack(JItemRegistry.REWIND_MOCK_ITEM.get(), stack.getCount());
        CompoundTag nbt = mockStack.getOrCreateTag();
        // Register which item it's mocking and copy all relevant NBT data
        nbt.putString("RewindMockItem", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        if (stack.getTag() != null) {
            nbt.put("MockData", stack.getTag());
        }

        return mockStack;
    }

    @Override
    public String getDescriptionId(ItemStack stack) {
        return getMockedStack(stack).getDescriptionId();
    }
}
