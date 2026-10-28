package com.example.undoo;

import com.mojang.serialization.Codec;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

@Mod(UndooMod.MODID)
public class UndooMod {
    public static final String MODID = "undoo";
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, MODID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MODID);
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MODID);

    public static final DeferredHolder<Block, Block> NETHER_BLOCK = BLOCKS.register("nether_portal_block", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(1.0f).sound(SoundType.GLASS).setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MODID, "nether_portal_block")))));
    public static final DeferredHolder<Block, Block> END_BLOCK = BLOCKS.register("end_portal_block", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(1.0f).sound(SoundType.GLASS).setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MODID, "end_portal_block")))));

    public static final DeferredHolder<Item, Item> NETHER_ITEM = ITEMS.register("nether_portal", () -> new BlockItem(NETHER_BLOCK.get(), new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, "nether_portal")))));
    public static final DeferredHolder<Item, Item> END_ITEM = ITEMS.register("end_portal", () -> new BlockItem(END_BLOCK.get(), new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, "end_portal")))));
    public static final DeferredHolder<Item, Item> TNT = ITEMS.register("big_tnt", () -> new BigTntItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, "big_tnt")))));
    public static final DeferredHolder<Item, Item> ENCHANTER = ITEMS.register("mega_enchanter", () -> new MegaEnchanterItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, "mega_enchanter")))));
    public static final DeferredHolder<Item, Item> MOB = ITEMS.register("fastsetting_mob", () -> new FastsettingMobItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, "fastsetting_mob")))));
    public static final DeferredHolder<Item, Item> WAND = ITEMS.register("magic_wand", () -> new MagicWandItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, "magic_wand")))));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("undoo_tab", () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.undoo")).icon(() -> new ItemStack(TNT.get())).displayItems((params, out) -> {
        out.accept(TNT.get()); out.accept(NETHER_ITEM.get()); out.accept(END_ITEM.get()); out.accept(ENCHANTER.get()); out.accept(MOB.get()); out.accept(WAND.get());
        out.accept(Items.LAVA_BUCKET); out.accept(Items.WATER_BUCKET);
        ItemStack sword = new ItemStack(Items.NETHERITE_SWORD);
        ItemEnchantments.Mutable mut = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        params.holders().lookupOrThrow(Registries.ENCHANTMENT).listElements().forEach(h -> mut.set(h, 255));
        sword.set(DataComponents.ENCHANTMENTS, mut.toImmutable());
        out.accept(sword);
        out.accept(Items.STONE); out.accept(Items.GRASS_BLOCK); out.accept(Items.DIRT);
    }).build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Float>> POWER = COMPONENTS.register("explosion_power", () -> DataComponentType.<Float>builder().persistent(Codec.FLOAT).networkSynchronized(ByteBufCodecs.FLOAT).build());

    public static final DeferredHolder<MenuType<?>, MenuType<TntMenu>> TNT_MENU = MENUS.register("tnt_menu", () -> IMenuTypeExtension.create((id, inv, buf) -> new TntMenu(id, inv)));
    public static final DeferredHolder<MenuType<?>, MenuType<EnchanterMenu>> ENCH_MENU = MENUS.register("enchanter_menu", () -> IMenuTypeExtension.create((id, inv, buf) -> new EnchanterMenu(id, inv)));
    public static final DeferredHolder<MenuType<?>, MenuType<TradeMenu>> TRADE_MENU = MENUS.register("trade_menu", () -> IMenuTypeExtension.create((id, inv, buf) -> new TradeMenu(id, inv)));
    public static final DeferredHolder<MenuType<?>, MenuType<MobConstructorMenu>> MOB_MENU = MENUS.register("mob_menu", () -> IMenuTypeExtension.create((id, inv, buf) -> new MobConstructorMenu(id, inv)));
    public static final DeferredHolder<MenuType<?>, MenuType<SavedMobsMenu>> SAVED_MENU = MENUS.register("saved_menu", () -> IMenuTypeExtension.create((id, inv, buf) -> new SavedMobsMenu(id, inv)));

    public static final List<SavedMob> SAVED = new ArrayList<>();

    public UndooMod(IEventBus bus) {
        ITEMS.register(bus); BLOCKS.register(bus); TABS.register(bus); MENUS.register(bus); COMPONENTS.register(bus);
        NeoForge.EVENT_BUS.register(CommandBlocker.class);
        bus.addListener(this::registerScreens);
        bus.addListener(this::registerPackets);
    }

    private void registerScreens(RegisterMenuScreensEvent event) {
        event.register(TNT_MENU.get(), TntScreen::new);
        event.register(ENCH_MENU.get(), EnchanterScreen::new);
        event.register(TRADE_MENU.get(), TradeScreen::new);
        event.register(MOB_MENU.get(), MobConstructorScreen::new);
        event.register(SAVED_MENU.get(), SavedMobsScreen::new);
    }

    private void registerPackets(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar reg = event.registrar(MODID);
        reg.playToServer(SetTradePacket.TYPE, SetTradePacket.STREAM_CODEC, SetTradePacket::handle);
        reg.playToServer(SaveMobPacket.TYPE, SaveMobPacket.STREAM_CODEC, SaveMobPacket::handle);
        reg.playToServer(SpawnMobPacket.TYPE, SpawnMobPacket.STREAM_CODEC, SpawnMobPacket::handle);
        reg.playToServer(DeleteMobPacket.TYPE, DeleteMobPacket.STREAM_CODEC, DeleteMobPacket::handle);
    }
}

class CommandBlocker { @SubscribeEvent public static void onCmd(CommandEvent e) { if (e.getParseResults().getReader().getString().startsWith("give") && e.getParseResults().getReader().getString().contains("undoo:")) e.setCanceled(true); } }

class BigTntItem extends Item {
    public BigTntItem(Properties p) { super(p); }
    public static float getPower(ItemStack s) { return s.getOrDefault(UndooMod.POWER.get(), 4f); }
    public InteractionResult use(Level l, Player p, InteractionHand h) {
        ItemStack s = p.getItemInHand(h);
        if (p.isShiftKeyDown()) { if (!l.isClientSide()) l.explode(null, p.getX(), p.getY(), p.getZ(), getPower(s), Level.ExplosionInteraction.TNT); return InteractionResult.SUCCESS; }
        if (!l.isClientSide() && p instanceof ServerPlayer sp) sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new TntMenu(id, inv), Component.literal("Сила взрыва")));
        return InteractionResult.SUCCESS;
    }
    public void appendHoverText(ItemStack s, TooltipContext c, List<Component> t, TooltipFlag f) { t.add(Component.literal("Сила: " + (int)getPower(s))); }
}

class MegaEnchanterItem extends Item {
    public MegaEnchanterItem(Properties p) { super(p); }
    public InteractionResult use(Level l, Player p, InteractionHand h) { if (!l.isClientSide() && p instanceof ServerPlayer sp) sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new EnchanterMenu(id, inv), Component.literal("Зачарователь"))); return InteractionResult.SUCCESS; }
}

class FastsettingMobItem extends Item {
    public FastsettingMobItem(Properties p) { super(p); }
    public InteractionResult use(Level l, Player p, InteractionHand h) { if (!l.isClientSide() && p instanceof ServerPlayer sp) sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new MobConstructorMenu(id, inv), Component.literal("Конструктор"))); return InteractionResult.SUCCESS; }
}

class MagicWandItem extends Item { public MagicWandItem(Properties p) { super(p); } public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext c) { return InteractionResult.SUCCESS; } }

class TntMenu extends AbstractContainerMenu {
    public TntMenu(int id, Inventory inv) { super(UndooMod.TNT_MENU.get(), id); this.addSlot(new Slot(new SimpleContainer(1), 0, 80, 20) { public boolean mayPlace(ItemStack s) { return false; } }); }
    public ItemStack quickMoveStack(Player p, int i) { return ItemStack.EMPTY; } public boolean stillValid(Player p) { return true; }
}
class EnchanterMenu extends AbstractContainerMenu { public EnchanterMenu(int id, Inventory inv) { super(UndooMod.ENCH_MENU.get(), id); } public ItemStack quickMoveStack(Player p, int i) { return ItemStack.EMPTY; } public boolean stillValid(Player p) { return true; } }
class TradeMenu extends AbstractContainerMenu { public TradeMenu(int id, Inventory inv) { super(UndooMod.TRADE_MENU.get(), id); } public ItemStack quickMoveStack(Player p, int i) { return ItemStack.EMPTY; } public boolean stillValid(Player p) { return true; } }
class MobConstructorMenu extends AbstractContainerMenu { public MobConstructorMenu(int id, Inventory inv) { super(UndooMod.MOB_MENU.get(), id); } public ItemStack quickMoveStack(Player p, int i) { return ItemStack.EMPTY; } public boolean stillValid(Player p) { return true; } }
class SavedMobsMenu extends AbstractContainerMenu { public SavedMobsMenu(int id, Inventory inv) { super(UndooMod.SAVED_MENU.get(), id); } public ItemStack quickMoveStack(Player p, int i) { return ItemStack.EMPTY; } public boolean stillValid(Player p) { return true; } }

class TntScreen extends AbstractContainerScreen<TntMenu> {
    public TntScreen(TntMenu m, Inventory i, Component t) { super(m, i, t); }
    public void init() {
        super.init(); int cx = width / 2, cy = height / 2;
        addRenderableWidget(new AbstractSliderButton(cx - 80, cy - 40, 160, 20, Component.literal("Сила"), 0) { protected void updateMessage() {} protected void applyValue() {} });
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
                        ItemEnchantments.Mutable mut = new ItemEnchantments.Mutable(s.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY));
                        mut.set(h, 255); s.set(DataComponents.ENCHANTMENTS, mut.toImmutable());
                    }
                }
            }).bounds(cx - 80, cy + y, 160, 20).build());
            y += 22;
        }
    }
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float pt) { super.extractRenderState(g, mx, my, pt); }
}

class TradeScreen extends AbstractContainerScreen<TradeMenu> {
    private EditBox in, out;
    public TradeScreen(TradeMenu m, Inventory i, Component t) { super(m, i, t); }
    public void init() {
        super.init(); int cx = width / 2, cy = height / 2;
        in = new EditBox(minecraft.font, cx - 100, cy - 60, 200, 20, Component.literal("Вход"));
        out = new EditBox(minecraft.font, cx - 100, cy - 30, 200, 20, Component.literal("Выход"));
        addRenderableWidget(in); addRenderableWidget(out);
        addRenderableWidget(Button.builder(Component.literal("Применить"), b -> {
            if (minecraft != null && minecraft.player != null) { PacketDistributor.sendToServer(new SetTradePacket(in.getValue(), out.getValue())); minecraft.player.closeContainer(); }
        }).bounds(cx - 80, cy + 10, 160, 20).build());
    }
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float pt) { super.extractRenderState(g, mx, my, pt); }
}

class MobConstructorScreen extends AbstractContainerScreen<MobConstructorMenu> {
    private EditBox name, ent, mh, oh, hd, ch, lg, ft;
    public MobConstructorScreen(MobConstructorMenu m, Inventory i, Component t) { super(m, i, t); }
    public void init() {
        super.init(); int cx = width / 2, cy = height / 2 - 100;
        name = new EditBox(minecraft.font, cx - 100, cy, 200, 20, Component.literal("Имя"));
        ent = new EditBox(minecraft.font, cx - 100, cy + 25, 200, 20, Component.literal("ID моба"));
        mh = new EditBox(minecraft.font, cx - 100, cy + 50, 200, 20, Component.literal("Рука"));
        oh = new EditBox(minecraft.font, cx - 100, cy + 75, 200, 20, Component.literal("Левая"));
        hd = new EditBox(minecraft.font, cx - 100, cy + 100, 200, 20, Component.literal("Шлем"));
        ch = new EditBox(minecraft.font, cx - 100, cy + 125, 200, 20, Component.literal("Нагрудник"));
        lg = new EditBox(minecraft.font, cx - 100, cy + 150, 200, 20, Component.literal("Штаны"));
        ft = new EditBox(minecraft.font, cx - 100, cy + 175, 200, 20, Component.literal("Ботинки"));
        addRenderableWidget(name); addRenderableWidget(ent); addRenderableWidget(mh); addRenderableWidget(oh); addRenderableWidget(hd); addRenderableWidget(ch); addRenderableWidget(lg); addRenderableWidget(ft);
        addRenderableWidget(Button.builder(Component.literal("+"), b -> {
            PacketDistributor.sendToServer(new SaveMobPacket(name.getValue(), ent.getValue(), mh.getValue(), oh.getValue(), hd.getValue(), ch.getValue(), lg.getValue(), ft.getValue()));
            if (minecraft != null && minecraft.player != null) { minecraft.player.closeContainer(); minecraft.player.openMenu(new SimpleMenuProvider((id, inv, pl) -> new SavedMobsMenu(id, inv), Component.literal("Мобы"))); }
        }).bounds(cx - 20, cy + 205, 40, 20).build());
    }
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float pt) { super.extractRenderState(g, mx, my, pt); }
}

class SavedMobsScreen extends AbstractContainerScreen<SavedMobsMenu> {
    public SavedMobsScreen(SavedMobsMenu m, Inventory i, Component t) { super(m, i, t); }
    public void init() {
        super.init(); int cx = width / 2, cy = height / 2 - 60, y = 0;
        for (int i = 0; i < UndooMod.SAVED.size(); i++) {
            int idx = i; SavedMob mob = UndooMod.SAVED.get(i);
            addRenderableWidget(Button.builder(Component.literal(mob.name()), b -> {}).bounds(cx - 100, cy + y, 100, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Спавн"), b -> PacketDistributor.sendToServer(new SpawnMobPacket(idx))).bounds(cx, cy + y, 80, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Удалить"), b -> { PacketDistributor.sendToServer(new DeleteMobPacket(idx)); if (minecraft != null && minecraft.player != null) { minecraft.player.closeContainer(); minecraft.player.openMenu(new SimpleMenuProvider((id, inv, pl) -> new SavedMobsMenu(id, inv), Component.literal("Мобы"))); } }).bounds(cx + 85, cy + y, 80, 20).build());
            y += 25;
        }
    }
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float pt) { super.extractRenderState(g, mx, my, pt); }
}

record SavedMob(String name, String entityId, String mainHand, String offHand, String head, String chest, String legs, String feet) {}

record SetTradePacket(String in, String out) implements CustomPacketPayload {
    public static final Type<SetTradePacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(UndooMod.MODID, "set_trade"));
    public static final StreamCodec<FriendlyByteBuf, SetTradePacket> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, SetTradePacket::in, ByteBufCodecs.STRING_UTF8, SetTradePacket::out, SetTradePacket::new);
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void 
     handle(SetTradePacket p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) {
                var v = sp.level().getEntitiesOfClass(Villager.class, sp.getBoundingBox().inflate(5)).stream().findFirst().orElse(null);
                if (v != null) {
                    var inOpt = BuiltInRegistries.ITEM.get(Identifier.parse(p.in()));
                    var outOpt = BuiltInRegistries.ITEM.get(Identifier.parse(p.out()));
                    if (inOpt.isPresent() && outOpt.isPresent()) {
                        var o = new MerchantOffers();
                        o.add(new MerchantOffer(new ItemStack(inOpt.get().value(), 1), new ItemStack(outOpt.get().value(), 1), 100, 1, 0.05f));
                        v.setOffers(o);
                    }
                }
            }
        });
    }
}

record SaveMobPacket(String name, String entityId, String mainHand, String offHand, String head, String chest, String legs, String feet) implements CustomPacketPayload {
    public static final Type<SaveMobPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(UndooMod.MODID, "save_mob"));
    public static final StreamCodec<FriendlyByteBuf, SaveMobPacket> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, SaveMobPacket::name, ByteBufCodecs.STRING_UTF8, SaveMobPacket::entityId, ByteBufCodecs.STRING_UTF8, SaveMobPacket::mainHand, ByteBufCodecs.STRING_UTF8, SaveMobPacket::offHand, ByteBufCodecs.STRING_UTF8, SaveMobPacket::head, ByteBufCodecs.STRING_UTF8, SaveMobPacket::chest, ByteBufCodecs.STRING_UTF8, SaveMobPacket::legs, ByteBufCodecs.STRING_UTF8, SaveMobPacket::feet, SaveMobPacket::new);
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(SaveMobPacket p, IPayloadContext ctx) { ctx.enqueueWork(() -> UndooMod.SAVED.add(new SavedMob(p.name(), p.entityId(), p.mainHand(), p.offHand(), p.head(), p.chest(), p.legs(), p.feet()))); }
}

record SpawnMobPacket(int idx) implements CustomPacketPayload {
    public static final Type<SpawnMobPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(UndooMod.MODID, "spawn_mob"));
    public static final StreamCodec<FriendlyByteBuf, SpawnMobPacket> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.INT, SpawnMobPacket::idx, SpawnMobPacket::new);
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(SpawnMobPacket p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> { 
            if (ctx.player() instanceof ServerPlayer sp && p.idx() < UndooMod.SAVED.size()) { 
                SavedMob m = UndooMod.SAVED.get(p.idx()); 
                var entityTypeOpt = BuiltInRegistries.ENTITY_TYPE.get(Identifier.parse(m.entityId()));
                if (entityTypeOpt.isPresent()) {
                    var e = entityTypeOpt.get().value().create(sp.level()); 
                    if (e instanceof Mob mob) { 
                        mob.setPos(sp.getX(), sp.getY(), sp.getZ()); 
                        if(!m.mainHand().isEmpty()) { var opt = BuiltInRegistries.ITEM.get(Identifier.parse(m.mainHand())); if(opt.isPresent()) mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(opt.get().value())); }
                        if(!m.offHand().isEmpty()) { var opt = BuiltInRegistries.ITEM.get(Identifier.parse(m.offHand())); if(opt.isPresent()) mob.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(opt.get().value())); }
                        if(!m.head().isEmpty()) { var opt = BuiltInRegistries.ITEM.get(Identifier.parse(m.head())); if(opt.isPresent()) mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(opt.get().value())); }
                        if(!m.chest().isEmpty()) { var opt = BuiltInRegistries.ITEM.get(Identifier.parse(m.chest())); if(opt.isPresent()) mob.setItemSlot(EquipmentSlot.CHEST, new ItemStack(opt.get().value())); }
                        if(!m.legs().isEmpty()) { var opt = BuiltInRegistries.ITEM.get(Identifier.parse(m.legs())); if(opt.isPresent()) mob.setItemSlot(EquipmentSlot.LEGS, new ItemStack(opt.get().value())); }
                        if(!m.feet().isEmpty()) { var opt = BuiltInRegistries.ITEM.get(Identifier.parse(m.feet())); if(opt.isPresent()) mob.setItemSlot(EquipmentSlot.FEET, new ItemStack(opt.get().value())); }
                        sp.level().addFreshEntity(mob); 
                    } 
                }
            } 
        });
    }
}

record DeleteMobPacket(int idx) implements CustomPacketPayload {
    public static final Type<DeleteMobPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(UndooMod.MODID, "delete_mob"));
    public static final StreamCodec<FriendlyByteBuf, DeleteMobPacket> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.INT, DeleteMobPacket::idx, DeleteMobPacket::new);
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(DeleteMobPacket p, IPayloadContext ctx) { ctx.enqueueWork(() -> { if (p.idx() < UndooMod.SAVED.size()) UndooMod.SAVED.remove(p.idx()); }); }
}

@EventBusSubscriber(modid = UndooMod.MODID, value = Dist.CLIENT)
class KeyHandler {
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
                                                                                                    }
