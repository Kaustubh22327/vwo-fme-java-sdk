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
package com.wingify.constants;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Holds debug error template keys ({@code msg_t}) that are subject to sampling.
 * Keys not in this set are treated as {@code ALWAYS_SEND} and bypass sampling.
 *
 * <p>Current set targets high-volume missing-resource errors (event/feature not found).
 * Init/validation and network/settings errors are intentionally excluded and always send.
 */
public final class SampledDebugErrorTemplateKeys {

    /** Immutable set of high-volume debug error keys eligible for sampling. */
    private static final Set<String> SAMPLED_KEYS;

    static {
        Set<String> keys = new HashSet<>();
        keys.add("EVENT_NOT_FOUND");
        keys.add("FEATURE_NOT_FOUND");
        keys.add("FEATURE_NOT_FOUND_WITH_ID");
        SAMPLED_KEYS = Collections.unmodifiableSet(keys);
    }

    private SampledDebugErrorTemplateKeys() {}

    /**
     * Returns the set of debug error template keys that require sampling.
     * @return Unmodifiable set of sampled {@code msg_t} keys
     */
    public static Set<String> getSampledKeys() {
        return SAMPLED_KEYS;
    }

    /**
     * Checks whether a debug error template key should be sampled before sending.
     * @param messageTemplateKey The {@code msg_t} value from the debug event
     * @return {@code true} if the key is in the sampled set; {@code false} otherwise
     */
    public static boolean contains(String messageTemplateKey) {
        return messageTemplateKey != null && SAMPLED_KEYS.contains(messageTemplateKey);
    }
}
