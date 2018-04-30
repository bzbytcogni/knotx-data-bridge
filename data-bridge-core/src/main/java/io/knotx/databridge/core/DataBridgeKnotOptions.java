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

import com.google.common.collect.Lists;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.List;

/**
 * Describes Service Knot configuration
 */
@DataObject(generateConverter = true, publicConverter = false)
public class DataBridgeKnotOptions {

  /**
   * Default EB address of the verticle
   */
  public final static String DEFAULT_ADDRESS = "knotx.core.service";

  public final static List<DataSourceMetadata> DEFAULT_SERVICES_MOCK = Lists.newArrayList(
      new DataSourceMetadata()
          .setAddress("mock-service-adapter")
          .setName("mock")
          .setParams(new JsonObject().put("path", "/service/mock/.*"))
  );

  private String address;
  private List<DataSourceMetadata> services;
  private DeliveryOptions deliveryOptions;

  /**
   * Default constructor
   */
  public DataBridgeKnotOptions() {
    init();
  }

  /**
   * Copy constructor
   *
   * @param other the instance to copy
   */
  public DataBridgeKnotOptions(DataBridgeKnotOptions other) {
    this.address = other.address;
    this.services = new ArrayList<>(other.services);
    this.deliveryOptions = new DeliveryOptions(other.deliveryOptions);
  }

  /**
   * Create an settings from JSON
   *
   * @param json the JSON
   */
  public DataBridgeKnotOptions(JsonObject json) {
    init();
    DataBridgeKnotOptionsConverter.fromJson(json, this);
  }

  /**
   * Convert to JSON
   *
   * @return the JSON
   */
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    DataBridgeKnotOptionsConverter.toJson(this, json);
    return json;
  }

  private void init() {
    address = DEFAULT_ADDRESS;
    services = DEFAULT_SERVICES_MOCK;
    deliveryOptions = new DeliveryOptions();
  }

  /**
   * @return EB address
   */
  public String getAddress() {
    return address;
  }

  /**
   * Sets the EB address of the verticle. Default is 'knotx.core.service'
   *
   * @param address EB address of the verticle
   * @return a reference to this, so the API can be used fluently
   */
  public DataBridgeKnotOptions setAddress(String address) {
    this.address = address;
    return this;
  }

  /**
   * @return list of {@link DataSourceMetadata}
   */
  public List<DataSourceMetadata> getServices() {
    return services;
  }

  /**
   * Sets the mapping between service aliases and service adapters that will serve the data.
   *
   * @param services list of {@link DataSourceMetadata} objects representing service
   * @return a reference to this, so the API can be used fluently
   */
  public DataBridgeKnotOptions setServices(List<DataSourceMetadata> services) {
    this.services = services;
    return this;
  }

  /**
   * @return EB {@link DeliveryOptions}
   */
  public DeliveryOptions getDeliveryOptions() {
    return deliveryOptions;
  }

  /**
   * Sets the Vert.x EventBusDeliveryOptions for a given verticle
   *
   * @param deliveryOptions EB {@link DeliveryOptions}
   * @return a reference to this, so the API can be used fluently
   */
  public DataBridgeKnotOptions setDeliveryOptions(
      DeliveryOptions deliveryOptions) {
    this.deliveryOptions = deliveryOptions;
    return this;
  }
}
