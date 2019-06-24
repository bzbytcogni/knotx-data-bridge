# HTTP Action
HTTP Action is an [Action](https://github.com/Knotx/knotx-fragments/tree/master/handler/api#action) 
that connects to the external WEB endpoint that responds with JSON and saves the response into the 
[Fragment's](https://github.com/Knotx/knotx-fragments/tree/master/api#knotx-fragment-api) `payload`.

## How does it work
HTTP Action uses HTTP Client to connect to the external WEB endpoint. It expects HTTP response with
JSON body. To configure endpoint you will have to provide `domain` and `port` and additionally
a request `path` to the WEB endpoint. The `path` parameter could contain placeholders that will be
resolved with the logic described above.
After the result from the external WEB endpoint is obtained, it is merged with processed
[Fragment's](https://github.com/Knotx/knotx-fragments/tree/master/api#knotx-fragment-api) `payload`
and returned in the [`FragmentResult`](https://github.com/Knotx/knotx-fragments/blob/master/handler/api/docs/asciidoc/dataobjects.adoc#FragmentResult)
together with Transition.

#### Parametrized services calls
Http Action supports request parameters resolving for the `path` parameter which defines the final request URI.
Read more about placeholders in the [Knot.x Server Common Placeholders](https://github.com/Knotx/knotx-server-http/tree/master/common/placeholders#available-request-placeholders-support).

## How to use
Define HTTP Action using `http` factory and providing config `endpointOptions` in the Fragment's Handler
`actions` section.

```hocon
actions {
  book {
    factory = http
    config {
      endpointOptions {
        path = /service/mock/book.json
        domain = localhost
        port = 3000
        allowedRequestHeaders = ["Content-Type"]
      }
    }
  }
}
```

### Detailed configuration
All configuration options are explained in details in the [Config Options Cheetsheet](https://github.com/Knotx/knotx-data-bridge/tree/master/http/action/docs/asciidoc/dataobjects.adoc).