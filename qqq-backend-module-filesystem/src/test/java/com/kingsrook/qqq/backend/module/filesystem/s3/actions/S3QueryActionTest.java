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

package com.kingsrook.qqq.backend.module.filesystem.s3.actions;


import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.module.filesystem.TestUtils;
import com.kingsrook.qqq.backend.module.filesystem.base.FilesystemRecordBackendDetailFields;
import com.kingsrook.qqq.backend.module.filesystem.s3.BaseS3Test;
import com.kingsrook.qqq.backend.module.filesystem.s3.model.metadata.S3TableBackendDetails;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 **
 *******************************************************************************/
public class S3QueryActionTest extends BaseS3Test
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testQuery1() throws QException
   {
      QueryInput    queryInput    = initQueryRequest();
      S3QueryAction s3QueryAction = new S3QueryAction();
      s3QueryAction.setS3Utils(getS3Utils());
      QueryOutput queryOutput = s3QueryAction.execute(queryInput);
      Assertions.assertEquals(5, queryOutput.getRecords().size(), "Expected # of rows from unfiltered query");
      Assertions.assertTrue(queryOutput.getRecords().stream()
            .allMatch(record -> record.getBackendDetailString(FilesystemRecordBackendDetailFields.FULL_PATH).contains(BaseS3Test.TEST_FOLDER)),
         "All records should have a full-path in their backend details, matching the test folder name");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testGet() throws QException
   {
      QueryInput queryInput = new QueryInput(TestUtils.TABLE_NAME_BLOB_S3)
         .withFilter(new QQueryFilter(new QFilterCriteria("fileName", QCriteriaOperator.EQUALS, "BLOB-1.txt")));

      S3QueryAction s3QueryAction = new S3QueryAction();
      s3QueryAction.setS3Utils(getS3Utils());
      QueryOutput queryOutput = s3QueryAction.execute(queryInput);
      Assertions.assertEquals(1, queryOutput.getRecords().size(), "Expected # of rows from query");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QueryInput initQueryRequest() throws QException
   {
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.defineS3CSVPersonTable().getName());
      return queryInput;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testQueryForCardinalityOne() throws QException
   {
      QueryInput queryInput = new QueryInput(TestUtils.TABLE_NAME_BLOB_S3);
      queryInput.setFilter(new QQueryFilter());

      S3QueryAction s3QueryAction = new S3QueryAction();
      s3QueryAction.setS3Utils(getS3Utils());

      QueryOutput queryOutput = s3QueryAction.execute(queryInput);
      assertEquals(3, queryOutput.getRecords().size(), "Unfiltered query should find all rows");

      queryInput.setFilter(new QQueryFilter(new QFilterCriteria("fileName", QCriteriaOperator.EQUALS, "BLOB-1.txt")));
      queryOutput = s3QueryAction.execute(queryInput);
      assertEquals(1, queryOutput.getRecords().size(), "Filtered query should find 1 row");
      assertEquals("BLOB-1.txt", queryOutput.getRecords().get(0).getValueString("fileName"));

      ////////////////////////////////////////////////////////////////
      // put a glob on the table - now should only find 2 txt files //
      ////////////////////////////////////////////////////////////////
      QInstance instance = TestUtils.defineInstance();
      ((S3TableBackendDetails) (instance.getTable(TestUtils.TABLE_NAME_BLOB_S3).getBackendDetails()))
         .withGlob("*.txt");
      reInitInstanceInContext(instance);

      /////////////////////////////////////////////////////
      // make sure to reset the table in the query input //
      /////////////////////////////////////////////////////
      queryInput.setTableName(TestUtils.TABLE_NAME_BLOB_S3);

      queryInput.setFilter(new QQueryFilter());
      queryOutput = s3QueryAction.execute(queryInput);
      assertEquals(2, queryOutput.getRecords().size(), "Query should use glob and find 2 rows");

      //////////////////////////////
      // add a limit to the query //
      //////////////////////////////
      queryInput.setFilter(new QQueryFilter().withLimit(1));
      queryOutput = s3QueryAction.execute(queryInput);
      assertEquals(1, queryOutput.getRecords().size(), "Query with limit should be respected");
   }



   /*******************************************************************************
    ** We had a bug where, if both the backend and table have no basePath ("prefix"),
    ** then our file-listing was doing a request with a prefix starting with /, which
    ** causes no results, so, this test is to show that isn't happening.
    *******************************************************************************/
   @Test
   public void testQueryForCardinalityOneInBackendWithoutPrefix() throws QException
   {
      QueryInput queryInput = new QueryInput(TestUtils.TABLE_NAME_BLOB_S3_SANS_PREFIX);
      queryInput.setFilter(new QQueryFilter());

      S3QueryAction s3QueryAction = new S3QueryAction();
      s3QueryAction.setS3Utils(getS3Utils());

      QueryOutput queryOutput = s3QueryAction.execute(queryInput);
      assertEquals(3, queryOutput.getRecords().size(), "Unfiltered query should find all rows");

      queryInput.setFilter(new QQueryFilter(new QFilterCriteria("fileName", QCriteriaOperator.EQUALS, "BLOB-1.txt")));
      queryOutput = s3QueryAction.execute(queryInput);
      assertEquals(1, queryOutput.getRecords().size(), "Filtered query should find 1 row");
      assertEquals("BLOB-1.txt", queryOutput.getRecords().get(0).getValueString("fileName"));

      ////////////////////////////////////////////////////////////////
      // put a glob on the table - now should only find 2 txt files //
      ////////////////////////////////////////////////////////////////
      QInstance instance = TestUtils.defineInstance();
      ((S3TableBackendDetails) (instance.getTable(TestUtils.TABLE_NAME_BLOB_S3_SANS_PREFIX).getBackendDetails()))
         .withGlob("*.txt");
      reInitInstanceInContext(instance);

      /////////////////////////////////////////////////////
      // make sure to reset the table in the query input //
      /////////////////////////////////////////////////////
      queryInput.setTableName(TestUtils.TABLE_NAME_BLOB_S3_SANS_PREFIX);

      queryInput.setFilter(new QQueryFilter());
      queryOutput = s3QueryAction.execute(queryInput);
      assertEquals(2, queryOutput.getRecords().size(), "Query should use glob and find 2 rows");

      //////////////////////////////
      // add a limit to the query //
      //////////////////////////////
      queryInput.setFilter(new QQueryFilter().withLimit(1));
      queryOutput = s3QueryAction.execute(queryInput);
      assertEquals(1, queryOutput.getRecords().size(), "Query with limit should be respected");
   }

}