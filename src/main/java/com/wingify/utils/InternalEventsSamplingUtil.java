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
package com.wingify.utils;

import com.wingify.constants.Constants;
import com.wingify.constants.SampledDebugErrorTemplateKeys;
import com.wingify.models.InternalEventSamplingSettings;
import com.wingify.models.Settings;

/**
 * Utility methods for reading internal event sampling configuration from settings
 * and evaluating sampling decisions at runtime (server-side Java SDK).
 */
public final class InternalEventsSamplingUtil {

    private InternalEventsSamplingUtil() {}

    /**
     * Returns the default server sampling percentage when settings omit the value.
     * @return Default sampling percentage (10%)
     */
    public static double getDefaultSamplingPercent() {
        return Constants.INTERNAL_EVENTS_DEFAULT_SAMPLING_PERCENT_SERVER;
    }

    /**
     * Normalizes a sampling value to a valid percentage in the range [0, 100].
     * Falls back to the server default when the value is missing or invalid.
     * @param samplingValue Raw value from settings
     * @return Valid sampling percentage between 0 and 100
     */
    public static double normalizeSamplingPercent(Double samplingValue) {
        if (samplingValue != null && samplingValue >= 0 && samplingValue <= 100) {
            return samplingValue;
        }

        return getDefaultSamplingPercent();
    }

    /**
     * Reads the server sampling percentage from a runtime sampling config object.
     * @param runtimeSamplingConfig Nested config (e.g. {@code sampling.usage})
     * @return Sampling percentage for the server runtime
     */
    public static double getRuntimeSamplingPercent(
        InternalEventSamplingSettings.PlatformSamplingRates runtimeSamplingConfig
    ) {
        if (runtimeSamplingConfig == null) {
            return getDefaultSamplingPercent();
        }

        return normalizeSamplingPercent(runtimeSamplingConfig.getServer());
    }

    /**
     * Reads the usage-stats sampling percentage for the server runtime from settings.
     * @param settings Parsed settings from the server
     * @return Configured usage-stats sampling percentage (0–100)
     */
    public static double getUsageStatsSamplingPercent(Settings settings) {
        // get sampling from settings
        InternalEventSamplingSettings.EventCategorySamplingRates sampling =
            settings != null ? settings.getSampling() : null;
        InternalEventSamplingSettings.PlatformSamplingRates usageSampling =
            sampling != null ? sampling.getUsage() : null;
        return getRuntimeSamplingPercent(usageSampling);
    }

    /**
     * Reads the debug-event sampling percentage for the server runtime from settings.
     * @param settings Parsed settings from the server
     * @return Configured debug sampling percentage (0–100)
     */
    public static double getDebugEventSamplingPercent(Settings settings) {
        InternalEventSamplingSettings.EventCategorySamplingRates sampling =
            settings != null ? settings.getSampling() : null;
        InternalEventSamplingSettings.PlatformSamplingRates debugSampling =
            sampling != null ? sampling.getDebug() : null;
        return getRuntimeSamplingPercent(debugSampling);
    }

    /**
     * Determines whether sampling should be evaluated before send.
     * Controlled by {@code alwaysApplySampling.server}; defaults to {@code false}.
     * When {@code false}, usage-stats and sampled debug events are always sent.
     * @param settings Parsed settings from the server
     * @return {@code true} when a sampling check is required before sending
     */
    public static boolean shouldApplySampling(Settings settings) {
        if (settings == null) {
            return Constants.INTERNAL_EVENTS_DEFAULT_ALWAYS_APPLY_SAMPLING;
        }

        InternalEventSamplingSettings.AlwaysApplySampling alwaysApplySampling =
            settings.getAlwaysApplySampling();
        if (alwaysApplySampling == null || alwaysApplySampling.getServer() == null) {
            return Constants.INTERNAL_EVENTS_DEFAULT_ALWAYS_APPLY_SAMPLING;
        }

        return alwaysApplySampling.getServer();
    }

    /**
     * Evaluates whether an event qualifies under the configured sampling percentage.
     * @param samplingPercent Configured sampling percentage (0–100)
     * @param randomValue Random value in the range [0, 1)
     * @return {@code true} when the random draw falls within the sampling threshold
     */
    public static boolean passesSamplingPercent(double samplingPercent, double randomValue) {
        // Map random [0,1) to integer percent bucket [0,100]
        int normalizedRandomPercent = (int) Math.floor(randomValue * 101);
        return normalizedRandomPercent <= samplingPercent;
    }

    /**
     * Checks whether a debug error template key is in the sampled set.
     * @param messageTemplateKey The {@code msg_t} value from the debug event
     * @return {@code true} when the key requires sampling before send
     */
    public static boolean isSampledDebugErrorTemplateKey(String messageTemplateKey) {
        return SampledDebugErrorTemplateKeys.contains(messageTemplateKey);
    }
}
