package net.arna.jcraft.mixin;

import net.arna.jcraft.common.item.AuMockItem;
import net.arna.jcraft.common.item.RewindMockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public class ItemStackMixin {

    @Inject(method = "is(Lnet/minecraft/world/item/Item;)Z", at = @At("HEAD"), cancellable = true)
    private void mockItem(Item item, CallbackInfoReturnable<Boolean> cir) {
        ItemStack thiz = (ItemStack) (Object) this;
        if (AuMockItem.isMockItem(thiz)) {
            cir.setReturnValue(AuMockItem.getMockedStack(thiz).is(item));
        }
        else if (RewindMockItem.isMockItem(thiz)) {
            cir.setReturnValue(RewindMockItem.getMockedStack(thiz).is(item));
        }
    }

    @Inject(method = "matches", at = @At("HEAD"), cancellable = true)
    private static void mockItemEqualsCheck(ItemStack left, ItemStack right, CallbackInfoReturnable<Boolean> cir) {

        if (!AuMockItem.isMockItem(left) && !AuMockItem.isMockItem(right)) {

            if (!RewindMockItem.isMockItem(left) && !RewindMockItem.isMockItem(right)) {
                return;
            }
            ItemStack stack1 = RewindMockItem.isMockItem(left) ? RewindMockItem.getMockedStack(left) : left;
            ItemStack stack2 = RewindMockItem.isMockItem(right) ? RewindMockItem.getMockedStack(right) : right;
            cir.setReturnValue(ItemStack.matches(stack1, stack2));
            return;
        }

        ItemStack stack1 = AuMockItem.isMockItem(left) ? AuMockItem.getMockedStack(left) : left;
        ItemStack stack2 = AuMockItem.isMockItem(right) ? AuMockItem.getMockedStack(right) : right;

        cir.setReturnValue(ItemStack.matches(stack1, stack2));
    }
}
