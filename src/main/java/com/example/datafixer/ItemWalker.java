package com.example.datafixer;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.datafix.IDataFixer;
import net.minecraft.util.datafix.IDataWalker;

public class ItemWalker implements IDataWalker {

    @Override
    public NBTTagCompound process(IDataFixer fixer, NBTTagCompound compound, int versionIn) {
        ExampleMod.printNBTCompound(compound, "[" + versionIn + "] ItemWalker");
        return compound;
    }
}
