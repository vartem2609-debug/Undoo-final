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
import net.minecraft.world.effect.MobEffectInstance;
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

// --- ЭКРАНЫ ---

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
    private EditBox in, out, inEnch, outEnch, lvl;
    public TradeScreen(TradeMenu m, Inventory i, Component t) { super(m, i, t); }
    public void init() {
        super.init(); int cx = width / 2, cy = height / 2;
        in = new EditBox(minecraft.font, cx - 120, cy - 80, 240, 20, Component.literal("Вход (ID)"));
        inEnch = new EditBox(minecraft.font, cx - 120, cy - 55, 160, 20, Component.literal("Чар (ID)"));
        lvl = new EditBox(minecraft.font, cx + 45, cy - 55, 75, 20, Component.literal("Ур."));
        out = new EditBox(minecraft.font, cx - 120, cy - 30, 240, 20, Component.literal("Выход (ID)"));
        outEnch = new EditBox(minecraft.font, cx - 120, cy - 5, 160, 20, Component.literal("Чар (ID)"));
        
        addRenderableWidget(in); addRenderableWidget(inEnch); addRenderableWidget(lvl);
        addRenderableWidget(out); addRenderableWidget(outEnch);
        
        addRenderableWidget(Button.builder(Component.literal("Применить"), b -> {
            if (minecraft != null && minecraft.player != null) { 
                ClientPacketDistributor.sendToServer(new SetTradePacket(in.getValue(), inEnch.getValue(), lvl.getValue(), out.getValue(), outEnch.getValue())); 
                minecraft.player.closeContainer(); 
            }
        }).bounds(cx - 80, cy + 30, 160, 20).build());
    }
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float pt) { super.extractRenderState(g, mx, my, pt); }
}

class MobConstructorScreen extends AbstractContainerScreen<MobConstructorMenu> {
    private EditBox name, ent, mh, mhEnch, oh, ohEnch, hd, hdEnch, ch, chEnch, lg, lgEnch, ft, ftEnch, effects;
    public MobConstructorScreen(MobConstructorMenu m, Inventory i, Component t) { super(m, i, t); }
    public void init() {
        super.init(); int cx = width / 2 - 120, cy = height / 2 - 140;
        name = new EditBox(minecraft.font, cx, cy, 100, 20, Component.literal("Имя"));
        ent = new EditBox(minecraft.font, cx + 110, cy, 120, 20, Component.literal("ID моба"));
        
        mh = new EditBox(minecraft.font, cx, cy + 25, 140, 20, Component.literal("Рука"));
        mhEnch = new EditBox(minecraft.font, cx + 145, cy + 25, 85, 20, Component.literal("Чар"));
        oh = new EditBox(minecraft.font, cx, cy + 50, 140, 20, Component.literal("Левая"));
        ohEnch = new EditBox(minecraft.font, cx + 145, cy + 50, 85, 20, Component.literal("Чар"));
        hd = new EditBox(minecraft.font, cx, cy + 75, 140, 20, Component.literal("Шлем"));
        hdEnch = new EditBox(minecraft.font, cx + 145, cy + 75, 85, 20, Component.literal("Чар"));
        ch = new EditBox(minecraft.font, cx, cy + 100, 140, 20, Component.literal("Нагрудник"));
        chEnch = new EditBox(minecraft.font, cx + 145, cy + 100, 85, 20, Component.literal("Чар"));
        lg = new EditBox(minecraft.font, cx, cy + 125, 140, 20, Component.literal("Штаны"));
        lgEnch = new EditBox(minecraft.font, cx + 145, cy + 125, 85, 20, Component.literal("Чар"));
        ft = new EditBox(minecraft.font, cx, cy + 150, 140, 20, Component.literal("Ботинки"));
        ftEnch = new EditBox(minecraft.font, cx + 145, cy + 150, 85, 20, Component.literal("Чар"));
        
        effects = new EditBox(minecraft.font, cx, cy + 175, 230, 20, Component.literal("Эффекты (id:уровень)"));

        addRenderableWidget(name); addRenderableWidget(ent);
        addRenderableWidget(mh); addRenderableWidget(mhEnch); addRenderableWidget(oh); addRenderableWidget(ohEnch);
        addRenderableWidget(hd); addRenderableWidget(hdEnch); addRenderableWidget(ch); addRenderableWidget(chEnch);
        addRenderableWidget(lg); addRenderableWidget(lgEnch); addRenderableWidget(ft); addRenderableWidget(ftEnch);
        addRenderableWidget(effects);

        addRenderableWidget(Button.builder(Component.literal("Сохранить моба"), b -> {
            ClientPacketDistributor.sendToServer(new SaveMobPacket(
                name.getValue(), ent.getValue(), 
                mh.getValue(), mhEnch.getValue(), oh.getValue(), ohEnch.getValue(),
                hd.getValue(), hdEnch.getValue(), ch.getValue(), chEnch.getValue(),
                lg.getValue(), lgEnch.getValue(), ft.getValue(), ftEnch.getValue(),
                effects.getValue()
            ));
            if (minecraft != null && minecraft.player != null) { minecraft.player.closeContainer(); }
        }).bounds(cx + 50, cy + 205, 130, 20).build());
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

// --- ПАКЕТЫ И ДАННЫЕ ---

record SavedMob(
    String name, String entityId,
    String mainHand, String mainHandEnch, String offHand, String offHandEnch,
    String head, String headEnch, String chest, String chestEnch,
    String legs, String legsEnch, String feet, String feetEnch,
    String effects
) {}

record SetTradePacket(String in, String inEnch, String lvl, String out, String outEnch) implements CustomPacketPayload {
    public static final Type<SetTradePacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(UndooMod.MODID, "set_trade"));
    public static final StreamCodec<FriendlyByteBuf, SetTradePacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, SetTradePacket::in,
        ByteBufCodecs.STRING_UTF8, SetTradePacket::inEnch,
        ByteBufCodecs.STRING_UTF8, SetTradePacket::lvl,
        ByteBufCodecs.STRING_UTF8, SetTradePacket::out,
        ByteBufCodecs.STRING_UTF8, SetTradePacket::outEnch,
        SetTradePacket::new
    );
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(SetTradePacket p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> { 
            if (ctx.player() instanceof ServerPlayer sp) { 
                var v = sp.level().getEntitiesOfClass(Villager.class, sp.getBoundingBox().inflate(5)).stream().findFirst().orElse(null); 
                if (v != null) { 
                    var inItem = BuiltInRegistries.ITEM.get(Identifier.parse(p.in()));
                    var outItem = BuiltInRegistries.ITEM.get(Identifier.parse(p.out()));
                    
                    if (inItem.isPresent() && outItem.isPresent()) {
                        ItemStack inStack = new ItemStack(inItem.get().value(), 1);
                        ItemStack outStack = new ItemStack(outItem.get().value(), 1);
                        
                        int level = 1;
                        try { level = Integer.parseInt(p.lvl()); } catch (Exception ignored) {}
                        
                        if (!p.inEnch().isEmpty()) {
                            var enchOpt = sp.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(Identifier.parse(p.inEnch()));
                            if (enchOpt.isPresent()) {
                                ItemEnchantments.Mutable mut = new ItemEnchantments.Mutable(inStack.getOrDefault(net.minecraft.core.component.DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY));
                                mut.set(enchOpt.get(), level);
                                inStack.set(net.minecraft.core.component.DataComponents.ENCHANTMENTS, mut.toImmutable());
                            }
                        }
                        
                        if (!p.outEnch().isEmpty()) {
                            var enchOpt = sp.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(Identifier.parse(p.outEnch()));
                            if (enchOpt.isPresent()) {
                                ItemEnchantments.Mutable mut = new ItemEnchantments.Mutable(outStack.getOrDefault(net.minecraft.core.component.DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY));
                                mut.set(enchOpt.get(), level);
                                outStack.set(net.minecraft.core.component.DataComponents.ENCHANTMENTS, mut.toImmutable());
                            }
                        }
                        
                        var o = new MerchantOffers(); 
                        o.add(new MerchantOffer(new ItemCost(inItem.get().value(), 1), outStack, 100, 1, 0.05f)); 
                        v.setOffers(o); 
                    }
                } 
            } 
        });
    }
}

record SaveMobPacket(
    String name, String entityId,
    String mainHand, String mainHandEnch, String offHand, String offHandEnch,
    String head, String headEnch, String chest, String chestEnch,
    String legs, String legsEnch, String feet, String feetEnch,
    String effects
) implements CustomPacketPayload {
    public static final Type<SaveMobPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(UndooMod.MODID, "save_mob"));
    public static final StreamCodec<FriendlyByteBuf, SaveMobPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, SaveMobPacket::name,
        ByteBufCodecs.STRING_UTF8, SaveMobPacket::entityId,
        ByteBufCodecs.STRING_UTF8, SaveMobPacket::mainHand,
        ByteBufCodecs.STRING_UTF8, SaveMobPacket::mainHandEnch,
        ByteBufCodecs.STRING_UTF8, SaveMobPacket::offHand,
        ByteBufCodecs.STRING_UTF8, SaveMobPacket::offHandEnch,
        ByteBufCodecs.STRING_UTF8, SaveMobPacket::head,
        ByteBufCodecs.STRING_UTF8, SaveMobPacket::headEnch,
        ByteBufCodecs.STRING_UTF8, SaveMobPacket::chest,
        ByteBufCodecs.STRING_UTF8, SaveMobPacket::chestEnch,
        ByteBufCodecs.STRING_UTF8, SaveMobPacket::legs,
        ByteBufCodecs.STRING_UTF8, SaveMobPacket::legsEnch,
        ByteBufCodecs.STRING_UTF8, SaveMobPacket::feet,
        ByteBufCodecs.STRING_UTF8, SaveMobPacket::feetEnch,
        ByteBufCodecs.STRING_UTF8, SaveMobPacket::effects,
        SaveMobPacket::new
    );
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(SaveMobPacket p, IPayloadContext ctx) { 
        ctx.enqueueWork(() -> UndooMod.SAVED.add(new SavedMob(
            p.name(), p.entityId(),
            p.mainHand(), p.mainHandEnch(), p.offHand(), p.offHandEnch(),
            p.head(), p.headEnch(), p.chest(), p.chestEnch(),
            p.legs(), p.legsEnch(), p.feet(), p.feetEnch(),
            p.effects()
        ))); 
    }
}

record SpawnMobPacket(int idx) implements CustomPacketPayload {
    public static final Type<SpawnMobPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(UndooMod.MODID, "spawn_mob"));
    public static final StreamCodec<FriendlyByteBuf, SpawnMobPacket> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.INT, SpawnMobPacket::idx, SpawnMobPacket::new);
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
    
    public static void handle(SpawnMobPacket p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> { 
            if (ctx.player() instanceof ServerPlayer sp && p.idx() >= 0 && p.idx() < UndooMod.SAVED.size()) { 
                SavedMob m = UndooMod.SAVED.get(p.idx()); 
                var entityTypeOpt = BuiltInRegistries.ENTITY_TYPE.get(Identifier.parse(m.entityId()));
                if (entityTypeOpt.isPresent()) {
                    var e = entityTypeOpt.get().value().create(sp.level(), EntitySpawnReason.MOB_SUMMONED); 
                    if (e instanceof Mob mob) { 
                        mob.setPos(sp.getX(), sp.getY(), sp.getZ()); 
                        
                        applyEquip(mob, EquipmentSlot.MAINHAND, m.mainHand(), m.mainHandEnch(), sp);
                        applyEquip(mob, EquipmentSlot.OFFHAND, m.offHand(), m.offHandEnch(), sp);
                        applyEquip(mob, EquipmentSlot.HEAD, m.head(), m.headEnch(), sp);
                        applyEquip(mob, EquipmentSlot.CHEST, m.chest(), m.chestEnch(), sp);
                        applyEquip(mob, EquipmentSlot.LEGS, m.legs(), m.legsEnch(), sp);
                        applyEquip(mob, EquipmentSlot.FEET, m.feet(), m.feetEnch(), sp);
                        
                        if (!m.effects().isEmpty()) {
                            for (String effectStr : m.effects().split(",")) {
                                String[] parts = effectStr.split(":");
                                if (parts.length >= 1) {
                                    var effectOpt = BuiltInRegistries.MOB_EFFECT.get(Identifier.parse(parts[0].trim()));
                                    if (effectOpt.isPresent()) {
                                        int amp = 1;
                                        if (parts.length == 2) {
                                            try { amp = Integer.parseInt(parts[1].trim()); } catch (Exception ignored) {}
                                        }
                                        mob.addEffect(new MobEffectInstance(effectOpt.get(), 9999999, amp - 1));
                                    }
                                }
                            }
                        }
                        if (!m.name().isEmpty()) {package com.example.undoo;

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
import net.minecraft.world.effect.MobEffectInstance;
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

// --- ЭКРАНЫ ---

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
    private EditBox in, out, inEnch, outEnch, lvl;
    public TradeScreen(TradeMenu m, Inventory i, Component t) { super(m, i, t); }
    public void init() {
        super.init(); int cx = width / 2, cy = height / 2;
        in = new EditBox(minecraft.font, cx - 120, cy - 80, 240, 20, Component.literal("Вход (ID)"));
        inEnch = new EditBox(minecraft.font, cx - 120, cy - 55, 160, 20, Component.literal("Чар (ID)"));
        lvl = new EditBox(minecraft.font, cx + 45, cy - 55, 75, 20, Component.literal("Ур."));
        out = new EditBox(minecraft.font, cx - 120, cy - 30, 240, 20, Component.literal("Выход (ID)"));
        outEnch = new EditBox(minecraft.font, cx - 120, cy - 5, 160, 20, Component.literal("Чар (ID)"));
        
        addRenderableWidget(in); addRenderableWidget(inEnch); addRenderableWidget(lvl);
        addRenderableWidget(out); addRenderableWidget(outEnch);
        
        addRenderableWidget(Button.builder(Component.literal("Применить"), b -> {
            if (minecraft != null && minecraft.player != null) { 
                ClientPacketDistributor.sendToServer(new SetTradePacket(in.getValue(), inEnch.getValue(), lvl.getValue(), out.getValue(), outEnch.getValue())); 
                minecraft.player.closeContainer(); 
            }
        }).bounds(cx - 80, cy + 30, 160, 20).build());
    }
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float pt) { super.extractRenderState(g, mx, my, pt); }
}

class MobConstructorScreen extends AbstractContainerScreen<MobConstructorMenu> {
    private EditBox name, ent, mh, mhEnch, oh, ohEnch, hd, hdEnch, ch, chEnch, lg, lgEnch, ft, ftEnch, effects;
    public MobConstructorScreen(MobConstructorMenu m, Inventory i, Component t) { super(m, i, t); }
    public void init() {
        super.init(); int cx = width / 2 - 120, cy = height / 2 - 140;
        name = new EditBox(minecraft.font, cx, cy, 100, 20, Component.literal("Имя"));
        ent = new EditBox(minecraft.font, cx + 110, cy, 120, 20, Component.literal("ID моба"));
        
        mh = new EditBox(minecraft.font, cx, cy + 25, 140, 20, Component.literal("Рука"));
        mhEnch = new EditBox(minecraft.font, cx + 145, cy + 25, 85, 20, Component.literal("Чар"));
        oh = new EditBox(minecraft.font, cx, cy + 50, 140, 20, Component.literal("Левая"));
        ohEnch = new EditBox(minecraft.font, cx + 145, cy + 50, 85, 20, Component.literal("Чар"));
        hd = new EditBox(minecraft.font, cx, cy + 75, 140, 20, Component.literal("Шлем"));
        hdEnch = new EditBox(minecraft.font, cx + 145, cy + 75, 85, 20, Component.literal("Чар"));
        ch = new EditBox(minecraft.font, cx, cy + 100, 140, 20, Component.literal("Нагрудник"));
        chEnch = new EditBox(minecraft.font, cx + 145, cy + 100, 85, 20, Component.literal("Чар"));
        lg = new EditBox(minecraft.font, cx, cy + 125, 140, 20, Component.literal("Штаны"));
        lgEnch = new EditBox(minecraft.font, cx + 145, cy + 125, 85, 20, Component.literal("Чар"));
        ft = new EditBox(minecraft.font, cx, cy + 150, 140, 20, Component.literal("Ботинки"));
        ftEnch = new EditBox(minecraft.font, cx + 145, cy + 150, 85, 20, Component.literal("Чар"));
        
        effects = new EditBox(minecraft.font, cx, cy + 175, 230, 20, Component.literal("Эффекты (id:уровень,id:уровень)"));

        addRenderableWidget(name); addRenderableWidget(ent);
        addRenderableWidget(mh); addRenderableWidget(mhEnch); addRenderableWidget(oh); addRenderableWidget(ohEnch);
        addRenderableWidget(hd); addRenderableWidget(hdEnch); addRenderableWidget(ch); addRenderableWidget(chEnch);
        addRenderableWidget(lg); addRenderableWidget(lgEnch); addRenderableWidget(ft); addRenderableWidget(ftEnch);
        addRenderableWidget(effects);

        addRenderableWidget(Button.builder(Component.literal("Сохранить моба"), b -> {
            ClientPacketDistributor.sendToServer(new SaveMobPacket(
                name.getValue(), ent.getValue(), 
                mh.getValue(), mhEnch.getValue(), oh.getValue(), ohEnch.getValue(),
                hd.getValue(), hdEnch.getValue(), ch.getValue(), chEnch.getValue(),
                lg.getValue(), lgEnch.getValue(), ft.getValue(), ftEnch.getValue(),
                effects.getValue()
            ));
            if (minecraft != null && minecraft.player != null) { minecraft.player.closeContainer(); }
        }).bounds(cx + 50, cy + 205, 130, 20).build());
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

// --- ПАКЕТЫ И ДАННЫЕ ---

record SavedMob(
    String name, String entityId,
    String mainHand, String mainHandEnch, String offHand, String offHandEnch,
    String head, String headEnch, String chest, String chestEnch,
    String legs, String legsEnch, String feet, String feetEnch,
    String effects
) {}

record SetTradePacket(String in, String inEnch, String lvl, String out, String outEnch) implements CustomPacketPayload {
    public static final Type<SetTradePacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(UndooMod.MODID, "set_trade"));
    public static final StreamCodec<FriendlyByteBuf, SetTradePacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, SetTradePacket::in,
        ByteBufCodecs.STRING_UTF8, SetTradePacket::inEnch,
        ByteBufCodecs.STRING_UTF8, SetTradePacket::lvl,
        ByteBufCodecs.STRING_UTF8, SetTradePacket::out,
        ByteBufCodecs.STRING_UTF8, SetTradePacket::outEnch,
        SetTradePacket::new
    );
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(SetTradePacket p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> { 
            if (ctx.player() instanceof ServerPlayer sp) { 
                var v = sp.level().getEntitiesOfClass(Villager.class, sp.getBoundingBox().inflate(5)).stream().findFirst().orElse(null); 
                if (v != null) { 
                    var inItem = BuiltInRegistries.ITEM.get(Identifier.parse(p.in()));
                    var outItem = BuiltInRegistries.ITEM.get(Identifier.parse(p.out()));
                    
                    if (inItem.isPresent() && outItem.isPresent()) {
                        ItemStack inStack = new ItemStack(inItem.get().value(), 1);
                        ItemStack outStack = new ItemStack(outItem.get().value(), 1);
                        
                        int level = 1;
                        try { level = Integer.parseInt(p.lvl()); } catch (Exception ignored) {}
                        
                        if (!p.inEnch().isEmpty()) {
                            var enchOpt = sp.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(Identifier.parse(p.inEnch()));
                            if (enchOpt.isPresent()) {
                                ItemEnchantments.Mutable mut = new ItemEnchantments.Mutable(inStack.getOrDefault(net.minecraft.core.component.DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY));
                                mut.set(enchOpt.get(), level);
                                inStack.set(net.minecraft.core.component.DataComponents.ENCHANTMENTS, mut.toImmutable());
                            }
                        }
                        
                        if (!p.outEnch().isEmpty()) {
                            var enchOpt = sp.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(Identifier.parse(p.outEnch()));
                            if (enchOpt.isPresent()) {
                                ItemEnchantments.Mutable mut = new ItemEnchantments.Mutable(outStack.getOrDefault(net.minecraft.core.component.DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY));
                                mut.set(enchOpt.get(), level);
                                outStack.set(net.minecraft.core.component.DataComponents.ENCHANTMENTS, mut.toImmutable());
                            }
                        }
                        
                        var o = new MerchantOffers(); 
                        o.add(new MerchantOffer(new ItemCost(inItem.get().value(), 1), outStack, 100, 1, 0.05f)); 
                        v.setOffers(o); 
                    }
                } 
            } 
        });
    }
}

record SaveMobPacket(
    String name, String entityId,
    String mainHand, String mainHandEnch, String offHand, String offHandEnch,
    String head, String headEnch, String chest, String chestEnch,
    String legs, String legsEnch, String feet, String feetEnch,
    String effects
) implements CustomPacketPayload {
    public static final Type<SaveMobPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(UndooMod.MODID, "save_mob"));
    public static final StreamCodec<FriendlyByteBuf, SaveMobPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, SaveMobPacket::name,
        ByteBufCodecs.STRING_UTF8, SaveMobPacket::entityId,
        ByteBufCodecs.STRING_UTF8, SaveMobPacket::mainHand,
        ByteBufCodecs.STRING_UTF8, SaveMobPacket::mainHandEnch,
        ByteBufCodecs.STRING_UTF8, SaveMobPacket::offHand,
        ByteBufCodecs.STRING_UTF8, SaveMobPacket::offHandEnch,
        ByteBufCodecs.STRING_UTF8, SaveMobPacket::head,
        ByteBufCodecs.STRING_UTF8, SaveMobPacket::headEnch,
        ByteBufCodecs.STRING_UTF8, SaveMobPacket::chest,
        ByteBufCodecs.STRING_UTF8, SaveMobPacket::chestEnch,
        ByteBufCodecs.STRING_UTF8, SaveMobPacket::legs,
        ByteBufCodecs.STRING_UTF8, SaveMobPacket::legsEnch,
        ByteBufCodecs.STRING_UTF8, SaveMobPacket::feet,
        ByteBufCodecs.STRING_UTF8, SaveMobPacket::feetEnch,
        ByteBufCodecs.STRING_UTF8, SaveMobPacket::effects,
        SaveMobPacket::new
    );
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(SaveMobPacket p, IPayloadContext ctx) { 
        ctx.enqueueWork(() -> UndooMod.SAVED.add(new SavedMob(
            p.name(), p.entityId(),
            p.mainHand(), p.mainHandEnch(), p.offHand(), p.offHandEnch(),
            p.head(), p.headEnch(), p.chest(), p.chestEnch(),
            p.legs(), p.legsEnch(), p.feet(), p.feetEnch(),
            p.effects()
        ))); 
    }
}

record SpawnMobPacket(int idx) implements CustomPacketPayload {
    public static final Type<SpawnMobPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(UndooMod.MODID, "spawn_mob"));
    public static final StreamCodec<FriendlyByteBuf, SpawnMobPacket> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.INT, SpawnMobPacket::idx, SpawnMobPacket::new);
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
    
    public static void handle(SpawnMobPacket p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> { 
            if (ctx.player() instanceof ServerPlayer sp && p.idx() >= 0 && p.idx() < UndooMod.SAVED.size()) { 
                SavedMob m = UndooMod.SAVED.get(p.idx()); 
                var entityTypeOpt = BuiltInRegistries.ENTITY_TYPE.get(Identifier.parse(m.entityId()));
                if (entityTypeOpt.isPresent()) {
                    var e = entityTypeOpt.get().value().create(sp.level(), EntitySpawnReason.MOB_SUMMONED); 
                    if (e instanceof Mob mob) { 
                        mob.setPos(sp.getX(), sp.getY(), sp.getZ()); 
                        
                        applyEquip(mob, EquipmentSlot.MAINHAND, m.mainHand(), m.mainHandEnch(), sp);
                        applyEquip(mob, EquipmentSlot.OFFHAND, m.offHand(), m.offHandEnch(), sp);
                        applyEquip(mob, EquipmentSlot.HEAD, m.head(), m.headEnch(), sp);
                        applyEquip(mob, EquipmentSlot.CHEST, m.chest(), m.chestEnch(), sp);
                        applyEquip(mob, EquipmentSlot.LEGS, m.legs(), m.legsEnch(), sp);
                        applyEquip(mob, EquipmentSlot.FEET, m.feet(), m.feetEnch(), sp);
                        
                        if (!m.effects().isEmpty()) {
                            for (String effectStr : m.effects().split(",")) {
                                String[] parts = effectStr.split(":");
                                if (parts.length >= 1) {
                                    var effectOpt = BuiltInRegistries.MOB_EFFECT.get(Identifier.parse(parts[0].trim()));
                                    if (effectOpt.isPresent()) {
                                        int amp = 1;
                                        if (parts.length == 2) {
                                            try { amp = Integer.parseInt(parts[1].trim()); } catch (Exception ignored) {}
                                        }
                                        mob.addEffect(new MobEffectInstance(effectOpt.get(), 9999999, amp - 1));
                                    }
                                }
                            }
                        }
                        
                        if (!m.name().isEmpty()) {
                            mob.setCustomName(Component.literal(m.name()));
                            mob.setCustomNameVisible(true);
                        }
                        
                        sp.level().addFreshEntity(mob); 
                    } 
                }
            } 
        });
    }
    
    private static void applyEquip(Mob mob, EquipmentSlot slot, String itemId, String enchId, ServerPlayer sp) {
        if (itemId.isEmpty()) return;
        var itemOpt = BuiltInRegistries.ITEM.get(Identifier.parse(itemId));
        if (itemOpt.isPresent()) {
            ItemStack stack = new ItemStack(itemOpt.get().value());
            if (!enchId.isEmpty()) {
                var enchOpt = sp.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(Identifier.parse(enchId));
                if (enchOpt.isPresent()) {
                    ItemEnchantments.Mutable mut = new ItemEnchantments.Mutable(stack.getOrDefault(net.minecraft.core.component.DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY));
                    mut.set(enchOpt.get(), 255);
                    stack.set(net.minecraft.core.component.DataComponents.ENCHANTMENTS, mut.toImmutable(
