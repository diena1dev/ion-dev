package net.horizonsend.ion.server.miscellaneous.utils

import org.bukkit.Material
import org.bukkit.Registry
import org.bukkit.block.data.Bisected
import java.util.EnumSet

/**
 * This should be used instead of Material.values() to avoid encountering legacy materials
 */
val MATERIALS: Registry<Material> = Registry.MATERIAL

fun getMatchingMaterials(filter: (Material) -> Boolean): EnumSet<Material> =
	MATERIALS.filterTo(EnumSet.noneOf(Material::class.java), filter)

val ALL_GLASS_TYPES = getMatchingMaterials { it.name.endsWith("_STAINED_GLASS") } + Material.GLASS
val STAINED_GLASS_TYPES = getMatchingMaterials { it.name.endsWith("_STAINED_GLASS") }
val Material.isGlass: Boolean get() = this == Material.GLASS || this.isStainedGlass
val Material.isStainedGlass: Boolean get() = STAINED_GLASS_TYPES.contains(this)
val Material.isTintedGlass: Boolean get() = equals(Material.TINTED_GLASS)

val STAINED_GLASS_PANE_TYPES = getMatchingMaterials { it.name.endsWith("_STAINED_GLASS_PANE") }
val Material.isGlassPane: Boolean get() = this == Material.GLASS_PANE || this.isStainedGlassPane
val Material.isStainedGlassPane: Boolean get() = STAINED_GLASS_PANE_TYPES.contains(this)

val Material.isLava: Boolean get() = this == Material.LAVA

val Material.isWater: Boolean get() = this == Material.WATER

val Material.isRedstoneLamp: Boolean get() = this == Material.REDSTONE_LAMP

val Material.isDaylightSensor: Boolean get() = this == Material.DAYLIGHT_DETECTOR

val BUTTON_TYPES = getMatchingMaterials { it.name.endsWith("_BUTTON") }
val Material.isButton: Boolean get() = BUTTON_TYPES.contains(this)

val CANDLE_TYPES = getMatchingMaterials { it.name.endsWith("CANDLE") }
val Material.isCandle get() = CANDLE_TYPES.contains(this)

val CAKE_TYPES = getMatchingMaterials { it.name.endsWith("CAKE") }

val DOOR_TYPES = getMatchingMaterials { it.name.endsWith("_DOOR") }
val Material.isDoor: Boolean get() = DOOR_TYPES.contains(this)

val TRAPDOOR_TYPES = getMatchingMaterials { it.name.endsWith("_TRAPDOOR") }
val Material.isTrapdoor: Boolean get() = TRAPDOOR_TYPES.contains(this)

val Material.isSeaLantern: Boolean get() = this == Material.SEA_LANTERN

val PRESSURE_PLATE_TYPES = getMatchingMaterials { it.name.endsWith("_PRESSURE_PLATE") }

val STAIR_TYPES = getMatchingMaterials { it.name.endsWith("_STAIRS") }
val Material.isStairs: Boolean get() = STAIR_TYPES.contains(this)

val SHULKER_BOX_TYPES = getMatchingMaterials { it.name.endsWith("SHULKER_BOX") }
val Material.isShulkerBox: Boolean get() = SHULKER_BOX_TYPES.contains(this)

val LEAF_TYPES = getMatchingMaterials { it.name.endsWith("_LEAVES") }
val Material.isLeaves: Boolean get() = LEAF_TYPES.contains(this)

val LOG_TYPES = getMatchingMaterials { it.name.endsWith("_LOG") }
val Material.isLog: Boolean get() = LOG_TYPES.contains(this)

val WOOD_TYPES = getMatchingMaterials { it.name.endsWith("_WOOD") }
val Material.isWood: Boolean get() = WOOD_TYPES.contains(this)

val WALL_SIGN_TYPES = getMatchingMaterials { it.name.endsWith("_WALL_SIGN") }
val Material.isWallSign: Boolean get() = WALL_SIGN_TYPES.contains(this)

val SIGN_TYPES = getMatchingMaterials { it.name.endsWith("_SIGN") }
val Material.isSign: Boolean get() = SIGN_TYPES.contains(this)

val TERRACOTTA_TYPES = getMatchingMaterials { it.name.contains("TERRACOTTA") }
val Material.isTerracotta: Boolean get() = TERRACOTTA_TYPES.contains(this)

val GLAZED_TERRACOTTA_TYPES = getMatchingMaterials { it.name.endsWith("_GLAZED_TERRACOTTA") }
val Material.isGlazedTerracotta: Boolean get() = GLAZED_TERRACOTTA_TYPES.contains(this)

val STAINED_TERRACOTTA_TYPES = getMatchingMaterials { it.name.endsWith("_TERRACOTTA") && !it.isGlazedTerracotta }
val Material.isStainedTerracotta: Boolean get() = STAINED_TERRACOTTA_TYPES.contains(this)

val NETHER_WART_TYPES = getMatchingMaterials { it.name.endsWith(("_WART_BLOCK")) }
val Material.isNetherWart: Boolean get() = NETHER_WART_TYPES.contains(this)

val CONCRETE_POWDER_TYPES = getMatchingMaterials { it.name.endsWith("_CONCRETE_POWDER") }
val Material.isConcretePowder: Boolean get() = CONCRETE_POWDER_TYPES.contains(this)

val CONCRETE_TYPES = getMatchingMaterials { it.name.endsWith("_CONCRETE") }
val Material.isConcrete: Boolean get() = CONCRETE_TYPES.contains(this)

val PLANKS_TYPES = getMatchingMaterials { it.name.endsWith("_PLANKS") }

val WOOL_TYPES = getMatchingMaterials { it.name.endsWith("_WOOL") }
val Material.isWool: Boolean get() = WOOL_TYPES.contains(this)

val CARPET_TYPES = getMatchingMaterials { it.name.endsWith("_CARPET") }
val Material.isCarpet: Boolean get() = CARPET_TYPES.contains(this)

val SLAB_TYPES = getMatchingMaterials { it.name.endsWith("_SLAB") }
val Material.isSlab: Boolean get() = SLAB_TYPES.contains(this)

val BANNER_TYPES = getMatchingMaterials { it.name.endsWith("BANNER") }

val BED_TYPES = getMatchingMaterials { it.name.endsWith("_BED") }
val Material.isBed: Boolean get() = BED_TYPES.contains(this)

val FENCE_TYPES = getMatchingMaterials { it.name.endsWith("_FENCE") }
val Material.isFence: Boolean get() = FENCE_TYPES.contains(this)

val FENCE_GATE_TYPES = getMatchingMaterials { it.name.endsWith("_FENCE_GATE") }

val WALL_TYPES = getMatchingMaterials { it.name.endsWith("_WALL") }
val Material.isWall: Boolean get() = WALL_TYPES.contains(this)

val CHISELED_TYPES = getMatchingMaterials { it.name.startsWith("CHISELED_") }

val FROGLIGHT_TYPES = getMatchingMaterials { it.name.endsWith("_FROGLIGHT") }
val Material.isFroglight: Boolean get() = FROGLIGHT_TYPES.contains(this)

val ANVIL_TYPES = getMatchingMaterials { it.name.endsWith("ANVIL") }
val Material.isAnvil: Boolean get() = ANVIL_TYPES.contains(this)

val COPPER_TYPES = getMatchingMaterials { it.name.contains("COPPER") }
val Material.isCopper: Boolean get() = COPPER_TYPES.contains(this)

val SAPLING_TYPES = getMatchingMaterials { it.name.endsWith("_SAPLING") }
val Material.isTankPassable: Boolean get() = TANK_PASSABLE_TYPES.contains(this)

// Bisected is double plants
val TANK_PASSABLE_TYPES = getMatchingMaterials { it.isAir || it.data == Bisected::class.java }

val COPPER_BLOCK_TYPES = enumSetOf(
	Material.COPPER_BLOCK,
	Material.EXPOSED_COPPER,
	Material.WEATHERED_COPPER,
	Material.OXIDIZED_COPPER,
	Material.WAXED_COPPER_BLOCK,
	Material.WAXED_EXPOSED_COPPER,
	Material.WAXED_WEATHERED_COPPER,
	Material.WAXED_OXIDIZED_COPPER
)

val Material.isCopperBlock get() = COPPER_BLOCK_TYPES.contains(this)

val COPPER_BULB_TYPES = enumSetOf(
	Material.COPPER_BULB,
	Material.EXPOSED_COPPER_BULB,
	Material.WEATHERED_COPPER_BULB,
	Material.OXIDIZED_COPPER_BULB,
	Material.WAXED_COPPER_BULB,
	Material.WAXED_EXPOSED_COPPER_BULB,
	Material.WAXED_WEATHERED_COPPER_BULB,
	Material.WAXED_OXIDIZED_COPPER_BULB
)

val Material.isCopperBulb get() = COPPER_BULB_TYPES.contains(this)

val ALL_CHISELED_COPPER_TYPES = enumSetOf(
	Material.CHISELED_COPPER,
	Material.EXPOSED_CHISELED_COPPER,
	Material.WEATHERED_CHISELED_COPPER,
	Material.OXIDIZED_CHISELED_COPPER,
	Material.WAXED_CHISELED_COPPER,
	Material.WAXED_EXPOSED_CHISELED_COPPER,
	Material.WAXED_WEATHERED_CHISELED_COPPER,
	Material.WAXED_OXIDIZED_CHISELED_COPPER
)

val WAXED_CHISELED_COPPER_TYPES = enumSetOf(
	Material.WAXED_CHISELED_COPPER,
	Material.WAXED_EXPOSED_CHISELED_COPPER,
	Material.WAXED_WEATHERED_CHISELED_COPPER,
	Material.WAXED_OXIDIZED_CHISELED_COPPER
)

val UNWAXED_CHISELED_COPPER_TYPES = enumSetOf(
	Material.CHISELED_COPPER,
	Material.EXPOSED_CHISELED_COPPER,
	Material.WEATHERED_CHISELED_COPPER,
	Material.OXIDIZED_CHISELED_COPPER,
)

val Material.isChiseledCopper get() = ALL_CHISELED_COPPER_TYPES.contains(this)
