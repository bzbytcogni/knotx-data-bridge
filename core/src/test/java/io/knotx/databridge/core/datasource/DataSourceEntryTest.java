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


import static io.knotx.databridge.core.attribute.DataSourceAttribute.ATTRIBUTE_SEPARATOR;
import static io.knotx.databridge.core.attribute.DataSourceAttribute.DATA_PARAMS_KEY_PREFIX;
import static io.knotx.databridge.core.attribute.DataSourceAttribute.DATA_SERVICE_KEY_PREFIX;

import io.knotx.databridge.core.DataBridgeKnotOptions;
import io.knotx.databridge.core.attribute.DataSourceAttribute;
import io.knotx.junit5.util.FileReader;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class DataSourceEntryTest {

  private static final String NAMESPACE = "first";

  private static final String DATA_SOURCE_NAME_KEY =
      DATA_SERVICE_KEY_PREFIX + ATTRIBUTE_SEPARATOR + NAMESPACE;
  private static final String DATA_SOURCE_PARAMS_KEY =
      DATA_PARAMS_KEY_PREFIX + ATTRIBUTE_SEPARATOR + NAMESPACE;

  private static DataBridgeKnotOptions CONFIG_WITH_DEFAULT_PARAMS;

  private static DataBridgeKnotOptions CONFIG_NO_DEFAULT_PARAMS;

  @BeforeAll
  public static void setUp() throws Exception {
    CONFIG_WITH_DEFAULT_PARAMS = new DataBridgeKnotOptions(
        new JsonObject(FileReader.readText("service-correct.json"))
    );
    CONFIG_NO_DEFAULT_PARAMS = new DataBridgeKnotOptions(
        new JsonObject(FileReader.readText("service-correct-no-params.json"))
    );
  }

  @Test
  public void mergePayload_pathFromParamsAttribute() {
    DataSourceEntry serviceEntry = new DataSourceEntry(
        DataSourceAttribute.from(DATA_SOURCE_NAME_KEY, "first-service"),
        DataSourceAttribute.from(DATA_SOURCE_PARAMS_KEY, "{\"path\":\"first-service\"}"));
    serviceEntry
        .mergeParams(CONFIG_WITH_DEFAULT_PARAMS.getDataDefinitions().iterator().next().getParams());
    Assertions.assertEquals("first-service", serviceEntry.getParams().getString("path"));
  }

  @Test
  public void mergePayload_pathFromConfigAttribute() {
    DataSourceEntry serviceEntry = new DataSourceEntry(
        DataSourceAttribute.from(DATA_SOURCE_NAME_KEY, "first-service"),
        DataSourceAttribute.from(DATA_SOURCE_PARAMS_KEY, "{}"));
    serviceEntry
        .mergeParams(CONFIG_WITH_DEFAULT_PARAMS.getDataDefinitions().iterator().next().getParams());
    Assertions.assertEquals("/service/mock/first.json", serviceEntry.getParams().getString("path"));
  }

  @Test
  public void mergePayload_nameFromParamsAttribute() {
    DataSourceEntry serviceEntry = new DataSourceEntry(
        DataSourceAttribute.from(DATA_SOURCE_NAME_KEY, "first-service"),
        DataSourceAttribute.from(DATA_SOURCE_PARAMS_KEY, "{\"name\":\"first-service-name\"}"));
    serviceEntry
        .mergeParams(CONFIG_WITH_DEFAULT_PARAMS.getDataDefinitions().iterator().next().getParams());
    Assertions.assertEquals("/service/mock/first.json", serviceEntry.getParams().getString("path"));
    Assertions.assertEquals("first-service-name", serviceEntry.getParams().getString("name"));
  }

  @Test
  public void mergePayload_whenNoDefaultParams_expectDefinedParamsUsed() {
    DataSourceEntry serviceEntry = new DataSourceEntry(
        DataSourceAttribute.from(DATA_SOURCE_NAME_KEY, "first-service"),
        DataSourceAttribute.from(DATA_SOURCE_PARAMS_KEY, "{\"path\":\"some-other-service.json\"}"));
    serviceEntry
        .mergeParams(CONFIG_NO_DEFAULT_PARAMS.getDataDefinitions().iterator().next().getParams());
    Assertions.assertEquals("some-other-service.json", serviceEntry.getParams().getString("path"));
  }
}
