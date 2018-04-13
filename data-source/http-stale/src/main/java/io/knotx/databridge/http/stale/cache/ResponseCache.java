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
package io.knotx.databridge.http.stale.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.Optional;

public final class ResponseCache implements StaleCache<String, CacheableAdapterResponse> {

  private final Cache<String, CacheableAdapterResponse> cache;

  private final long expiryInterval;

  public ResponseCache(int cacheSize, int expiryIntervalInSeconds) {
    cache = CacheBuilder.newBuilder().maximumSize(cacheSize).build();
    expiryInterval = expiryIntervalInSeconds * 1000L;
  }

  public Optional<CacheableAdapterResponse> get(String cacheKey) {
    return Optional.ofNullable(cache.getIfPresent(cacheKey));
  }

  private long getExpiryTime() {
    return System.currentTimeMillis() + expiryInterval;
  }

  @Override
  public boolean put(String key, CacheableAdapterResponse value) {
    value.setTimeToExpire(getExpiryTime());
    cache.put(key, value);
    return true;
  }
}
