package com.example.undoo;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.SimpleMenuProvider;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = UndooMod.MODID, value = Dist.CLIENT)
public class ClientEvents {
    public static final KeyMapping.Category CAT = new KeyMapping.Category(Identifier.fromNamespaceAndPath("undoo", "main"));
    public static final KeyMapping KEY = new KeyMapping("key.undoo.open_menu", GLFW.GLFW_KEY_I, CAT);

    @SubscribeEvent public static void reg(RegisterKeyMappingsEvent e) { e.registerCategory(CAT); e.register(KEY); }

    @SubscribeEvent public static void tick(ClientTickEvent.Post e) {
        while (KEY.consumeClick()) { var p = Minecraft.getInstance().player; if (p != null && p.getMainHandItem().getItem() instanceof BigTntItem) p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new TntMenu(id, inv), Component.literal("ТНТ"))); }
    }

    @SubscribeEvent public static void click(InputEvent.MouseButton.Pre e) {
        if (e.getAction() == GLFW.GLFW_PRESS && e.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            var p = Minecraft.getInstance().player;
            if (p != null && p.isShiftKeyDown() && Minecraft.getInstance().options.keyJump.isDown() && p.getMainHandItem().getItem() instanceof MagicWandItem) {
                p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new TradeMenu(id, inv), Component.literal("Палочка")));
                e.setCanceled(true);
            }
        }
    }

    @SubscribeEvent public static void regScreens(RegisterMenuScreensEvent e) {
        e.register(Menus.TNT_MENU.get(), TntScreen::new);
        e.register(Menus.ENCH_MENU.get(), EnchanterScreen::new);
        e.register(Menus.TRADE_MENU.get(), TradeScreen::new);
        e.register(Menus.MOB_MENU.get(), MobConstructorScreen::new);
        e.register(Menus.SAVED_MENU.get(), SavedMobsScreen::new);
    }
}
