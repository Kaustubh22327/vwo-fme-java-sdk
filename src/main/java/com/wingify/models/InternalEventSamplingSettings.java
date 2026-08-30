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
package com.wingify.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

// Models for internal event sampling configuration from DaCDN settings.
public final class InternalEventSamplingSettings {

    private InternalEventSamplingSettings() {}

    // Sampling percentages per platform/runtime.
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PlatformSamplingRates {

        @JsonProperty("server")
        private Double server;

        @JsonProperty("client")
        private Double client;

        @JsonProperty("serverless")
        private Double serverless;

        public Double getServer() {
            return server;
        }

        public void setServer(Double server) {
            this.server = server;
        }

        public Double getClient() {
            return client;
        }

        public void setClient(Double client) {
            this.client = client;
        }

        public Double getServerless() {
            return serverless;
        }

        public void setServerless(Double serverless) {
            this.serverless = serverless;
        }
    }

    /**
     * Sampling rates grouped by event category.
     * Maps to the {@code sampling} object in settings:
     * {@code { usage: { server, client, serverless }, debug: { server, client, serverless } }}.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class EventCategorySamplingRates {

        @JsonProperty("usage")
        private PlatformSamplingRates usage;

        @JsonProperty("debug")
        private PlatformSamplingRates debug;

        public PlatformSamplingRates getUsage() {
            return usage;
        }

        public void setUsage(PlatformSamplingRates usage) {
            this.usage = usage;
        }

        public PlatformSamplingRates getDebug() {
            return debug;
        }

        public void setDebug(PlatformSamplingRates debug) {
            this.debug = debug;
        }
    }

    
    // Flags indicating whether sampling should always be applied per platform/runtime.
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AlwaysApplySampling {

        @JsonProperty("server")
        private Boolean server;

        @JsonProperty("client")
        private Boolean client;

        @JsonProperty("serverless")
        private Boolean serverless;

        public Boolean getServer() {
            return server;
        }

        public void setServer(Boolean server) {
            this.server = server;
        }

        public Boolean getClient() {
            return client;
        }

        public void setClient(Boolean client) {
            this.client = client;
        }

        public Boolean getServerless() {
            return serverless;
        }

        public void setServerless(Boolean serverless) {
            this.serverless = serverless;
        }
    }
}
