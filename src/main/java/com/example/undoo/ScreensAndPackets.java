package com.example.undoo;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.bus.api.IEventBus;
import java.util.ArrayList;
import java.util.List;

class Packets {
    public static void register(IEventBus bus) {
        bus.addListener((RegisterPayloadHandlersEvent e) -> {
            PayloadRegistrar reg = e.registrar(UndooMod.MODID);
            reg.playToServer(SetTradePacket.TYPE, SetTradePacket.STREAM_CODEC, SetTradePacket::handle);
            reg.playToServer(SaveMobPacket.TYPE, SaveMobPacket.STREAM_CODEC, SaveMobPacket::handle);
            reg.playToServer(SpawnMobPacket.TYPE, SpawnMobPacket.STREAM_CODEC, SpawnMobPacket::handle);
            reg.playToServer(DeleteMobPacket.TYPE, DeleteMobPacket.STREAM_CODEC, DeleteMobPacket::handle);
        });
    }
}

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

class TradeScreen extends AbstractContainerScreen<TradeMenu> {
    private EditBox in, out;
    public TradeScreen(TradeMenu m, Inventory i, Component t) { super(m, i, t); }
    public void init() {
        super.init(); int cx = width / 2, cy = height / 2;
        in = new EditBox(minecraft.font, cx - 100, cy - 60, 200, 20, Component.literal("Вход"));
        out = new EditBox(minecraft.font, cx - 100, cy - 30, 200, 20, Component.literal("Выход"));
        addRenderableWidget(in); addRenderableWidget(out);
        addRenderableWidget(Button.builder(Component.literal("Применить"), b -> {
            if (minecraft != null && minecraft.player != null) { ClientPacketDistributor.sendToServer(new SetTradePacket(in.getValue(), out.getValue())); minecraft.player.closeContainer(); }
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
            ClientPacketDistributor.sendToServer(new SaveMobPacket(name.getValue(), ent.getValue(), mh.getValue(), oh.getValue(), hd.getValue(), ch.getValue(), lg.getValue(), ft.getValue()));
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
            addRenderableWidget(Button.builder(Component.literal("Спавн"), b -> ClientPacketDistributor.sendToServer(new SpawnMobPacket(idx))).bounds(cx, cy + y, 80, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Удалить"), b -> { ClientPacketDistributor.sendToServer(new DeleteMobPacket(idx)); if (minecraft != null && minecraft.player != null) { minecraft.player.closeContainer(); minecraft.player.openMenu(new SimpleMenuProvider((id, inv, pl) -> new SavedMobsMenu(id, inv), Component.literal("Мобы"))); } }).bounds(cx + 85, cy + y, 80, 20).build());
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
    public static void handle(SetTradePacket p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> { 
            if (ctx.player() instanceof ServerPlayer sp) { 
                var v = sp.level().getEntitiesOfClass(Villager.class, sp.getBoundingBox().inflate(5)).stream().findFirst().orElse(null); 
                if (v != null) { 
                    var inOpt = BuiltInRegistries.ITEM.get(Identifier.parse(p.in()));
                    var outOpt = BuiltInRegistries.ITEM.get(Identifier.parse(p.out()));
                    if (inOpt.isPresent() && outOpt.isPresent()) {
                        var o = new MerchantOffers(); 
                        o.add(new MerchantOffer(new ItemCost(inOpt.get().value(), 1), new ItemStack(outOpt.get().value(), 1), 100, 1, 0.05f)); 
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
                    var e = entityTypeOpt.get().value().create(sp.level(), EntitySpawnReason.MOB_SUMMONED); 
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
