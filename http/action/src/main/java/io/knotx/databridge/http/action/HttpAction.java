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

import static io.knotx.fragments.handler.api.domain.FragmentResult.ERROR_TRANSITION;
import static io.netty.handler.codec.http.HttpStatusClass.CLIENT_ERROR;
import static io.netty.handler.codec.http.HttpStatusClass.SERVER_ERROR;
import static io.netty.handler.codec.http.HttpStatusClass.SUCCESS;

import io.knotx.commons.http.request.AllowedHeadersFilter;
import io.knotx.commons.http.request.DataObjectsUtil;
import io.knotx.commons.http.request.MultiMapCollector;
import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.handler.api.Action;
import io.knotx.fragments.handler.api.actionlog.ActionLogLevel;
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
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.eventbus.ReplyFailure;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import io.vertx.reactivex.core.MultiMap;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.web.client.HttpRequest;
import io.vertx.reactivex.ext.web.client.HttpResponse;
import io.vertx.reactivex.ext.web.client.WebClient;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

public class HttpAction implements Action {

  public static final String HTTP_ACTION_TYPE = "HTTP";
  public static final String TIMEOUT_TRANSITION = "_timeout";
  private static final Logger LOGGER = LoggerFactory.getLogger(HttpAction.class);
  private static final String METADATA_HEADERS_KEY = "headers";
  private static final String METADATA_STATUS_CODE_KEY = "statusCode";
  private static final String PLACEHOLDER_PREFIX_PAYLOAD = "payload";
  private static final String PLACEHOLDER_PREFIX_CONFIG = "config";
  private static final String JSON = "JSON";
  private static final String APPLICATION_JSON = "application/json";
  private static final String CONTENT_TYPE = "Content-Type";
  private static final String RESULT = "result";
  private static final String RESPONSE = "response";
  private static final String REQUEST = "request";
  private final boolean isJsonPredicate;
  private final boolean isForceJson;

  private final EndpointOptions endpointOptions;
  private final WebClient webClient;
  private final String actionAlias;
  private final HttpActionOptions httpActionOptions;
  private final ResponsePredicatesProvider predicatesProvider;
  private final ActionLogLevel logLevel;
  private static final ResponsePredicate IS_JSON_RESPONSE = ResponsePredicate
      .create(ResponsePredicate.JSON, result -> {
        throw new ReplyException(ReplyFailure.RECIPIENT_FAILURE, result.message());
      });

  HttpAction(Vertx vertx, HttpActionOptions httpActionOptions, String actionAlias,
      ActionLogLevel logLevel) {
    this.httpActionOptions = httpActionOptions;
    this.webClient = WebClient.create(io.vertx.reactivex.core.Vertx.newInstance(vertx),
        httpActionOptions.getWebClientOptions());
    this.endpointOptions = httpActionOptions.getEndpointOptions();
    this.actionAlias = actionAlias;
    predicatesProvider = new ResponsePredicatesProvider();
    this.isJsonPredicate = this.httpActionOptions.getResponseOptions().getPredicates()
        .contains(JSON);
    this.isForceJson = httpActionOptions.getResponseOptions().isForceJson();
    this.logLevel = logLevel;
  }

  @Override
  public void apply(FragmentContext fragmentContext,
      Handler<AsyncResult<FragmentResult>> resultHandler) {
    final ActionLogger actionLogger = ActionLogger.create(actionAlias, logLevel);
    Single<FragmentResult> result = process(fragmentContext, actionLogger);
    result.subscribe(onSuccess -> {
      Future<FragmentResult> resultFuture = Future.succeededFuture(onSuccess);
      resultFuture.setHandler(resultHandler);
    }, onError -> {
      final Future<FragmentResult> resultFuture;
      actionLogger.error(onError);
      resultFuture = Future.succeededFuture(
          new FragmentResult(fragmentContext.getFragment(), FragmentResult.ERROR_TRANSITION,
              actionLogger.toLog().toJson()));
      resultFuture.setHandler(resultHandler);
    });
  }

  private Single<FragmentResult> process(FragmentContext fragmentContext,
      ActionLogger actionLogger) {
    return Single.just(fragmentContext)
        .map((ctx) -> buildRequest(ctx, actionLogger))
        .flatMap(
            request -> callEndpoint(request)
                .doOnSuccess(resp -> logResponse(request, resp))
                .map(EndpointResponse::fromHttpResponse)
                .onErrorReturn(this::handleTimeout)
                .map(resp -> Pair.of(request, resp)))
        .flatMap(pair -> getFragmentResult(fragmentContext, pair.getLeft(), pair.getRight(),
            actionLogger));
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

    if (isJsonPredicate) {
      request.expect(io.vertx.reactivex.ext.web.client.predicate.ResponsePredicate
          .newInstance(IS_JSON_RESPONSE));
    }
    attachResponsePredicatesToRequest(request,
        httpActionOptions.getResponseOptions().getPredicates());
    endpointRequest.getHeaders().entries()
        .forEach(entry -> request.putHeader(entry.getKey(), entry.getValue()));

    return request.rxSend();
  }

  private void attachResponsePredicatesToRequest(HttpRequest<Buffer> request,
      Set<String> predicates) {
    predicates.stream()
        .filter(p -> !JSON.equals(p))
        .forEach(p -> request.expect(predicatesProvider.fromName(p)));
  }

  private EndpointRequest buildRequest(FragmentContext context, ActionLogger actionLogger) {
    ClientRequest clientRequest = context.getClientRequest();
    SourceDefinitions sourceDefinitions = buildSourceDefinitions(context, clientRequest);
    String path = PlaceholdersResolver.resolve(endpointOptions.getPath(), sourceDefinitions);
    MultiMap requestHeaders = getRequestHeaders(clientRequest);
    actionLogger.info(REQUEST, new JsonObject().put("path", path)
        .put("requestHeaders", JsonObject.mapFrom(requestHeaders)));
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
      EndpointRequest endpointRequest, EndpointResponse endpointResponse,
      ActionLogger actionLogger) {
    String transition = ERROR_TRANSITION;

    ActionRequest request = createActionRequest(endpointRequest);
    ActionPayload payload;
    if (SUCCESS.contains(endpointResponse.getStatusCode().code())) {
      actionLogger.info("rawBody", endpointResponse.getBody().toString());
      payload = handleSuccessResponse(endpointResponse, request, actionLogger);
      transition = FragmentResult.SUCCESS_TRANSITION;
    } else {
      payload = handleErrorResponse(request, endpointResponse.getStatusCode().toString(),
          endpointResponse.getStatusMessage(), actionLogger);
      if (isTimeout(endpointResponse)) {
        transition = TIMEOUT_TRANSITION;
      }
    }
    updateResponseMetadata(endpointResponse, payload);

    Fragment fragment = fragmentContext.getFragment();
    fragment.appendPayload(actionAlias, payload.toJson());
    return Single.just(new FragmentResult(fragment, transition, actionLogger.toLog().toJson()));
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
      String statusMessage, ActionLogger actionLogger) {
    ActionPayload payload = ActionPayload.error(request, statusCode, statusMessage);
    actionLogger.error(RESPONSE, payload.getResponse().toJson());
    return payload;
  }

  private ActionPayload handleSuccessResponse(EndpointResponse response, ActionRequest request,
      ActionLogger actionLogger) {
    ActionPayload payload = ActionPayload
        .success(request, bodyToJson(response.getBody().toString()));
    actionLogger.info(RESPONSE, payload.getResponse().toJson());
    Object result = payload.getResult();
    if (result instanceof JsonObject) {
      actionLogger.info(RESULT, (JsonObject) result);
    } else if (result instanceof JsonArray) {
      actionLogger.info(RESULT, (JsonArray) result);
    }
    if (isForceJson || isJsonPredicate || isContentTypeHeaderJson(response)) {
      return ActionPayload.success(request, bodyToJson(response.getBody().toString()));
    } else {
      return ActionPayload.success(request, response.getBody().toString());
    }
  }

  private boolean isContentTypeHeaderJson(EndpointResponse endpointResponse) {
    String contentType = endpointResponse.getHeaders().get(CONTENT_TYPE);
    return contentType != null && contentType.contains(APPLICATION_JSON);
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
