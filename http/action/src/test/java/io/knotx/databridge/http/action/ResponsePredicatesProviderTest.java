package io.knotx.databridge.http.action;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.vertx.reactivex.ext.web.client.predicate.ResponsePredicate;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ResponsePredicatesProviderTest {

  private static final String STATUS_200 = "SC_OK";

  private static final String STATUS_404 = "SC_NOT_FOUND";

  private static final String JSON = "JSON";

  private ResponsePredicatesProvider predicatesProvider = new ResponsePredicatesProvider();

  public static Stream<Arguments> dataResponsePredicates() {
    return Stream.of(
        Arguments.of(STATUS_200, ResponsePredicate.SC_OK),
        Arguments.of(STATUS_404, ResponsePredicate.SC_NOT_FOUND),
        Arguments.of(JSON, ResponsePredicate.JSON)
    );
  }

  @ParameterizedTest(name = "Expect valid response predicate")
  @MethodSource("dataResponsePredicates")
  void shouldReturnValidResponsePredicate(String name, ResponsePredicate expectedPredicate) {
    assertEquals(expectedPredicate, predicatesProvider.get(name));
  }
}
