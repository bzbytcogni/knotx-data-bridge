[![Build Status](https://dev.azure.com/knotx/Knotx/_apis/build/status/Knotx.knotx-data-bridge?branchName=master)](https://dev.azure.com/knotx/Knotx/_build/latest?definitionId=11&branchName=master)
[![CodeFactor](https://www.codefactor.io/repository/github/knotx/knotx-template-engine/badge)](https://www.codefactor.io/repository/github/knotx/knotx-template-engine)
[![codecov](https://codecov.io/gh/Knotx/knotx-data-bridge/branch/master/graph/badge.svg)](https://codecov.io/gh/Knotx/knotx-data-bridge)

# Knot.x Data Bridge
Data Bridge module contains [**Actions**](https://github.com/Knotx/knotx-fragments/tree/master/handler/api#action)
implementations that are "bridge" between Knot.x [Fragments Processing](https://github.com/Knotx/knotx-fragments)
mechanism and external data sources (like Web APIs, databases, caches etc.).

## How does it work
Data Bridge Actions logic is to collect the data from external data sources and update processed [Fragment's](https://github.com/Knotx/knotx-fragments/tree/master/api#knotx-fragment-api)
`payload` with it. Such operation enables [further Actions in the Task](https://github.com/Knotx/knotx-fragments#how-does-it-works) 
to use that data during later processing (e.g. during template processing).

## Data Bridge Actions
Currently, the only Data Bridge implementation is an `http` action. You may read more about its details
in the [module docs](https://github.com/Knotx/knotx-data-bridge/tree/master/http).

## Community
Knot.x gives one communication channel that is described [here](https://github.com/Knotx/knotx#community).

## Bugs
All feature requests and bugs can be filed as issues on [Gitub](https://github.com/Knotx/knotx-data-bridge/issues).
Do not use Github issues to ask questions, post them on the [User Group](https://groups.google.com/forum/#!forum/knotx) or [Gitter Chat](https://gitter.im/Knotx/Lobby).

## Licence
**Knot.x modules** are licensed under the [Apache License, Version 2.0 (the "License")](https://www.apache.org/licenses/LICENSE-2.0.txt)
