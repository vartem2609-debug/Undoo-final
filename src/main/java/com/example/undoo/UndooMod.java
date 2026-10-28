package com.example.undoo;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.serialization.Codec;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.lwjgl.glfw.GLFW;

import java.util.List;

@Mod(UndooMod.MODID)
public class UndooMod {
    public static final String MODID = "undoo";

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, MODID);
    public static final DeferredHolder<Item, Item> BIG_TNT = ITEMS.register("big_tnt", () -> new BigTntItem(new Item.Properties()));
    public static final DeferredHolder<Item, Item> END_PORTAL = ITEMS.register("end_portal", () -> new EndPortalItem(new Item.Properties()));
    public static final DeferredHolder<Item, Item> NETHER_PORTAL = ITEMS.register("nether_portal", () -> new NetherPortalItem(new Item.Properties()));
    public static final DeferredHolder<Item, Item> MEGA_ENCHANTER = ITEMS.register("mega_enchanter", () -> new MegaEnchanterItem(new Item.Properties()));

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MODID);
    public static final DeferredHolder<MenuType<?>, MenuType<TntMenu>> TNT_MENU = MENUS.register("tnt_menu", () -> IMenuTypeExtension.create(TntMenu::new));

    public static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MODID);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Float>> EXPLOSION_POWER = COMPONENTS.register("explosion_power", () -> DataComponentType.<Float>builder().persistent(Codec.FLOAT).networkSynchronized(ByteBufCodecs.FLOAT).build());

    public UndooMod(IEventBus modBus) {
        ITEMS.register(modBus);
        MENUS.register(modBus);
        COMPONENTS.register(modBus);
        NeoForge.EVENT_BUS.register(CommandBlocker.class);
        modBus.addListener(this::addCreative);
        modBus.addListener(this::registerScreens);
        modBus.addListener(this::registerPackets);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            NeoForge.EVENT_BUS.register(ClientStuff.GameBusEvents.class);
            modBus.addListener(ClientStuff.ModBusEvents::regKeys);
        }
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SEARCH) {
            event.accept(BIG_TNT.get());
            event.accept(END_PORTAL.get());
            event.accept(NETHER_PORTAL.get());
            event.accept(MEGA_ENCHANTER.get());
        }
    }

    private void registerScreens(RegisterMenuScreensEvent event) {
        if (FMLEnvironment.dist == Dist.CLIENT) event.register(TNT_MENU.get(), ClientStuff.TntScreen::new);
    }

    private void registerPackets(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(MODID);
        registrar.playToServer(OpenTntMenuPacket.TYPE, OpenTntMenuPacket.STREAM_CODEC, OpenTntMenuPacket::handle);
    }
}

class CommandBlocker {
    @SubscribeEvent
    public static void onCommand(CommandEvent event) {
        String cmd = event.getParseResults().getReader().getString();
        if (cmd.startsWith("give") && (cmd.contains("undoo:big_tnt") || cmd.contains("undoo:end_portal") || cmd.contains("undoo:nether_portal") || cmd.contains("undoo:mega_enchanter"))) {
            event.setCanceled(true);
        }
    }
}

class BigTntItem extends Item {
    public BigTntItem(Properties p) { super(p); }
    public static float getPower(ItemStack s) { return s.getOrDefault(UndooMod.EXPLOSION_POWER.get(), 4f); }
    public static void setPower(ItemStack s, float p) { s.set(UndooMod.EXPLOSION_POWER.get(), Math.max(1f, Math.min(500f, p))); }
    
    @Override 
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide()) level.explode(null, player.getX(), player.getY(), player.getZ(), getPower(stack), Level.ExplosionInteraction.TNT);
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            sp.openMenu(new SimpleMenuProvider((id, inv, p) -> new TntMenu(id, inv, stack), Component.literal("Сила взрыва")), buf -> buf.writeItem(stack));
        }
        return InteractionResult.SUCCESS;
    }
    
    @Override 
    public void appendHoverText(ItemStack s, TooltipContext c, List<Component> t, TooltipFlag f) {
        t.add(Component.literal("Сила: " + (int) getPower(s)));
        t.add(Component.literal("ПКМ — меню | Shift+ПКМ — взрыв"));
    }
}

class NetherPortalItem extends Item {
    public NetherPortalItem(Properties p) { super(p); }
    @Override 
    public InteractionResult use(Level l, Player p, InteractionHand h) {
        if (p instanceof ServerPlayer sp) {
            var n = sp.server.getLevel(Level.NETHER);
            if (n != null) sp.teleportTo(n, (int) sp.getX() / 8, 100, (int) sp.getZ() / 8, sp.getYRot(), sp.getXRot());
        }
        return InteractionResult.SUCCESS;
    }
}

class EndPortalItem extends Item {
    public EndPortalItem(Properties p) { super(p); }
    @Override 
    public InteractionResult use(Level l, Player p, InteractionHand h) {
        if (p instanceof ServerPlayer sp) {
            var e = sp.server.getLevel(Level.END);
            if (e != null) sp.teleportTo(e, 1000, 100, 0, sp.getYRot(), sp.getXRot());
        }
        return InteractionResult.SUCCESS;
    }
}

class MegaEnchanterItem extends Item {
    public MegaEnchanterItem(Properties p) { super(p); }
    @Override 
    public InteractionResult use(Level l, Player p, InteractionHand h) {
        if (!l.isClientSide()) {
            ItemStack t = p.getOffhandItem();
            if (!t.isEmpty()) {
                var reg = l.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
                var mut = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                reg.stream().forEach(e -> mut.set(e, 255));
                t.set(DataComponents.ENCHANTMENTS, mut.toImmutable());
            }
        }
        return InteractionResult.SUCCESS;
    }
}

class TntMenu extends AbstractContainerMenu {
    private final ItemStack tntStack;
    public TntMenu(int id, Inventory inv, FriendlyByteBuf buf) { this(id, inv, ItemStack.STREAM_CODEC.decode(buf)); }
    public TntMenu(int id, Inventory inv, ItemStack stack) {
        super(UndooMod.TNT_MENU.get(), id);
        this.tntStack = stack;
        Container container = new SimpleContainer(1);
        this.addSlot(new Slot(container, 0, 80, 20) { @Override public boolean mayPlace(ItemStack s) { return false; } @Override public ItemStack getItem() { return tntStack; } });
        for (int i = 0; i < 3; ++i) { for (int j = 0; j < 9; ++j) { this.addSlot(new Slot(inv, j + i * 9 + 9, 8 + j * 18, 84 + i * 18)); } }
        for (int k = 0; k < 9; ++k) { this.addSlot(new Slot(inv, k, 8 + k * 18, 142)); }
    }
    public ItemStack getTntStack() { return tntStack; }
    @Override public ItemStack quickMoveStack(Player p, int i) { return ItemStack.EMPTY; }
    @Override public boolean stillValid(Player p) { return true; }
}

record OpenTntMenuPacket() implements CustomPacketPayload {
    public static final Type<OpenTntMenuPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath("undoo", "open_tnt_menu"));
    public static final StreamCodec<FriendlyByteBuf, OpenTntMenuPacket> STREAM_CODEC = StreamCodec.unit(new OpenTntMenuPacket());
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(OpenTntMenuPacket p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) {
                var stack = sp.getMainHandItem();
                sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new TntMenu(id, inv, stack), Component.literal("Сила взрыва")), buf -> buf.writeItem(stack));
            }
        });
    }
}

class ClientStuff {
    @EventBusSubscriber(modid = UndooMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModBusEvents {
        public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(Identifier.fromNamespaceAndPath("undoo", "main"));
        public static final KeyMapping OPEN_MENU = new KeyMapping("key.undoo.open_menu", GLFW.GLFW_KEY_I, CATEGORY);
        
        @SubscribeEvent public static void regKeys(RegisterKeyMappingsEvent e) { 
            e.registerCategory(CATEGORY);
            e.register(OPEN_MENU); 
        }
    }

    @EventBusSubscriber(modid = UndooMod.MODID, value = Dist.CLIENT)
    public static class GameBusEvents {
        @SubscribeEvent public static void onKey(ClientTickEvent.Post e) {
            while (ModBusEvents.OPEN_MENU.consumeClick()) {
                var p = Minecraft.getInstance().player;
                if (p != null && p.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof BigTntItem) PacketDistributor.sendToServer(new OpenTntMenuPacket());
            }
        }
    }

    public static class TntScreen extends AbstractContainerScreen<TntMenu> {
        private float power;
        public TntScreen(TntMenu m, Inventory i, Component t) { super(m, i, t); this.power = BigTntItem.getPower(m.getTntStack()); }
        @Override protected void init() {
            super.init();
            int cx = width / 2, cy = height / 2;
            addRenderableWidget(new AbstractSliderButton(cx - 80, cy - 40, 160, 20, Component.literal("Сила: " + (int) power), (power - 1) / 499.0) {
                @Override protected void updateMessage() { setMessage(Component.literal("Сила: " + (int) (this.value * 499 + 1))); }
                @Override protected void applyValue() { power = (float) (this.value * 499 + 1); BigTntItem.setPower(menu.getTntStack(), power); }
            });
            addRenderableWidget(Button.builder(Component.literal("💥 ВЗОРВАТЬ 💥"), b -> { BigTntItem.setPower(menu.getTntStack(), power); if (minecraft != null && minecraft.player != null) minecraft.player.closeContainer(); }).bounds(cx - 80, cy - 10, 160, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Закрыть"), b -> { if (minecraft != null && minecraft.player != null) minecraft.player.closeContainer(); }).bounds(cx - 40, cy + 30, 80, 20).build());
        }
        @Override protected void renderBg(GuiGraphics g, float p, int mx, int my) {
            int x = (width - 176) / 2, y = (height - 166) / 2;
            g.fill(x, y, x + 176, y + 166, 0xFF1A0033); g.fill(x + 1, y + 1, x + 175, y + 165, 0xFF330066);
            g.fill(x, y, x + 176, y + 2, 0xFFFF00FF); g.fill(x, y + 164, x + 176, y + 166, 0xFFFF00FF);
            g.fill(x, y, x + 2, y + 166, 0xFFFF00FF); g.fill(x + 174, y, x + 176, y + 166, 0xFFFF00FF);
        }
        @Override public void render(GuiGraphics g, int mx, int my, float pt) { renderBackground(g, mx, my, pt); super.render(g, mx, my, pt); renderTooltip(g, mx, my); }
    }
        }
