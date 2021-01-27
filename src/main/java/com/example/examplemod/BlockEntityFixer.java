package com.example.examplemod;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.datafix.IFixableData;

public class BlockEntityFixer implements IFixableData {

    @Override
    public int getFixVersion() {
        return 9;
    }

    @Override
    public NBTTagCompound fixTagCompound(NBTTagCompound compound) {
        // Dynamically fix items in tile entities
        if (!ExampleMod.dataFixes) return compound;

        ExampleMod.printNBTCompound(compound, "BlockEntity");

        fixItemsInCompound(compound);

        return compound;
    }

    // Recursively travel through the entire NBT tree
    public static void fixItemsInCompound(NBTTagCompound compound) {
        // If tag has both id of type string and Damage of type short
        // This is the safest and fastest but some mods might use a different name for id and/or Damage,
        // in which case compound.toString().contains() and replace() could be used instead, but this would run
        // for all visited tree elements on all depths and it is probably too expensive
        if (compound.hasKey("id", 8) && compound.hasKey("Damage", 2)) {
            // Make the necessary replacements
            String savedName = compound.getString("id");
            short savedMetadata = compound.getShort("Damage");
            for (FixDefinition fix : ExampleMod.fixDefinitions) {
                // Check if this fix should handle this item
                if (fix.oldName.toString().equals(savedName) && fix.oldMetadata == savedMetadata) {
                    // Fix the item
                    compound.setString("id", fix.newBlockState.getBlock().getRegistryName().toString());
                    compound.setShort("Damage", (short) fix.newBlockState.getBlock().getMetaFromState(fix.newBlockState));
                    break;
                }
            }
        }

        if (compound.getKeySet().size() > 0) {
            // If this compound has any elements, go through all of it's compounds and compound lists
            compound.getKeySet().forEach(key -> {
                // Check if this element is an NBTCompound
                if (compound.hasKey(key, 10)) {
                    //ExampleMod.logger.debug("Processing compound {}", key);
                    fixItemsInCompound(compound.getCompoundTag(key));
                }

                // Check if this element is an NBTCompound List
                if (compound.hasKey(key, 9)) {
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

