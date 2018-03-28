/*
 * Copyright (C) 2018 Knot.x Project
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
package io.knotx.databridge.adapter.http;

import io.knotx.adapter.common.http.HttpAdapterConfiguration;
import io.vertx.core.json.JsonObject;

class DataSourceAdapterConfiguration extends HttpAdapterConfiguration {

  private static final int DEFAULT_CACHE_SIZE = 100;
  private static final int DEFAULT_CACHE_TTL = 60;

  private final int cacheTtl;

  private final int cacheSize;

  DataSourceAdapterConfiguration(JsonObject config) {
    super(config);
    cacheTtl = config.getInteger("cacheTtl", DEFAULT_CACHE_TTL);
    cacheSize = config.getInteger("cacheSize", DEFAULT_CACHE_SIZE);
  }

  int getCacheTtl() {
    return cacheTtl;
  }

  int getCacheSize() {
    return cacheSize;
  }
}
