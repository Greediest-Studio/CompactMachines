package org.dave.compactmachines3.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import org.dave.compactmachines3.CompactMachines3;
import org.dave.compactmachines3.utility.ChunkUtils;
import org.dave.compactmachines3.world.WorldSavedDataMachines;
import org.dave.compactmachines3.world.tools.DimensionTools;

public class MessageMachineChunk implements IMessage, IMessageHandler<MessageMachineChunk, IMessage> {
    protected int id;
    protected NBTTagCompound data;

    public MessageMachineChunk() {
    }

    public MessageMachineChunk(int id) {
        this.id = id;
        BlockPos roomPos = WorldSavedDataMachines.getInstance().getMachineRoomPosition(this.id);
        if (roomPos == null) {
            this.data = new NBTTagCompound();
        } else {
            WorldSavedDataMachines data = WorldSavedDataMachines.getInstance();
            org.dave.compactmachines3.reference.EnumMachineSize sizeEnum = data.machineSizes.getOrDefault(id, org.dave.compactmachines3.reference.EnumMachineSize.MAXIMUM);
            int size = sizeEnum.getDimension();

            int startX = roomPos.getX();
            int startZ = roomPos.getZ();
            int endX = startX + size;
            int endZ = startZ + size;

            int minChunkX = startX >> 4;
            int maxChunkX = endX >> 4;
            int minChunkZ = startZ >> 4;
            int maxChunkZ = endZ >> 4;

            NBTTagCompound parent = new NBTTagCompound();
            NBTTagList chunkList = new NBTTagList();

            // The machine room vertical origin is at Y=40 in our fake world
            int minY = 40;
            int maxY = 40 + size;

            for (int cx = minChunkX; cx <= maxChunkX; cx++) {
                for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                    Chunk chunk = DimensionTools.getServerMachineWorld().getChunk(new BlockPos(cx * 16, 0, cz * 16));
                    if (chunk == null) {
                        continue;
                    }
                    NBTTagCompound chunkTag = ChunkUtils.writeChunkToNBTRange(chunk, DimensionTools.getServerMachineWorld(), new NBTTagCompound(), minY, maxY);
                    chunkList.appendTag(chunkTag);
                }
            }

            parent.setTag("chunks", chunkList);
            this.data = parent;
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        id = buf.readInt();
        try {
            data = ByteBufUtils.readTag(buf);
        } catch (Exception e) {
            CompactMachines3.logger.debug("Unable to read nbt data from buffer: {}", e.getMessage());
            data = new NBTTagCompound();
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(id);
        ByteBufUtils.writeTag(buf, data);
    }

    @Override
    public IMessage onMessage(MessageMachineChunk message, MessageContext ctx) {
        if(!CompactMachines3.clientWorldData.isInitialized()) {
            return null;
        }

        // Clients might need to update the rendering of the machine block and its neighbors
        try {
            if (message.data.hasKey("chunks")) {
                NBTTagList list = message.data.getTagList("chunks", 10);
                for (int i = 0; i < list.tagCount(); i++) {
                    NBTTagCompound chunkTag = list.getCompoundTagAt(i);
                    CompactMachines3.clientWorldData.worldClone.providerClient.loadChunkFromNBT(chunkTag);
                }
            } else if (!message.data.getKeySet().isEmpty()) {
                CompactMachines3.clientWorldData.worldClone.providerClient.loadChunkFromNBT(message.data);
            }
        } catch (Exception e) {
            CompactMachines3.logger.debug("Failed loading machine chunks: {}", e.getMessage());
        }
        return null;
    }
}
