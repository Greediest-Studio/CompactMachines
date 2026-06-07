package org.dave.compactmachines3.world;

import com.google.common.base.MoreObjects;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import org.dave.compactmachines3.utility.ChunkUtils;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class WorldCloneChunkProvider implements IChunkProvider {
    World world;

    private final Chunk blankChunk;
    private final Long2ObjectMap<NBTTagCompound> chunkTags = new Long2ObjectOpenHashMap<>(8192);
    private final Long2ObjectMap<NBTBase> renderTags = new Long2ObjectOpenHashMap<>(8192);
    private final Long2ObjectMap<List<BlockPos>> toRender = new Long2ObjectOpenHashMap<>(8192);
    private final Long2ObjectMap<List<BlockPos>> tileEntities = new Long2ObjectOpenHashMap<>(8192);
    private int tileEntityListVersion = 0;
    private final Long2ObjectMap<Chunk> loadedChunks = new Long2ObjectOpenHashMap<Chunk>(8192) {
        protected void rehash(int p_rehash_1_)
        {
            if (p_rehash_1_ > this.key.length)
            {
                super.rehash(p_rehash_1_);
            }
        }
    };

    public WorldCloneChunkProvider(World worldIn) {
        this.blankChunk = new EmptyChunk(worldIn, 0, 0);
        this.world = worldIn;
    }

    public boolean loadChunkFromNBT(NBTTagCompound tag) {
        if (tag.isEmpty()) {
            return false;
        }

        long chunkPos = ChunkPos.asLong(tag.getInteger("xPos"), tag.getInteger("zPos"));
        NBTTagCompound previousTag = this.chunkTags.get(chunkPos);
        if (previousTag != null && previousTag.equals(tag)) {
            return false;
        }

        boolean renderChanged = !isSameTag(this.renderTags.get(chunkPos), tag, "Sections");

        Chunk chunk = ChunkUtils.readChunkFromNBT(world, tag);
        chunk.markLoaded(true);
        this.loadedChunks.put(chunkPos, chunk);
        this.tileEntities.put(chunkPos, new ArrayList<>(chunk.getTileEntityMap().keySet()));
        this.tileEntityListVersion++;

        if (renderChanged) {
            this.toRender.put(chunkPos, buildRenderList(chunk));
            this.renderTags.put(chunkPos, copyTag(tag, "Sections"));
        }

        this.chunkTags.put(chunkPos, tag.copy());
        return renderChanged;
    }

    private List<BlockPos> buildRenderList(Chunk chunk) {
        List<BlockPos> result = new ArrayList<>();
        for (ExtendedBlockStorage storage : chunk.getBlockStorageArray()) {
            if (storage == Chunk.NULL_BLOCK_STORAGE || storage.isEmpty()) {
                continue;
            }

            int sectionY = storage.getYLocation();
            for (int x = 15; x >= 0; x--) {
                for (int z = 15; z >= 0; z--) {
                    for (int y = 15; y >= 0; y--) {
                        IBlockState state = storage.get(x, y, z);
                        if (state.getBlock() == Blocks.AIR) {
                            continue;
                        }

                        if (state.getBlock() == Blocks.BARRIER) {
                            continue;
                        }

                        result.add(chunk.getPos().getBlock(x, sectionY + y, z));
                    }
                }
            }
        }
        return result;
    }

    private boolean isSameTag(@Nullable NBTBase previous, NBTTagCompound tag, String key) {
        if (!tag.hasKey(key)) {
            return previous == null;
        }

        return previous != null && previous.equals(tag.getTag(key));
    }

    @Nullable
    private NBTBase copyTag(NBTTagCompound tag, String key) {
        return tag.hasKey(key) ? tag.getTag(key).copy() : null;
    }

    @Nullable
    @Override
    public Chunk getLoadedChunk(int x, int z) {
        return this.loadedChunks.get(ChunkPos.asLong(x, z));
    }

    @Override
    public Chunk provideChunk(int x, int z) {
        return MoreObjects.firstNonNull(this.getLoadedChunk(x, z), this.blankChunk);
    }

    public List<BlockPos> getRenderListForChunk(int x, int z) {
        return this.toRender.get(ChunkPos.asLong(x, z));
    }

    public List<BlockPos> getTileEntityListForChunk(int x, int z) {
        return this.tileEntities.get(ChunkPos.asLong(x, z));
    }

    public int getTileEntityListVersion() {
        return this.tileEntityListVersion;
    }

    public void clear() {
        this.loadedChunks.clear();
        this.toRender.clear();
        this.tileEntities.clear();
        this.chunkTags.clear();
        this.renderTags.clear();
        this.tileEntityListVersion++;
    }

    @Override
    public boolean tick() {
        return false;
    }

    @Override
    public String makeString() {
        return String.format("WorldCloneChunkCache: %d", this.loadedChunks.size());
    }

    @Override
    public boolean isChunkGeneratedAt(int x, int z) {
        return this.loadedChunks.containsKey(ChunkPos.asLong(x, z));
    }
}
