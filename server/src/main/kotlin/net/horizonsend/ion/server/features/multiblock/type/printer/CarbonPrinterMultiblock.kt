package net.horizonsend.ion.server.features.multiblock.type.printer

import net.horizonsend.ion.server.features.multiblock.shape.MultiblockShape
import net.horizonsend.ion.server.miscellaneous.utils.isConcretePowder
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

abstract class AbstractCarbonPrinterMultiblock : PrinterMultiblock() {
	override val signText = createSignText(
		"&bCarbon",
		"&fPrinter",
		"",
		"&7-:[=]:-"
	)
	override val description: Component get() = text("Transforms Cobblestone into Concrete Powder.")
	override fun getOutput(product: Material): ItemStack = ItemStack(product, 2)

	override fun MultiblockShape.RequirementBuilder.printerMachineryBlock() = sponge()
	override fun MultiblockShape.RequirementBuilder.printerCoreBlock() = type(Material.MAGMA_BLOCK)
	override fun MultiblockShape.RequirementBuilder.printerProductBlock() = filteredTypes("any concrete powder") { it.isConcretePowder }
}

object CarbonPrinterMultiblock : AbstractCarbonPrinterMultiblock() {
	override val mirrored = false
	override val displayName: Component get() = text("Carbon Printer")
}

object CarbonPrinterMultiblockMirrored : AbstractCarbonPrinterMultiblock() {
	override val mirrored = true
	override val displayName: Component get() = text("Carbon Printer (Mirrored)")
}
