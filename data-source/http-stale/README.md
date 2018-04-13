# HTTP Adapter
The module allows integrations with *HTTP endpoints*, in which the *JSON response* can be cached and re-used.

## When to use it?

Criteria:
 - we integrate with HTTP endpoint which responds with JSON
 - the endpoint response can be cached and reused for future requests
 
Examples of scenarios:
 - to reduce the load on a specific endpoint
 - to reduce the final Knot.x response time (do not wait for the HTTP response)

## How does it work?
The adapter caches HTTP responses with configurable TTL. When the cache does not contain the response,
the adapter calls HTTP endpoint, then saves the response in the cache and responds with the response.
When a new request is processed by the adapter and the response is in the cache there are two scenarios:
- cached value is stale (expired), the adapter responds with the stale value and schedules asynchronous HTTP 
endpoint call to refresh the cache
- cached value is valid, the adapter responds with the value

## How to configure?
See the [Knot.x deployment documentation](https://github.com/Cognifide/knotx/wiki/KnotxDeployment) to 
use Cacheable Http Adapter. 

The configuration allows to configure:

| Name                        | Type                                | Mandatory      | Description  |
|-------:                     |:-------:                            |:-------:       |-------|
| `address`                   | `String`                            | &#10004;       | Event bus address of the module. |
| `clientOptions`             | [io.vertx.core.http.HttpClientOptions](http://vertx.io/docs/apidocs/io/vertx/core/http/HttpClientOptions.html)          | &#10004;       | Http connection settings.|
| `address`                   | `String`                            | &#10004;       | Event bus address of the module. |
| `cacheTtl`                  | `Integer`                           | &#10004;       | Cache TTL. |
| `cacheSize`                 | `Integer`                           | &#10004;       | The maximum size of the cache. |
| `services`                  | `Array of JsonObject`               | &#10004;       | Service path regexp, endpoint domain and port. |
