package com.example.examplemod;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.datafix.IFixableData;

public class GeneralFixer implements IFixableData {

    private final TagCompoundFixer fixer;

    public GeneralFixer(TagCompoundFixer fixer) {
        this.fixer = fixer;
    }

    @Override
    public int getFixVersion() {
        return 12;
    }

    @Override
    public NBTTagCompound fixTagCompound(NBTTagCompound compound) {
        fixer.fixTagCompound(compound);
        return compound;
    }

    @FunctionalInterface
    public interface TagCompoundFixer {
        void fixTagCompound(NBTTagCompound compound);
    }
}
