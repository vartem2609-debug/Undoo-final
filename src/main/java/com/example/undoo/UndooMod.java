package com.example.undoo;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Collections;

@Mod(UndooMod.MODID)
public class UndooMod {
    public static final String MODID = "undoo";
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, MODID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MODID);

    public static final DeferredHolder<Block, Block> NETHER_BLOCK = BLOCKS.register("nether_portal_block", () -> new PortalBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(1.0f).sound(SoundType.GLASS).setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MODID, "nether_portal_block"))), true));
    public static final DeferredHolder<Block, Block> END_BLOCK = BLOCKS.register("end_portal_block", () -> new PortalBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(1.0f).sound(SoundType.GLASS).setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MODID, "end_portal_block"))), false));

    public static final DeferredHolder<Item, Item> NETHER_ITEM = ITEMS.register("nether_portal", () -> new BlockItem(NETHER_BLOCK.get(), new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, "nether_portal"))).component(DataComponents.ITEM_MODEL, Identifier.fromNamespaceAndPath(MODID, "nether_portal"))));
    public static final DeferredHolder<Item, Item> END_ITEM = ITEMS.register("end_portal", () -> new BlockItem(END_BLOCK.get(), new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, "end_portal"))).component(DataComponents.ITEM_MODEL, Identifier.fromNamespaceAndPath(MODID, "end_portal"))));
    public static final DeferredHolder<Item, Item> TNT = ITEMS.register("big_tnt", () -> new BigTntItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, "big_tnt"))).component(DataComponents.ITEM_MODEL, Identifier.fromNamespaceAndPath(MODID, "big_tnt"))));
    public static final DeferredHolder<Item, Item> ENCHANTER = ITEMS.register("mega_enchanter", () -> new MegaEnchanterItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, "mega_enchanter"))).component(DataComponents.ITEM_MODEL, Identifier.fromNamespaceAndPath(MODID, "mega_enchanter"))));
    public static final DeferredHolder<Item, Item> MOB = ITEMS.register("fastsetting_mob", () -> new FastsettingMobItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, "fastsetting_mob"))).component(DataComponents.ITEM_MODEL, Identifier.fromNamespaceAndPath(MODID, "fastsetting_mob"))));
    public static final DeferredHolder<Item, Item> WAND = ITEMS.register("magic_wand", () -> new MagicWandItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, "magic_wand"))).component(DataComponents.ITEM_MODEL, Identifier.fromNamespaceAndPath(MODID, "magic_wand"))));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("undoo_tab", () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.undoo")).icon(() -> new ItemStack(TNT.get())).displayItems((params, out) -> {
        out.accept(TNT.get()); out.accept(NETHER_ITEM.get()); out.accept(END_ITEM.get()); out.accept(ENCHANTER.get()); out.accept(MOB.get()); out.accept(WAND.get());
        out.accept(Items.LAVA_BUCKET); out.accept(Items.WATER_BUCKET);
        ItemStack sword = new ItemStack(Items.NETHERITE_SWORD);
        var reg = params.holders().lookupOrThrow(Registries.ENCHANTMENT);
        var mut = new net.minecraft.world.item.enchantment.ItemEnchantments.Mutable(net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);
        reg.listElements().forEach(h -> mut.set(h, 255));
        sword.set(DataComponents.ENCHANTMENTS, mut.toImmutable());
        out.accept(sword);
    }).build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Float>> POWER = COMPONENTS.register("explosion_power", () -> DataComponentType.<Float>builder().persistent(Codec.FLOAT).networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.FLOAT).build());

    public UndooMod(IEventBus bus) {
        ITEMS.register(bus); BLOCKS.register(bus); TABS.register(bus); COMPONENTS.register(bus);
        Menus.register(bus); Packets.register(bus);
        NeoForge.EVENT_BUS.register(CommandBlocker.class);
    }
}

class PortalBlock extends Block {
    private final boolean isNether;
    public PortalBlock(Properties p, boolean isNether) {
        super(p);
        this.isNether = isNether;
    }

    @Override
    public InteractionResult useWithoutItem(net.minecraft.world.level.block.state.BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            if (isNether) {
                ServerLevel nether = sp.level().getServer().getLevel(Level.NETHER);
                if (nether != null) {
                    sp.teleportTo(nether, sp.getX() / 8, 100, sp.getZ() / 8, Collections.emptySet(), sp.getYRot(), sp.getXRot(), true);
                }
            } else {
                ServerLevel end = sp.level().getServer().getLevel(Level.END);
                if (end != null) {
                    sp.teleportTo(end, 1000, 100, 0, Collections.emptySet(), sp.getYRot(), sp.getXRot(), true);
                }
            }
        }
        return InteractionResult.SUCCESS;
    }
}

class CommandBlocker { @SubscribeEvent public static void onCmd(CommandEvent e) { if (e.getParseResults().getReader().getString().startsWith("give") && e.getParseResults().getReader().getString().contains("undoo:")) e.setCanceled(true); } }
