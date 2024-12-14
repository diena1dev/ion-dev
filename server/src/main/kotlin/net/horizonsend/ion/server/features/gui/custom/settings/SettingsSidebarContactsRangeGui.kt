package net.horizonsend.ion.server.features.gui.custom.settings

import net.horizonsend.ion.server.features.gui.GuiItem
import net.horizonsend.ion.server.features.sidebar.MainSidebar
import net.horizonsend.ion.server.features.sidebar.command.SidebarContactsCommand
import net.horizonsend.ion.server.miscellaneous.utils.applyDisplayName
import net.kyori.adventure.text.Component.text
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import xyz.xenondevs.inventoryaccess.component.AdventureComponentWrapper
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.invui.item.builder.ItemBuilder
import xyz.xenondevs.invui.item.impl.AbstractItem
import xyz.xenondevs.invui.item.impl.controlitem.ControlItem
import xyz.xenondevs.invui.window.AnvilWindow
import xyz.xenondevs.invui.window.Window

class SettingsSidebarContactsRangeGui(val player: Player) {

    private var currentWindow: Window? = null

    private fun createGui(): Gui {
        val gui = Gui.normal()

        gui.setStructure(". v x")

        gui.addIngredient('x', SetContactsDistanceButton())
            .addIngredient('.', RenameItem())
            .addIngredient('v', SettingsSidebarContactsGui(player).ReturnToSidebarContactsButton())

        return gui.build()
    }

    fun open(player: Player): Window {
        val gui = createGui()

        val window = AnvilWindow.single()
            .setViewer(player)
            .setTitle(AdventureComponentWrapper(text("Set Contacts Distance")))
            .setGui(gui)
            .build()

        return window
    }

    fun openMainWindow() {
        currentWindow = open(player).apply { open() }
    }

    private class SetContactsDistanceButton : ControlItem<Gui>() {
        override fun getItemProvider(gui: Gui?): ItemProvider {
            val builder = ItemBuilder(GuiItem.RIGHT.makeItem().applyDisplayName(text("Set Contacts Distance")))
            return builder
        }

        override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
            val currentWindow = (windows.find { it.viewer == player })
            val anvilWindow = if (currentWindow is AnvilWindow) currentWindow else return
            val currentText = anvilWindow.renameText ?: return
            val currentInt = currentText.toIntOrNull() ?: return

            SidebarContactsCommand.onSetContactsDistance(player, currentInt)
            SettingsSidebarContactsGui(player).openMainWindow()
        }
    }

    private class RenameItem : AbstractItem() {
        override fun getItemProvider(): ItemProvider {
            return ItemBuilder(GuiItem.LIST.makeItem().applyDisplayName(text("Enter Range (0-${MainSidebar.CONTACTS_RANGE})")))
        }

        override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {}
    }
}
