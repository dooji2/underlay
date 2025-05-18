package com.dooji.underlay.main;

import com.dooji.underlay.main.network.UnderlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(Underlay.MOD_ID)
public class Underlay {
    public static final String MOD_ID = "underlay";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final TagKey<Block> OVERLAY_TAG = TagKey.create(BuiltInRegistries.BLOCK.key(), ResourceLocation.fromNamespaceAndPath(MOD_ID, "overlay"));
    private static final TagKey<Block> EXCLUDE_TAG = TagKey.create(BuiltInRegistries.BLOCK.key(), ResourceLocation.fromNamespaceAndPath(MOD_ID, "exclude"));

    public Underlay(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        modEventBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(UnderlayNetworking::init);
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        for (ServerLevel world : event.getServer().getAllLevels()) {
            LOGGER.info("Loading overlays for world: " + world.dimension().location());
            UnderlayManager.loadOverlays(world);
            reloadDatapackBlocks(world);
        }
    }

    private static void reloadDatapackBlocks(ServerLevel world) {
        UnderlayApi.CUSTOM_BLOCKS_DP.clear();
        var blocks = world.registryAccess().registryOrThrow(BuiltInRegistries.BLOCK.key());

        blocks.getTag(OVERLAY_TAG).ifPresent(tag -> tag.forEach(entry -> {
            Block block = entry.value();
            if (!blocks.getOrCreateTag(EXCLUDE_TAG).contains(entry)) {
                UnderlayApi.registerDatapackOverlayBlock(block);
            }
        }));
    }
}
