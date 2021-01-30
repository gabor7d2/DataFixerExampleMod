package com.example.datafixer.blocks;

import com.example.datafixer.ExampleMod;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.LinkedHashMap;

@Mod.EventBusSubscriber
public class RegistryHandler {

    public static final LinkedHashMap<ResourceLocation, Block> BLOCKS = new LinkedHashMap<>();
    public static final LinkedHashMap<ResourceLocation, Item> ITEMS = new LinkedHashMap<>();

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        ExampleMod.logger.debug("Registering blocks");
        createBlocks();
        BLOCKS.forEach((name, block) -> event.getRegistry().register(block));
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        ExampleMod.logger.debug("Registering items");
        ITEMS.forEach((name, item) -> event.getRegistry().register(item));
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void registerModels(ModelRegistryEvent event) {
        ExampleMod.logger.debug("Registering models");
        ITEMS.forEach((name, item) -> ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(name, "inventory")));
    }

    private static void createBlocks() {
        Block test = createBlock("test1", new Block(Material.ROCK));
        //Block test = createBlock("test2", new Block(Material.ROCK));

        BLOCKS.put(test.getRegistryName(), test);
    }

    public static <T extends Item> T createItem(String name, T item) {
        item.setRegistryName(name);
        return item;
    }

    private static <T extends Block> T createBlock(String name, T block) {
        block.setRegistryName(name);
        block.setUnlocalizedName(name);
        block.setCreativeTab(CreativeTabs.DECORATIONS);
        ITEMS.put(block.getRegistryName(), new ItemBlock(block).setRegistryName(block.getRegistryName()));
        return block;
    }
}
