package net.horizonsend.ion.server.miscellaneous.utils

import net.horizonsend.ion.common.utils.text.template
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.empty
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor.GREEN
import net.kyori.adventure.text.format.NamedTextColor.RED
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.invui.gui.PagedGui
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.invui.item.builder.ItemBuilder
import xyz.xenondevs.invui.item.builder.setDisplayName
import xyz.xenondevs.invui.item.builder.setLore
import xyz.xenondevs.invui.item.impl.AbstractItem
import xyz.xenondevs.invui.item.impl.SimpleItem
import xyz.xenondevs.invui.item.impl.controlitem.PageItem

object UIUtils {


	fun ItemStack.uiItem(onClick: (Player, InventoryClickEvent, ClickType) -> Unit): Item = uiItem(onClick)

	fun ItemStack.uiItem(
		name: Component? = null,
		lore: List<Component>? = null,
		onClick: (Player, InventoryClickEvent, ClickType) -> Unit
	): Item = createUIItem(this, name, lore, onClick)

	fun createUIItem(
		item: Material,
		name: Component? = null,
		lore: List<Component>? = null,
		onClick: (Player, InventoryClickEvent, ClickType) -> Unit
	): Item = createUIItem(ItemStack(item), name, lore, onClick)

	/**
	 * Creates a UI Item
	 *
	 * @param item the item stack to display
	 * @param name the name of the item
	 * @param lore the lore of the item
	 * @param onClick the function to be executed when
	 **/
	fun createUIItem(
		item: ItemStack,
		name: Component? = null,
		lore: List<Component>? = null,
		onClick: (Player, InventoryClickEvent, ClickType) -> Unit
	): Item = object : AbstractItem() {
		override fun getItemProvider(): ItemProvider {
			val builder = ItemBuilder(item)

			name?.let { builder.setDisplayName(name) }
			lore?.let { builder.setLore(lore) }

			return builder
		}

		override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
			onClick(player, event, clickType)
		}
	}

	// Items
	fun getBackItem() : PageItem = object : PageItem(false) {
		override fun getItemProvider(gui: PagedGui<*>): ItemProvider {
			val itemBuilder = ItemBuilder(Material.RED_STAINED_GLASS_PANE)

			itemBuilder.setDisplayName(text("Previous Page", RED))

			val lore = if (gui.hasNextPage()) template(text("Go to page {0}/{1}", RED), (gui.currentPage), gui.pageAmount)
			else text("You can't go further back", RED)

			itemBuilder.setLore(listOf(lore))

			return itemBuilder
		}
	}

	fun getForwardItem() : PageItem = object : PageItem(false) {
		override fun getItemProvider(gui: PagedGui<*>): ItemProvider {
			val itemBuilder = ItemBuilder(Material.LIME_STAINED_GLASS_PANE)

			itemBuilder.setDisplayName(text("Next Page", GREEN))

			val lore = if (gui.hasNextPage()) template(text("Go to page {0}/{1}", GREEN), (gui.currentPage + 2), gui.pageAmount)
				else text("There are no more pages", GREEN)

			itemBuilder.setLore(listOf(lore))

			return itemBuilder
		}
	}

	fun getBorderItem() = SimpleItem(ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setDisplayName(empty()))
}
