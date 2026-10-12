/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.values;


import java.io.Serializable;
import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.values.SearchPossibleValueSourceInput;
import com.kingsrook.qqq.backend.core.model.actions.values.SearchPossibleValueSourceOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.frontend.QFrontendFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValue;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValuePicker;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Record picker requests on a table possible value source: CONTAINS search,
 ** allow-listed filters and sorts, keyset paging without counts, and only the
 ** picker's display fields returned per option.
 *******************************************************************************/
class SearchPossibleValueSourcePickerTest extends BaseTest
{

   /*******************************************************************************
    ** Six shapes; the shape source declares a picker.
    *******************************************************************************/
   @BeforeEach
   void beforeEach() throws QException
   {
      MemoryRecordStore.getInstance().reset();
      QContext.getQInstance().getPossibleValueSource(TestUtils.POSSIBLE_VALUE_SOURCE_SHAPE).setPicker(new QPossibleValuePicker()
         .withThumbnailField("type")
         .withSubtitleFields(List.of("noOfSides"))
         .withFilterFields(List.of("noOfSides", "isPolygon"))
         .withSearchFields(List.of("name"))
         .withSort("Newest", "id", false)
         .withSort("Name", "name", true)
         .withPageSize(2));

      String[] names = { "Triangle", "Square", "Circle", "Rectangle", "Pentagon", "Hexagon" };
      int[]    sides = { 3, 4, 0, 4, 5, 6 };
      List<QRecord> records = new java.util.ArrayList<>();
      for(int i = 0; i < names.length; i++)
      {
         records.add(new QRecord().withValue("id", i + 1).withValue("name", names[i]).withValue("noOfSides", sides[i])
            .withValue("isPolygon", sides[i] > 0).withValue("type", "https://img.example/" + names[i].toLowerCase() + ".png"));
      }
      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_SHAPE).withRecords(records));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void afterEach()
   {
      MemoryRecordStore.getInstance().reset();
      QContext.getQInstance().getPossibleValueSource(TestUtils.POSSIBLE_VALUE_SOURCE_SHAPE).setPicker(null);
   }



   /*******************************************************************************
    ** The default sort (newest first) pages by keyset: each page continues after
    ** the last id, and hasMore says whether another page follows.
    *******************************************************************************/
   @Test
   void testKeysetPagingWithoutCounts() throws QException
   {
      SearchPossibleValueSourceOutput page = search(input());
      assertEquals(List.of(6, 5), ids(page));
      assertTrue(page.getHasMore());

      page = search(input().withAfterId(5));
      assertEquals(List.of(4, 3), ids(page));
      assertTrue(page.getHasMore());

      page = search(input().withAfterId(3));
      assertEquals(List.of(2, 1), ids(page));
      assertFalse(page.getHasMore());

      ///////////////////////////////////////////////////////
      // ascending primary key continues with greater ids //
      ///////////////////////////////////////////////////////
      page = search(input().withPickerFilter(new QQueryFilter().withOrderBy(new QFilterOrderBy("id", true))).withAfterId("2"));
      assertEquals(List.of(3, 4), ids(page));
   }



   /*******************************************************************************
    ** Other sorts page by skip.
    *******************************************************************************/
   @Test
   void testOffsetPagingForOtherSorts() throws QException
   {
      QQueryFilter byName = new QQueryFilter().withOrderBy(new QFilterOrderBy("name", true));
      assertEquals(List.of(3, 6), ids(search(input().withPickerFilter(byName))));
      assertEquals(List.of(5, 4), ids(search(input().withPickerFilter(byName.clone()).withSkip(2))));
   }



   /*******************************************************************************
    ** Search matches anywhere in the picker search fields (CONTAINS), not just
    ** at the start as the inline dropdown does.
    *******************************************************************************/
   @Test
   void testContainsSearch() throws QException
   {
      SearchPossibleValueSourceOutput output = search(input().withSearchTerm("angle").withLimit(10));
      assertThat(output.getResults()).extracting(QPossibleValue::getLabel).containsExactlyInAnyOrder("Triangle", "Rectangle");
      assertFalse(output.getHasMore());

      //////////////////////////////////////////////////////
      // case-insensitive, whatever the backend's LIKE is //
      //////////////////////////////////////////////////////
      output = search(input().withSearchTerm("tRIANGLE").withLimit(10));
      assertThat(output.getResults()).extracting(QPossibleValue::getLabel).containsExactly("Triangle");
   }



   /*******************************************************************************
    ** Filters on picker filter fields apply; only the picker display fields come
    ** back per option, never other columns of the table.
    *******************************************************************************/
   @Test
   void testFiltersAndDisplayValues() throws QException
   {
      SearchPossibleValueSourceOutput output = search(input().withLimit(10)
         .withPickerFilter(new QQueryFilter(new QFilterCriteria("noOfSides", QCriteriaOperator.EQUALS, 4))));
      assertEquals(List.of(4, 2), ids(output));

      assertEquals("https://img.example/rectangle.png", output.getPickerValues().get(4).get("type"));
      assertEquals(4, output.getPickerValues().get(4).get("noOfSides"));
      assertThat(output.getPickerValues().get(4)).doesNotContainKeys("name", "isPolygon", "id");
   }



   /*******************************************************************************
    ** The selection (looked up by id) carries its thumbnail too.
    *******************************************************************************/
   @Test
   void testLookupByIdCarriesValues() throws QException
   {
      SearchPossibleValueSourceOutput output = search(input().withIdList(List.of(3)));
      assertEquals(List.of(3), ids(output));
      assertEquals("https://img.example/circle.png", output.getPickerValues().get(3).get("type"));
   }



   /*******************************************************************************
    ** Filters and sorts outside the allow-lists are refused, as is a picker
    ** request on a source without a picker.
    *******************************************************************************/
   @Test
   void testRefusesUndeclaredFieldsAndSources()
   {
      assertThrows(QBadRequestException.class, () -> search(input().withPickerFilter(new QQueryFilter(new QFilterCriteria("name", QCriteriaOperator.EQUALS, "Square")))));
      assertThrows(QBadRequestException.class, () -> search(input().withPickerFilter(new QQueryFilter()
         .withSubFilter(new QQueryFilter(new QFilterCriteria("type", QCriteriaOperator.IS_NOT_BLANK))))));
      assertThrows(QBadRequestException.class, () -> search(input().withPickerFilter(new QQueryFilter().withOrderBy(new QFilterOrderBy("type")))));

      QContext.getQInstance().getPossibleValueSource(TestUtils.POSSIBLE_VALUE_SOURCE_SHAPE).setPicker(null);
      assertThrows(QBadRequestException.class, () -> search(input()));
   }



   /*******************************************************************************
    ** Non-picker searches are unchanged (prefix match, no picker values).
    *******************************************************************************/
   @Test
   void testOrdinarySearchUnchanged() throws QException
   {
      SearchPossibleValueSourceInput input = new SearchPossibleValueSourceInput().withPossibleValueSourceName(TestUtils.POSSIBLE_VALUE_SOURCE_SHAPE).withSearchTerm("angle");
      SearchPossibleValueSourceOutput output = new SearchPossibleValueSourceAction().execute(input);
      assertEquals(0, output.getResults().size());
      assertNull(output.getPickerValues());
      assertNull(output.getHasMore());
   }



   /*******************************************************************************
    ** Fields on the source carry the picker (and its table) in frontend meta data.
    *******************************************************************************/
   @Test
   void testFrontendFieldMetaData()
   {
      QFrontendFieldMetaData field = new QFrontendFieldMetaData(new QFieldMetaData("shapeId", QFieldType.INTEGER).withPossibleValueSourceName(TestUtils.POSSIBLE_VALUE_SOURCE_SHAPE));
      assertEquals("type", field.getPicker().getThumbnailField());
      assertEquals(TestUtils.TABLE_NAME_SHAPE, field.getPickerTableName());

      QFrontendFieldMetaData plain = new QFrontendFieldMetaData(new QFieldMetaData("state", QFieldType.INTEGER).withPossibleValueSourceName(TestUtils.POSSIBLE_VALUE_SOURCE_STATE));
      assertNull(plain.getPicker());
      assertNull(plain.getPickerTableName());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static SearchPossibleValueSourceInput input()
   {
      return (new SearchPossibleValueSourceInput()
         .withPossibleValueSourceName(TestUtils.POSSIBLE_VALUE_SOURCE_SHAPE)
         .withPickerRequest(true)
         .withLimit(null));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static SearchPossibleValueSourceOutput search(SearchPossibleValueSourceInput input) throws QException
   {
      return (new SearchPossibleValueSourceAction().execute(input));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static List<Serializable> ids(SearchPossibleValueSourceOutput output)
   {
      return (output.getResults().stream().map(pv -> (Serializable) pv.getId()).toList());
   }
}
