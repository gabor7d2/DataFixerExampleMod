package com.example.datafixer.fixes;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.ResourceLocation;

public class BlockFixDefinition {

    final ResourceLocation oldName;
    final short oldMetadata;
    final IBlockState newBlockState;

    public BlockFixDefinition(final ResourceLocation oldName, final short oldMetadata, final IBlockState newBlockState) {
        this.oldName = oldName;
        this.oldMetadata = oldMetadata;
        this.newBlockState = newBlockState;
    }
}