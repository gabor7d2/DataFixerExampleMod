package com.example.datafixer;

import com.example.datafixer.blocks.RegistryHandler;
import com.example.datafixer.fixes.BlockFixer;
import com.example.datafixer.fixes.BlockFixDefinition;
import com.example.datafixer.fixes.GeneralFixer;
import com.google.common.collect.ImmutableList;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.datafix.FixTypes;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ModFixs;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.ZipperUtil;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.List;

@Mod(modid = ExampleMod.MODID, name = ExampleMod.NAME, version = ExampleMod.VERSION)
public class ExampleMod {
    public static final String MODID = "examplemod";
    public static final String NAME = "Data Fixer Example Mod";
    public static final String VERSION = "1.8";

    public static Logger logger;

    // The first version which introduced wrong data
    public static final String WRONG_DATA_VERSION_START = "1.1";

    // The last version which used wrong data
    public static final String WRONG_DATA_VERSION_END = "1.3";

    // Whether there is currently a world loaded, used to prevent multiple WorldEvent.Load events
    public static boolean worldLoaded = false;

    public static DataFixInfoWorldData worldSavedData;

    public static List<BlockFixDefinition> blockFixDefinitions;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        MinecraftForge.EVENT_BUS.register(this);
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        createFixDefinitions();
        registerFixes();
    }

    private void createFixDefinitions() {
        final ImmutableList.Builder<BlockFixDefinition> fixDefs = new ImmutableList.Builder<>();

        HashMap<Integer, IBlockState> stoneVariants = new HashMap<>();
        for (int i = 0; i <= 6; i++) {
            stoneVariants.put(i, Block.getBlockFromName("minecraft:stone").getStateFromMeta(i));
        }

        for (short i = 1; i <= 6; i++) {
            fixDefs.add(new BlockFixDefinition(
                    new ResourceLocation("minecraft", "stone"), i, stoneVariants.get(i - 1)));
        }

        fixDefs.add(new BlockFixDefinition(
                new ResourceLocation("minecraft", "grass"), (short) 0, stoneVariants.get(6)));

        fixDefs.add(new BlockFixDefinition(
                new ResourceLocation("examplemod:test2"), (short) 0, RegistryHandler.BLOCKS.get(new ResourceLocation("examplemod:test1")).getStateFromMeta(0)));

        blockFixDefinitions = fixDefs.build();
    }

    private void registerFixes() {
        final ModFixs modFixs = FMLCommonHandler.instance().getDataFixer().init(ExampleMod.MODID, 12);
        modFixs.registerFix(FixTypes.CHUNK, new BlockFixer());

        modFixs.registerFix(FixTypes.ENTITY, new GeneralFixer(compound -> {
            // Dynamically fix items in entities' inventories/hand etc.
            ExampleMod.printNBTCompound(compound, "Entity");
            if (worldSavedData != null && worldSavedData.dataFixes) GeneralFixer.fixItemsInCompound(compound);
        }));

        modFixs.registerFix(FixTypes.BLOCK_ENTITY, new GeneralFixer(compound -> {
            // Dynamically fix items in entities' inventories/hand etc.
            ExampleMod.printNBTCompound(compound, "BlockEntity");
            if (worldSavedData != null && worldSavedData.dataFixes) GeneralFixer.fixItemsInCompound(compound);
        }));

        modFixs.registerFix(FixTypes.PLAYER, new GeneralFixer(compound -> {
            // Dynamically fix items in joining player's inventories, except when the player is host of an IntegratedServer
            ExampleMod.printNBTCompound(compound, "Player");
            if (worldSavedData != null && worldSavedData.dataFixes) GeneralFixer.fixItemsInCompound(compound);
        }));

        /*modFixs.registerFix(FixTypes.LEVEL, new GeneralFixer(compound -> {
            ExampleMod.printNBTCompound(compound, "Level");
            if (dataFixes) GeneralFixer.fixItemsInCompound(compound);
        }));*/

        //modFixs.registerFix(FixTypes.ITEM_INSTANCE, new GeneralFix(compound -> printNBTCompound(compound, "Item")));
        //FMLCommonHandler.instance().getDataFixer().registerWalker(FixTypes.ITEM_INSTANCE, new ItemWalker());
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        if (!event.getWorld().isRemote && !worldLoaded) {
            worldLoaded = true;
            logger.debug("Opened world save");

            // Check if this world was previously started being fixed
            worldSavedData = DataFixInfoWorldData.load(event.getWorld());
            if (worldSavedData != null) {
                logger.debug("This world was previously flagged with dataFixes {}", worldSavedData.dataFixes ? "enabled" : "disabled");
                return;
            }
            worldSavedData = new DataFixInfoWorldData();

            // Any code from this point should only ever run at most once for a world

            try {
                File saveDir = event.getWorld().getSaveHandler().getWorldDirectory();
                if (!saveDir.exists()) return;
                File levelDat = new File(saveDir, "level.dat");
                if (!levelDat.exists()) {
                    // Most likely a freshly created world, no need for data fixes
                    logger.debug("Level.dat doesn't exist!");
                    logger.debug("World is freshly created, no need for dataFixes.");
                    DataFixInfoWorldData.save(event.getWorld(), worldSavedData);
                    return;
                }
                NBTTagCompound nbt = CompressedStreamTools.readCompressed(new FileInputStream(levelDat));
                NBTTagCompound fmlTag = nbt.getCompoundTag("FML");

                if (fmlTag.hasKey("ModList")) {
                    NBTTagList modList = fmlTag.getTagList("ModList", (byte) 10);
                    for (int i = 0; i < modList.tagCount(); i++) {
                        NBTTagCompound mod = modList.getCompoundTagAt(i);
                        if (!mod.getString("ModId").equals(MODID)) continue;
                        String versionInSave = mod.getString("ModVersion");

                        // Check if the mod version in the save is between WRONG_DATA_VERSION_START and WRONG_DATA_VERSION_END
                        int startVer = VersionUtil.compare(versionInSave, WRONG_DATA_VERSION_START);
                        int endVer = VersionUtil.compare(versionInSave, WRONG_DATA_VERSION_END);
                        if ((startVer == 1 || startVer == 0) && (endVer == -1 || endVer == 0)) {
                            // Create a backup of the world in case something goes wrong with the conversion
                            createWorldBackup();

                            // This world was last opened with a "wrong" version
                            worldSavedData.dataFixes = true;
                            worldSavedData.fixHostPlayerInventory = true;
                        } else if (startVer == -1) {
                            // This world was last opened with a version that didn't introduce wrong data yet
                            // TODO set some boolean in worldSavedData to use in the fixing process for determining what kind of conversion to do
                        }
                        // Make sure to persist whether this world needs to be fixed
                        DataFixInfoWorldData.save(event.getWorld(), worldSavedData);

                        logger.debug("Mod version in save is {}, which means dataFixes should be {}.", versionInSave, worldSavedData.dataFixes ? "enabled" : "disabled");
                    }
                } else logger.debug("NBT doesnt have ModList key");
            } catch (Exception e) {
                logger.error("Error on WorldLoad");
                e.printStackTrace();
            }
        }
    }

    @SubscribeEvent
    public void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        logger.debug("PlayerLoggedIn");

        // If data fixing is disabled for the current world, skip
        if (!worldSavedData.dataFixes) return;

        EntityPlayer player = event.player;

        // The first time this world is loaded in singleplayer, fixHostPlayerInventory
        // is going to be set to false, and never run again.
        // Itemfixing here only happens for the host player of this singleplayer world,
        // any other case (players joining a DedicatedServer or a LAN IntegratedServer)
        // is covered by the Player Datafixer.

        // Check if this is an IntegratedServer
        if (!FMLCommonHandler.instance().getMinecraftServerInstance().isDedicatedServer()) {
            // Fix items in player's inventory
            for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
                NBTTagCompound slotNBT = player.inventory.getStackInSlot(i).serializeNBT();
                logger.debug("PlayerInventory contents at index {}: {}", i, slotNBT);
                if (worldSavedData.fixHostPlayerInventory) {
                    GeneralFixer.fixItemsInCompound(slotNBT);
                    player.inventory.setInventorySlotContents(i, new ItemStack(slotNBT));
                }
            }

            // Fix items in player's EnderChest
            for (int i = 0; i < player.getInventoryEnderChest().getSizeInventory(); i++) {
                NBTTagCompound slotNBT = player.getInventoryEnderChest().getStackInSlot(i).serializeNBT();
                logger.debug("Player EnderChest contents at index {}: {}", i, slotNBT);
                if (worldSavedData.fixHostPlayerInventory) {
                    GeneralFixer.fixItemsInCompound(slotNBT);
                    player.getInventoryEnderChest().setInventorySlotContents(i, new ItemStack(slotNBT));
                }
            }

            worldSavedData.fixHostPlayerInventory = false;
            DataFixInfoWorldData.save(event.player.getEntityWorld(), worldSavedData);
        } else logger.debug("Server is dedicated, skipping PlayerLoggedIn fixes.");
    }

    @EventHandler
    public void serverStopped(FMLServerStoppedEvent event) {
        worldLoaded = false;
        worldSavedData = null;
        logger.debug("Closed world save");
    }

    @SubscribeEvent
    public void missingMappingsBlock(RegistryEvent.MissingMappings<Block> event) {
        logger.debug(event.getAllMappings().size());
        logger.debug(event.getMappings().size());
        logger.debug(event.getName());

        for (RegistryEvent.MissingMappings.Mapping<Block> entry : event.getAllMappings()) {
            logger.debug(entry.id);
            logger.debug(entry.key);
            entry.ignore();
            //if (entry.key.toString().equals("examplemod:test2")) entry.remap(RegistryHandler.BLOCKS.get(new ResourceLocation("examplemod:test1")));
        }
    }

    @SubscribeEvent
    public void missingMappingsItem(RegistryEvent.MissingMappings<Item> event) {
        logger.debug(event.getAllMappings().size());
        logger.debug(event.getMappings().size());
        logger.debug(event.getName());

        for (RegistryEvent.MissingMappings.Mapping<Item> entry : event.getAllMappings()) {
            logger.debug(entry.id);
            logger.debug(entry.key);
            entry.ignore();
            //if (entry.key.toString().equals("examplemod:test2")) entry.remap(RegistryHandler.ITEMS.get(new ResourceLocation("examplemod:test1")));
        }
    }

    private static void createWorldBackup() {
        try {
            logger.info("Creating world backup before starting DataFixers...");
            ZipperUtil.backupWorld();
        } catch (Exception e) {
            logger.error("Error creating backup!!!");
            e.printStackTrace();
            // Maybe the server should be closed to prevent damage? like how Forge MissingMappings does it
        }
    }

    public static void printNBTCompound(NBTTagCompound rootCompound, String type) {
        String str = rootCompound.toString().trim();
        if (str.length() > 2) ExampleMod.logger.debug("{} Fixer: {}", type, str);
    }

    public static void printNBTCompoundMatch(NBTTagCompound compound) {
        String str = compound.toString().trim();
        if (str.length() > 2) ExampleMod.logger.debug("Found NBTCompound match: {}", str);
    }
}
