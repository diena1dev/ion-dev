package net.horizonsend.ion.server.features.economy.bazaar

import net.horizonsend.ion.server.miscellaneous.registrations.legacy.CustomItems as LegacyCustomItems
import com.github.stefvanschie.inventoryframework.gui.GuiItem
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.mongodb.client.FindIterable
import net.horizonsend.ion.common.database.Oid
import net.horizonsend.ion.common.database.schema.economy.BazaarItem
import net.horizonsend.ion.common.database.schema.misc.SLPlayer
import net.horizonsend.ion.common.database.schema.nations.Settlement
import net.horizonsend.ion.common.database.schema.nations.Territory
import net.horizonsend.ion.common.extensions.information
import net.horizonsend.ion.common.extensions.serverError
import net.horizonsend.ion.common.extensions.userError
import net.horizonsend.ion.common.utils.miscellaneous.toCreditsString
import net.horizonsend.ion.common.utils.text.toCreditComponent
import net.horizonsend.ion.server.IonServerComponent
import net.horizonsend.ion.server.command.economy.BazaarCommand
import net.horizonsend.ion.server.features.customitems.CustomItems
import net.horizonsend.ion.server.features.customitems.CustomItems.customItem
import net.horizonsend.ion.server.features.economy.city.TradeCities
import net.horizonsend.ion.server.features.economy.city.TradeCityData
import net.horizonsend.ion.server.features.economy.city.TradeCityType
import net.horizonsend.ion.server.features.nations.gui.guiButton
import net.horizonsend.ion.server.features.nations.gui.lore
import net.horizonsend.ion.server.features.nations.gui.playerClicker
import net.horizonsend.ion.server.features.nations.region.Regions
import net.horizonsend.ion.server.features.nations.region.types.RegionTerritory
import net.horizonsend.ion.server.miscellaneous.utils.MenuHelper
import net.horizonsend.ion.server.miscellaneous.utils.MenuHelper.setLore
import net.horizonsend.ion.server.miscellaneous.utils.MenuHelper.setLoreComponent
import net.horizonsend.ion.server.miscellaneous.utils.MenuHelper.setName
import net.horizonsend.ion.server.miscellaneous.utils.Tasks
import net.horizonsend.ion.server.miscellaneous.utils.VAULT_ECO
import net.horizonsend.ion.server.miscellaneous.utils.displayNameComponent
import net.horizonsend.ion.server.miscellaneous.utils.displayNameString
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.empty
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.Component.textOfChildren
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.NamedTextColor.AQUA
import net.kyori.adventure.text.format.NamedTextColor.GOLD
import net.kyori.adventure.text.format.NamedTextColor.RED
import net.kyori.adventure.text.format.NamedTextColor.WHITE
import net.kyori.adventure.text.format.NamedTextColor.YELLOW
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.litote.kmongo.and
import org.litote.kmongo.ascendingSort
import org.litote.kmongo.descendingSort
import org.litote.kmongo.eq
import org.litote.kmongo.gt
import org.litote.kmongo.ne
import java.util.Optional
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.reflect.KProperty

object Bazaars : IonServerComponent() {
	val strings = mutableListOf<String>().apply {
		addAll(Material.values().filter { it.isItem && !it.isLegacy }.map { it.name })
		addAll(LegacyCustomItems.all().map { it.id })
		addAll(CustomItems.identifiers)
	}

	/** Usually optional data for bazaar. Optional in the case of a global search. */
	data class CityInfo(val territoryId: Oid<Territory>, val remote: Boolean)

    fun onClickBazaarNPC(player: Player, city: TradeCityData) {
		val territoryId: Oid<Territory> = city.territoryId

		openCityMenu(territoryId, player, false)
	}

	/**
	 * Opens the menu for the city
	 *
	 * The city's items are broken down by category
	 *
	 * Buttons:
	 * 	- searchButton: Searches all the city's items
	 * 	- backButton: Returns to the global bazaar menu (all cities)
	 * 	- Categories: The items, sorted by creative mode category
	 **/
	fun openCityMenu(territoryId: Oid<Territory>, player: Player, remote: Boolean): Unit = Tasks.async {
		MenuHelper.run {
			val backButton = backButton(text = "Go Back to City Selection") { BazaarCommand.onBrowse(player) }

			val searchButton = searchButton(
				searchInputText = "Enter Item Name",
				backButtonText = "Go Back to City",
				searchFunction = { input: String -> getGuiItems(search(territoryId, input), CityInfo(territoryId, remote)) },
				searchBackFunction = { openCityMenu(territoryId, it, remote) }
			)

			val titleButtons: List<GuiItem> = listOf(
				backButton,
				searchButton
			)

			val items: List<GuiItem> = getCategoryButtons(territoryId, remote)
			val cityName = TradeCities.getIfCity(Regions[territoryId])?.displayName ?: return@async player.serverError("Territory is no longer a city")

			Tasks.sync {
				player.openPaginatedMenu("City: $cityName", items, titleButtons)
			}
		}
	}

	/** Returns the category buttons, to be used only for a single city's items. */
	private fun getCategoryButtons(territoryId: Oid<Territory>, remote: Boolean): List<GuiItem> {
		val cityItems = getCityItems(territoryId)

		val containedCategories = ItemCategory.all().filter {
			category -> cityItems.any { category.items.contains(it.itemString) }
		}

		return containedCategories.map { category ->
			guiButton(category.displayItem) {
				openCategoryMenu(category, territoryId, playerClicker, remote)
			}
			.setName(category.displayName)
		}
	}

	/** Returns the items from all the items that are in the category */
	private fun getCategoryItems(category: ItemCategory, allItems: FindIterable<BazaarItem>, cityInfo: CityInfo?): List<GuiItem> {
		val items = allItems.filter { category.items.contains(it.itemString) }

		return getGuiItems(items, cityInfo)
	}

	/**
	 * Opens the menu for the items of a single category for a city
	 *
	 * Buttons:
	 * 	- searchButton: Searches all the city's items
	 * 	- backButton: Returns to the city categories menu
	 * 	- Items: Types of items for sale at this city. Will open a menu for multiple listings if there are more than one.
	 **/
	private fun openCategoryMenu(category: ItemCategory, territoryId: Oid<Territory>, player: Player, remote: Boolean) = Tasks.async {
		MenuHelper.run {
			val backButton = backButton(text = "Go Back to Category Selection") { openCityMenu(territoryId, player, remote) }

			val searchButton = searchButton(
				searchInputText = "Enter Item Name",
				backButtonText = "Go Back to City",
				searchFunction = { input: String -> getGuiItems(search(territoryId, input), CityInfo(territoryId, remote)) },
				searchBackFunction = { openCityMenu(territoryId, it, remote) }
			)

			val titleButtons: List<GuiItem> = listOf(
				backButton,
				searchButton
			)

			val items: List<GuiItem> = getCategoryItems(category, getCityItems(territoryId), CityInfo(territoryId, remote))
			val cityName = TradeCities.getIfCity(Regions[territoryId])?.displayName ?: return@async player.serverError("Territory is no longer a city")

			Tasks.sync {
				player.openPaginatedMenu("City: $cityName", items, titleButtons)
			}
		}
	}

	fun getGuiItems(bazaarItems: List<BazaarItem>, cityInfo: CityInfo?): List<GuiItem> =
		bazaarItems
			.map(BazaarItem::itemString)
			// only one per item string
			.distinct()
			// convert to GuiItem
			.map { itemString ->
				val item: ItemStack = fromItemString(itemString)

				return@map if (cityInfo != null) {
					val territoryId = cityInfo.territoryId
					val remote = cityInfo.remote
					val items = BazaarItem.count(and(BazaarItem::cityTerritory eq territoryId, BazaarItem::itemString eq itemString, BazaarItem::stock gt 0))

					val lore = if (items > 1) {
						listOf(text("View all $itemString on sale at this city", AQUA).decoration(TextDecoration.ITALIC, false))
					} else {
						val listing = BazaarItem.findOne(and(BazaarItem::cityTerritory eq territoryId, BazaarItem::itemString eq itemString, BazaarItem::stock gt 0))!!
						val sellerName = SLPlayer.getName(listing.seller)!!

						listOf(
							textOfChildren(text("Seller: ", AQUA), text(sellerName)).decoration(TextDecoration.ITALIC, false),
							textOfChildren(text("Stock: ", AQUA), text(listing.stock)).decoration(TextDecoration.ITALIC, false)
						)
					}

					return@map MenuHelper.guiButton(item) { openItemTypeMenu(playerClicker, territoryId, itemString, SortingBy.STOCK, true, remote) }
						.setLoreComponent(lore)
				} else {
					MenuHelper.guiButton(item) {
						openItemTypeMenu(playerClicker, itemString, SortingBy.STOCK, true)
					}
				}
			}

	private fun getCityItems(territoryId: Oid<Territory>): FindIterable<BazaarItem> = BazaarItem
		.find(and(BazaarItem::cityTerritory eq territoryId, BazaarItem::stock gt 0))

	enum class SortingBy(val property: KProperty<*>, private val displayType: Material) {
		PRICE(BazaarItem::price, Material.GOLD_INGOT),
		STOCK(BazaarItem::stock, Material.NAME_TAG)

		;

		val lore = listOf("Left click to sort descending,", "right click to sort ascending.")

		fun getButton(itemString: String, cityInfo: CityInfo?): GuiItem = MenuHelper.guiButton(displayType) {
			playerClicker.closeInventory()

			if (cityInfo == null) { openItemTypeMenu(playerClicker, itemString, this@SortingBy, isLeftClick) } else {
				openItemTypeMenu(playerClicker, cityInfo.territoryId, itemString, this@SortingBy, isLeftClick, cityInfo.remote)
			}
		}
		.setName("Sort By $this@SortingBy")
		.setLore(lore)

		companion object {
			fun getButtons(itemString: String, cityInfo: CityInfo?) = values().map { it.getButton(itemString, cityInfo) }
		}
	}

	private fun getListingsForItem(player: Player, items: FindIterable<BazaarItem>, sort: SortingBy, descend: Boolean, cityInfo: CityInfo?): List<GuiItem> {
		val territory = Regions.find(player.location).filterIsInstance<RegionTerritory>().firstOrNull()

		return items
			.let { if (descend) it.descendingSort(sort.property) else it.ascendingSort(sort.property) }
			.map { bazaarItem ->
				val itemStack = fromItemString(bazaarItem.itemString)

				val sellerName = SLPlayer.getName(bazaarItem.seller) ?: error("Failed to get name of ${bazaarItem.seller}")
				val priceString = bazaarItem.price.toCreditsString()
				val stock = bazaarItem.stock

				val remote: Boolean = cityInfo?.remote ?: (bazaarItem.cityTerritory != territory?.id)
				val cityData:  TradeCityData = cityInfo?.territoryId?.let { TradeCities.getIfCity(Regions[cityInfo.territoryId]) }
					?: TradeCities.getIfCity(Regions[bazaarItem.cityTerritory])
					?: error("$territory is no longer a city!")

				return@map MenuHelper.guiButton(itemStack) {
					openPurchaseMenu(playerClicker, bazaarItem, sellerName, 0, remote)
				}
				.setName(priceString)
				.setLoreComponent(listOf(
					textOfChildren(text("Seller: ", AQUA), text(sellerName, WHITE)).decoration(TextDecoration.ITALIC, false),
					textOfChildren(text("Stock: ", AQUA), text(stock, WHITE)).decoration(TextDecoration.ITALIC, false),
					textOfChildren(text("Price: ", AQUA), text(priceString, GOLD)).decoration(TextDecoration.ITALIC, false),
					if (cityInfo == null) textOfChildren(text("City: ", AQUA), text(cityData.displayName, WHITE)).decoration(TextDecoration.ITALIC, false) else empty()
				))
			}
			.toList()
	}

	/** Opens the menu containing listings for a specific item. Searches all territories */
	private fun openItemTypeMenu(
		player: Player,
		item: String,
		sort: SortingBy,
		descend: Boolean
	): Unit = Tasks.async {
		MenuHelper.run {
			val titleItems: List<GuiItem> = SortingBy.getButtons(item, null) + backButton { BazaarCommand.onBrowse(it) }

			val items = BazaarItem.find(and(BazaarItem::itemString eq item, BazaarItem::stock gt 0))
			val guiItems: List<GuiItem> = getListingsForItem(player, items, sort, descend, null)

			val name = fromItemString(item).displayNameString

			Tasks.sync {
				player.openPaginatedMenu(name, guiItems, titleItems)
			}
		}
	}

	/** Opens the menu containing listings for a specific item */
	private fun openItemTypeMenu(
        player: Player,
        terrId: Oid<Territory>,
        item: String,
        sort: SortingBy,
        descend: Boolean,
        remote: Boolean
	): Unit = Tasks.async {
		val city: TradeCityData = TradeCities.getIfCity(Regions[terrId])
			?: return@async player.serverError("Territory is no longer a city")

		val items = BazaarItem.find(and(BazaarItem::cityTerritory eq terrId, BazaarItem::itemString eq item, BazaarItem::stock gt 0))

		// If ony one item jump straight to the purchase menu
		if (items.count() == 1) {
			val bazaarItem = items.first()!!

			Tasks.sync { openPurchaseMenu(player, bazaarItem, SLPlayer.getName(bazaarItem.seller)!!, 0, remote) }
			return@async
		}

		MenuHelper.run {
			val titleItems: List<GuiItem> = SortingBy.getButtons(item, CityInfo(terrId, remote)) + backButton { openCityMenu(terrId, it, remote) }
			val guiItems = getListingsForItem(player, items, sort, descend, CityInfo(terrId, remote))
			val name = fromItemString(item).displayNameString

			Tasks.sync {
				player.openPaginatedMenu("$name @ ${city.displayName}", guiItems, titleItems)
			}
		}
	}

	private fun priceMult(remote: Boolean) = if (remote) 4 else 1

	private fun openPurchaseMenu(
		player: Player,
		item: BazaarItem,
		sellerName: String,
		currentAmount: Int,
		remote: Boolean
	) {
		MenuHelper.apply {
			val pane = outlinePane(0, 0, 9, 1)

			val priceMult = priceMult(remote)

			fun addButton(amount: Int) {
				val buttonType = if (amount < 0) Material.RED_STAINED_GLASS_PANE else Material.LIME_STAINED_GLASS_PANE
				val buttonItem = ItemStack(buttonType)
				buttonItem.amount = amount.absoluteValue
				pane.addItem(
					guiButton(buttonItem) {
						val newAmount = currentAmount + amount
						if (item.stock - newAmount >= 0 && newAmount >= 0) {
							val cost: Double = newAmount * item.price * priceMult

							if (!VAULT_ECO.has(playerClicker, cost)) {
								playerClicker.userError(
									"You don't have enough credits! Cost for $newAmount: ${cost.toCreditsString()}" +
										if (priceMult > 1) " (Price multiplied x $priceMult due to browsing remotely)" else ""
								)
							} else {
								openPurchaseMenu(playerClicker, item, sellerName, newAmount, remote)
							}
						}
					}.setName((if (amount < 0) "Subtract" else "Add") + " ${amount.absoluteValue}")
				)
			}

			addButton(-32)
			addButton(-8)
			addButton(-1)
			addButton(1)
			addButton(8)
			addButton(32)
			addButton(64)

			pane.addItem(backButton { openItemTypeMenu(it, item.cityTerritory, item.itemString, SortingBy.STOCK, true, remote) })

			val name = fromItemString(item.itemString).displayNameString

			if (currentAmount == 0) {
				pane.addItem(guiButton(Material.BARRIER).setName("Buy at least one item"))
			} else {
				val lore = mutableListOf<Component>()

				lore += text("Buy $currentAmount of $name for ${item.price * currentAmount * priceMult}", WHITE).decoration(TextDecoration.ITALIC, false)

				if (priceMult > 1) {
					lore += text("(Price multiplied x $priceMult due to browsing remotely)", RED).decoration(TextDecoration.ITALIC, false)
				}

				pane.addItem(
					guiButton(Material.HOPPER) {
						playerClicker.closeInventory()
						tryBuy(playerClicker, item, currentAmount, remote)
					}.setName(text("Purchase").color(NamedTextColor.GREEN)).lore(lore)
				)
			}

			gui(1, "$currentAmount/${item.stock} $sellerName's $name").withPane(pane).show(player)
		}
	}

	private fun search(territoryId: Oid<Territory>, search: String): List<BazaarItem> =
		getCityItems(territoryId).filter { it.itemString.contains(search, true) }

	fun searchAll(search: String): List<BazaarItem> = BazaarItem.all().filter { it.itemString.contains(search, true) }

	private fun tryBuy(player: Player, item: BazaarItem, amount: Int, remote: Boolean) {
		val price: Double = item.price
		val revenue: Double = amount * price
		val priceMult = priceMult(remote)
		val cost: Double = revenue * priceMult

		if (!VAULT_ECO.has(player, cost)) {
			return player.userError(
				"You can't afford that! Cost for $amount: ${cost.toCreditsString()}" +
					if (priceMult > 1) " (Price multiplied x $priceMult due to browsing remotely)" else ""
			)
		}

		val city: TradeCityData = TradeCities.getIfCity(Regions[item.cityTerritory]) ?: return

		Tasks.async {
			if (!BazaarItem.hasStock(item._id, amount)) {
				return@async player.information("Item no longer has $amount in stock")
			}

			if (BazaarItem.matches(item._id, BazaarItem::price ne price)) {
				return@async player.userError("Price has changed")
			}

			val itemStack = fromItemString(item.itemString)

			BazaarItem.removeStock(item._id, amount)

			val tax = (city.tax * revenue).roundToInt()
			BazaarItem.depositMoney(item._id, revenue - tax)
			if (city.type == TradeCityType.SETTLEMENT) {
				Settlement.deposit(city.settlementId, tax)
			}

			Tasks.sync {
				VAULT_ECO.withdrawPlayer(player, cost)
				val (fullStacks, remainder) = dropItems(itemStack, amount, player)

				val buyMessage = text().color(NamedTextColor.GREEN).append(textOfChildren(
					text("Bought "),
					text(fullStacks, WHITE),
					if (itemStack.maxStackSize == 1) {
						textOfChildren(text(" "), itemStack.displayNameComponent.append(if (fullStacks == 1) text("") else text("s")))
					} else {
						textOfChildren(
							if (fullStacks == 1) text(" stack and ") else text(" stacks and "),
							text(remainder, WHITE),
							if (remainder == 1) text(" item") else text(" items"),
							text(" of "),
							itemStack.displayNameComponent
						)
					},
					text(" for "),
					cost.toCreditComponent()
				))

				if (priceMult > 1) buyMessage.append(textOfChildren(
					text(" (Price multiplied by ", YELLOW),
					text(priceMult, WHITE),
					text(" due to browsing remotely)", YELLOW))
				)

				player.sendMessage(
					buyMessage
				)
			}
		}
	}

	fun toItemString(item: ItemStack): String {
		return item.customItem?.identifier ?: LegacyCustomItems[item]?.id ?: item.type.toString()
	}

	val stringItemCache: LoadingCache<String, Optional<ItemStack>> = CacheBuilder.newBuilder().build(
		CacheLoader.from { string -> Optional.ofNullable(stringToItem(string)) }
	)

	fun fromItemString(string: String): ItemStack = stringItemCache[string].get().clone()

	fun stringToItem(string: String): ItemStack? {
		// if a custom item is found, use that
		CustomItems.getByIdentifier(string)?.let { return it.constructItemStack() }
		LegacyCustomItems[string]?.let { return it.itemStack(1) }

		val material: Material = try { Material.valueOf(string) } catch (e: Throwable) { return null }

		if (!material.isItem) return null
		return ItemStack(material, 1)
	}

	fun dropItems(itemStack: ItemStack, amount: Int, sender: Player): Pair<Int, Int> {
		val maxStackSize = itemStack.maxStackSize
		val fullStacks = amount / maxStackSize

		fun add(amount: Int) {
			val stack = itemStack.clone().apply { this.amount = amount }
			val remainder: HashMap<Int, ItemStack> = sender.inventory.addItem(stack)

			// remainder is when the inventory didn't have space

			for (remainingItem in remainder.values) {
				sender.world.dropItem(sender.eyeLocation, remainingItem)
			}
		}

		repeat(fullStacks) { add(maxStackSize) }
		val remainder = amount % maxStackSize

		if (remainder > 0) {
			add(remainder)
		}

		return Pair(fullStacks, remainder)
	}
}
