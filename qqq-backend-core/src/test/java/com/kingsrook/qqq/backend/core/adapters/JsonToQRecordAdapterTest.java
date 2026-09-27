/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.adapters;


import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


/*******************************************************************************
 ** Unit test for JsonToQRecordAdapter
 **
 *******************************************************************************/
class JsonToQRecordAdapterTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void test_buildRecordsFromJson_nullInput()
   {
      testExpectedToThrow(null);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void test_buildRecordsFromJson_emptyStringInput()
   {
      testExpectedToThrow("");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void test_buildRecordsFromJson_inputDoesntLookLikeJson()
   {
      testExpectedToThrow("<HTML>");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void test_buildRecordsFromJson_inputLooksLikeJsonButIsMalformed()
   {
      testExpectedToThrow("{json=not}");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void testExpectedToThrow(String json)
   {
      try
      {
         JsonToQRecordAdapter jsonToQRecordAdapter = new JsonToQRecordAdapter();
         List<QRecord>        qRecords             = jsonToQRecordAdapter.buildRecordsFromJson(json, TestUtils.defineTablePerson(), null);
         System.out.println(qRecords);
      }
      catch(IllegalArgumentException iae)
      {
         System.out.println("Threw expected exception");
         return;
      }

      fail("Didn't throw expected exception");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void test_buildRecordsFromJson_emptyList()
   {
      JsonToQRecordAdapter jsonToQRecordAdapter = new JsonToQRecordAdapter();
      List<QRecord>        qRecords             = jsonToQRecordAdapter.buildRecordsFromJson("[]", TestUtils.defineTablePerson(), null);
      assertNotNull(qRecords);
      assertTrue(qRecords.isEmpty());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void test_buildRecordsFromJson_inputObject()
   {
      JsonToQRecordAdapter jsonToQRecordAdapter = new JsonToQRecordAdapter();
      List<QRecord> qRecords = jsonToQRecordAdapter.buildRecordsFromJson("""
         {
            "firstName":"Joe",
            "lastName":"Dimagio"
         }
         """, TestUtils.defineTablePerson(), null);
      assertNotNull(qRecords);
      assertEquals(1, qRecords.size());
      assertEquals("Joe", qRecords.get(0).getValue("firstName"));
      assertEquals("Dimagio", qRecords.get(0).getValue("lastName"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void test_buildRecordsFromJson_inputList()
   {
      JsonToQRecordAdapter jsonToQRecordAdapter = new JsonToQRecordAdapter();
      List<QRecord> qRecords = jsonToQRecordAdapter.buildRecordsFromJson("""
         [
            { "firstName":"Tyler", "lastName":"Samples" },
            { "firstName":"Tim", "lastName":"Chamberlain" }
         ]
         """, TestUtils.defineTablePerson(), null);
      assertNotNull(qRecords);
      assertEquals(2, qRecords.size());
      assertEquals("Tyler", qRecords.get(0).getValue("firstName"));
      assertEquals("Samples", qRecords.get(0).getValue("lastName"));
      assertEquals("Tim", qRecords.get(1).getValue("firstName"));
      assertEquals("Chamberlain", qRecords.get(1).getValue("lastName"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void test_buildRecordsFromJsonWithoutTable_inputList()
   {
      JsonToQRecordAdapter jsonToQRecordAdapter = new JsonToQRecordAdapter();
      List<QRecord> qRecords = jsonToQRecordAdapter.buildRecordsFromJson("""
         [
            { "firstName":"Tyler", "last":"Samples" },
            { "firstName":"Tim", "lastName":"Chamberlain" }
         ]
         """, null, null);
      assertNotNull(qRecords);
      assertEquals(2, qRecords.size());
      assertEquals("Tyler", qRecords.get(0).getValue("firstName"));
      assertEquals("Samples", qRecords.get(0).getValue("last"));
      assertEquals("Tim", qRecords.get(1).getValue("firstName"));
      assertEquals("Chamberlain", qRecords.get(1).getValue("lastName"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void test_buildRecordsFromJson_inputListWithNonObjectMembers()
   {
      testExpectedToThrow("[ 1701 ]");
   }



}
