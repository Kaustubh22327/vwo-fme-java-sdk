/**
 * Copyright 2024-2026 Wingify Software Pvt. Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wingify.services;

import com.wingify.models.Settings;
import com.wingify.utils.InternalEventsSamplingUtil;

import java.util.function.Supplier;

/**
 * Applies sampling rules for internal SDK events:
 * {@code vwo_sdkUsageStats} and sampled {@code vwo_sdkDebug}.
 */
public class InternalEventsSamplingService {

    /** Provides random values in [0, 1) for sampling checks; overridable in tests. */
    private final Supplier<Double> randomValueProvider;

    /**
     * Creates a sampling service using {@link Math#random()} for sampling checks.
     */
    public InternalEventsSamplingService() {
        this(Math::random);
    }

    /**
     * Creates a sampling service with a custom random value provider (used in tests).
     * @param randomValueProvider Supplier returning a value in [0, 1)
     */
    public InternalEventsSamplingService(Supplier<Double> randomValueProvider) {
        this.randomValueProvider = randomValueProvider;
    }

    /**
     * Determines whether the usage-stats event should be sent.
     * On server, always sends unless {@code alwaysApplySampling.server} is enabled.
     * @param settings Parsed settings from the server
     * @return {@code true} when the usage-stats event should be sent
     */
    public boolean shouldSendUsageStatsEvent(Settings settings) {
        if (!InternalEventsSamplingUtil.shouldApplySampling(settings)) {
            return true;
        }

        double usageStatsSamplingPercent = InternalEventsSamplingUtil.getUsageStatsSamplingPercent(settings);
        return InternalEventsSamplingUtil.passesSamplingPercent(usageStatsSamplingPercent, randomValueProvider.get());
    }

    /**
     * Determines whether a sampled debug event should be sent.
     * Only called for {@code msg_t} keys in the sampled set; always-send keys bypass this.
     * When {@code alwaysApplySampling.server} is false, sampled debug events are always sent.
     * @param settings Parsed settings from the server
     * @return {@code true} when the sampled debug event should be sent
     */
    public boolean shouldSendSampledDebugEvent(Settings settings) {
        if (!InternalEventsSamplingUtil.shouldApplySampling(settings)) {
            return true;
        }

        double debugSamplingPercent = InternalEventsSamplingUtil.getDebugEventSamplingPercent(settings);
        return InternalEventsSamplingUtil.passesSamplingPercent(debugSamplingPercent, randomValueProvider.get());
    }
}
