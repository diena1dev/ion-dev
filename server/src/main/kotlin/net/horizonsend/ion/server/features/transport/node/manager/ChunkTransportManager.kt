package net.horizonsend.ion.server.features.transport.node.manager

import net.horizonsend.ion.server.features.transport.NewTransport
import net.horizonsend.ion.server.features.transport.cache.FluidTransportCache
import net.horizonsend.ion.server.features.transport.cache.PowerTransportCache
import net.horizonsend.ion.server.features.transport.node.manager.extractors.ChunkExtractorManager
import net.horizonsend.ion.server.features.transport.node.manager.extractors.ExtractorManager
import net.horizonsend.ion.server.features.transport.node.manager.holders.ChunkNetworkHolder
import net.horizonsend.ion.server.features.world.chunk.IonChunk
import net.horizonsend.ion.server.miscellaneous.utils.coordinates.BlockKey
import net.horizonsend.ion.server.miscellaneous.utils.coordinates.toBlockKey
import org.bukkit.block.Block
import org.bukkit.block.data.BlockData

class ChunkTransportManager(val chunk: IonChunk) : TransportManager() {
	override val extractorManager: ExtractorManager = ChunkExtractorManager(this)
	override val powerNodeManager = ChunkNetworkHolder(this) { PowerTransportCache(it) }
	override val fluidNodeManager = ChunkNetworkHolder(this) { FluidTransportCache(it) }
//	val pipeGrid = PowerNodeManager(this) // TODO

	fun setup() {
		powerNodeManager.handleLoad()
		fluidNodeManager.handleLoad()
		NewTransport.registerTransportManager(this)
	}

	fun onUnload() {
		powerNodeManager.handleUnload()
		fluidNodeManager.handleUnload()
		NewTransport.removeTransportManager(this)
	}

	fun invalidateCache(x: Int, y: Int, z: Int) {
		invalidateCache(toBlockKey(x, y, z))
	}

	fun invalidateCache(key: BlockKey) {
		powerNodeManager.network.invalidate(key)
		fluidNodeManager.network.invalidate(key)
	}

	fun processBlockRemoval(key: BlockKey) {
		powerNodeManager.network.invalidate(key)
		fluidNodeManager.network.invalidate(key)
//		pipeGrid.processBlockRemoval(key)
	}

	fun processBlockChange(block: Block) {
		powerNodeManager.network.invalidate(toBlockKey(block.x, block.y, block.z))
		fluidNodeManager.network.invalidate(toBlockKey(block.x, block.y, block.z))
//		pipeGrid.processBlockAddition(key, new)
	}

	fun processBlockChange(position: BlockKey, data: BlockData) {
		powerNodeManager.network.invalidate(position)
		fluidNodeManager.network.invalidate(position)
//		pipeGrid.processBlockAddition(key, new)
	}

	fun refreshBlock(position: BlockKey) {
		powerNodeManager.network.invalidate(position)
		fluidNodeManager.network.invalidate(position)
//		pipeGrid.processBlockAddition(key, new)
	}
}
