package net.horizonsend.ion.server.features.transport.node.power

import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import net.horizonsend.ion.server.features.multiblock.util.getBlockSnapshotAsync
import net.horizonsend.ion.server.features.transport.grid.ChunkPowerNetwork
import net.horizonsend.ion.server.features.transport.grid.ChunkTransportNetwork
import net.horizonsend.ion.server.features.transport.node.type.MultiNode
import net.horizonsend.ion.server.miscellaneous.registrations.persistence.NamespacedKeys.NODE_COVERED_POSITIONS
import net.horizonsend.ion.server.miscellaneous.registrations.persistence.NamespacedKeys.SOLAR_CELL_COUNT
import net.horizonsend.ion.server.miscellaneous.registrations.persistence.NamespacedKeys.SOLAR_CELL_EXTRACTORS
import net.horizonsend.ion.server.miscellaneous.utils.coordinates.BlockKey
import net.horizonsend.ion.server.miscellaneous.utils.coordinates.getRelative
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.BlockFace
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType.INTEGER
import org.bukkit.persistence.PersistentDataType.LONG_ARRAY
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin

/**
 * Represents a solar panel, or multiple
 **/
class SolarPanelNode : MultiNode<SolarPanelNode, SolarPanelNode> {
	/** The number of solar cells contained in this node */
	var cellNumber: Int = 1
	override val positions: MutableSet<BlockKey> = LongOpenHashSet()
	/** The positions of extractors in this solar panel */
	val extractorPositions = LongOpenHashSet()

	override val transferableNeighbors: MutableSet<TransportNode> = ObjectOpenHashSet()

	override fun isTransferable(position: Long, node: TransportNode): Boolean {
		TODO("Not yet implemented")
	}

	override fun storeData(persistentDataContainer: PersistentDataContainer) {
		persistentDataContainer.set(SOLAR_CELL_COUNT, INTEGER, cellNumber)
		persistentDataContainer.set(NODE_COVERED_POSITIONS, LONG_ARRAY, positions.toLongArray())
		persistentDataContainer.set(SOLAR_CELL_EXTRACTORS, LONG_ARRAY, extractorPositions.toLongArray())
	}

	override fun loadData(persistentDataContainer: PersistentDataContainer) {
		cellNumber = persistentDataContainer.getOrDefault(SOLAR_CELL_COUNT, INTEGER, 1)

		val coveredPositions = persistentDataContainer.get(NODE_COVERED_POSITIONS, LONG_ARRAY)
		coveredPositions?.let { positions.addAll(it.asIterable()) }

		val extractors = persistentDataContainer.get(SOLAR_CELL_EXTRACTORS, LONG_ARRAY)
		extractors?.let { extractorPositions.addAll(it.asIterable()) }
	}

	override suspend fun handleRemoval(network: ChunkTransportNetwork, position: BlockKey) {
		// Need to handle the extractor positions manually
		when {
			// Removed extractor, easier to find
			extractorPositions.contains(position) -> removePosition(
				network as ChunkPowerNetwork,
				position,
				listOf(getRelative(position, BlockFace.UP, 1), getRelative(position, BlockFace.UP, 2))
			)

			// Need to find extractor, search downward form position
			else -> {
				val extractorPosition: BlockKey = (0..2).firstNotNullOf { y ->
					getRelative(position, BlockFace.DOWN, y).takeIf { extractorPositions.contains(it) }
				}

				removePosition(
					network as ChunkPowerNetwork,
					extractorPosition,
					listOf(getRelative(extractorPosition, BlockFace.UP, 1), getRelative(extractorPosition, BlockFace.UP, 2))
				)
			}
		}

		rebuildNode(network, position)
	}

	fun addPosition(network: ChunkPowerNetwork, extractorKey: BlockKey, others: Iterable<BlockKey>) {
		extractorPositions += extractorKey
		addPosition(network, extractorKey)

		positions += others
		for (position: BlockKey in positions) {
			network.nodes[position] = this
		}

		cellNumber++
	}

	fun removePosition(network: ChunkPowerNetwork, extractorKey: BlockKey, others: Iterable<BlockKey>) {
		extractorPositions -= extractorKey
		network.nodes.remove(extractorKey)
		positions.remove(extractorKey)

		positions += others
		for (position: BlockKey in positions) {
			network.nodes.remove(position)
		}

		cellNumber--
	}

	override suspend fun rebuildNode(network: ChunkTransportNetwork, position: BlockKey) {
		// Create new nodes, automatically merging together
		extractorPositions.forEach {
			PowerNodeFactory.addSolarPanel(network as ChunkPowerNetwork, it)
		}
	}

	override fun drainTo(network: ChunkTransportNetwork, new: SolarPanelNode) {
		super.drainTo(network, new)

		new.extractorPositions.addAll(extractorPositions)
		new.cellNumber += cellNumber
	}

	private var lastTicked: Long = System.currentTimeMillis()

	/**
	 * Returns the amount of power between ticks
	 **/
	fun getPower(network: ChunkPowerNetwork): Int {
		val daylightMultiplier: Double = if (network.world.environment == World.Environment.NORMAL) {
			val daylight = sin((network.world.time / (12000.0 / PI)) - (PI / 2))
			max(0.0, daylight)
		} else 0.5

		val time = System.currentTimeMillis()
		val diff = time - this.lastTicked

		return ((diff / 1000.0) * 5 * cellNumber * daylightMultiplier).toInt()
	}

	/**
	 * Returns whether the individual solar panel from the extractor location is intact
	 **/
	suspend fun isIntact(world: World, extractorKey: BlockKey): Boolean {
		return matchesSolarPanelStructure(world, extractorKey)
	}

	companion object {
		const val POWER_PER_SECOND = 50

		suspend fun matchesSolarPanelStructure(world: World, key: BlockKey): Boolean {
			if (getBlockSnapshotAsync(world, key)?.type != Material.CRAFTING_TABLE) return false
			val diamond = getRelative(key, BlockFace.UP)

			if (getBlockSnapshotAsync(world, diamond)?.type != Material.DIAMOND_BLOCK) return false
			val cell = getRelative(diamond, BlockFace.UP)

			return getBlockSnapshotAsync(world, cell)?.type == Material.DAYLIGHT_DETECTOR
		}
	}
}
