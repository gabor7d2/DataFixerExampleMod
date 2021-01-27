package com.example.examplemod;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;

import static com.example.examplemod.ExampleMod.MODID;

public class ExampleWorldSavedData extends WorldSavedData {
    private static final String DATA_NAME = MODID + "data";

    // Whether we are fixing wrong data in the currently loaded world
    public boolean dataFixes = false;

    // Required constructors
    public ExampleWorldSavedData() {
        super(DATA_NAME);
    }

    public ExampleWorldSavedData(String s) {
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

    public static ExampleWorldSavedData load(World world) {
        MapStorage storage = world.getMapStorage();
        return (ExampleWorldSavedData) storage.getOrLoadData(ExampleWorldSavedData.class, DATA_NAME);
    }

    public static void save(World world, ExampleWorldSavedData data) {
        MapStorage storage = world.getMapStorage();
        storage.setData(DATA_NAME, data);
        data.markDirty();
        storage.saveAllData();
    }
}