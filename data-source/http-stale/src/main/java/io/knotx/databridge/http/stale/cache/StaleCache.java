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

import java.util.Optional;

public interface StaleCache<K, V extends Expirable> {

  /**
   * Returns Optional.empty() if value is not in the cache. Otherwise it returns stale value which
   * can be expired or not.
   */
  Optional<V> get(K key);

  /**
   * Updates changed value.
   */
  boolean put(K key, V value);

}
