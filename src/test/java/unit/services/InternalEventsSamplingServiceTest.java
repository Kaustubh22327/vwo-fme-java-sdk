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
package unit.services;

import com.wingify.constants.Constants;
import com.wingify.models.InternalEventSamplingSettings;
import com.wingify.models.Settings;
import com.wingify.services.InternalEventsSamplingService;
import com.wingify.utils.InternalEventsSamplingUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for internal SDK event sampling.
 *
 * <p>Covers two layers:</p>
 * <ul>
 *   <li>{@link InternalEventsSamplingUtil} — reads DaCDN sampling config and evaluates pure sampling math</li>
 *   <li>{@link InternalEventsSamplingService} — applies send/drop decisions for usage-stats and sampled debug events</li>
 * </ul>
 *
 * <p>Sampling config shape from settings:</p>
 * <pre>
 * {
 *   "sampling": {
 *     "usage": { "server": 20 },
 *     "debug": { "server": 50 }
 *   },
 *   "alwaysApplySampling": { "server": true }
 * }
 * </pre>
 *
 * <p>On the Java server SDK, only {@code server} runtime values are consumed at send time.</p>
 */
public class InternalEventsSamplingServiceTest {

    // Reusable settings with usage=20%, debug=50%, alwaysApplySampling.server=true
    private final Settings mockSamplingSettings = createMockSamplingSettings();

    /**
     * Builds a representative {@link Settings} object for sampling tests.
     * Mirrors the nested structure defined in {@link InternalEventSamplingSettings}.
     */
    private Settings createMockSamplingSettings() {
        InternalEventSamplingSettings.PlatformSamplingRates usageSampling =
            new InternalEventSamplingSettings.PlatformSamplingRates();
        usageSampling.setServer(20.0);

        InternalEventSamplingSettings.PlatformSamplingRates debugSampling =
            new InternalEventSamplingSettings.PlatformSamplingRates();
        debugSampling.setServer(50.0);

        InternalEventSamplingSettings.EventCategorySamplingRates sampling =
            new InternalEventSamplingSettings.EventCategorySamplingRates();
        sampling.setUsage(usageSampling);
        sampling.setDebug(debugSampling);

        InternalEventSamplingSettings.AlwaysApplySampling alwaysApplySampling =
            new InternalEventSamplingSettings.AlwaysApplySampling();
        // Enables usage-stats sampling on server (default SDK behavior skips it when false)
        alwaysApplySampling.setServer(true);

        Map<String, Object> sdkMetaInfo = new HashMap<>();
        sdkMetaInfo.put("wasInitializedEarlier", false);

        Settings settings = new Settings();
        settings.setSdkMetaInfo(sdkMetaInfo);
        settings.setSampling(sampling);
        settings.setAlwaysApplySampling(alwaysApplySampling);

        return settings;
    }

    // --- InternalEventsSamplingUtil: defaults and config parsing ---

    @Test
    @DisplayName("Server default sampling should be used when value is absent or invalid")
    public void serverDefaultSamplingIsUsedWhenValueMissing() {
        // Default server sampling percent is 10 when config is missing or out of range [0, 100]
        assertEqualsDouble(
            Constants.INTERNAL_EVENTS_DEFAULT_SAMPLING_PERCENT_SERVER,
            InternalEventsSamplingUtil.getDefaultSamplingPercent()
        );
        assertEqualsDouble(10, InternalEventsSamplingUtil.normalizeSamplingPercent(null));
        assertEqualsDouble(10, InternalEventsSamplingUtil.normalizeSamplingPercent(150.0));
    }

    @Test
    @DisplayName("Runtime defaults should be used when sampling values are absent")
    public void runtimeDefaultsAreUsedWhenSamplingAbsent() {
        // Empty settings should fall back to the server default for both usage and debug categories
        assertEqualsDouble(10, InternalEventsSamplingUtil.getUsageStatsSamplingPercent(new Settings()));
        assertEqualsDouble(10, InternalEventsSamplingUtil.getDebugEventSamplingPercent(new Settings()));
    }

    @Test
    @DisplayName("Usage stats sampling should be read for server runtime")
    public void usageStatsSamplingIsReadForServerRuntime() {
        assertEqualsDouble(20, InternalEventsSamplingUtil.getUsageStatsSamplingPercent(mockSamplingSettings));
    }

    @Test
    @DisplayName("alwaysApplySampling.server should be read from settings")
    public void alwaysApplySamplingServerIsReadFromSettings() {
        assertTrue(InternalEventsSamplingUtil.shouldApplySampling(mockSamplingSettings));
        // Missing alwaysApplySampling defaults to false
        assertFalse(InternalEventsSamplingUtil.shouldApplySampling(new Settings()));
    }

    // --- InternalEventsSamplingUtil: sampling math and debug key classification ---

    @Test
    @DisplayName("Sampling should pass when random value is within threshold")
    public void samplingPassesWithinThreshold() {
        // 0.1 maps to bucket 10, which is within a 20% threshold
        assertTrue(InternalEventsSamplingUtil.passesSamplingPercent(20, 0.1));
    }

    @Test
    @DisplayName("Sampling should fail when random value is above threshold")
    public void samplingFailsAboveThreshold() {
        // 0.99 maps to bucket 99, which exceeds a 20% threshold
        assertFalse(InternalEventsSamplingUtil.passesSamplingPercent(20, 0.99));
    }

    @Test
    @DisplayName("Sampled debug keys should be detected")
    public void sampledDebugKeysAreDetected() {
        // High-volume missing-resource msg_t keys that go through debug sampling before send
        assertTrue(InternalEventsSamplingUtil.isSampledDebugErrorTemplateKey("EVENT_NOT_FOUND"));
        assertTrue(InternalEventsSamplingUtil.isSampledDebugErrorTemplateKey("FEATURE_NOT_FOUND"));
    }

    @Test
    @DisplayName("Non-sampled debug keys should not be detected")
    public void nonSampledDebugKeysAreNotDetected() {
        // Keys outside the sampled set are always sent (ALWAYS_SEND)
        assertFalse(InternalEventsSamplingUtil.isSampledDebugErrorTemplateKey("INVALID_OPTIONS"));
        assertFalse(InternalEventsSamplingUtil.isSampledDebugErrorTemplateKey("EXECUTION_FAILED"));
        assertFalse(InternalEventsSamplingUtil.isSampledDebugErrorTemplateKey("NETWORK_CALL_FAILED"));
        assertFalse(InternalEventsSamplingUtil.isSampledDebugErrorTemplateKey(""));
    }

    // --- InternalEventsSamplingService: usage-stats send decisions ---

    @Test
    @DisplayName("Usage stats should always send on server when alwaysApplySampling.server is false")
    public void usageStatsAlwaysSendWhenAlwaysApplySamplingFalse() {
        // Fixed random value; sampling gate is bypassed entirely when alwaysApplySampling.server is false
        InternalEventsSamplingService samplingService = new InternalEventsSamplingService(() -> 0.0);
        InternalEventSamplingSettings.AlwaysApplySampling alwaysApplySampling =
            new InternalEventSamplingSettings.AlwaysApplySampling();
        alwaysApplySampling.setServer(false);

        InternalEventSamplingSettings.PlatformSamplingRates usageSampling =
            new InternalEventSamplingSettings.PlatformSamplingRates();
        // Even with 0% configured, event should still send because sampling is not applied
        usageSampling.setServer(0.0);

        InternalEventSamplingSettings.EventCategorySamplingRates sampling =
            new InternalEventSamplingSettings.EventCategorySamplingRates();
        sampling.setUsage(usageSampling);

        Settings settings = new Settings();
        settings.setAlwaysApplySampling(alwaysApplySampling);
        settings.setSampling(sampling);

        assertTrue(samplingService.shouldSendUsageStatsEvent(settings));
    }

    @Test
    @DisplayName("Usage stats sampling should apply on server when alwaysApplySampling.server is true")
    public void usageStatsSamplingAppliesWhenAlwaysApplySamplingTrue() {
        // random=1.0 always fails sampling; with 0% usage threshold the event is dropped
        InternalEventsSamplingService samplingService = new InternalEventsSamplingService(() -> 1.0);
        InternalEventSamplingSettings.AlwaysApplySampling alwaysApplySampling =
            new InternalEventSamplingSettings.AlwaysApplySampling();
        alwaysApplySampling.setServer(true);

        InternalEventSamplingSettings.PlatformSamplingRates usageSampling =
            new InternalEventSamplingSettings.PlatformSamplingRates();
        usageSampling.setServer(0.0);

        InternalEventSamplingSettings.EventCategorySamplingRates sampling =
            new InternalEventSamplingSettings.EventCategorySamplingRates();
        sampling.setUsage(usageSampling);

        Settings settings = new Settings();
        settings.setAlwaysApplySampling(alwaysApplySampling);
        settings.setSampling(sampling);

        assertFalse(samplingService.shouldSendUsageStatsEvent(settings));
    }

    // --- InternalEventsSamplingService: sampled debug send decisions ---

    @Test
    @DisplayName("Sampled debug events should always send when alwaysApplySampling.server is false")
    public void sampledDebugEventsAlwaysSendWhenAlwaysApplySamplingFalse() {
        InternalEventsSamplingService samplingService = new InternalEventsSamplingService(() -> 1.0);
        InternalEventSamplingSettings.AlwaysApplySampling alwaysApplySampling =
            new InternalEventSamplingSettings.AlwaysApplySampling();
        alwaysApplySampling.setServer(false);

        InternalEventSamplingSettings.PlatformSamplingRates debugSampling =
            new InternalEventSamplingSettings.PlatformSamplingRates();
        debugSampling.setServer(0.0);

        InternalEventSamplingSettings.EventCategorySamplingRates sampling =
            new InternalEventSamplingSettings.EventCategorySamplingRates();
        sampling.setDebug(debugSampling);

        Settings settings = new Settings();
        settings.setAlwaysApplySampling(alwaysApplySampling);
        settings.setSampling(sampling);

        assertTrue(samplingService.shouldSendSampledDebugEvent(settings));
    }

    @Test
    @DisplayName("Sampled debug events should apply debug sampling when alwaysApplySampling.server is true")
    public void sampledDebugEventsApplyDebugSamplingWhenAlwaysApplySamplingTrue() {
        // mockSamplingSettings has debug.server=50%, alwaysApplySampling.server=true; random=0.0 always passes
        InternalEventsSamplingService samplingService = new InternalEventsSamplingService(() -> 0.0);
        assertTrue(samplingService.shouldSendSampledDebugEvent(mockSamplingSettings));
    }

    @Test
    @DisplayName("Sampled debug events should be blocked when alwaysApplySampling is true and sampling percent is zero")
    public void sampledDebugEventsBlockedWhenAlwaysApplySamplingTrueAndSamplingZero() {
        InternalEventsSamplingService samplingService = new InternalEventsSamplingService(() -> 1.0);

        InternalEventSamplingSettings.AlwaysApplySampling alwaysApplySampling =
            new InternalEventSamplingSettings.AlwaysApplySampling();
        alwaysApplySampling.setServer(true);

        InternalEventSamplingSettings.PlatformSamplingRates debugSampling =
            new InternalEventSamplingSettings.PlatformSamplingRates();
        debugSampling.setServer(0.0);

        InternalEventSamplingSettings.EventCategorySamplingRates sampling =
            new InternalEventSamplingSettings.EventCategorySamplingRates();
        sampling.setDebug(debugSampling);

        Settings settings = new Settings();
        settings.setAlwaysApplySampling(alwaysApplySampling);
        settings.setSampling(sampling);

        assertFalse(samplingService.shouldSendSampledDebugEvent(settings));
    }

    // Compares doubles with a small tolerance to avoid floating-point assertion noise.
    private void assertEqualsDouble(double expected, double actual) {
        assertTrue(Math.abs(expected - actual) < 0.0001, "Expected " + expected + " but got " + actual);
    }
}
