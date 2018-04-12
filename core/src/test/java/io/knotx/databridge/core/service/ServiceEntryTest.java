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
package io.knotx.databridge.core.service;


import io.knotx.databridge.core.DataBridgeKnotOptions;
import io.knotx.databridge.core.attribute.DataSourceAttribute;
import io.knotx.databridge.core.attribute.DataSourceAttribute.AtributeType;
import io.knotx.junit.util.FileReader;
import io.vertx.core.json.JsonObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ServiceEntryTest {

  private static final String NAMESPACE = "first";

  private DataBridgeKnotOptions configWithDefaultParams;

  private DataBridgeKnotOptions configNoDefaultParams;

  @Before
  public void setUp() throws Exception {
    configWithDefaultParams = new DataBridgeKnotOptions(
        new JsonObject(FileReader.readText("service-correct.json"))
    );
    configNoDefaultParams = new DataBridgeKnotOptions(
        new JsonObject(FileReader.readText("service-correct-no-params.json"))
    );
  }


  @Test
  public void mergePayload_pathFromParamsAttribute() {
    ServiceEntry serviceEntry = new ServiceEntry(
        DataSourceAttribute.of(AtributeType.NAME).withNamespace(NAMESPACE)
            .withValue("first-service"),
        DataSourceAttribute.of(AtributeType.PARAMS).withNamespace(NAMESPACE)
            .withValue("{\"path\":\"first-service\"}"));
    serviceEntry
        .mergeParams(configWithDefaultParams.getServices().stream().findFirst().get().getParams());
    Assert.assertEquals("first-service", serviceEntry.getParams().getString("path"));
  }

  @Test
  public void mergePayload_pathFromConfigAttribute() {
    ServiceEntry serviceEntry = new ServiceEntry(
        DataSourceAttribute.of(AtributeType.NAME).withNamespace(NAMESPACE)
            .withValue("first-service"),
        DataSourceAttribute.of(AtributeType.PARAMS).withNamespace(NAMESPACE).withValue("{}"));
    serviceEntry
        .mergeParams(configWithDefaultParams.getServices().stream().findFirst().get().getParams());
    Assert.assertEquals("/service/mock/first.json", serviceEntry.getParams().getString("path"));
  }

  @Test
  public void mergePayload_nameFromParamsAttribute() {
    ServiceEntry serviceEntry = new ServiceEntry(
        DataSourceAttribute.of(AtributeType.NAME).withNamespace(NAMESPACE)
            .withValue("first-service"),
        DataSourceAttribute.of(AtributeType.PARAMS).withNamespace(NAMESPACE)
            .withValue("{\"name\":\"first-service-name\"}"));
    serviceEntry
        .mergeParams(configWithDefaultParams.getServices().stream().findFirst().get().getParams());
    Assert.assertEquals("/service/mock/first.json", serviceEntry.getParams().getString("path"));
    Assert.assertEquals("first-service-name", serviceEntry.getParams().getString("name"));
  }

  @Test
  public void mergePayload_whenNoDefaultParams_expectDefinedParamsUsed() throws Exception {
    ServiceEntry serviceEntry = new ServiceEntry(
        DataSourceAttribute.of(AtributeType.NAME).withNamespace(NAMESPACE)
            .withValue("first-service"),
        DataSourceAttribute.of(AtributeType.PARAMS).withNamespace(NAMESPACE)
            .withValue("{\"path\":\"some-other-service.json\"}"));
    serviceEntry
        .mergeParams(configNoDefaultParams.getServices().stream().findFirst().get().getParams());
    Assert.assertEquals("some-other-service.json", serviceEntry.getParams().getString("path"));
  }
}
