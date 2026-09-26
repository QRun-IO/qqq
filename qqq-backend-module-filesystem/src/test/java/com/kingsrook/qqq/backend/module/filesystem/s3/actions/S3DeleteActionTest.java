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


import java.util.List;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.module.filesystem.TestUtils;
import com.kingsrook.qqq.backend.module.filesystem.s3.BaseS3Test;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 **
 *******************************************************************************/
public class S3DeleteActionTest extends BaseS3Test
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void test() throws QException
   {
      QInstance qInstance = TestUtils.defineInstance();

      int initialCount = count(TestUtils.TABLE_NAME_BLOB_S3);

      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.TABLE_NAME_BLOB_S3);
      insertInput.setRecords(List.of(
         new QRecord().withValue("fileName", "file2.txt").withValue("contents", "Hi, Bob.")));

      S3InsertAction insertAction = new S3InsertAction();
      insertAction.setS3Utils(getS3Utils());
      insertAction.execute(insertInput);

      assertEquals(initialCount + 1, count(TestUtils.TABLE_NAME_BLOB_S3));

      S3DeleteAction deleteAction = new S3DeleteAction();
      deleteAction.setS3Utils(getS3Utils());
      DeleteOutput deleteOutput = deleteAction.execute(new DeleteInput(TestUtils.TABLE_NAME_BLOB_S3).withPrimaryKeys(List.of("file2.txt")));
      assertEquals(1, deleteOutput.getDeletedRecordCount());
      assertEquals(0, deleteOutput.getRecordsWithErrors().size());

      assertEquals(initialCount, count(TestUtils.TABLE_NAME_BLOB_S3));
   }


   /***************************************************************************
    **
    ***************************************************************************/
   private Integer count(String tableName) throws QException
   {
      CountInput countInput = new CountInput();
      countInput.setTableName(tableName);
      S3CountAction s3CountAction = new S3CountAction();
      s3CountAction.setS3Utils(getS3Utils());
      CountOutput countOutput = s3CountAction.execute(countInput);
      return countOutput.getCount();
   }

}