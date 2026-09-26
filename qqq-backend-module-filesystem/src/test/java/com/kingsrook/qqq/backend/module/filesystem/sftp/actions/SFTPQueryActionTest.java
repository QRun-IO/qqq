/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.module.filesystem.sftp.actions;


import java.util.List;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.collections.MapBuilder;
import com.kingsrook.qqq.backend.module.filesystem.TestUtils;
import com.kingsrook.qqq.backend.module.filesystem.sftp.BaseSFTPTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Unit test for SFTPQueryAction 
 *******************************************************************************/
class SFTPQueryActionTest extends BaseSFTPTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testSimpleQuery() throws QException
   {
      QueryInput  queryInput  = new QueryInput(TestUtils.TABLE_NAME_SFTP_FILE);
      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      Assertions.assertEquals(5, queryOutput.getRecords().size(), "Expected # of rows from unfiltered query");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testSimpleQueryForOneFile() throws QException
   {
      QueryInput  queryInput  = new QueryInput(TestUtils.TABLE_NAME_SFTP_FILE);
      queryInput.setFilter(new QQueryFilter(new QFilterCriteria("fileName", QCriteriaOperator.EQUALS, "testfile-1.txt")));
      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      Assertions.assertEquals(1, queryOutput.getRecords().size(), "Expected # of rows from unfiltered query");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testQueryVariantsTable() throws Exception
   {
      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_VARIANT_OPTIONS).withRecords(List.of(
         new QRecord().withValue("id", 1).withValue("basePath", BaseSFTPTest.BACKEND_FOLDER),
         new QRecord().withValue("id", 2).withValue("basePath", "empty-folder"),
         new QRecord().withValue("id", 3).withValue("basePath", "non-existing-path")
      )));

      mkdirInSftpContainerUnderHomeTestuser("empty-folder/files");

      QueryInput queryInput = new QueryInput(TestUtils.TABLE_NAME_SFTP_FILE_VARIANTS);
      assertThatThrownBy(() -> new QueryAction().execute(queryInput))
         .hasMessageContaining("Could not find Backend Variant information in session under key 'variant-options-table' for Backend");

      QContext.getQSession().setBackendVariants(MapBuilder.of(TestUtils.TABLE_NAME_VARIANT_OPTIONS, 1));
      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      Assertions.assertEquals(5, queryOutput.getRecords().size(), "Expected # of rows from unfiltered query");

      QContext.getQSession().setBackendVariants(MapBuilder.of(TestUtils.TABLE_NAME_VARIANT_OPTIONS, 2));
      queryOutput = new QueryAction().execute(queryInput);
      Assertions.assertEquals(0, queryOutput.getRecords().size(), "Expected # of rows from unfiltered query");

      QContext.getQSession().setBackendVariants(MapBuilder.of(TestUtils.TABLE_NAME_VARIANT_OPTIONS, 3));
      assertThatThrownBy(() -> new QueryAction().execute(queryInput))
         .rootCause()
         .hasMessageContaining("No such file");

      // Assertions.assertEquals(5, queryOutput.getRecords().size(), "Expected # of rows from unfiltered query");
   }

}