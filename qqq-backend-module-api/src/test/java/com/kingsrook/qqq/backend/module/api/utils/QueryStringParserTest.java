/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kingsrook.qqq.backend.module.api.utils;


import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for com.kingsrook.qqq.backend.module.api.utils.QueryStringParser
 *******************************************************************************/
class QueryStringParserTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test()
   {
      assertEquals(Map.of(), QueryStringParser.parseQueryStringSingleValuePerKey(null));
      assertEquals(Map.of(), QueryStringParser.parseQueryStringSingleValuePerKey(""));
      assertEquals(Map.of("foo", "bar"), QueryStringParser.parseQueryStringSingleValuePerKey("foo=bar"));
      assertEquals(Map.of("foo", "bar=baz"), QueryStringParser.parseQueryStringSingleValuePerKey("foo=bar=baz"));
      assertEquals(Map.of("foo", "bar", "baz", ""), QueryStringParser.parseQueryStringSingleValuePerKey("foo=bar&baz="));
      assertEquals(Map.of("foo", "bar", "baz", "1"), QueryStringParser.parseQueryStringSingleValuePerKey("foo=bar&baz=1"));

      assertEquals(Map.of(), QueryStringParser.parseQueryStringMultiValuePerKey(null));
      assertEquals(Map.of(), QueryStringParser.parseQueryStringMultiValuePerKey(""));
      assertEquals(Map.of("foo", List.of("bar")), QueryStringParser.parseQueryStringMultiValuePerKey("foo=bar"));
      assertEquals(Map.of("foo", List.of("bar"), "baz", List.of("")), QueryStringParser.parseQueryStringMultiValuePerKey("foo=bar&baz="));
      assertEquals(Map.of("foo", List.of("bar"), "baz", List.of("1")), QueryStringParser.parseQueryStringMultiValuePerKey("foo=bar&baz=1"));

      assertEquals(Map.of("foo", List.of("bar", "baz")), QueryStringParser.parseQueryStringMultiValuePerKey("foo=bar&foo=baz"));
      assertEquals(Map.of("foo", List.of("bar", "baz"), "bar", List.of("1")), QueryStringParser.parseQueryStringMultiValuePerKey("foo=bar&foo=baz&bar=1"));
   }

}