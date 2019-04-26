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
package io.knotx.databridge.http.action.common.placeholders;

import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class UriInfoHelperTest {

  public static Stream<Arguments> data() {
    return Stream.of(
        Arguments.of("/a/b", "/a/b", null, null, null),
        Arguments.of("/a/b.html", "/a/b", null, "html", null),
        Arguments.of("/a/b.s1.html", "/a/b", "s1", "html", null),
        Arguments.of("/a/b.s1.s2.html", "/a/b", "s1.s2", "html", null),
        Arguments.of("/a/b.s1.html/c/d", "/a/b", "s1", "html", "/c/d"),
        Arguments.of("/a/b.s1.s2.html/c/d", "/a/b", "s1.s2", "html", "/c/d"),
        Arguments.of("/a/b.html/c/d.s.txt", "/a/b", null, "html", "/c/d.s.txt"),
        Arguments.of("/a/b.s1.html/c/d.s.txt", "/a/b", "s1", "html", "/c/d.s.txt"),
        Arguments.of("/a/b.s1.s2.html/c/d.s.txt", "/a/b", "s1.s2", "html", "/c/d.s.txt"),
        Arguments.of("/a/b?q=v", "/a/b", null, null, null),
        Arguments.of("/a/b.html?q=v", "/a/b", null, "html", null),
        Arguments.of("/a/b.s1.html?q=v", "/a/b", "s1", "html", null),
        Arguments.of("/a/b.s1.s2.html?q=v", "/a/b", "s1.s2", "html", null),
        Arguments.of("/a/b.s1.html/c/d?q=v", "/a/b", "s1", "html", "/c/d"),
        Arguments.of("/a/b.s1.s2.html/c/d?q=v", "/a/b", "s1.s2", "html", "/c/d"),
        Arguments.of("/a/b.html/c/d.s.txt?q=v", "/a/b", null, "html", "/c/d.s.txt"),
        Arguments.of("/a/b.s1.html/c/d.s.txt?q=v", "/a/b", "s1", "html", "/c/d.s.txt"),
        Arguments.of("/a/b.s1.s2.html/c/d.s.txt?q=v", "/a/b", "s1.s2", "html", "/c/d.s.txt"),
        Arguments.of("/a/b#f", "/a/b", null, null, null),
        Arguments.of("/a/b.html#f", "/a/b", null, "html", null),
        Arguments.of("/a/b.s1.html#f", "/a/b", "s1", "html", null),
        Arguments.of("/a/b.s1.s2.html#f", "/a/b", "s1.s2", "html", null),
        Arguments.of("/a/b.s1.html/c/d#f", "/a/b", "s1", "html", "/c/d"),
        Arguments.of("/a/b.s1.s2.html/c/d#f", "/a/b", "s1.s2", "html", "/c/d"),
        Arguments.of("/a/b.html/c/d.s.txt#f", "/a/b", null, "html", "/c/d.s.txt"),
        Arguments.of("/a/b.s1.html/c/d.s.txt#f", "/a/b", "s1", "html", "/c/d.s.txt"),
        Arguments.of("/a/b.s1.s2.html/c/d.s.txt#f", "/a/b", "s1.s2", "html", "/c/d.s.txt")
    );
  }

  @ParameterizedTest(name = "{index}: {0}")
  @MethodSource("data")
  public void getUriInfo_whenGivenUrl_expectProperlyDecomposedUrl(
      String uri, String path, String selectorString, String extension, String suffix) {
    final SlingUriInfo uriInfo = SlingUriInfoHelper.getUriInfo(uri);
    boolean result = StringUtils.equals(uriInfo.getPath(), path);
    result &= StringUtils.equals(uriInfo.getSelectorString(), selectorString);
    result &= StringUtils.equals(uriInfo.getExtension(), extension);
    result &= StringUtils.equals(uriInfo.getSuffix(), suffix);
    Assertions.assertTrue(result);
  }
}