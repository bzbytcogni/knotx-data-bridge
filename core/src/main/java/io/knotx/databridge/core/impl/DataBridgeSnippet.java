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
package io.knotx.databridge.core.impl;

import com.google.common.base.MoreObjects;
import io.knotx.databridge.core.attribute.DataSourceAttribute;
import io.knotx.databridge.core.attribute.DataSourceAttribute.AtributeType;
import io.knotx.databridge.core.datasource.DataSourceEntry;
import io.knotx.fragment.Fragment;
import io.knotx.knotengine.api.SnippetFragment;
import io.reactivex.Observable;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DataBridgeSnippet {

  private static final String DATA_SERVICE =
      ".*" + DataSourceAttribute.ATTRIBUTE_SELECTOR + DataSourceAttribute.ATTRIBUTE_SEPARATOR
          + AtributeType.NAME + ".*";
  private static final String DATA_PARAMS =
      ".*" + DataSourceAttribute.ATTRIBUTE_SELECTOR + DataSourceAttribute.ATTRIBUTE_SEPARATOR
          + AtributeType.PARAMS + ".*";

  private SnippetFragment snippetFragment;
  List<DataSourceEntry> services;

  private DataBridgeSnippet() {
    //hidden constructor
  }

  /**
   * Factory method that creates context from the {@link Fragment}. All services and params are
   * extracted to separate entries.
   *
   * @param snippetFragment - fragment from which the context will be created.
   * @return a DataBridgeSnippet that wraps given fragment.
   */
  public static DataBridgeSnippet from(SnippetFragment snippetFragment) {
    final Fragment fragment = snippetFragment.getDelegate();
    List<DataSourceAttribute> dataSourceNameAttributes = fragment.getConfiguration().stream()
        .filter(attr -> attr.getKey().matches(DATA_SERVICE))
        .map(e -> DataSourceAttribute.from(e.getKey(), e.getValue().toString()))
        .collect(Collectors.toList());

    Map<String, DataSourceAttribute> dataSourceParamsAttributes = fragment.getConfiguration().stream()
        .filter(attribute -> attribute.getKey().matches(DATA_PARAMS))
        .map(e -> DataSourceAttribute.from(e.getKey(), e.getValue().toString()))
        .collect(Collectors.toMap(DataSourceAttribute::getNamespace, Function.identity()));

    return new DataBridgeSnippet()
        .fragment(snippetFragment)
        .services(
            dataSourceNameAttributes.stream()
                .map(dsName -> new DataSourceEntry(dsName,
                    dataSourceParamsAttributes.get(dsName.getNamespace())))
                .collect(Collectors.toList())
        );
  }

  /**
   * @return an {@link Observable} that emits a list of {@link DataSourceEntry} that were registered
   * with current {@link Fragment}.
   */
  public Observable<DataSourceEntry> services() {
    return Observable.fromIterable(services);
  }

  /**
   * @return a fragment wrapped in this context.
   */
  public SnippetFragment fragment() {
    return snippetFragment;
  }

  private DataBridgeSnippet fragment(SnippetFragment snippetFragment) {
    this.snippetFragment = snippetFragment;
    return this;
  }

  private DataBridgeSnippet services(List<DataSourceEntry> services) {
    this.services = services;
    return this;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("fragment", snippetFragment)
        .add("services", services)
        .toString();
  }
}
