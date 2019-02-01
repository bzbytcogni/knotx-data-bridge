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
package io.knotx.databridge.core.attribute;

import java.util.Arrays;
import java.util.List;

public class DataSourceAttribute {

  public static final String ATTRIBUTE_SELECTOR = "databridge";
  public static final String ATTRIBUTE_SEPARATOR = "-";
  private static final int ATTRIBUTE_DATA_OFFSET = (ATTRIBUTE_SELECTOR + ATTRIBUTE_SEPARATOR)
      .length();

  private String namespace;

  private AtributeType type;

  private String value;

  public static DataSourceAttribute of(AtributeType type) {
    DataSourceAttribute builder = new DataSourceAttribute();
    builder.type = type;
    return builder;
  }

  public static DataSourceAttribute from(String key, String value) {
    String keyPostfix = key
        .substring(key.indexOf(ATTRIBUTE_SELECTOR + ATTRIBUTE_SEPARATOR) + ATTRIBUTE_DATA_OFFSET);
    String[] attrParts = keyPostfix.split(ATTRIBUTE_SEPARATOR);

    DataSourceAttribute builder = DataSourceAttribute.of(AtributeType.from(attrParts[0]));
    if (attrParts.length == 2) {
      builder.namespace = attrParts[1];
    }
    builder.value = value;
    return builder;
  }


  public DataSourceAttribute withNamespace(String namespace) {
    this.namespace = namespace;
    return this;
  }

  public DataSourceAttribute withValue(String value) {
    this.value = value;
    return this;
  }

  public String getNamespace() {
    return namespace;
  }

  public AtributeType getType() {
    return type;
  }

  public String getValue() {
    return value;
  }

  public enum AtributeType {
    NAME("name"), PARAMS("params");

    private String name;

    AtributeType(String name) {
      this.name = name;
    }

    public static AtributeType from(String value) {
      List<AtributeType> list = Arrays.asList(AtributeType.values());
      return list.stream().filter(m -> m.name.equals(value)).findAny()
          .orElseThrow(IllegalArgumentException::new);
    }

    @Override
    public String toString() {
      return name;
    }

  }

}
