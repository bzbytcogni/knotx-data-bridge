# How to extend
You can find here separate modules with basic code example of extending _Knot.x Data Bridge_

## Adapter starterkit

Module with very basic implementation of Data Source adapter.

 - `ExampleDataSourceAdapter` [_Verticle_](http://vertx.io/docs/apidocs/io/vertx/core/Verticle.html) implementation to integrate with _Knot.x_
 - `ExampleDataSourceAdapterProxy` Proxy to handle incoming request
 - `ExampleDataSourceOptions` Java model to read the configuration
 - `ExampleDataSourceAdapterProxyTest` Test class for testing contract between _Knot.x Data Bridge_ and adapter implementation
 
For more information please refer to [Adapt Service without Web API](http://knotx.io/tutorials/adapt-service-without-webapi)
