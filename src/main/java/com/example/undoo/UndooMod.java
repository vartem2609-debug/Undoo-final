package com.example.undoo;

import com.mojang.blaze3d.platform.InputConstants;
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
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
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
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // Блоки
    public static final DeferredHolder<Block, Block> NETHER_PORTAL_BLOCK = BLOCKS.register("nether_portal_block", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(1.0f).sound(SoundType.GLASS).setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MODID, "nether_portal_block")))));
    public static final DeferredHolder<Block, Block> END_PORTAL_BLOCK = BLOCKS.register("end_portal_block", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(1.0f).sound(SoundType.GLASS).setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MODID, "end_portal_block")))));

    // Предметы
    public static final DeferredHolder<Item, Item> NETHER_PORTAL_ITEM = ITEMS.register("nether_portal", () -> new BlockItem(NETHER_PORTAL_BLOCK.get(), new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, "nether_portal")))));
    public static final DeferredHolder<Item, Item> END_PORTAL_ITEM = ITEMS.register("end_portal", () -> new BlockItem(END_PORTAL_BLOCK.get(), new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, "end_portal")))));
    public static final DeferredHolder<Item, Item> BIG_TNT = ITEMS.register("big_tnt", () -> new BigTntItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, "big_tnt")))));
    public static final DeferredHolder<Item, Item> MEGA_ENCHANTER = ITEMS.register("mega_enchanter", () -> new MegaEnchanterItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, "mega_enchanter")))));
    public static final DeferredHolder<Item, Item> FASTSETTING_MOB = ITEMS.register("fastsetting_mob", () -> new FastsettingMobItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, "fastsetting_mob")))));
    public static final DeferredHolder<Item, Item> MAGIC_WAND = ITEMS.register("magic_wand", () -> new MagicWandItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, "magic_wand")))));

    // Вкладка
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> UNDOO_TAB = CREATIVE_MODE_TABS.register("undoo_tab", () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.undoo")).icon(() -> new ItemStack(BIG_TNT.get())).displayItems((parameters, output) -> {
        output.accept(BIG_TNT.get()); output.accept(NETHER_PORTAL_ITEM.get()); output.accept(END_PORTAL_ITEM.get()); output.accept(MEGA_ENCHANTER.get()); output.accept(FASTSETTING_MOB.get()); output.accept(MAGIC_WAND.get());
        output.accept(Items.LAVA_BUCKET); output.accept(Items.WATER_BUCKET);
        ItemStack godSword = new ItemStack(Items.NETHERITE_SWORD); applyAllEnchantments(parameters.holders(), godSword); output.accept(godSword);
        output.accept(Items.STONE); output.accept(Items.GRASS_BLOCK); output.accept(Items.DIRT);
    }).build());

    private static void applyAllEnchantments(net.minecraft.core.HolderLookup.RegistryLookup<Enchantment> registry, ItemStack stack) {
        ItemEnchantments.Mutable mut = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        registry.listElements().forEach(holder -> mut.set(holder, 255));
        stack.set(DataComponents.ENCHANTMENTS, mut.toImmutable());
    }

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MODID);
    public static final DeferredHolder<MenuType<?>, MenuType<TntMenu>> TNT_MENU = MENUS.register("tnt_menu", () -> IMenuTypeExtension.create((id, inv, buf) -> new TntMenu(id, inv)));
    public static final DeferredHolder<MenuType<?>, MenuType<EnchanterMenu>> ENCHANTER_MENU = MENUS.register("enchanter_menu", () -> IMenuTypeExtension.create((id, inv, buf) -> new EnchanterMenu(id, inv)));
    public static final DeferredHolder<MenuType<?>, MenuType<TradeMenu>> TRADE_MENU = MENUS.register("trade_menu", () -> IMenuTypeExtension.create((id, inv, buf) -> new TradeMenu(id, inv)));
    public static final DeferredHolder<MenuType<?>, MenuType<MobConstructorMenu>> MOB_CONSTRUCTOR_MENU = MENUS.register("mob_constructor_menu", () -> IMenuTypeExtension.create((id, inv, buf) -> new MobConstructorMenu(id, inv)));
    public static final DeferredHolder<MenuType<?>, MenuType<SavedMobsMenu>> SAVED_MOBS_MENU = MENUS.register("saved_mobs_menu", () -> IMenuTypeExtension.create((id, inv, buf) -> new SavedMobsMenu(id, inv)));

    public static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MODID);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Float>> EXPLOSION_POWER = COMPONENTS.register("explosion_power", () -> DataComponentType.<Float>builder().persistent(Codec.FLOAT).networkSynchronized(ByteBufCodecs.FLOAT).build());

    public static final List<SavedMob> SAVED_MOBS = new ArrayList<>();

    public UndooMod(IEventBus modBus) {
        ITEMS.register(modBus); BLOCKS.register(modBus); CREATIVE_MODE_TABS.register(modBus); MENUS.register(modBus); COMPONENTS.register(modBus);
        NeoForge.EVENT_BUS.register(CommandBlocker.class);
        modBus.addListener(this::registerScreens);
        modBus.addListener(this::registerPackets);
    }

    private void registerScreens(RegisterMenuScreensEvent event) {
        event.register(TNT_MENU.get(), ClientInit::getTntScreen);
        event.register(ENCHANTER_MENU.get(), ClientInit::getEnchanterScreen);
        event.register(TRADE_MENU.get(), ClientInit::getTradeScreen);
        event.register(MOB_CONSTRUCTOR_MENU.get(), ClientInit::getMobConstructorScreen);
        event.register(SAVED_MOBS_MENU.get(), ClientInit::getSavedMobsScreen);
    }

    private void registerPackets(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(MODID);
        registrar.playToServer(SetTradePacket.TYPE, SetTradePacket.STREAM_CODEC, SetTradePacket::handle);
        registrar.playToServer(SaveMobPacket.TYPE, SaveMobPacket.STREAM_CODEC, SaveMobPacket::handle);
        registrar.playToServer(SpawnSavedMobPacket.TYPE, SpawnSavedMobPacket.STREAM_CODEC, SpawnSavedMobPacket::handle);
        registrar.playToServer(DeleteSavedMobPacket.TYPE, DeleteSavedMobPacket.STREAM_CODEC, DeleteSavedMobPacket::handle);
    }
}

// ================= ПАКЕТЫ =================

record SetTradePacket(String inputItem, String outputItem) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
    public static final Type<SetTradePacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(UndooMod.MODID, "set_trade"));
    public static final net.minecraft.network.codec.StreamCodec<FriendlyByteBuf, SetTradePacket> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.composite(ByteBufCodecs.STRING_UTF8, SetTradePacket::inputItem, ByteBufCodecs.STRING_UTF8, SetTradePacket::outputItem, SetTradePacket::new);
    @Override public Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() { return TYPE; }
    public static void handle(SetTradePacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) {
                var target = sp.level().getEntitiesOfClass(Villager.class, sp.getBoundingBox().inflate(5.0)).stream().findFirst().orElse(null);
                if (target != null) {
                    Item inputItem = BuiltInRegistries.ITEM.get(Identifier.parse(packet.inputItem));
                    Item outputItem = BuiltInRegistries.ITEM.get(Identifier.parse(packet.outputItem));
                    if (inputItem != null && outputItem != null) {
                        MerchantOffers offers = new MerchantOffers();
                        offers.add(new MerchantOffer(new ItemStack(inputItem, 1), new ItemStack(outputItem, 1), 100, 1, 0.05f));
                        target.setOffers(offers);
                    }
                }
            }
        });
    }
}

record SaveMobPacket(String name, String entityId, String mainHand, String offHand, String head, String chest, String legs, String feet) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
    public static final Type<SaveMobPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(UndooMod.MODID, "save_mob"));
    public static final net.minecraft.network.codec.StreamCodec<FriendlyByteBuf, SaveMobPacket> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.composite(ByteBufCodecs.STRING_UTF8, SaveMobPacket::name, ByteBufCodecs.STRING_UTF8, SaveMobPacket::entityId, ByteBufCodecs.STRING_UTF8, SaveMobPacket::mainHand, ByteBufCodecs.STRING_UTF8, SaveMobPacket::offHand, ByteBufCodecs.STRING_UTF8, SaveMobPacket::head, ByteBufCodecs.STRING_UTF8, SaveMobPacket::chest, ByteBufCodecs.STRING_UTF8, SaveMobPacket::legs, ByteBufCodecs.STRING_UTF8, SaveMobPacket::feet, SaveMobPacket::new);
    @Override public Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() { return TYPE; }
    public static void handle(SaveMobPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> UndooMod.SAVED_MOBS.add(new SavedMob(packet.name(), packet.entityId(), packet.mainHand(), packet.offHand(), packet.head(), packet.chest(), packet.legs(), packet.feet())));
    }
}

record SpawnSavedMobPacket(int index) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
    public static final Type<SpawnSavedMobPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(UndooMod.MODID, "spawn_saved_mob"));
    public static final net.minecraft.network.codec.StreamCodec<FriendlyByteBuf, SpawnSavedMobPacket> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.composite(ByteBufCodecs.INT, SpawnSavedMobPacket::index, SpawnSavedMobPacket::new);
    @Override public Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() { return TYPE; }
    public static void handle(SpawnSavedMobPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp && packet.index() < UndooMod.SAVED_MOBS.size()) {
                MobSpawnerHelper.spawnCustomMob(sp.level(), sp.getX(), sp.getY(), sp.getZ(), UndooMod.SAVED_MOBS.get(packet.index()));
            }
        });
    }
}

record DeleteSavedMobPacket(int index) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
    public static final Type<DeleteSavedMobPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(UndooMod.MODID, "delete_saved_mob"));
    public static final net.minecraft.network.codec.StreamCodec<FriendlyByteBuf, DeleteSavedMobPacket> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.composite(ByteBufCodecs.INT, DeleteSavedMobPacket::index, DeleteSavedMobPacket::new);
    @Override public Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() { return TYPE; }
    public static void handle(DeleteSavedMobPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> { if (packet.index() < UndooMod.SAVED_MOBS.size()) UndooMod.SAVED_MOBS.remove(packet.index()); });
    }
}

// ================= ЛОГИКА =================

record SavedMob(String name, String entityId, String mainHand, String offHand, String head, String chest, String legs, String feet) {}

class MobSpawnerHelper {
    public static void spawnCustomMob(Level level, double x, double y, double z, SavedMob saved) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(Identifier.parse(saved.entityId()));
        if (type == null) return;
        var entity = type.create(level);
        if (entity instanceof Mob mob) {
            mob.setPos(x, y, z);
            if (!saved.mainHand().isEmpty()) mob.setItemSlot(EquipmentSlot.MAINHAND, createItem(saved.mainHand()));
            if (!saved.offHand().isEmpty()) mob.setItemSlot(EquipmentSlot.OFFHAND, createItem(saved.offHand()));
            if (!saved.head().isEmpty()) mob.setItemSlot(EquipmentSlot.HEAD, createItem(saved.head()));
            if (!saved.chest().isEmpty()) mob.setItemSlot(EquipmentSlot.CHEST, createItem(saved.chest()));
            if (!saved.legs().isEmpty()) mob.setItemSlot(EquipmentSlot.LEGS, createItem(saved.legs()));
            if (!saved.feet().isEmpty()) mob.setItemSlot(EquipmentSlot.FEET, createItem(saved.feet()));
            level.addFreshEntity(mob);
        }
    }
    private static ItemStack createItem(String id) {
        Item item = BuiltInRegistries.ITEM.get(Identifier.parse(id));
        return new ItemStack(item);
    }
}

// ================= ПРЕДМЕТЫ =================

class CommandBlocker {
    @SubscribeEvent public static void onCommand(CommandEvent event) {
        String cmd = event.getParseResults().getReader().getString();
        if (cmd.startsWith("give") && cmd.contains("undoo:")) event.setCanceled(true);
    }
}

class BigTntItem extends Item {
    public BigTntItem(Properties p) { super(p); }
    public static float getPower(ItemStack s) { return s.getOrDefault(UndooMod.EXPLOSION_POWER.get(), 4f); }
    public static void setPower(ItemStack s, float p) { s.set(UndooMod.EXPLOSION_POWER.get(), Math.max(1f, Math.min(500f, p))); }
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide()) level.explode(null, player.getX(), player.getY(), player.getZ(), getPower(stack), Level.ExplosionInteraction.TNT);
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide() && player instanceof ServerPlayer sp) sp.openMenu(new SimpleMenuProvider((id, inv, p) -> new TntMenu(id, inv), Component.literal("Сила взрыва")));
        return InteractionResult.SUCCESS;
    }
    public void appendHoverText(ItemStack s, TooltipContext c, List<Component> t, TooltipFlag f) {
        t.add(Component.literal("Сила: " + (int) getPower(s)));
        t.add(Component.literal("ПКМ — меню | Shift+ПКМ — взрыв"));
    }
}

class MegaEnchanterItem extends Item {
    public MegaEnchanterItem(Properties p) { super(p); }
    public InteractionResult use(Level l, Player p, InteractionHand h) {
        if (!l.isClientSide() && p instanceof ServerPlayer sp) sp.openMenu(new SimpleMenuProvider((id, inv, player) -> new EnchanterMenu(id, inv), Component.literal("Мега-Зачарователь")));
        return InteractionResult.SUCCESS;
    }
}

class FastsettingMobItem extends Item {
    public FastsettingMobItem(Properties p) { super(p); }
    public InteractionResult use(Level l, Player p, InteractionHand h) {
        if (!l.isClientSide() && p instanceof ServerPlayer sp) sp.openMenu(new SimpleMenuProvider((id, inv, player) -> new MobConstructorMenu(id, inv), Component.literal("Конструктор Мобов")));
        return InteractionResult.SUCCESS;
    }
}

class MagicWandItem extends Item {
    public MagicWandItem(Properties p) { super(p); }
    public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) { return InteractionResult.SUCCESS; }
}

// ================= МЕНЮ =================

class TntMenu extends AbstractContainerMenu {
    public TntMenu(int id, Inventory inv) { super(UndooMod.TNT_MENU.get(), id); Container c = new SimpleContainer(1); this.addSlot(new Slot(c, 0, 80, 20) { public boolean mayPlace(ItemStack s) { return false; } }); }
    public ItemStack quickMoveStack(Player p, int i) { return ItemStack.EMPTY; }
    public boolean stillValid(Player p) { return true; }
}

class EnchanterMenu extends AbstractContainerMenu {
    public EnchanterMenu(int id, Inventory inv) { super(UndooMod.ENCHANTER_MENU.get(), id); }
    public ItemStack quickMoveStack(Player p, int i) { return ItemStack.EMPTY; }
    public boolean stillValid(Player p) { return true; }
}

class TradeMenu extends AbstractContainerMenu {
    public TradeMenu(int id, Inventory inv) 
