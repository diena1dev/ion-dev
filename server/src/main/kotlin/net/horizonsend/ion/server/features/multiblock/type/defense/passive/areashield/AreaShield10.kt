package net.horizonsend.ion.server.features.multiblock.type.defense.passive.areashield

import net.horizonsend.ion.server.features.multiblock.shape.MultiblockShape

object AreaShield10 : AreaShield(radius = 10) {
	override fun MultiblockShape.buildStructure() {
		z(+0) {
			y(-1) {
				x(-1).anyStairs()
				x(+0).powerInput()
				x(+1).anyStairs()
			}

			y(+0) {
				x(+0).anyGlass()
			}
		}

		z(+1) {
			y(-1) {
				x(-1).ironBlock()
				x(+0).sponge()
				x(+1).ironBlock()
			}

			y(+0) {
				x(-1).anyGlassPane()
				x(+0).sponge()
				x(+1).anyGlassPane()
			}
		}

		z(+2) {
			y(-1) {
				x(-1).anyStairs()
				x(+0).solidBlock()
				x(+1).anyStairs()
			}

			y(0) {
				x(0).anyGlass()
			}
		}
	}
}
