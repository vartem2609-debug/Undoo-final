package com.example.undoo;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;

class TntScreen extends AbstractContainerScreen<TntMenu> {
    public TntScreen(TntMenu m, Inventory i, Component t) { super(m, i, t); }
    public void init() {
        super.init(); int cx = width / 2, cy = height / 2;
        addRenderableWidget(new net.minecraft.client.gui.components.AbstractSliderButton(cx - 80, cy - 40, 160, 20, Component.literal("Сила"), 0) { protected void updateMessage() {} protected void applyValue() {} });
        addRenderableWidget(Button.builder(Component.literal("Взрыв"), b -> { if (minecraft != null && minecraft.player != null) minecraft.player.closeContainer(); }).bounds(cx - 80, cy - 10, 160, 20).build());
    }
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float pt) { super.extractRenderState(g, mx, my, pt); }
}

class EnchanterScreen extends AbstractContainerScreen<EnchanterMenu> {
    public EnchanterScreen(EnchanterMenu m, Inventory i, Component t) { super(m, i, t); }
    public void init() {
        super.init(); int cx = width / 2, cy = height / 2 - 60;
        var reg = minecraft.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        int y = 0;
        for (var h : reg.listElements().toList()) {
            addRenderableWidget(Button.builder(Component.literal(Component.translatable(h.value().description().getString()).getString() + " 255"), b -> {
                if (minecraft != null && minecraft.player != null) {
                    ItemStack s = minecraft.player.getMainHandItem();
                    if (!s.isEmpty() && !(s.getItem() instanceof MegaEnchanterItem)) {
                        ItemEnchantments.Mutable mut = new ItemEnchantments.Mutable(s.getOrDefault(net.minecraft.core.component.DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY));
                        mut.set(h, 255); s.set(net.minecraft.core.component.DataComponents.ENCHANTMENTS, mut.toImmutable());
                    }
                }
            }).bounds(cx - 80, cy + y, 160, 20).build());
            y += 22;
        }
    }
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float pt) { super.extractRenderState(g, mx, my, pt); }
                            }
