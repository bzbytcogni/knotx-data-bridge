/*
 * Copyright (C) 2019 Knot.x Project
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
package io.knotx.databridge.http.action;

import io.knotx.databridge.http.action.common.configuration.EndpointOptions;
import io.knotx.databridge.http.action.common.placeholders.UriTransformer;
import io.knotx.fragment.Fragment;
import io.knotx.fragments.handler.api.Action;
import io.knotx.fragments.handler.api.domain.FragmentContext;
import io.knotx.fragments.handler.api.domain.FragmentResult;
import io.knotx.fragments.handler.api.domain.payload.ActionPayload;
import io.knotx.fragments.handler.api.domain.payload.ActionRequest;
import io.knotx.server.api.context.ClientRequest;
import io.knotx.server.util.AllowedHeadersFilter;
import io.knotx.server.util.DataObjectsUtil;
import io.knotx.server.util.MultiMapCollector;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpStatusClass;
import io.reactivex.Single;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.MultiMap;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.web.client.HttpRequest;
import io.vertx.reactivex.ext.web.client.HttpResponse;
import io.vertx.reactivex.ext.web.client.WebClient;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

public class HttpAction implements Action {

  private static final Logger LOGGER = LoggerFactory.getLogger(HttpAction.class);
  public static final String HTTP_ACTION_TYPE = "HTTP";

  private final EndpointOptions endpointOptions;
  private final WebClient webClient;
  private final String actionAlias;

  HttpAction(Vertx vertx, HttpActionOptions httpActionOptions, String actionAlias) {
    this.webClient = WebClient.create(io.vertx.reactivex.core.Vertx.newInstance(vertx),
        httpActionOptions.getWebClientOptions());
    this.endpointOptions = httpActionOptions.getEndpointOptions();
    this.actionAlias = actionAlias;
  }

  @Override
  public void apply(FragmentContext fragmentContext,
      Handler<AsyncResult<FragmentResult>> resultHandler) {
    Single<FragmentResult> result = process(fragmentContext);
    result.subscribe(onSuccess -> {
      Future<FragmentResult> resultFuture = Future.succeededFuture(onSuccess);
      resultFuture.setHandler(resultHandler);
    }, onError -> {
      Future<FragmentResult> resultFuture = Future.failedFuture(onError);
      resultFuture.setHandler(resultHandler);
    });
  }

  public Single<FragmentResult> process(FragmentContext fragmentContext) {
    return Single.just(fragmentContext.getClientRequest())
        .map(this::buildRequest)
        .flatMap(
            request -> callEndpoint(request)
                .doOnSuccess(resp -> logResponse(request, resp)))
        .flatMap(response -> wrapResponse(fragmentContext, response));
  }

  private Single<HttpResponse<Buffer>> callEndpoint(EndpointRequest endpointRequest) {
    HttpRequest<Buffer> request = webClient
        .request(HttpMethod.GET, endpointOptions.getPort(), endpointOptions.getDomain(),
            endpointRequest.getPath());

    endpointRequest.getHeaders().entries()
        .forEach(entry -> request.putHeader(entry.getKey(), entry.getValue()));

    return request.rxSend();
  }

  private EndpointRequest buildRequest(ClientRequest clientRequest) {
    String path = UriTransformer.resolveServicePath(endpointOptions.getPath(), clientRequest);
    MultiMap requestHeaders = getRequestHeaders(clientRequest);
    return new EndpointRequest(path, requestHeaders);
  }


  private void logResponse(EndpointRequest endpointRequest, HttpResponse<Buffer> resp) {
    // TODO use util here
    if (resp.statusCode() >= 400 && resp.statusCode() < 600) {
      LOGGER.error("{} {} -> Got response {}, headers[{}]",
          logResponseData(endpointRequest, resp));
    } else if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("{} {} -> Got response {}, headers[{}]",
          logResponseData(endpointRequest, resp));
    }
  }

  private Object[] logResponseData(EndpointRequest request,
      HttpResponse<Buffer> resp) {
    Object[] data = {
        HttpMethod.GET,
        toUrl(request),
        resp.statusCode(),
        DataObjectsUtil.toString(resp.headers())};

    return data;
  }

  private String toUrl(EndpointRequest request) {
    return new StringBuilder(endpointOptions.getDomain()).append(":")
        .append(endpointOptions.getPort())
        .append(request.getPath()).toString();
  }

  private MultiMap getRequestHeaders(ClientRequest clientRequest) {
    MultiMap filteredHeaders = getFilteredHeaders(clientRequest.getHeaders(),
        endpointOptions.getAllowedRequestHeadersPatterns());
    if (endpointOptions.getAdditionalHeaders() != null) {
      endpointOptions.getAdditionalHeaders()
          .forEach(entry -> filteredHeaders.add(entry.getKey(), entry.getValue().toString()));
    }
    return filteredHeaders;
  }

  private MultiMap getFilteredHeaders(MultiMap headers, List<Pattern> allowedHeaders) {
    return headers.names().stream()
        .filter(AllowedHeadersFilter.create(allowedHeaders))
        .collect(MultiMapCollector.toMultiMap(o -> o, headers::getAll));
  }

  private Single<FragmentResult> wrapResponse(FragmentContext fragmentContext,
      HttpResponse<Buffer> response) {
    return toBody(response)
        .doOnSuccess(this::traceServiceCall)
        .map(buffer -> {
          // TODO handle error responses better
          Fragment fragment = fragmentContext.getFragment();
          appendResponseToPayload(fragment, response, buffer.toString());
          final String transition;
          if (HttpStatusClass.SUCCESS != HttpStatusClass.valueOf(response.statusCode())) {
            transition = FragmentResult.ERROR_TRANSITION;
          } else {
            transition = FragmentResult.SUCCESS_TRANSITION;
          }

          return new FragmentResult(fragment, transition);
        });
  }

  private void appendResponseToPayload(Fragment fragment, HttpResponse<Buffer> response,
      String responseBody) {

    ActionRequest request = new ActionRequest(HTTP_ACTION_TYPE,
        response.getDelegate().getHeader("Location"));

    ActionPayload payload;
    if (isSuccess(response)) {
      Object responseData;
      if (StringUtils.isBlank(responseBody)) {
        responseData = new JsonObject();
      } else if (responseBody.startsWith("[")) {
        responseData = new JsonArray(responseBody);
      } else {
        responseData = new JsonObject(responseBody);
      }
      payload = ActionPayload.success(request, responseData);
    } else {

      payload = ActionPayload.error(request,
          HttpResponseStatus.valueOf(response.statusCode()).toString(), response.statusMessage());
    }
    fragment.appendPayload(actionAlias, payload.toJson());

  }

  private boolean isSuccess(HttpResponse<Buffer> response) {
    return response.statusCode() == HttpResponseStatus.OK.code();
  }

  private Single<Buffer> toBody(HttpResponse<Buffer> response) {
    if (response.body() != null) {
      return Single.just(response.body());
    } else {
      LOGGER.warn("Service returned empty body");
      return Single.just(Buffer.buffer());
    }
  }

  private void traceServiceCall(Buffer results) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Service call returned <{}>", results.toString());
    }
  }

}
