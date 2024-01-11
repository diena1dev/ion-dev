package net.horizonsend.ion.server.features.economy.bazaar

import net.horizonsend.ion.common.database.schema.economy.BazaarItem
import net.horizonsend.ion.common.extensions.serverError
import net.horizonsend.ion.common.utils.text.colors.HEColorScheme
import net.horizonsend.ion.common.utils.text.template
import net.horizonsend.ion.server.features.economy.city.CityNPCs
import net.horizonsend.ion.server.features.economy.city.TradeCities
import net.horizonsend.ion.server.features.economy.city.TradeCityData
import net.horizonsend.ion.server.features.nations.region.Regions
import net.horizonsend.ion.server.features.nations.region.types.RegionTerritory
import net.horizonsend.ion.server.features.space.Space
import net.horizonsend.ion.server.miscellaneous.registrations.legacy.CustomItem
import net.horizonsend.ion.server.miscellaneous.registrations.legacy.CustomItems
import net.horizonsend.ion.server.miscellaneous.utils.UIUtils
import net.kyori.adventure.text.Component.empty
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor.RED
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.entity.Player
import org.litote.kmongo.eq
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.gui.TabGui
import xyz.xenondevs.invui.gui.structure.Markers
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.invui.item.builder.ItemBuilder
import xyz.xenondevs.invui.item.builder.setDisplayName
import xyz.xenondevs.invui.item.impl.AsyncItem
import xyz.xenondevs.invui.item.impl.SimpleItem
import xyz.xenondevs.invui.item.impl.controlitem.TabItem
import xyz.xenondevs.invui.window.Window

object BazaarGui {
	private fun bazaarItemLoreMessageFormat(content: String, vararg params: Any?) = template(
		message = text(content, HEColorScheme.HE_LIGHT_BLUE),
		paramColor = HEColorScheme.HE_LIGHT_GRAY,
		useQuotesAroundObjects = false,
		*params
	)

	fun openMainMenu(player: Player) {
		val normalWindow = Window.single()
			.setViewer(player)
			.setGui(buildMainMenu(player))
			.setTitle("InvUI")
			.build()
			.open()
	}

	fun getLocationData(player: Player) {

	}

	private fun getCitiesPurchaseMenu(player: Player): Gui {
		val cities: List<TradeCityData> = CityNPCs.BAZAAR_CITY_TERRITORIES
			.map { Regions.get<RegionTerritory>(it) }
//			.filter { Sector.getSector(it.world) == sector }
			.mapNotNull(TradeCities::getIfCity)

		val items = cities.map { getCityItem(player, it) }.toTypedArray()

		val gui2 = Gui.empty(9, 3)

		gui2.addItems(*items)

		return gui2
	}

	private fun getCityItem(player: Player, city: TradeCityData): Item {
		val territoryId = city.territoryId
		val territory: RegionTerritory = Regions[territoryId]

		// attempt to get the planet icon, just use a detonator if unavailable
		// TODO: When porting over planet icons, change the legacy uranium icon too
		val item: CustomItem = Space.getPlanet(territory.world)?.planetIcon ?: CustomItems.MINERAL_URANIUM

		return AsyncItem {
			val remote: Boolean = Regions.findFirstOf<RegionTerritory>(player.location)?.id != territoryId
			val soldCount = BazaarItem.count(BazaarItem::cityTerritory eq territoryId)
			val uniqueCount = BazaarItem.find(BazaarItem::cityTerritory eq territoryId).map(BazaarItem::itemString).distinct().count()

			val lore = listOf(
				bazaarItemLoreMessageFormat("{0} trade city on {1}", city.type.displayName, territory.world),
				bazaarItemLoreMessageFormat("{0} listings, {1} unique items", soldCount, uniqueCount),
				if (remote) text("Warning: You are not within this city's territory. Purchases will have a 4x cost penalty.", RED) else empty()
			)

			UIUtils.createUIItem(
				item = item.itemStack(1),
				name = text(city.displayName),
				lore = lore,
			) { player, event, type ->
				player.serverError("TODO")
			}.itemProvider
		}
	}

	private fun getCategoriesPurchaseMenu(player: Player): Gui {
		val gui2 = Gui.empty(9, 3)
		gui2.fill(SimpleItem(ItemBuilder(Material.IRON_INGOT)), true)
		return gui2
	}

	private fun getAllPurchaseMenu(player: Player): Gui {
		val gui2 = Gui.empty(9, 3)
		gui2.fill(SimpleItem(ItemBuilder(Material.RAW_COPPER)), true)
		return gui2
	}

	private fun getCitiesRequestMenu(player: Player): Gui {
		val gui2 = Gui.empty(9, 3)
		gui2.fill(SimpleItem(ItemBuilder(Material.OAK_BOAT)), true)
		return gui2
	}
	private fun getCategoriesRequestMenu(player: Player): Gui {
		val gui2 = Gui.empty(9, 3)
		gui2.fill(SimpleItem(ItemBuilder(Material.RED_STAINED_GLASS_PANE)), true)
		return gui2
	}
	private fun getAllRequestMenu(player: Player): Gui {
		val gui2 = Gui.empty(9, 3)
		gui2.fill(SimpleItem(ItemBuilder(Material.DIAMOND)), true)
		return gui2
	}

	private fun getTabItem(tab: Int): TabItem = object : TabItem(tab) {
		override fun getItemProvider(gui: TabGui): ItemProvider = when (tab) {
			0 -> ItemBuilder(Material.BRICKS)
				.setDisplayName(text("View Sold Items by City").decoration(TextDecoration.ITALIC, false))
			1 -> ItemBuilder(Material.DROPPER)
				.setDisplayName(text("View Sold Items by Category").decoration(TextDecoration.ITALIC, false))
			2 -> ItemBuilder(Material.NAME_TAG)
				.setDisplayName(text("Search all Sold Items").decoration(TextDecoration.ITALIC, false))
			3 -> ItemBuilder(Material.BRICKS)
				.setDisplayName(text("View Requests by City").decoration(TextDecoration.ITALIC, false))
			4 -> ItemBuilder(Material.DROPPER)
				.setDisplayName(text("View Requests by Category").decoration(TextDecoration.ITALIC, false))
			else -> ItemBuilder(Material.NAME_TAG)
				.setDisplayName(text("Search all Requests").decoration(TextDecoration.ITALIC, false))
		}
	}

	fun buildMainMenu(player: Player): Gui = TabGui.normal()
		.setStructure(
			"# 0 1 2 # 3 4 5 #",
			"x x x x x x x x x",
			"x x x x x x x x x",
			"x x x x x x x x x"
		)
		.addIngredient('x', Markers.CONTENT_LIST_SLOT_VERTICAL)
		.addIngredient('#', UIUtils.getBorderItem())
		.addIngredient('0', getTabItem(0))
		.addIngredient('1', getTabItem(1))
		.addIngredient('2', getTabItem(2))
		.addIngredient('3', getTabItem(3))
		.addIngredient('4', getTabItem(4))
		.addIngredient('5', getTabItem(5))
		.setTabs(listOf(
			getCitiesPurchaseMenu(player),
			getCategoriesPurchaseMenu(player),
			getAllPurchaseMenu(player),
			getCitiesRequestMenu(player),
			getCategoriesRequestMenu(player),
			getAllRequestMenu(player)
		))
		.build()


}


