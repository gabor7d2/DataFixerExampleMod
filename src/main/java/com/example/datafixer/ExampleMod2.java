package com.example.datafixer;

/*
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.datafix.FixTypes;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ModFixs;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;

@Mod(modid = ExampleMod.MODID, name = ExampleMod.NAME, version = ExampleMod.VERSION)
public class ExampleMod2
{
    public static final String MODID = "examplemod";
    public static final String NAME = "Example Mod";
    public static final String VERSION = "1.1";

    public static Logger logger;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();
        MinecraftForge.EVENT_BUS.register(this);
    }

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        final ModFixs modFixs = FMLCommonHandler.instance().getDataFixer().init(ExampleMod.MODID, 8);
        modFixs.registerFix(FixTypes.CHUNK, new BlockFix());
        modFixs.registerFix(FixTypes.ITEM_INSTANCE, new ItemFlattening());
        modFixs.registerFix(FixTypes.BLOCK_ENTITY, new BlockEntityFix());
        modFixs.registerFix(FixTypes.ENTITY, new EntityFix());
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        //event.getWorld().getWorldInfo().getAdditionalProperty("FML");
        //logger.debug(event.getWorld().getWorldInfo().getWorldName());

        */
/*if (!event.getWorld().isRemote) {
            ExampleWorldSavedData data = ExampleWorldSavedData.get(event.getWorld());
            logger.log(Level.DEBUG, data.mapName);
            logger.log(Level.DEBUG, data.test);
        }*//*

        if (!event.getWorld().isRemote) {
            try {
                File saveDir = event.getWorld().getSaveHandler().getWorldDirectory();
                if (!saveDir.exists()) {
                    logger.debug("Savedir doesn't exist!");
                    return;
                }
                File levelDat = new File(saveDir, "level.dat");
                if (!levelDat.exists()) {
                    logger.debug("Level.dat doesn't exist!");
                    return;
                }
                NBTTagCompound nbt = CompressedStreamTools.readCompressed(new FileInputStream(levelDat));
                NBTTagCompound fmlTag = nbt.getCompoundTag("FML");

                if (fmlTag.hasKey("ModList"))
                {
                    NBTTagList modList = fmlTag.getTagList("ModList", (byte)10);
                    for (int i = 0; i < modList.tagCount(); i++)
                    {
                        NBTTagCompound mod = modList.getCompoundTagAt(i);
                        if (!mod.getString("ModId").equals("examplemod")) continue;
                        logger.debug("Examplemod version in save is: " + mod.getString("ModVersion"));
                    }
                } else logger.debug("Nbt doesnt have ModList key");
            } catch (Exception e) {
                logger.error("Error on WorldLoad");
                e.printStackTrace();
            }
        }
    }

    @SubscribeEvent(priority= EventPriority.NORMAL, receiveCanceled=true)
    public void onEvent(ChunkEvent.Load event)
    {
        */
/*Chunk chunk = event.getChunk();
        ChunkPos chunkPos = chunk.getPos();

        for (TileEntity te : chunk.getTileEntityMap().values()) {
            BlockPos absolutePos = te.getPos();
            logger.debug("Found TE at {} {} {} - {}:{} ", absolutePos.getX(), absolutePos.getY(), absolutePos.getZ(), te.getBlockType().getRegistryName(), te.getBlockMetadata());
            logger.debug("Tile data: {}", te.serializeNBT());
        }*//*


        */
/*for (int x = 0; x < 16; ++x)
        {
            for (int z = 0; z < 16; ++z)
            {
                for (int y = chunk.getHeightValue(x, z)-20; y < chunk.getHeightValue(x, z)+1; ++y)
                {
                    IBlockState blockState = chunk.getBlockState(x, y, z);
                    int metadata = blockState.getBlock().getMetaFromState(blockState);
                    String name = blockState.getBlock().getRegistryName().toString();
                    if (name.equals("minecraft:stone"))
                    {
                        BlockPos absolutePos = chunkPos.getBlock(x, y, z);
                        if (x == 15 && z == 15) {
                            //logger.debug("Replacing block at {} {} {} (minecraft:stone:{})", absolutePos.getX(), absolutePos.getY(), absolutePos.getZ(), metadata);
                        }
                        //theChunk.setBlockState(new BlockPos(x, y, z), Blocks.SLIME_BLOCK.getDefaultState());
                    }
                }
            }
        }

        chunk.markDirty();*//*

    }

    */
/*@SubscribeEvent
    public void onWorldSave(WorldEvent.Save event) {
        if (!event.getWorld().isRemote) {
            ExampleWorldSavedData data = ExampleWorldSavedData.get(event.getWorld());
            logger.log(Level.DEBUG, data.mapName);
            logger.log(Level.DEBUG, data.test);
        }
    }*//*

}
*/
