package com.example.examplemod;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;

import static com.example.examplemod.ExampleMod.MODID;

public class DataFixInfoWorldData extends WorldSavedData {
    private static final String DATA_NAME = MODID + "data";

    // Whether we are fixing wrong data in the currently loaded world
    public boolean dataFixes = false;

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
        if (nbt.hasKey("dataFixes")) {
            dataFixes = nbt.getBoolean("dataFixes");
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound.setBoolean("dataFixes", dataFixes);
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