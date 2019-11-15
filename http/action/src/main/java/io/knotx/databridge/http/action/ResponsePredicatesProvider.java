package io.knotx.databridge.http.action;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.lang.reflect.Field;

public class ResponsePredicatesProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(ResponsePredicatesProvider.class);

  public ResponsePredicatesProvider() {
  }

  public io.vertx.reactivex.ext.web.client.predicate.ResponsePredicate get(
      String predicateName) {
    Class predicateClass = io.vertx.reactivex.ext.web.client.predicate.ResponsePredicate.class;
    io.vertx.reactivex.ext.web.client.predicate.ResponsePredicate responsePredicate = null;
    try {
      Field predicateField = predicateClass.getField(predicateName);
      responsePredicate = (io.vertx.reactivex.ext.web.client.predicate.ResponsePredicate) predicateField
          .get(this);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      LOGGER.error("Cannot access ResponsePredicate identified by: {}", predicateName);
    }
    return responsePredicate;
  }
}
