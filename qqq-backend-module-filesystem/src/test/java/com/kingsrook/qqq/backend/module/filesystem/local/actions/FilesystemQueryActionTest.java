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

package com.kingsrook.qqq.backend.module.filesystem.local.actions;


import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.module.filesystem.TestUtils;
import com.kingsrook.qqq.backend.module.filesystem.base.FilesystemRecordBackendDetailFields;
import com.kingsrook.qqq.backend.module.filesystem.base.actions.AbstractPostReadFileCustomizer;
import com.kingsrook.qqq.backend.module.filesystem.base.actions.FilesystemTableCustomizers;
import com.kingsrook.qqq.backend.module.filesystem.local.model.metadata.FilesystemTableBackendDetails;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 **
 *******************************************************************************/
public class FilesystemQueryActionTest extends FilesystemActionTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testQuery1() throws QException
   {
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.defineLocalFilesystemJSONPersonTable().getName());
      QueryOutput queryOutput = new FilesystemQueryAction().execute(queryInput);
      assertEquals(3, queryOutput.getRecords().size(), "Unfiltered query should find all rows");
      assertTrue(queryOutput.getRecords().stream()
            .allMatch(record -> record.getBackendDetailString(FilesystemRecordBackendDetailFields.FULL_PATH).contains(TestUtils.BASE_PATH)),
         "All records should have a full-path in their backend details, matching the test folder name");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testQueryWithFileCustomizer() throws QException
   {
      QueryInput queryInput = new QueryInput();
      QInstance  instance   = TestUtils.defineInstance();

      QTableMetaData table = instance.getTable(TestUtils.TABLE_NAME_PERSON_LOCAL_FS_JSON);
      table.withCustomizer(FilesystemTableCustomizers.POST_READ_FILE.getRole(), new QCodeReference(ValueUpshifter.class));
      reInitInstanceInContext(instance);

      queryInput.setTableName(TestUtils.defineLocalFilesystemJSONPersonTable().getName());
      QueryOutput queryOutput = new FilesystemQueryAction().execute(queryInput);
      assertEquals(3, queryOutput.getRecords().size(), "Unfiltered query should find all rows");
      assertTrue(
         queryOutput.getRecords().stream().allMatch(record -> record.getValueString("email").matches(".*KINGSROOK.COM")),
         "All records should have their email addresses up-shifted.");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testQueryForCardinalityOne() throws QException
   {
      FilesystemQueryAction filesystemQueryAction = new FilesystemQueryAction();

      QueryInput queryInput = new QueryInput(TestUtils.TABLE_NAME_BLOB_LOCAL_FS);
      queryInput.setFilter(new QQueryFilter());
      QueryOutput queryOutput = filesystemQueryAction.execute(queryInput);
      assertEquals(3, queryOutput.getRecords().size(), "Unfiltered query should find all rows");

      queryInput.setFilter(new QQueryFilter(new QFilterCriteria("fileName", QCriteriaOperator.EQUALS, "BLOB-1.txt")));
      queryOutput = filesystemQueryAction.execute(queryInput);
      assertEquals(1, queryOutput.getRecords().size(), "Filtered query should find 1 row");
      assertEquals("BLOB-1.txt", queryOutput.getRecords().get(0).getValueString("fileName"));

      ////////////////////////////////////////////////////////////////
      // put a glob on the table - now should only find 2 txt files //
      ////////////////////////////////////////////////////////////////
      QInstance instance = TestUtils.defineInstance();
      ((FilesystemTableBackendDetails) (instance.getTable(TestUtils.TABLE_NAME_BLOB_LOCAL_FS).getBackendDetails()))
         .withGlob("*.txt");
      reInitInstanceInContext(instance);

      /////////////////////////////////////////////////////
      // make sure to reset the table in the query input //
      /////////////////////////////////////////////////////
      queryInput.setTableName(TestUtils.TABLE_NAME_BLOB_LOCAL_FS);

      queryInput.setFilter(new QQueryFilter());
      queryOutput = filesystemQueryAction.execute(queryInput);
      assertEquals(2, queryOutput.getRecords().size(), "Query should use glob and find 2 rows");

      //////////////////////////////
      // add a limit to the query //
      //////////////////////////////
      queryInput.setFilter(new QQueryFilter().withLimit(1));
      queryOutput = filesystemQueryAction.execute(queryInput);
      assertEquals(1, queryOutput.getRecords().size(), "Query with limit should be respected");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class ValueUpshifter extends AbstractPostReadFileCustomizer
   {
      @Override
      public String customizeFileContents(String s)
      {
         return (s.replaceAll("kingsrook.com", "KINGSROOK.COM"));
      }
   }

}