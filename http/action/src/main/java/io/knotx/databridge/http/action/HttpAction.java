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

import static io.netty.handler.codec.http.HttpStatusClass.CLIENT_ERROR;
import static io.netty.handler.codec.http.HttpStatusClass.SERVER_ERROR;
import static io.netty.handler.codec.http.HttpStatusClass.SUCCESS;
import static java.util.stream.Collectors.joining;

import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import io.knotx.commons.http.request.AllowedHeadersFilter;
import io.knotx.commons.http.request.DataObjectsUtil;
import io.knotx.commons.http.request.MultiMapCollector;
import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.handler.api.Action;
import io.knotx.fragments.handler.api.actionlog.ActionLogger;
import io.knotx.fragments.handler.api.domain.FragmentContext;
import io.knotx.fragments.handler.api.domain.FragmentResult;
import io.knotx.fragments.handler.api.domain.payload.ActionPayload;
import io.knotx.fragments.handler.api.domain.payload.ActionRequest;
import io.knotx.server.api.context.ClientRequest;
import io.knotx.server.common.placeholders.PlaceholdersResolver;
import io.knotx.server.common.placeholders.SourceDefinitions;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Single;
import io.reactivex.exceptions.Exceptions;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.MultiMap;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.web.client.HttpRequest;
import io.vertx.reactivex.ext.web.client.HttpResponse;
import io.vertx.reactivex.ext.web.client.WebClient;

public class HttpAction implements Action {

  public static final String HTTP_ACTION_TYPE = "HTTP";
  public static final String TIMEOUT_TRANSITION = "_timeout";
  private static final Logger LOGGER = LoggerFactory.getLogger(HttpAction.class);
  private static final String METADATA_HEADERS_KEY = "headers";
  private static final String METADATA_STATUS_CODE_KEY = "statusCode";
  private static final String PLACEHOLDER_PREFIX_PAYLOAD = "payload";
  private static final String PLACEHOLDER_PREFIX_CONFIG = "config";

  private final EndpointOptions endpointOptions;
  private final WebClient webClient;
  private final String actionAlias;
  private final HttpActionOptions httpActionOptions;
  private final ActionLogger actionLogger;

  HttpAction(Vertx vertx, HttpActionOptions httpActionOptions, String actionAlias) {
    this.httpActionOptions = httpActionOptions;
    this.webClient = WebClient.create(io.vertx.reactivex.core.Vertx.newInstance(vertx),
        httpActionOptions.getWebClientOptions());
    this.endpointOptions = httpActionOptions.getEndpointOptions();
    this.actionAlias = actionAlias;
    this.actionLogger = ActionLogger.create(actionAlias, httpActionOptions.getActionLogLevel());
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

  private Single<FragmentResult> process(FragmentContext fragmentContext) {
    return Single.just(fragmentContext)
        .map(this::buildRequest)
        .flatMap(
            request -> callEndpoint(request)
                .doOnSuccess(resp -> logResponse(request, resp))
                .map(EndpointResponse::fromHttpResponse)
                .onErrorReturn(this::handleTimeout)
                .map(resp -> Pair.of(request, resp)))
        .flatMap(pair -> getFragmentResult(fragmentContext, pair.getLeft(), pair.getRight()));
  }

  private EndpointResponse handleTimeout(Throwable throwable) {
    if (throwable instanceof TimeoutException) {
      LOGGER.error("Error timeout: ", throwable);
      return new EndpointResponse(HttpResponseStatus.REQUEST_TIMEOUT);
    }
    throw Exceptions.propagate(throwable);
  }

  private Single<HttpResponse<Buffer>> callEndpoint(EndpointRequest endpointRequest) {
    HttpRequest<Buffer> request = webClient
        .request(HttpMethod.GET, endpointOptions.getPort(), endpointOptions.getDomain(),
            endpointRequest.getPath())
        .timeout(httpActionOptions.getRequestTimeoutMs());

    endpointRequest.getHeaders().entries()
        .forEach(entry -> request.putHeader(entry.getKey(), entry.getValue()));

    return request.rxSend();
  }

  private EndpointRequest buildRequest(FragmentContext context) {
    ClientRequest clientRequest = context.getClientRequest();
    SourceDefinitions sourceDefinitions = buildSourceDefinitions(context, clientRequest);
    String path = PlaceholdersResolver.resolve(endpointOptions.getPath(), sourceDefinitions);
    MultiMap requestHeaders = getRequestHeaders(clientRequest);
    return new EndpointRequest(path, requestHeaders);
  }

  private SourceDefinitions buildSourceDefinitions(FragmentContext context,
      ClientRequest clientRequest) {
    return SourceDefinitions.builder()
        .addClientRequestSource(clientRequest)
        .addJsonObjectSource(context.getFragment()
            .getPayload(), PLACEHOLDER_PREFIX_PAYLOAD)
        .addJsonObjectSource(context.getFragment()
            .getConfiguration(), PLACEHOLDER_PREFIX_CONFIG)
        .build();
  }

  private void logResponse(EndpointRequest endpointRequest, HttpResponse<Buffer> resp) {
    if (CLIENT_ERROR.contains(resp.statusCode()) || SERVER_ERROR.contains(resp.statusCode())) {
      LOGGER.error("{} {} -> Error response {}, headers[{}]",
          logResponseData(endpointRequest, resp));
    } else if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("{} {} -> Got response {}, headers[{}]",
          logResponseData(endpointRequest, resp));
    }
  }

  private Object[] logResponseData(EndpointRequest request,
      HttpResponse<Buffer> resp) {
    return new Object[]{
        HttpMethod.GET,
        toUrl(request),
        resp.statusCode(),
        DataObjectsUtil.toString(resp.headers())};
  }

  private String toUrl(EndpointRequest request) {
    return endpointOptions.getDomain() + ":" + endpointOptions.getPort() + request.getPath();
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

  private Single<FragmentResult> getFragmentResult(FragmentContext fragmentContext,
      EndpointRequest endpointRequest, EndpointResponse endpointResponse) {
    String transition = FragmentResult.ERROR_TRANSITION;
    ActionRequest request = createActionRequest(endpointRequest);
    ActionPayload payload;
    if (SUCCESS.contains(endpointResponse.getStatusCode().code())) {
      try {
        payload = handleSuccessResponse(endpointResponse, request);
        transition = FragmentResult.SUCCESS_TRANSITION;
      } catch (DecodeException e) {
        payload = handleInvalidResponseBodyFormat(request, e);
      }
    } else {
      payload = handleErrorResponse(request, endpointResponse.getStatusCode().toString(),
          endpointResponse.getStatusMessage());
      if (isTimeout(endpointResponse)) {
        transition = TIMEOUT_TRANSITION;
      }
    }
    updateResponseMetadata(endpointResponse, payload);

    logActionData(endpointRequest, endpointResponse, payload);

    Fragment fragment = fragmentContext.getFragment();
    fragment.appendPayload(actionAlias, payload.toJson());
    return Single.just(new FragmentResult(fragment, transition, actionLogger.toLog()));
  }

  private void logActionData(EndpointRequest endpointRequest, EndpointResponse endpointResponse,
      ActionPayload payload) {
    actionLogger.info("request", endpointRequest, this::toDebugJson);
    actionLogger.info("response", endpointResponse, this::toDebugJson);
    actionLogger.info("payload", payload, ActionPayload::toJson);
  }

  private JsonObject toDebugJson(EndpointResponse response) {
    return new JsonObject().put("statusCode", response.getStatusCode().code())
        .put("headers", toHeaders(response.getHeaders()));
  }

  private String toHeaders(MultiMap headers) {
    return headers
        .names()
        .stream()
        .collect(joining(","));
  }

  private JsonObject toDebugJson(EndpointRequest request) {
    return new JsonObject().put("path", request.getPath())
        .put("headers", toHeaders(request.getHeaders()));
  }

  private ActionRequest createActionRequest(EndpointRequest endpointRequest) {
    ActionRequest request = new ActionRequest(HTTP_ACTION_TYPE, endpointRequest.getPath());
    request.appendMetadata(METADATA_HEADERS_KEY, headersToJsonObject(endpointRequest.getHeaders()));
    return request;
  }

  private void updateResponseMetadata(EndpointResponse response, ActionPayload payload) {
    payload.getResponse()
        .appendMetadata(METADATA_STATUS_CODE_KEY, String.valueOf(response.getStatusCode().code()))
        .appendMetadata(METADATA_HEADERS_KEY, headersToJsonObject(response.getHeaders()));
  }

  private ActionPayload handleErrorResponse(ActionRequest request, String statusCode,
      String statusMessage) {
    return ActionPayload.error(request, statusCode, statusMessage);
  }

  private ActionPayload handleInvalidResponseBodyFormat(ActionRequest request, DecodeException e) {
    return ActionPayload.error(request, "Response body is not a valid JSON!", e.getMessage());
  }

  private ActionPayload handleSuccessResponse(EndpointResponse response, ActionRequest request) {
    return ActionPayload.success(request, bodyToJson(response.getBody().toString()));
  }

  private Object bodyToJson(String responseBody) {
    Object responseData;
    if (StringUtils.isBlank(responseBody)) {
      responseData = new JsonObject();
    } else if (responseBody.startsWith("[")) {
      responseData = new JsonArray(responseBody);
    } else {
      responseData = new JsonObject(responseBody);
    }
    return responseData;
  }

  private boolean isTimeout(EndpointResponse response) {
    return HttpResponseStatus.REQUEST_TIMEOUT == response.getStatusCode();
  }

  private JsonObject headersToJsonObject(MultiMap headers) {
    JsonObject responseHeaders = new JsonObject();
    headers.entries().forEach(entry -> {
      final JsonArray values;
      if (responseHeaders.containsKey(entry.getKey())) {
        values = responseHeaders.getJsonArray(entry.getKey());
      } else {
        values = new JsonArray();
      }
      responseHeaders.put(entry.getKey(), values.add(entry.getValue())
      );
    });
    return responseHeaders;
  }

}
