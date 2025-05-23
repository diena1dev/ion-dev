package net.horizonsend.ion.server.features.transport.nodes.inputs

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import net.horizonsend.ion.server.features.multiblock.entity.MultiblockEntity
import net.horizonsend.ion.server.features.transport.util.CacheType
import net.horizonsend.ion.server.miscellaneous.utils.coordinates.BlockKey
import java.util.concurrent.ConcurrentHashMap

abstract class InputManager {
	private val typeManagers = ConcurrentHashMap<CacheType, TypeManager>()

	private fun getTypeManager(type: CacheType): TypeManager {
		return typeManagers.getOrPut(type) { TypeManager(this, type) }
	}

	fun registerInput(type: CacheType, location: BlockKey, holder: MultiblockEntity) {
		getTypeManager(type).add(location, holder)
	}

	fun deRegisterInput(type: CacheType, location: BlockKey, holder: MultiblockEntity) {
		getTypeManager(type).remove(location, holder)
	}

	fun getHolders(type: CacheType, location: BlockKey): ObjectOpenHashSet<MultiblockEntity> {
		return getTypeManager(type).getAllHolders(location)
	}

	fun getInputData(type: CacheType, location: BlockKey): TypeManager.InputData? =
		getTypeManager(type).getRaw(location)

	fun getLocations(type: CacheType) = getTypeManager(type).getAllLocations()

	class TypeManager(val manager: InputManager, val type: CacheType) {
		private val inputLocations = ConcurrentHashMap<BlockKey, InputData>()

		fun getAllLocations() = inputLocations.keys

		fun getRaw(location: BlockKey): InputData? = inputLocations[location]

		fun add(location: BlockKey, holder: MultiblockEntity) {
			when (val present: InputData? = inputLocations[location]) {
				is SingleMultiblockInput -> inputLocations[location] = SharedMultiblockInput.of(present.holder, holder)
				is SharedMultiblockInput -> present.add(holder)
				null -> inputLocations[location] = SingleMultiblockInput(holder)
			}
		}

		fun remove(location: BlockKey, holder: MultiblockEntity) {
			when (val present: InputData? = inputLocations.get(location)) {
				is SingleMultiblockInput -> if (present.holder == holder) inputLocations.remove(location)
				is SharedMultiblockInput -> present.remove(holder)
				null -> return
			}
		}

		fun getAllHolders(location: BlockKey): ObjectOpenHashSet<MultiblockEntity> {
			return inputLocations.get(location)?.getHolders() ?: ObjectOpenHashSet()
		}

		fun removeAll(location: BlockKey) {
			inputLocations.remove(location)
		}

		fun contains(location: BlockKey): Boolean {
			return inputLocations.contains(location)
		}

		sealed interface InputData {
			fun contains(holder: MultiblockEntity): Boolean
			fun getHolders(): ObjectOpenHashSet<MultiblockEntity>
		}

		data class SingleMultiblockInput(val holder: MultiblockEntity) : InputData {
			override fun contains(holder: MultiblockEntity): Boolean {
				return this.holder == holder
			}

			override fun getHolders(): ObjectOpenHashSet<MultiblockEntity> {
				return ObjectOpenHashSet.of(holder)
			}
		}

		class SharedMultiblockInput : InputData {
			private val holders: ObjectOpenHashSet<MultiblockEntity> = ObjectOpenHashSet()

			override fun contains(holder: MultiblockEntity): Boolean {
				return holders.contains(holder)
			}

			override fun getHolders(): ObjectOpenHashSet<MultiblockEntity> {
				return ObjectOpenHashSet(holders)
			}

			fun add(multiblockEntity: MultiblockEntity) {
				holders.add(multiblockEntity)
			}

			fun remove(multiblockEntity: MultiblockEntity) {
				holders.remove(multiblockEntity)
			}

			fun getAllHolders() = holders.clone()

			companion object {
				fun of(vararg entities: MultiblockEntity): SharedMultiblockInput {
					val new = SharedMultiblockInput()
					entities.forEach(new::add)
					return new
				}
			}
		}
	}
}
