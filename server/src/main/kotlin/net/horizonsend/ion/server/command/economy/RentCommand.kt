package net.horizonsend.ion.server.command.economy

import co.aikar.commands.annotation.CommandAlias
import net.horizonsend.ion.server.command.SLCommand
import org.bukkit.entity.Player

@CommandAlias("rent")
object RentCommand : SLCommand() {
	fun onPay(sender: Player) {}
	fun onRelease(sender: Player) {}
	fun onRent(sender: Player) {}
	fun balance(sender: Player) {}
	fun list(sender: Player) {}
}
