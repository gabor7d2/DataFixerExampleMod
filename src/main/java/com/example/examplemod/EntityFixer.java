package com.example.examplemod;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.datafix.IFixableData;

public class EntityFixer implements IFixableData {

    @Override
    public int getFixVersion() {
        return 9;
    }

    @Override
    public NBTTagCompound fixTagCompound(NBTTagCompound compound) {
        // Dynamically fix items in entities' inventories/hand etc.
        if (!ExampleMod.dataFixes) return compound;

        ExampleMod.printNBTCompound(compound, "Entity");

        BlockEntityFixer.fixItemsInCompound(compound);

        return compound;
    }
}
