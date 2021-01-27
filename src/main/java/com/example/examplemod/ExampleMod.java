package com.example.examplemod;

import com.google.common.collect.ImmutableList;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.datafix.FixTypes;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ModFixs;
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
    public static final String NAME = "Example Mod";
    public static final String VERSION = "1.6";

    public static Logger logger;

    // The first version which introduced wrong data
    public static final String WRONG_DATA_VERSION_START = "1.1";

    // The last version which used wrong data
    public static final String WRONG_DATA_VERSION_END = "1.3";

    // Whether there is currently a world loaded
    public static boolean worldLoaded = false;

    // Whether we are fixing wrong data in the currently loaded world
    public static boolean dataFixes = false;

    // Whether the host player's inventory should be fixed, only ever true once for a world
    public static boolean fixHostPlayerInventory = false;

    public static List<FixDefinition> fixDefinitions;

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
        final ImmutableList.Builder<FixDefinition> fixDefs = new ImmutableList.Builder<>();

        HashMap<Integer, IBlockState> stoneVariants = new HashMap<>();
        for (int i = 0; i <= 6; i++) {
            stoneVariants.put(i, Block.getBlockFromName("minecraft:stone").getStateFromMeta(i));
        }

        for (short i = 1; i <= 6; i++) {
            fixDefs.add(new FixDefinition(
                    new ResourceLocation("minecraft", "stone"), i, stoneVariants.get(i - 1)));
        }

        fixDefs.add(new FixDefinition(
                new ResourceLocation("minecraft", "grass"), (short) 0, stoneVariants.get(6)));

        fixDefinitions = fixDefs.build();
    }

    private void registerFixes() {
        final ModFixs modFixs = FMLCommonHandler.instance().getDataFixer().init(ExampleMod.MODID, 12);
        modFixs.registerFix(FixTypes.CHUNK, new BlockFixer());

        modFixs.registerFix(FixTypes.ENTITY, new GeneralFixer(compound -> {
            // Dynamically fix items in entities' inventories/hand etc.
            //ExampleMod.printNBTCompound(compound, "Entity");
            if (dataFixes) GeneralFixer.fixItemsInCompound(compound);
        }));

        modFixs.registerFix(FixTypes.BLOCK_ENTITY, new GeneralFixer(compound -> {
            // Dynamically fix items in entities' inventories/hand etc.
            //ExampleMod.printNBTCompound(compound, "BlockEntity");
            if (dataFixes) GeneralFixer.fixItemsInCompound(compound);
        }));

        modFixs.registerFix(FixTypes.PLAYER, new GeneralFixer(compound -> {
            // Dynamically fix items in joining player's inventories, except when the player is host of an IntegratedServer
            ExampleMod.printNBTCompound(compound, "Player");
            if (dataFixes) GeneralFixer.fixItemsInCompound(compound);
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
            DataFixInfoWorldData worldSavedData = DataFixInfoWorldData.load(event.getWorld());
            if (worldSavedData != null) {
                dataFixes = worldSavedData.dataFixes;
                logger.debug("This world was previously flagged with dataFixes {}", dataFixes ? "enabled" : "disabled");
                return;
            }
            worldSavedData = new DataFixInfoWorldData();

            // Any code from this point should only ever run once for a world

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
                            dataFixes = true;
                            worldSavedData.dataFixes = true;
                            fixHostPlayerInventory = true;
                        } else if (startVer == -1) {
                            // This world was last opened with a version that didn't introduce wrong data yet
                            // TODO set some boolean to use in the fixing process for determining what kind of conversion to do
                        }
                        // Make sure to persist whether this world needs to be fixed
                        DataFixInfoWorldData.save(event.getWorld(), worldSavedData);

                        logger.debug("Mod version in save is {}, which means dataFixes should be {}.", versionInSave, dataFixes ? "enabled" : "disabled");
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

        EntityPlayer player = event.player;

        // In theory, fixHostPlayerInventory should only ever be true once in a world's lifetime
        // Itemfixing here only happens for the host player of this singleplayer world,
        // any other case (players joining a DedicatedServer or a LAN IntegratedServer)
        // is covered by the Player Datafixer.

        // Note: This fix is affected by the game crashing between WorldLoad and PlayerLoggedIn events
        // if the crash happens at that time, the host player's inventory will NOT be fixed, and will stay that way.
        // The other case where a player's inventory won't get fixed is if a singleplayer world was copied to a server
        // and then the conversion ran on the server, then the inventory in level.dat (the singleplayer inventory) is
        // going to remain the same, and if copied back to open with singleplayer, that inventory will be the same

        // Check if this is an IntegratedServer
        if (Minecraft.getMinecraft().isSingleplayer()) {
            fixHostPlayerInventory = false;

            // Fix items in player's inventory
            for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
                NBTTagCompound slotNBT = player.inventory.getStackInSlot(i).serializeNBT();
                logger.debug("PlayerInventory contents at index {}: {}", i, slotNBT);
                if (fixHostPlayerInventory) {
                    GeneralFixer.fixItemsInCompound(slotNBT);
                    player.inventory.setInventorySlotContents(i, new ItemStack(slotNBT));
                }
            }

            // Fix items in player's enderchest
            for (int i = 0; i < player.getInventoryEnderChest().getSizeInventory(); i++) {
                NBTTagCompound slotNBT = player.getInventoryEnderChest().getStackInSlot(i).serializeNBT();
                logger.debug("Player EnderChest contents at index {}: {}", i, slotNBT);
                if (fixHostPlayerInventory) {
                    GeneralFixer.fixItemsInCompound(slotNBT);
                    player.getInventoryEnderChest().setInventorySlotContents(i, new ItemStack(slotNBT));
                }
            }
        } else logger.debug("Server is dedicated, skipping PlayerLoggedIn fixes.");
    }

    @EventHandler
    public void serverStopped(FMLServerStoppedEvent event) {
        worldLoaded = false;
        dataFixes = false;
        logger.debug("Closed world save");
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
        if (str.length() > 2) ExampleMod.logger.debug("Found NBTCompound match:{}", str);
    }
}
