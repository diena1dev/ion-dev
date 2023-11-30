package net.horizonsend.ion.common.database.schema.economy

import net.horizonsend.ion.common.database.DbObject
import net.horizonsend.ion.common.database.Oid
import net.horizonsend.ion.common.database.OidDbObjectCompanion
import net.horizonsend.ion.common.database.schema.misc.SLPlayerId
import net.horizonsend.ion.common.database.schema.nations.Territory
import org.litote.kmongo.ensureIndex
import org.litote.kmongo.ensureUniqueIndex
import java.util.Date

class BazaarOrder(
	override val _id: Oid<BazaarItem>,
	val cityTerritory: Oid<Territory>,
	val requestingPlayer: SLPlayerId,
	var itemString: String,
	var requestedOn: Date,

	var pricePerUnit: Double,
	var requestedAmount: Int,
	var balance: Double
) : DbObject {
	companion object : OidDbObjectCompanion<BazaarItem>(BazaarItem::class, setup = {
		ensureIndex(BazaarOrder::cityTerritory)
		ensureIndex(BazaarOrder::requestingPlayer)
		ensureIndex(BazaarOrder::itemString)

		// don't allow one person to sell the same type of item for different prices in one city
		ensureUniqueIndex(BazaarOrder::cityTerritory, BazaarOrder::requestingPlayer, BazaarOrder::itemString)
	}) {

	}
}
