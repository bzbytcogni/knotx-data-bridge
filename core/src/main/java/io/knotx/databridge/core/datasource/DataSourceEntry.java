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
package io.knotx.databridge.core.datasource;


import io.knotx.databridge.core.attribute.DataSourceAttribute;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;

import com.google.common.base.MoreObjects;

import io.vertx.core.json.JsonObject;

public class DataSourceEntry {

  private String namespace;
  private String name;
  private String address;
  private String cacheKey;
  private JsonObject params;

  public DataSourceEntry(DataSourceEntry serviceEntry) {
    this.namespace = serviceEntry.namespace;
    this.name = serviceEntry.name;
    this.address = serviceEntry.address;
    this.cacheKey = serviceEntry.cacheKey;
    this.params = serviceEntry.params.copy();
  }

  public DataSourceEntry(DataSourceAttribute serviceAttribute, DataSourceAttribute paramsAttribute) {
    this.namespace = serviceAttribute.getNamespace();
    this.name = serviceAttribute.getValue();
    this.params = getParams(paramsAttribute);
    this.cacheKey = String.format("%s|%s", getName(), getParams());
  }

  public DataSourceEntry mergeParams(JsonObject defaultParams) {
    if (defaultParams != null) {
      this.params = defaultParams.copy().mergeIn(this.params);
    }
    return this;
  }

  public String getNamespace() {
    return namespace;
  }

  public String getName() {
    return name;
  }

  public String getAddress() {
    return address;
  }

  DataSourceEntry setAddress(String address) {
    this.address = address;
    return this;
  }

  public String getCacheKey() {
    return cacheKey;
  }

  public DataSourceEntry setCacheKey(String newCacheKey) {
    if (StringUtils.isNotEmpty(newCacheKey)) {
      this.cacheKey = newCacheKey;
    }
    return this;
  }

  public JsonObject getParams() {
    return params;
  }

  public JsonObject getResultWithNamespaceAsKey(JsonObject result) {
    if (StringUtils.isNotEmpty(namespace)) {
      return new JsonObject().put(namespace, result);
    } else {
      return result;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof DataSourceEntry) {
      final DataSourceEntry other = (DataSourceEntry) o;
      return new EqualsBuilder()
          .append(namespace, other.getNamespace())
          .append(name, other.getName())
          .append(cacheKey, other.getCacheKey())
          .append(params, other.getParams())
          .isEquals();
    } else {
      return false;
    }

  }

  @Override
  public int hashCode() {
    return Objects.hash(namespace, name, cacheKey, params);
  }

  private JsonObject getParams(DataSourceAttribute paramsAttribute) {
    final JsonObject result;
    if (paramsAttribute == null || StringUtils.isEmpty(paramsAttribute.getValue())) {
      result = new JsonObject();
    } else {
      result = new JsonObject(paramsAttribute.getValue());
    }
    return result;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("namespace", namespace)
        .add("name", name)
        .add("address", address)
        .add("cacheKey", cacheKey)
        .add("params", params)
        .toString();
  }
}
