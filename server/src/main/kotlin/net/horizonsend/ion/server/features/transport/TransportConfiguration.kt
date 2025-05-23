package net.horizonsend.ion.server.features.transport

import kotlinx.serialization.Serializable
import net.horizonsend.ion.server.configuration.util.DurationConfig
import java.util.concurrent.TimeUnit

@Serializable
data class TransportConfiguration(
	val transportThreadCount: Int = 16,
	val taskTimeout: DurationConfig = DurationConfig(TimeUnit.SECONDS, 1),
	val extractorConfiguration: ExtractorConfiguration = ExtractorConfiguration(),
	val generalConfiguration: GeneralTransportConfiguration = GeneralTransportConfiguration(),
	val powerConfiguration: PowerTransportConfiguration = PowerTransportConfiguration(),
) {
	@Serializable
	data class ExtractorConfiguration(
		val extractorTickIntervalMS: Long = 1000
	)

	@Serializable
	data class GeneralTransportConfiguration(
		val maxPathfindDepth: Int = 2000
	)

	@Serializable
	data class PowerTransportConfiguration(
		val powerTransferRate: Int = 1000,
		val solarPanelTickPower: Int = 5,
		val solarPanelTickInterval: Int = 10,
	)
}
