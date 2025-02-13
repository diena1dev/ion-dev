package net.horizonsend.ion.server.features.starship.factory

import com.sk89q.worldedit.extent.clipboard.Clipboard
import it.unimi.dsi.fastutil.objects.Object2ObjectRBTreeMap
import net.horizonsend.ion.common.database.schema.starships.Blueprint
import net.horizonsend.ion.server.features.multiblock.type.shipfactory.ShipFactoryEntity
import net.horizonsend.ion.server.features.multiblock.type.shipfactory.ShipFactorySettings
import net.horizonsend.ion.server.miscellaneous.utils.coordinates.BlockKey
import net.horizonsend.ion.server.miscellaneous.utils.coordinates.Vec3i
import net.horizonsend.ion.server.miscellaneous.utils.coordinates.toBlockKey
import net.horizonsend.ion.server.miscellaneous.utils.loadClipboard
import net.horizonsend.ion.server.miscellaneous.utils.toBukkitBlockData
import org.bukkit.block.data.BlockData
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

abstract class ShipFactoryBlockProcessor(
	protected val blueprint: Blueprint,
	protected val settings: ShipFactorySettings,
	protected val entity: ShipFactoryEntity
) {
	protected val clipboard: Clipboard by lazy { blueprint.loadClipboard() }

	// Use a RB tree map for key ordering.
	protected val blockMap = Object2ObjectRBTreeMap<BlockKey, BlockData>()
	protected val blockQueue = ArrayDeque<Long>()

	protected open val clipboardNormalizationOffset: Vec3i = getClipboardOffset()
	protected open val target = calculateTarget()

	protected fun loadBlockQueue() {
		val min = clipboard.minimumPoint
		val max = clipboard.maximumPoint

		for (y in min.y()..max.y()) for (x in min.x()..max.x()) for (z in min.z()..max.z()) {
			val vec3i = Vec3i(x, y, z)

			val baseBlock = clipboard.getFullBlock(x, y, z)
			val data = baseBlock.toImmutableState().toBukkitBlockData()

			if (data.material.isAir) continue

			val worldKey = toBlockKey(toWorldCoordinates(vec3i))

			blockMap[worldKey] = data
			blockQueue.add(worldKey)
		}
	}

	protected fun toWorldCoordinates(pos: Vec3i): Vec3i {
		if (settings.rotation == 0) return pos + clipboardNormalizationOffset + target

		val regionCenter = Vec3i(
			clipboard.region.center.x().toInt(),
			clipboard.region.center.y().toInt(),
			clipboard.region.center.z().toInt()
		)
		val localized = pos - regionCenter

		val cosTheta: Double = cos(Math.toRadians(settings.rotation.toDouble()))
		val sinTheta: Double = sin(Math.toRadians(settings.rotation.toDouble()))

		val rotatedVector =  Vec3i(
			(localized.x.toDouble() * cosTheta - localized.z.toDouble() * sinTheta).roundToInt(),
			localized.y,
			(localized.x.toDouble() * sinTheta + localized.z.toDouble() * cosTheta).roundToInt()
		)

		return rotatedVector + regionCenter + clipboardNormalizationOffset + target
	}

	protected fun calculateTarget(): Vec3i {
		return entity.getPosRelative(0, 0, 4) + Vec3i(settings.offsetX, settings.offsetY, settings.offsetZ)
	}

	protected fun getClipboardOffset(): Vec3i {
		val structureDirection = entity.structureDirection
		val rightDirection = entity.structureDirection

		val negativeX = if (structureDirection.modX == 0) rightDirection.modX < 0 else structureDirection.modX < 0
		val negativeZ = if (structureDirection.modZ == 0) rightDirection.modZ < 0 else structureDirection.modZ < 0

		val x = if (negativeX) clipboard.region.minimumPoint.x() else clipboard.region.maximumPoint.x()
		val y = clipboard.region.minimumPoint.y()
		val z = if (negativeZ) clipboard.region.minimumPoint.z() else clipboard.region.maximumPoint.z()

		val clipboardOffsetX = (x - clipboard.region.center.x() * 2).roundToInt()
		val clipboardOffsetY = (-y.toDouble()).roundToInt()
		val clipboardOffsetZ = (z - clipboard.region.center.z() * 2).roundToInt()

		return Vec3i(clipboardOffsetX, clipboardOffsetY, clipboardOffsetZ)
	}
}
