package net.arna.jcraft.common.item;

import net.arna.jcraft.api.attack.moves.BlockMarkerMove;
import net.arna.jcraft.api.registry.JItemRegistry;
import net.arna.jcraft.common.marker.BlockMarkerMoves;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.UUID;

public class RewindMockItem extends Item {
    private static final ItemStack FALLBACK = new ItemStack(Items.DIRT);

    public RewindMockItem() {
        super(new Properties());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(final Level level, final Player player, final InteractionHand usedHand) {
        final ItemStack mockItem = player.getItemInHand(usedHand);
        if (!isMockItem(mockItem)) { // should never happen
            return InteractionResultHolder.fail(mockItem);
        }
        final ItemStack resolvedItem = resolveMockStack(mockItem);
        if (resolvedItem == mockItem) {
            return InteractionResultHolder.fail(mockItem);
        }
        player.setItemInHand(usedHand, resolvedItem);
        return InteractionResultHolder.success(resolvedItem);
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

    public static ItemStack createMockStack(ItemStack stack, BlockMarkerMove move) {
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
        nbt.putUUID("RewindUuid", move.getUuid());
        nbt.putInt("RewindRun", move.getIteration().size());

        return mockStack;
    }

    public static ItemStack resolveMockStack(ItemStack stack) {
        if (!isMockItem(stack)) {
            return stack;
        }
        CompoundTag nbt = stack.getOrCreateTag();
        if (!nbt.contains("RewindUuid") || !nbt.contains("RewindRun")) {
            return ItemStack.EMPTY;
        }
        UUID uuid = nbt.getUUID("RewindUuid");
        int run = nbt.getInt("RewindRun");
        Optional<BlockMarkerMove> move = BlockMarkerMoves.findFirst(m -> uuid.equals(m.getUuid()));
        // if in doubt, delete
        if (move.isEmpty()) {
            return ItemStack.EMPTY;
        }
        // if the move is still running, return the stack
        if (move.get().getIteration().size() == run) {
            return stack;
        }
        // if the rewind was successful executed
        if (move.get().getIteration().get(run)) {
            return ItemStack.EMPTY;
        }
        // otherwise rewind didn't happen, make item real
        return getMockedStack(stack);
    }

    @Override
    public String getDescriptionId(ItemStack stack) {
        return getMockedStack(stack).getDescriptionId();
    }
}
