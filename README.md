# Knot.x Data Bridge
Knot.x Data Bridge collects all dynamic data from external data sources and exposes them for further 
[processing](https://github.com/Cognifide/knotx/wiki/KnotRouting).

## How does it work?
Knot.x Data Bridge defines data sources, that can be utilized in HTML markup, in [Fragments](https://github.com/Cognifide/knotx/wiki/Splitter). 
The data source can be any kind of system, from a web service, through low-level storages, such as a database
or cache, to a custom data provider.

The data source definition contains an unique name, parameters and a data source adapter:
```hocon
dataDefinitions = [
  {
    name = employees-rest-service
    params = "{ \"path\": \"/path/employees.json\" }"
    # params.path = /path/employees.json
    adapter = rest-http-adapter
  },
  {
    name = transactions-soap-service
    adapter = soap-http-adapter
  },
  {
    name = salaries-db-source
    params.query = "SELECT * FROM salaries;"
    adapter = postgres-db-adapter
  },
  {
    name = jobs-solr
    params.query = "query phrase"
    params.page = 1
    params.facet = "/jobs/it"
    adapter = solr-adapter
  }
]
```
The unique name is the data source identifier that can be used in the HTML template. Then we can 
specify default parameters that are used during the data source integration. The `params` attribute is a 
JSON object which can be easily split into key-value pairs with [HOCON syntax](https://github.com/lightbend/config/blob/master/HOCON.md#array-and-object-concatenation).
The last definition entry, the `adapter`, specifies the module deployed within Knot.x that calls the 
data source(s) and provides the data in the JSON format.

Let's see the configuration above. There are four data source definitions: 
- `employees-rest-service` - employees from the HTTP REST service
- `transactions-soap-service` - transactions from the HTTP SOAP service
- `cities-db-source` - cities from the PostgreSQL database
- `jobs-solr` - search results connected from the search engine
 
You can easily define your own business logic that for example fetches employees coming from the REST service, 
then for all directors checks their transactions using the SOAP service and finally validates salaries based 
on data from the PostgreSQL database. It can be easily hidden in the custom Adapter, see
more details in the Data Sources Adapters section.

Let's concentrate for now on the first simplest case - fetching employees from the HTTP REST service. The 
data source definition looks like:

```hocon
{
  name = employees-rest-service
  params = "{ \"path\": \"/path/employees.json\" }"
  # params.path = /path/employees.json
  adapter = rest-http-adapter
}
```

The data source definition is communication protocol agnostic so it does not contain HTTP details 
like SSL, HTTP/2 or pipelining support. All those details are hidden in the Data Source Adapter 
configuration that can map the `params` JSON object (the path in this case) to the HTTP specifics.
The `adapter` entry whose value is `rest-http-adapter` specifies the event bus address on which the 
module capable of calling the HTTP service listens.

Now let's see how the `employees-rest-service` data source in the HTML markup is configured. The 
`script` definition (Fragment) looks like:
                                                                                           
```html
<script data-knotx-knots="databridge,handlebars"
  data-knotx-databridge-name="employees-rest-service" data-knotx-databridge-params='{"path":"/overridden/path"}'
  data-knotx-databridge-name-mysalaries="salaries-db-source"
  type="text/knotx-snippet">
  ...
</script>
``` 

Data Bridge filters Fragments containing the `databridge` entry in the `data-knotx-knots` attribute
(the list of Knots). Then for all filtered fragments it collects responses from data sources specified in 
`data-knotx-databridge-name-{NAMESPACE}`=[data source definition name] attributes through subsequent 
asynchronous Adapters calls. All collected JSONs (see Data Source Adapter contract) are saved
in the [Fragment Context](https://github.com/Cognifide/knotx/wiki/Splitter#fragment). 
The `NAMESPACE` is optional and specifies the key under which the response from the data 
source is saved. The default namespace is `_result`. 

The final Fragment Context for our example looks like: 
```json
{
  "_result": { EMPLOYEES_JSON },
  "mysalaries": { SALARIES_JSON }
}
```

The data source parameters can be also configured in Fragment and merged with default ones using 
the `data-knotx-params-{NAMESPACE}={JSON DATA}` attribute. The attribute is matched with the data 
source definition based on a namespace.

## Data Source Caching
// TODO


# Data Sources Adapters
// TODO


## HTTP Data Source Adapter
// TODO

## Community
Knot.x gives one communication channel that is described [here](https://github.com/Cognifide/knotx#community).

## Bugs
All feature requests and bugs can be filed as issues on [Gitub](https://github.com/Knotx/knotx-data-bridge/issues). 
Do not use Github issues to ask questions, post them on the [User Group](https://groups.google.com/forum/#!forum/knotx) or [Gitter Chat](https://gitter.im/Knotx/Lobby).

## Licence
**Knot.x modules** are licensed under the [Apache License, Version 2.0 (the "License")](https://www.apache.org/licenses/LICENSE-2.0.txt)
