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
package io.knotx.databridge.http;

import io.knotx.databridge.api.DataSourceAdapterRequest;
import io.knotx.databridge.api.DataSourceAdapterResponse;
import io.knotx.databridge.api.reactivex.AbstractDataSourceAdapterProxy;
import io.knotx.databridge.http.common.configuration.HttpDataSourceAdapterOptions;
import io.knotx.databridge.http.common.http.HttpClientFacade;
import io.reactivex.Single;
import io.vertx.core.http.HttpMethod;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.client.WebClient;

public class HttpDataSourceAdapterProxy extends AbstractDataSourceAdapterProxy {

  private HttpClientFacade httpClientFacade;

  public HttpDataSourceAdapterProxy(Vertx vertx, HttpDataSourceAdapterOptions configuration) {
    this.httpClientFacade = new HttpClientFacade(
        WebClient.create(vertx, configuration.getClientOptions()), configuration);
  }

  @Override
  protected Single<DataSourceAdapterResponse> processRequest(DataSourceAdapterRequest message) {
    return httpClientFacade.process(message, HttpMethod.GET)
        .map(new DataSourceAdapterResponse()::setResponse);
  }

}
