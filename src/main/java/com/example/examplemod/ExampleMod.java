package com.example.examplemod;

import com.google.common.collect.ImmutableList;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
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
    public static final String VERSION = "1.4";

    public static Logger logger;

    // The first version which introduced wrong data
    public static final String WRONG_DATA_VERSION_START = "1.1";

    // The last version which used wrong data
    public static final String WRONG_DATA_VERSION_END = "1.3";

    // Whether there is currently a world loaded
    public static boolean worldLoaded = false;

    // Whether we are fixing wrong data in the currently loaded world
    public static boolean dataFixes = false;

    public static List<FixDefinition> fixDefinitions;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        MinecraftForge.EVENT_BUS.register(this);
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        createFixDefinitions();

        final ModFixs modFixs = FMLCommonHandler.instance().getDataFixer().init(ExampleMod.MODID, 11);
        modFixs.registerFix(FixTypes.CHUNK, new BlockFixer());
        modFixs.registerFix(FixTypes.BLOCK_ENTITY, new BlockEntityFixer());
        modFixs.registerFix(FixTypes.ENTITY, new EntityFixer());

        //modFixs.registerFix(FixTypes.ITEM_INSTANCE, new GeneralFix(compound -> printNBTCompound(compound, "Item")));
        modFixs.registerFix(FixTypes.PLAYER, new GeneralFixer(compound -> printNBTCompound(compound, "Player")));
        modFixs.registerFix(FixTypes.LEVEL, new GeneralFixer(compound -> printNBTCompound(compound, "Level")));

        FMLCommonHandler.instance().getDataFixer().registerWalker(FixTypes.ITEM_INSTANCE, new ItemWalker());
    }

    public void createFixDefinitions() {
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

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        if (!event.getWorld().isRemote && !worldLoaded) {
            worldLoaded = true;
            logger.debug("Opened world save");

            // Check if this world was previously started being fixed
            ExampleWorldSavedData worldSavedData = ExampleWorldSavedData.load(event.getWorld());
            if (worldSavedData != null) {
                dataFixes = worldSavedData.dataFixes;
                logger.debug("This world was previously flagged with dataFixes {}", dataFixes ? "enabled" : "disabled");
                return;
            }
            worldSavedData = new ExampleWorldSavedData();

            // Any code from this point should only ever run once for a world

            try {
                File saveDir = event.getWorld().getSaveHandler().getWorldDirectory();
                if (!saveDir.exists()) return;
                File levelDat = new File(saveDir, "level.dat");
                if (!levelDat.exists()) {
                    // Most likely a freshly created world, no need for data fixes
                    logger.debug("Level.dat doesn't exist!");
                    logger.debug("World is freshly created, no need for dataFixes.");
                    ExampleWorldSavedData.save(event.getWorld(), worldSavedData);
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
                            // This world was last opened with a "wrong" version
                            dataFixes = true;
                            worldSavedData.dataFixes = true;

                            // Create a backup of the world in case something goes wrong with the conversion
                            createWorldBackup();
                        } else if (startVer == -1) {
                            // This world was last opened with a version that didn't introduce wrong data yet
                            // TODO set some boolean to use in the fixing process for determining what kind of conversion to do
                        }
                        // Make sure to persist whether this world needs to be fixed
                        ExampleWorldSavedData.save(event.getWorld(), worldSavedData);

                        logger.debug("Mod version in save is {}, which means dataFixes are {}.", versionInSave, dataFixes ? "enabled" : "disabled");
                    }
                } else logger.debug("NBT doesnt have ModList key");
            } catch (Exception e) {
                logger.error("Error on WorldLoad");
                e.printStackTrace();
            }
        }
    }

    @SubscribeEvent
    public void entityJoinWorld(PlayerEvent.PlayerLoggedInEvent event) {
        logger.debug("PlayerLoggedIn");

        for (int i = 0; i < event.player.inventory.getSizeInventory(); i++) {
            logger.debug("PlayerInventory contents at index {}: {}", i, event.player.inventory.getStackInSlot(i).serializeNBT());
        }
        for (int i = 0; i < event.player.getInventoryEnderChest().getSizeInventory(); i++) {
            logger.debug("Player EnderChest contents at index {}: {}", i, event.player.inventory.getStackInSlot(i).serializeNBT());
        }

        /*event.player.inventoryContainer.inventoryItemStacks.forEach(itemStack -> {
            //logger.debug("{}:{}", itemStack.getItem().delegate.name(), itemStack.getMetadata());
            event.player.inventoryContainer.inventorySlots.forEach(slot -> {
                if (slot.getStack().getCount() > 0) {
                    ItemStack stack = slot.getStack();
                    stack.setCount(63);
                    event.player.inventory.setInventorySlotContents(32, stack);
                }
            });
        });*/
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
        }
    }

    public static void printNBTCompound(NBTTagCompound compound, String type) {
        String str = compound.toString().trim();
        if (str.length() > 2) ExampleMod.logger.debug("{} Fixer: {}", type, str);
    }
}
