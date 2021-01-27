package com.example.examplemod;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ObjectIntIdentityMap;
import net.minecraft.util.datafix.IFixableData;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.GameData;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

/**
 * A data fixer that remaps flattened blocks to their new states.
 *
 * @author Choonster
 */
public class BlockFixer implements IFixableData {

    @Override
    public int getFixVersion() {
        return 9;
    }

    @Override
    public NBTTagCompound fixTagCompound(final NBTTagCompound compound) {
        // Dynamically fix blocks in world
        if (!ExampleMod.dataFixes) return compound;
        //if (true) return compound;

        final ForgeRegistry<Block> blockRegistry = (ForgeRegistry<Block>) ForgeRegistries.BLOCKS;

        // Maps old block IDs to an array of flattening definitions indexed by their old metadata
        final Map<Integer, FixDefinition[]> flattingDefinitionsPerBlockID = new HashMap<>();

        ExampleMod.fixDefinitions.stream()
                .map(fixDefinition -> {
                    // Get the ID of the old name
                    int oldID = blockRegistry.getID(fixDefinition.oldName);

                    // If the ID exists in this save, return a pair of the ID and the definition; else return an empty pair
                    return Optional.ofNullable(oldID > 0 ? Pair.of(oldID, fixDefinition) : null);
                })
                .forEach(optionalPair -> {
                    optionalPair.ifPresent(pair -> { // If the ID exists in this save,
                        final Integer blockID = pair.getKey();
                        final FixDefinition fixDefinition = pair.getValue();

                        // Add the definition to the ID's array using the old metadata as an index
                        final FixDefinition[] fixDefinitions = flattingDefinitionsPerBlockID.computeIfAbsent(blockID, id -> new FixDefinition[16]);
                        fixDefinitions[fixDefinition.oldMetadata] = fixDefinition;
                    });
                });

        // If there aren't any blocks to flatten in this save, do nothing
        if (flattingDefinitionsPerBlockID.isEmpty()) {
            return compound;
        }

        final ObjectIntIdentityMap<IBlockState> blockStateIDMap = GameData.getBlockStateIDMap();

        try {
            final NBTTagCompound level = compound.getCompoundTag("Level");
            final NBTTagList sections = level.getTagList("Sections", 10);

            for (int sectionIndex = 0; sectionIndex < sections.tagCount(); ++sectionIndex) {
                final NBTTagCompound section = sections.getCompoundTagAt(sectionIndex);

                final int sectionY = section.getByte("Y");
                final byte[] blockIDs = section.getByteArray("Blocks");
                final NibbleArray metadataArray = new NibbleArray(section.getByteArray("Data"));
                final NibbleArray blockIDsExtension = section.hasKey("Add", Constants.NBT.TAG_BYTE_ARRAY) ? new NibbleArray(section.getByteArray("Add")) : new NibbleArray();
                boolean hasExtendedBlockIDs = section.hasKey("Add", Constants.NBT.TAG_BYTE_ARRAY);

                for (int blockIndex = 0; blockIndex < blockIDs.length; ++blockIndex) {
                    final int x = blockIndex & 15;
                    final int y = blockIndex >> 8 & 15;
                    final int z = blockIndex >> 4 & 15;
                    final int blockIDExtension = blockIDsExtension.get(x, y, z);
                    final int blockID = blockIDExtension << 8 | (blockIDs[blockIndex] & 255);
                    final int metadata = metadataArray.get(x, y, z);

                    final FixDefinition[] fixDefinitions = flattingDefinitionsPerBlockID.get(blockID);

                    if (fixDefinitions != null) {
                        final FixDefinition fixDefinition = fixDefinitions[metadata];

                        if (fixDefinition != null) {
                            // Calculate the new block ID, block ID extension and metadata from the block state's ID
                            final int blockStateID = blockStateIDMap.get(fixDefinition.newBlockState);
                            final byte newBlockID = (byte) (blockStateID >> 4 & 255);
                            final byte newBlockIDExtension = (byte) (blockStateID >> 12 & 15);
                            final byte newMetadata = (byte) (blockStateID & 15);

                            // Update the block ID and metadata
                            blockIDs[blockIndex] = newBlockID;
                            metadataArray.set(x, y, z, newMetadata);

                            // Update the block ID extension if present
                            if (newBlockIDExtension != 0) {
                                hasExtendedBlockIDs = true;
                                blockIDsExtension.set(x, y, z, newBlockIDExtension);
                            }
                        }
                    }
                }

                // Update the block ID and metadata in the section
                section.setByteArray("Blocks", blockIDs);
                section.setByteArray("Data", metadataArray.getData());

                // Update the block ID extensions in the section, if present
                if (hasExtendedBlockIDs) {
                    section.setByteArray("Add", blockIDsExtension.getData());
                }
            }
        } catch (final Exception e) {
            ExampleMod.logger.error("Failed converting blocks:");
            e.printStackTrace();
        }

        return compound;
    }
}