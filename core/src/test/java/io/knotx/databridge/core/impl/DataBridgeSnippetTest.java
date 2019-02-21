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
package io.knotx.databridge.core.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.co.datumedge.hamcrest.json.SameJSONAs.sameJSONAs;

import io.knotx.databridge.core.datasource.DataSourceEntry;
import io.knotx.engine.api.FragmentEvent;
import io.knotx.fragment.Fragment;
import io.knotx.junit5.KnotxArgumentConverter;
import io.knotx.junit5.util.FileReader;
import io.vertx.core.json.JsonObject;
import java.io.IOException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.provider.CsvSource;

public class DataBridgeSnippetTest {

  @ParameterizedTest
  @CsvSource(value = {
      "fragment/one_service_no_params.json;{}",
      "fragment/one_service_invalid_params_bound.json;{}",
      "fragment/one_service_one_param.json;{\"path\":\"/overridden/path\"}",
      "fragment/one_service_many_params.json;{\"path\":\"/overridden/path\",\"anotherParam\":\"someValue\"}"
  }, delimiter = ';')
  public void from_whenFragmentContainsOneService_expectFragmentContextWithExtractedParams(
      String fragmentContentFile,
      String expectedParameters) throws Exception {

    FragmentEvent fragment = fromJsonFile(fragmentContentFile);
    final DataBridgeSnippet dataBridgeFragmentContext = DataBridgeSnippet.from(fragment);
    final DataSourceEntry serviceEntry = dataBridgeFragmentContext.services.get(0);
    assertThat(serviceEntry.getParams().toString(), sameJSONAs(expectedParameters));
  }

  @ParameterizedTest
  @CsvSource(value = {
      "fragment/one_service_no_params.json;1",
      "fragment/one_service_one_param.json;1",
      "fragment/one_service_many_params.json;1",
      "fragment/two_services.json;2",
      "fragment/five_services.json;5"
  }, delimiter = ';')
  public void from_whenFragmentContainsServices_expectFragmentContextWithProperNumberOfServicesExtracted(
      String fragmentContentFile,
      int numberOfExpectedServices) throws Exception {
    FragmentEvent fragment = fromJsonFile(fragmentContentFile);

    final DataBridgeSnippet dataBridgeFragmentContext = DataBridgeSnippet.from(fragment);
    assertThat(dataBridgeFragmentContext.services.size(), is(numberOfExpectedServices));
  }

  @ParameterizedTest
  @CsvSource(value = {
      "fragment/two_services_with_params.json;{\"first\":{\"first-service-key\":\"first-service-value\"},\"second\":{\"second-service-key\":\"second-service-value\"}}",
      "fragment/four_services_with_params_and_extra_param.json;{\"a\":{\"a\":\"a\"},\"b\":{\"b\":\"b\"},\"c\":{\"c\":\"c\"},\"d\":{\"d\":\"d\"}}"
  }, delimiter = ';')
  public void from_whenFragmentContainsServices_expectProperlyAssignedParams(
      String fragmentContentFile,
      @ConvertWith(KnotxArgumentConverter.class) JsonObject parameters) throws Exception {
    FragmentEvent fragment = fromJsonFile(fragmentContentFile);

    final DataBridgeSnippet dataBridgeFragmentContext = DataBridgeSnippet.from(fragment);
    dataBridgeFragmentContext.services.forEach(serviceEntry ->
        assertThat(serviceEntry.getParams().toString(),
            sameJSONAs(parameters.getJsonObject(serviceEntry.getNamespace()).toString())
        )
    );
  }

  private FragmentEvent fromJsonFile(String fragmentContentFile) throws IOException {
    final String fragment = FileReader.readText(fragmentContentFile);
    return new FragmentEvent(new Fragment(new JsonObject(fragment)), null);
  }

}
