package com.example.datafixer.fixes;

import com.example.datafixer.ExampleMod;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
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

    // Recursively travel through the entire NBT tree
    public static void fixItemsInCompound(NBTTagCompound compound) {
        // If tag has both id of type string and Damage of type short
        // This is the safest and fastest but some rare mods might use a different name for id and/or Damage
        // if they do not use or extend the vanilla ItemStack class for writing NBT data,
        // in which case compound.toString().contains() and replace() could be used instead, but this would run
        // for all visited tree elements on all depths and it is probably too expensive
        if (compound.hasKey("id", 8) && compound.hasKey("Damage", 2)) {
            // Make the necessary replacements
            String savedName = compound.getString("id");
            short savedMetadata = compound.getShort("Damage");
            ExampleMod.printNBTCompoundMatch(compound);
            for (BlockFixDefinition fixDef : ExampleMod.blockFixDefinitions) {
                // Check if this fix should handle this item
                if (fixDef.oldName.toString().equals(savedName) && fixDef.oldMetadata == savedMetadata) {
                    // Fix the item
                    // TODO ItemFixDefinition to also allow fixing items
                    compound.setString("id", fixDef.newBlockState.getBlock().getRegistryName().toString());
                    compound.setShort("Damage", (short) fixDef.newBlockState.getBlock().getMetaFromState(fixDef.newBlockState));
                    ExampleMod.logger.debug(" - Replaced with: {}", compound);
                    break;
                }
            }
        }

        // Replace every ItemStack that doesn't have NBT data with granite (stone:1), except applied energistics stuff
//        if (compound.hasKey("id", 8) && compound.hasKey("Damage", 2)) {
//            // Make the necessary replacements
//            String savedName = compound.getString("id");
//            ExampleMod.printNBTCompoundMatch(compound);
//            if (!compound.hasKey("tag") && !savedName.equals("minecraft:air") && !savedName.contains("appliedenergistics2")) {
//                compound.setString("id", "minecraft:stone");
//                compound.setShort("Damage", (short) 1);
//                ExampleMod.logger.debug(" - Replaced with: {}", compound);
//            }
//        }

        // Recursive travel
        if (compound.getKeySet().size() > 0) {
            // If this compound has any elements, go through all of it's compounds and compound lists
            compound.getKeySet().forEach(key -> {
                // Check if this element is an NBTCompound
                if (compound.hasKey(key, 10)) {
                    //ExampleMod.logger.debug("Processing compound {}", key);
                    fixItemsInCompound(compound.getCompoundTag(key));
                }

                // Check if this element is an NBTCompound List
                else if (compound.hasKey(key, 9)) {
                    //ExampleMod.logger.debug("Processing compound list {}", key);
                    NBTTagList compoundList = compound.getTagList(key, 10);
                    for (int i = 0; i < compoundList.tagCount(); i++) {
                        fixItemsInCompound(compoundList.getCompoundTagAt(i));
                    }
                }
            });
        }
    }
}
