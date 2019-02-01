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

import io.knotx.databridge.core.impl.DataBridgeFragmentContext;
import io.knotx.databridge.core.impl.FragmentProcessor;
import io.knotx.knotengine.api.AbstractKnotProxy;
import io.knotx.knotengine.api.SnippetFragment;
import io.knotx.knotengine.api.SnippetFragmentsContext;
import io.knotx.server.api.context.ClientResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.Vertx;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

public class DataBridgeKnotProxy extends AbstractKnotProxy {

  private static final Logger LOGGER = LoggerFactory.getLogger(DataBridgeKnotProxy.class);

  public static final String SUPPORTED_FRAGMENT_ID = "databridge";

  private FragmentProcessor snippetProcessor;

  public DataBridgeKnotProxy(Vertx vertx, DataBridgeKnotOptions options) {
    this.snippetProcessor = new FragmentProcessor(vertx, options);
  }

  @Override
  protected Single<SnippetFragmentsContext> processRequest(SnippetFragmentsContext knotContext) {
    return Optional.ofNullable(knotContext.getFragments())
        .map(fragments ->
            Observable.fromIterable(fragments)
                .filter(this::shouldProcess)
                .doOnNext(this::traceFragment)
                .map(DataBridgeFragmentContext::from)
                .flatMapSingle(
                    dataBridgeFragmentContext -> snippetProcessor.processSnippet(
                        dataBridgeFragmentContext, knotContext))
                .toList()
        ).orElse(Single.just(Collections.emptyList()))
        .map(result -> createSuccessResponse(knotContext))
        .onErrorReturn(error -> processError(knotContext, error));
  }

  @Override
  protected boolean shouldProcess(Set<String> knots) {
    return knots.contains(SUPPORTED_FRAGMENT_ID);
  }

  @Override
  protected SnippetFragmentsContext processError(SnippetFragmentsContext inputContext,
      Throwable error) {
    LOGGER.error("Error happened during Template processing", error);
    ClientResponse errorResponse = new ClientResponse()
        .setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());

    return new SnippetFragmentsContext(inputContext.getDelegate())
        .setClientRequest(inputContext.getClientRequest())
        .setClientResponse(errorResponse);
  }

  private SnippetFragmentsContext createSuccessResponse(SnippetFragmentsContext inputContext) {
    return new SnippetFragmentsContext(inputContext.getDelegate())
        .setClientRequest(inputContext.getClientRequest())
        .setClientResponse(inputContext.getClientResponse())
        .setFragments(
            Optional.ofNullable(inputContext.getFragments()).orElse(Collections.emptyList()))
        .setTransition(DEFAULT_TRANSITION);
  }

  private void traceFragment(SnippetFragment fragment) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Processing fragment {}", fragment.toJson().encodePrettily());
    }
  }
}
