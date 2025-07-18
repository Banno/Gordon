/*
 * Copyright (C) 2019 - 2025 Jack Henry & Associates, Inc.
 * Copyright (C) 2025 Bayerische Motorenwerke AG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
