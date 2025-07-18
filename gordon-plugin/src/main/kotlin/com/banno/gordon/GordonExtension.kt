package com.banno.gordon

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

abstract class GordonExtension @Inject constructor(
    objects: ObjectFactory
) {

    val poolingStrategy: Property<PoolingStrategy> = objects.property()
    val tabletShortestWidthDp: Property<Int> = objects.property()
    val retryQuota: Property<Int> = objects.property()
    val installTimeoutMillis: Property<Long> = objects.property()
    val testTimeoutMillis: Property<Long> = objects.property()
    val testFilter: Property<String> = objects.property()
    val testInstrumentationRunner: Property<String> = objects.property()
    val ignoreProblematicDevices: Property<Boolean> = objects.property()
    val leaveApksInstalledAfterRun: Property<Boolean> = objects.property()

    init {
        poolingStrategy.convention(PoolingStrategy.PoolPerDevice)
        tabletShortestWidthDp.convention(-1)
        retryQuota.convention(0)
        installTimeoutMillis.convention(120_000)
        testTimeoutMillis.convention(120_000)
        testFilter.convention("")
        testInstrumentationRunner.convention("")
        ignoreProblematicDevices.convention(false)
        leaveApksInstalledAfterRun.convention(false)
    }
}
