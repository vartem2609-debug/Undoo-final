package com.example.undoo;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.SimpleContainer;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;

// ПРИКОЛЮШКА: ТНТ, которая взрывается при зажатии Shift, или открывает меню
class BigTntItem extends Item {
    public BigTntItem(Properties p) { super(p); }
    public InteractionResult use(Level l, Player p, InteractionHand h) {
        if (p.isShiftKeyDown()) { 
            if (!l.isClientSide()) {
                l.explode(null, p.getX(), p.getY(), p.getZ(), 10f, Level.ExplosionInteraction.TNT); 
            }
            return InteractionResult.SUCCESS; 
        }
        if (!l.isClientSide() && p instanceof ServerPlayer sp) {
            sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new TntMenu(id, inv), Component.literal("Сила взрыва")));
        }
        return InteractionResult.SUCCESS;
    }
}

class MegaEnchanterItem extends Item {
    public MegaEnchanterItem(Properties p) { super(p); }
    public InteractionResult use(Level l, Player p, InteractionHand h) { 
        if (!l.isClientSide() && p instanceof ServerPlayer sp) {
            sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new EnchanterMenu(id, inv), Component.literal("Зачарователь"))); 
        }
        return InteractionResult.SUCCESS; 
    }
}

// ПРИКОЛЮШКА: Открывает конструктор мобов
class FastsettingMobItem extends Item {
    public FastsettingMobItem(Properties p) { super(p); }
    public InteractionResult use(Level l, Player p, InteractionHand h) { 
        if (!l.isClientSide() && p instanceof ServerPlayer sp) {
            sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new MobConstructorMenu(id, inv), Component.literal("Конструктор"))); 
        }
        return InteractionResult.SUCCESS; 
    }
}

// ПРИКОЛЮШКА: Магическая палочка (пока просто заглушка, но готова к магии)
class MagicWandItem extends Item { 
    public MagicWandItem(Properties p) { super(p); } 
    public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext c) { 
        return InteractionResult.SUCCESS; 
    } 
}

class Menus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, UndooMod.MODID);
    public static final DeferredHolder<MenuType<?>, MenuType<TntMenu>> TNT_MENU = MENUS.register("tnt_menu", () -> IMenuTypeExtension.create((id, inv, buf) -> new TntMenu(id, inv)));
    public static final DeferredHolder<MenuType<?>, MenuType<EnchanterMenu>> ENCH_MENU = MENUS.register("enchanter_menu", () -> IMenuTypeExtension.create((id, inv, buf) -> new EnchanterMenu(id, inv)));
    public static final DeferredHolder<MenuType<?>, MenuType<TradeMenu>> TRADE_MENU = MENUS.register("trade_menu", () -> IMenuTypeExtension.create((id, inv, buf) -> new TradeMenu(id, inv)));
    public static final DeferredHolder<MenuType<?>, MenuType<MobConstructorMenu>> MOB_MENU = MENUS.register("mob_menu", () -> IMenuTypeExtension.create((id, inv, buf) -> new MobConstructorMenu(id, inv)));
    public static final DeferredHolder<MenuType<?>, MenuType<SavedMobsMenu>> SAVED_MENU = MENUS.register("saved_menu", () -> IMenuTypeExtension.create((id, inv, buf) -> new SavedMobsMenu(id, inv)));
    public static void register(net.neoforged.bus.api.IEventBus bus) { MENUS.register(bus); }
}

class TntMenu extends AbstractContainerMenu {
    public TntMenu(int id, Inventory inv) { 
        super(Menus.TNT_MENU.get(), id); 
        this.addSlot(new Slot(new SimpleContainer(1), 0, 80, 20) { 
            public boolean mayPlace(ItemStack s) { return false; } 
        }); 
    }
    public ItemStack quickMoveStack(Player p, int i) { return ItemStack.EMPTY; } 
    public boolean stillValid(Player p) { return true; }
}

class EnchanterMenu extends AbstractContainerMenu { 
    public EnchanterMenu(int id, Inventory inv) { super(Menus.ENCH_MENU.get(), id); } 
    public ItemStack quickMoveStack(Player p, int i) { return ItemStack.EMPTY; } 
    public boolean stillValid(Player p) { return true; } 
}

class TradeMenu extends AbstractContainerMenu { 
    public TradeMenu(int id, Inventory inv) { super(Menus.TRADE_MENU.get(), id); } 
    public ItemStack quickMoveStack(Player p, int i) { return ItemStack.EMPTY; } 
    public boolean stillValid(Player p) { return true; } 
}

class MobConstructorMenu extends AbstractContainerMenu { 
    public MobConstructorMenu(int id, Inventory inv) { super(Menus.MOB_MENU.get(), id); } 
    public ItemStack quickMoveStack(Player p, int i) { return ItemStack.EMPTY; } 
    public boolean stillValid(Player p) { return true; } 
}

class SavedMobsMenu extends AbstractContainerMenu { 
    public SavedMobsMenu(int id, Inventory inv) { super(Menus.SAVED_MENU.get(), id); } 
    public ItemStack quickMoveStack(Player p, int i) { return ItemStack.EMPTY; } 
    public boolean stillValid(Player p) { return true; } 
                                                                                                                                                                                   }
