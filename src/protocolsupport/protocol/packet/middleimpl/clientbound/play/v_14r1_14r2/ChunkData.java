package protocolsupport.protocol.packet.middleimpl.clientbound.play.v_14r1_14r2;

import protocolsupport.protocol.ConnectionImpl;
import protocolsupport.protocol.packet.PacketType;
import protocolsupport.protocol.packet.middle.clientbound.play.MiddleChunkData;
import protocolsupport.protocol.packet.middleimpl.ClientBoundPacketData;
import protocolsupport.protocol.serializer.ArraySerializer;
import protocolsupport.protocol.serializer.ItemStackSerializer;
import protocolsupport.protocol.serializer.PositionSerializer;
import protocolsupport.protocol.serializer.VarNumberSerializer;
import protocolsupport.protocol.typeremapper.basic.BiomeRemapper;
import protocolsupport.protocol.typeremapper.block.FlatteningBlockData;
import protocolsupport.protocol.typeremapper.block.FlatteningBlockData.FlatteningBlockDataTable;
import protocolsupport.protocol.typeremapper.block.LegacyBlockData;
import protocolsupport.protocol.typeremapper.chunk.ChunkWriterVaries;
import protocolsupport.protocol.typeremapper.chunk.HeightMapTransformer;
import protocolsupport.protocol.typeremapper.legacy.LegacyBiomeData;
import protocolsupport.protocol.typeremapper.tile.TileEntityRemapper;
import protocolsupport.protocol.typeremapper.utils.RemappingTable.IdRemappingTable;

public class ChunkData extends MiddleChunkData {

	public ChunkData(ConnectionImpl connection) {
		super(connection);
	}

	protected final IdRemappingTable biomeRemappingTable = BiomeRemapper.REGISTRY.getTable(version);
	protected final IdRemappingTable blockDataRemappingTable = LegacyBlockData.REGISTRY.getTable(version);
	protected final FlatteningBlockDataTable flatteningBlockDataTable = FlatteningBlockData.REGISTRY.getTable(version);
	protected final TileEntityRemapper tileRemapper = TileEntityRemapper.getRemapper(version);

	@Override
	protected void writeToClient() {
		ClientBoundPacketData chunkdata = ClientBoundPacketData.create(PacketType.CLIENTBOUND_PLAY_CHUNK_SINGLE);
		PositionSerializer.writeIntChunkCoord(chunkdata, coord);
		chunkdata.writeBoolean(full);
		VarNumberSerializer.writeVarInt(chunkdata, blockMask);
		ItemStackSerializer.writeDirectTag(chunkdata, HeightMapTransformer.transform(heightmaps));
		ArraySerializer.writeVarIntByteArray(chunkdata, to -> {
			ChunkWriterVaries.writeSectionsCompact(
				to, blockMask, 14,
				blockDataRemappingTable,
				flatteningBlockDataTable,
				sections
			);
			if (full) {
				int[] legacyBiomeData = LegacyBiomeData.toLegacyBiomeData(biomes);
				for (int biomeId : legacyBiomeData) {
					to.writeInt(biomeRemappingTable.getRemap(biomeId));
				}
			}
		});
		ArraySerializer.writeVarIntTArray(
			chunkdata,
			tiles,
			(to, tile) -> ItemStackSerializer.writeDirectTag(to, tileRemapper.remap(tile).getNBT())
		);
		codec.write(chunkdata);
	}

}
