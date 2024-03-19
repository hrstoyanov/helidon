/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import io.helidon.common.features.api.Aot;
import io.helidon.common.features.api.Feature;
import io.helidon.common.features.api.HelidonFlavor;

/**
 * Provides support for EclipseStore Cache features integration.
 */
@SuppressWarnings({"requires-automatic", "requires-transitive-automatic"})
@Feature(value = "Eclipse Store Cache",
        description = "Eclipse Store Cache Integration",
        in = HelidonFlavor.SE,
        path = {"EclipseStore", "Cache"}
)
@Aot(false)
module io.helidon.integrations.eclipsestore.cache {
    requires transitive cache.api;
    requires transitive io.helidon.integrations.eclipsestore;
    requires transitive org.eclipse.store.cache;
    requires transitive org.eclipse.serializer;

    exports io.helidon.integrations.eclipsestore.cache;
}