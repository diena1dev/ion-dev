package net.horizonsend.ion.server.features.transport.node.power

import kotlinx.serialization.SerializationException
import net.horizonsend.ion.server.features.transport.grid.ChunkTransportNetwork
import net.horizonsend.ion.server.features.transport.node.NodeType
import net.horizonsend.ion.server.miscellaneous.registrations.persistence.NamespacedKeys.NODE_TYPE
import net.horizonsend.ion.server.miscellaneous.registrations.persistence.PDCSerializable
import net.horizonsend.ion.server.miscellaneous.utils.coordinates.BlockKey
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType

/**
 * Represents a single node, or step, in transport transportNetwork
 **/
interface TransportNode : PDCSerializable<TransportNode, TransportNode.Companion> {
	override val type: Companion get() = Companion

	/**
	 * The neighboring nodes that this node may transport to
	 **/
	val transferableNeighbors: MutableSet<TransportNode>

	/**
	 * Returns whether this node may transport to the provided node
	 **/
	fun isTransferable(position: Long, node: TransportNode): Boolean

	/**
	 * Store additional required data in the serialized container
	 **/
	fun storeData(persistentDataContainer: PersistentDataContainer)

	/**
	 * Load required data from the serialized container
	 **/
	fun loadData(persistentDataContainer: PersistentDataContainer)

	/**
	 * Handle placement into the network upon loading, after data has been loaded
	 **/
	fun loadIntoNetwork(network: ChunkTransportNetwork) {}

	/**
	 * Logic for handling the removal of this node
	 *
	 * Cleanup, splitting into multiple, etc
	 **/
	suspend fun handleRemoval(network: ChunkTransportNetwork, position: BlockKey) {}

	/**
	 * Builds relations between this node and transferrable nodes
	 **/
	suspend fun buildRelations(network: ChunkTransportNetwork, position: BlockKey)

	/**
	 * Additional logic to be run once the node is placed
	 **/
	suspend fun onPlace(network: ChunkTransportNetwork, position: BlockKey) {}

	companion object : PersistentDataType<PersistentDataContainer, TransportNode> {
		override fun getPrimitiveType() = PersistentDataContainer::class.java
		override fun getComplexType() = TransportNode::class.java

		override fun toPrimitive(complex: TransportNode, context: PersistentDataAdapterContext): PersistentDataContainer {
			val pdc = context.newPersistentDataContainer()
			pdc.set(NODE_TYPE, NodeType.type, NodeType[complex])

			complex.storeData(pdc)

			return pdc
		}

		override fun fromPrimitive(primitive: PersistentDataContainer, context: PersistentDataAdapterContext): TransportNode = try {
			val type = primitive.get(NODE_TYPE, NodeType.type)!!
			val instance = type.newInstance()

			instance.loadData(primitive)

			instance
		} catch (e: Throwable) {
			e.printStackTrace()
			throw SerializationException("Error deserializing multiblock data!")
		}
	}
}
