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
Http Action supports request parameters, `@payload` and `@configuration` resolving for the `path` parameter which defines the final request URI.
Read more about placeholders in the [Knot.x Server Common Placeholders](https://github.com/Knotx/knotx-server-http/tree/master/common/placeholders#available-request-placeholders-support).

The `@payload` and `@configuration` are values stored in [Fragment](https://github.com/Knotx/knotx-fragments/tree/master/api#knotx-fragment-api).
For this structures use corresponding prefixes: `payload` and `config` 

## How to use
Define HTTP Action using `http` factory and providing configs `endpointOptions` and `responseOptions` in the Fragment's Handler
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
      responseOptions {
        predicates = [JSON]
        forceJson = false
      }       
    }
  }
}
```

- `endpointOptions` config describes quite basic request parameters as `path`, `domain`, `port` and `allowedRequestHeaders` which
are necessary to perform http request 
- `responseOptions` config is responsible for handling incoming request properly. Here we can specify `predicates` - it's array
containing Vert.x response predicates, to get more familiar with it, please visit [this page](https://vertx.io/blog/http-response-validation-with-the-vert-x-web-client/).
Providing `JSON` predicate causes `Content-Type` check - when `Content-Type` won't be equal to `application/json` we'll get
error transition. We can also specify `forceJson` param. When `Content-Type` won't be equal to `application/json` and `forceJson`
is true, response will be processed as json. If it won't be json, request ends with error transition.

Table below shows default behaviour of HttpAction depending on provided `responseOptions` config and response:

| Content-Type     | forceJSON | JSON predicate | Body | Transition | Response |
| ---------------- |:---------:| --------------:|  ---:|  ---------:| --------:|
| application/json | false     | -              | JSON | _success   | JSON     |
| application/json | false     | -              | RAW  | _error     | -        |
| application/text | false     | -              | JSON | _success   | text     |
| application/text | true      | -              | JSON | _success   | JSON     |
| application/text | true      | -              | RAW  | _error     | -        |
| application/json | false     | JSON           | JSON | _success   | JSON     |
| application/text | false     | JSON           | JSON | _error     | -        |
| application/json | false     | JSON           | RAW  | _error     | -        |
| application/text | true      | JSON           | JSON | _error     | -        |

### Detailed configuration
All configuration options are explained in details in the [Config Options Cheetsheet](https://github.com/Knotx/knotx-data-bridge/tree/master/http/action/docs/asciidoc/dataobjects.adoc).
