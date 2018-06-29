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
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Attribute;

public class DataSourceAttribute {

  public static final String DATA_BRIDGE_ATTR_PREFIX = "data-knotx-databridge";

  private String namespace;

  private AtributeType type;

  private String value;

  public static DataSourceAttribute of(AtributeType type) {
    DataSourceAttribute builder = new DataSourceAttribute();
    builder.type = type;
    return builder;
  }

  public static DataSourceAttribute from(Attribute attr) {
    String[] attrParts = attr.getKey().split("-");

    DataSourceAttribute builder = DataSourceAttribute.of(AtributeType.from(attrParts[3]));
    if (attrParts.length == 5) {
      builder.namespace = attrParts[4];
    }
    builder.value = attr.getValue();
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

  public String build() {
    String result = DATA_BRIDGE_ATTR_PREFIX + "-" + type.name;
    if (StringUtils.isNotBlank(namespace)) {
      result += "-" + namespace;
    }
    if (StringUtils.isNotBlank(value)) {
      result += "=" + value;
    }
    return result;
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
