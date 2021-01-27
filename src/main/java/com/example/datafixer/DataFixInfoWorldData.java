package com.example.datafixer;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;

public class DataFixInfoWorldData extends WorldSavedData {
    private static final String DATA_NAME = ExampleMod.MODID + "data";

    // Whether we are fixing wrong data in the currently loaded world
    public boolean dataFixes = false;

    // Whether the host player's inventory should be fixed when they join, only true if
    // this world needs fixing and hasn't been opened in Singleplayer before
    public boolean fixHostPlayerInventory = false;

    // Required constructors
    public DataFixInfoWorldData() {
        super(DATA_NAME);
    }

    public DataFixInfoWorldData(String s) {
        super(s);
    }

    // WorldSavedData methods
    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        if (nbt.hasKey("dataFixes")) dataFixes = nbt.getBoolean("dataFixes");
        if (nbt.hasKey("fixHostPlayerInventory")) fixHostPlayerInventory = nbt.getBoolean("fixHostPlayerInventory");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound.setBoolean("dataFixes", dataFixes);
        compound.setBoolean("fixHostPlayerInventory", fixHostPlayerInventory);
        return compound;
    }

    public static DataFixInfoWorldData load(World world) {
        MapStorage storage = world.getMapStorage();
        return (DataFixInfoWorldData) storage.getOrLoadData(DataFixInfoWorldData.class, DATA_NAME);
    }

    public static void save(World world, DataFixInfoWorldData data) {
        MapStorage storage = world.getMapStorage();
        storage.setData(DATA_NAME, data);
        data.markDirty();
        storage.saveAllData();
    }
}