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
package io.knotx.databridge.core;

import com.google.common.base.Objects;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

/**
 * Describes a physical details of HTTP service endpoint the ServiceAdapter will use.
 */
@DataObject(generateConverter = true, publicConverter = false)
public class DataSourceDefinition {

  private String name;
  private String adapter;
  private String cacheKey;
  private JsonObject params;

  /**
   * Default constructor
   */
  public DataSourceDefinition() {
    init();
  }

  /**
   * Copy constructor
   *
   * @param other the instance to copy
   */
  public DataSourceDefinition(DataSourceDefinition other) {
    this.name = other.name;
    this.adapter = other.adapter;
    this.cacheKey = other.cacheKey;
    this.params = other.params.copy();
  }

  /**
   * Create an settings from JSON
   *
   * @param json the JSON
   */
  public DataSourceDefinition(JsonObject json) {
    init();
    DataSourceDefinitionConverter.fromJson(json, this);
  }

  /**
   * Convert to JSON
   *
   * @return the JSON
   */
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    DataSourceDefinitionConverter.toJson(this, json);
    return json;
  }

  private void init() {

  }


  /**
   * @return a name of the service
   */
  public String getName() {
    return name;
  }

  /**
   * Set the name of the service the will be used on html snippet level.
   *
   * @param name name of the service
   * @return a reference to this, so the API can be used fluently
   */
  public DataSourceDefinition setName(String name) {
    this.name = name;
    return this;
  }

  /**
   * @return EB adapter of the service adapter
   */
  public String getAdapter() {
    return adapter;
  }

  /**
   * Sets the EB adapter of the service adapter
   *
   * @param adapter EB adapter of the service adapter
   * @return a reference to this, so the API can be used fluently
   */
  public DataSourceDefinition setAdapter(String adapter) {
    this.adapter = adapter;
    return this;
  }

  /**
   * @return the cache key value
   */
  public String getCacheKey() {
    return cacheKey;
  }

  /**
   * Set the cache key
   *
   * @param cacheKey cache key
   * @return a reference to this, so the API can be used fluently
   */
  public DataSourceDefinition setCacheKey(String cacheKey) {
    this.cacheKey = cacheKey;
    return this;
  }

  /**
   * @return a JsonObject with service parameters
   */
  public JsonObject getParams() {
    return params;
  }

  /**
   * Set the service parameters to be consumed by the adapter.
   *
   * @param params JsonObject with the additional parameters
   * @return a reference to this, so the API can be used fluently
   */
  public DataSourceDefinition setParams(JsonObject params) {
    this.params = params;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DataSourceDefinition that = (DataSourceDefinition) o;
    return Objects.equal(name, that.name) &&
        Objects.equal(adapter, that.adapter) &&
        Objects.equal(cacheKey, that.cacheKey) &&
        Objects.equal(params, that.params);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(name, adapter, cacheKey, params);
  }
}
