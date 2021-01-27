package com.example.datafixer;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.ResourceLocation;

public class FixDefinition {

    final ResourceLocation oldName;
    final short oldMetadata;
    final IBlockState newBlockState;

    FixDefinition(final ResourceLocation oldName, final short oldMetadata, final IBlockState newBlockState) {
        this.oldName = oldName;
        this.oldMetadata = oldMetadata;
        this.newBlockState = newBlockState;
    }
}