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


import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.StorageAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.storage.StorageInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.collections.MapBuilder;
import com.kingsrook.qqq.backend.module.filesystem.TestUtils;
import com.kingsrook.qqq.backend.module.filesystem.sftp.BaseSFTPTest;
import com.kingsrook.qqq.backend.module.filesystem.sftp.model.metadata.SFTPTableBackendDetails;
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



   /*******************************************************************************
    ** Table globs filter both directory and explicit-file queries without changing files.
    *******************************************************************************/
   @Test
   void testGlobAppliesToDirectoryAndDirectFile() throws Exception
   {
      SFTPTableBackendDetails details = (SFTPTableBackendDetails) QContext.getQInstance().getTable(TestUtils.TABLE_NAME_SFTP_FILE).getBackendDetails();
      details.setGlob("testfile-[12].txt");
      Assertions.assertEquals(2, new QueryAction().execute(new QueryInput(TestUtils.TABLE_NAME_SFTP_FILE)).getRecords().size());
      Assertions.assertEquals(2, CountAction.execute(TestUtils.TABLE_NAME_SFTP_FILE, null));
      for(int number : List.of(1, 3))
      {
         QQueryFilter filter = new QQueryFilter(new QFilterCriteria("fileName", QCriteriaOperator.EQUALS, "testfile-" + number + ".txt"));
         Assertions.assertEquals(number == 1 ? 1 : 0, new QueryAction().execute(new QueryInput(TestUtils.TABLE_NAME_SFTP_FILE).withFilter(filter)).getRecords().size());
         Assertions.assertEquals(number == 1 ? 1 : 0, CountAction.execute(TestUtils.TABLE_NAME_SFTP_FILE, filter));
      }
      details.setGlob("*.absent");
      Assertions.assertEquals(0, CountAction.execute(TestUtils.TABLE_NAME_SFTP_FILE, null));
      Assertions.assertEquals(0, sftpContainer.execInContainer("test", "-f", REMOTE_DIR + "/testfile-3.txt").getExitCode());
   }



   /*******************************************************************************
    ** OpenSSH canonical paths and native sentinels verify bounds independently of the embedded server.
    *******************************************************************************/
   @Test
   void testTableBoundsAndSymlinks() throws Exception
   {
      String table = TestUtils.TABLE_NAME_SFTP_FILE;
      String outside = "/home/" + USERNAME + "/" + BACKEND_FOLDER + "/sentinel.txt";
      Assertions.assertEquals(0, sftpContainer.execInContainer("sh", "-c", "printf sentinel > \"$1\"", "fixture", outside).getExitCode());
      sftpContainer.execInContainer("chmod", "666", outside);
      sftpContainer.execInContainer("ln", "-s", "../sentinel.txt", REMOTE_DIR + "/outside-link.txt");
      sftpContainer.execInContainer("ln", "-s", "..", REMOTE_DIR + "/outside-directory");
      try
      {
         for(String reference : List.of("../sentinel.txt", "outside-link.txt", "outside-directory/sentinel.txt"))
         {
            QQueryFilter filter = new QQueryFilter(new QFilterCriteria("fileName", QCriteriaOperator.EQUALS, reference));
            Assertions.assertThrows(QException.class, () -> new QueryAction().execute(new QueryInput(table).withFilter(filter).withShouldFetchHeavyFields(true)));
            Assertions.assertThrows(QException.class, () ->
            {
               try(InputStream stream = new StorageAction().getInputStream(new StorageInput(table).withReference(reference)))
               {
                  stream.readAllBytes();
               }
            });
            Assertions.assertThrows(QException.class, () ->
            {
               try(OutputStream stream = new StorageAction().createOutputStream(new StorageInput(table).withReference(reference)))
               {
                  stream.write(65);
               }
            });
            QRecord record = InsertAction.executeForRecords(new InsertInput(table)
               .withRecord(new QRecord().withValue("fileName", reference).withValue("contents", "overwrite"))).get(0);
            Assertions.assertFalse(record.getErrors().isEmpty());
            var deleted = new DeleteAction().execute(new DeleteInput(table).withPrimaryKey(reference));
            Assertions.assertEquals(0, deleted.getDeletedRecordCount());
            Assertions.assertEquals(1, deleted.getRecordsWithErrors().size());
            Assertions.assertEquals("sentinel", sftpContainer.execInContainer("cat", outside).getStdout());
         }
         Assertions.assertEquals(0, sftpContainer.execInContainer("mkdir", REMOTE_DIR + "/nested").getExitCode());
         sftpContainer.execInContainer("chmod", "777", REMOTE_DIR + "/nested");
         sftpContainer.execInContainer("ln", "-s", "nested", REMOTE_DIR + "/safe-directory");
         try(OutputStream stream = new StorageAction().createOutputStream(new StorageInput(table).withReference("safe-directory/雪 space.txt")))
         {
            stream.write("inside".getBytes(StandardCharsets.UTF_8));
         }
         Assertions.assertEquals("inside", sftpContainer.execInContainer("cat", REMOTE_DIR + "/nested/雪 space.txt").getStdout());
         sftpContainer.execInContainer("ln", "-s", "nested/雪 space.txt", REMOTE_DIR + "/safe-link.txt");
         QRecord record = InsertAction.executeForRecords(new InsertInput(table)
            .withRecord(new QRecord().withValue("fileName", "safe-link.txt").withValue("contents", "updated"))).get(0);
         Assertions.assertTrue(record.getErrors().isEmpty());
         try(InputStream stream = new StorageAction().getInputStream(new StorageInput(table).withReference("safe-link.txt")))
         {
            Assertions.assertEquals("updated", new String(stream.readAllBytes(), StandardCharsets.UTF_8));
         }
         Assertions.assertEquals(1, new DeleteAction().execute(new DeleteInput(table).withPrimaryKey("safe-link.txt")).getDeletedRecordCount());
         Assertions.assertEquals("updated", sftpContainer.execInContainer("cat", REMOTE_DIR + "/nested/雪 space.txt").getStdout());
         var missing = new DeleteAction().execute(new DeleteInput(table).withPrimaryKey("absent.txt"));
         Assertions.assertEquals(0, missing.getDeletedRecordCount());
         Assertions.assertEquals(1, missing.getRecordsWithErrors().size());
      }
      finally
      {
         sftpContainer.execInContainer("rm", "-rf", outside, REMOTE_DIR + "/outside-link.txt", REMOTE_DIR + "/outside-directory", REMOTE_DIR + "/safe-directory", REMOTE_DIR + "/safe-link.txt", REMOTE_DIR + "/nested");
      }
   }

}