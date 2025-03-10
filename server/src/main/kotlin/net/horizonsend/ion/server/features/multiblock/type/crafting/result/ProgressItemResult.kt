package net.horizonsend.ion.server.features.multiblock.type.crafting.result

import net.horizonsend.ion.server.features.custom.items.CustomItem
import net.horizonsend.ion.server.features.custom.items.CustomItemRegistry.customItem
import net.horizonsend.ion.server.features.custom.items.type.ProgressHolder
import net.horizonsend.ion.server.features.multiblock.type.crafting.recipe.MultiblockRecipe
import org.bukkit.block.Sign
import org.bukkit.inventory.FurnaceInventory
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

/**
 * This item will convert into the result custom item after the amount of ticks provided (Assuming no lag and a 200 tick interval on the furnace)
 **/
class ProgressItemResult<R: CustomItem>(val result: R, time: Long, private val onComplete: ActionResult? = null) : ActionResult {
	// 200 is the standard furnace tick interval
	private val increment = 200.0 / time.toDouble()

	override fun execute(recipe: MultiblockRecipe<*>, craftingInventory: Inventory, sign: Sign) {
		if (craftingInventory !is FurnaceInventory) return

		val smelting = craftingInventory.smelting ?: return

		if (smelting.customItem is ProgressHolder) {
			incrementProgress(smelting, craftingInventory, recipe, craftingInventory, sign)
			return
		}

		craftingInventory.smelting = ProgressHolder.create(result)
	}

	private fun incrementProgress(progressItem: ItemStack, inventory: FurnaceInventory, recipe: MultiblockRecipe<*>, craftingInventory: Inventory, sign: Sign) {
		val current = ProgressHolder.getProgress(progressItem)

		if (ProgressHolder.setProgress(progressItem, current + increment)) {
			inventory.result = inventory.smelting
			inventory.smelting = null
			onComplete?.execute(recipe, craftingInventory, sign) ?: return
		}
	}
}
