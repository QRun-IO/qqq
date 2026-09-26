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


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.module.filesystem.TestUtils;
import com.kingsrook.qqq.backend.module.filesystem.base.FilesystemRecordBackendDetailFields;
import com.kingsrook.qqq.backend.module.filesystem.s3.BaseS3Test;
import com.kingsrook.qqq.backend.module.filesystem.s3.model.metadata.S3TableBackendDetails;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 **
 *******************************************************************************/
public class S3InsertActionTest extends BaseS3Test
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testCardinalityOne() throws QException, IOException
   {
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.TABLE_NAME_BLOB_S3);
      insertInput.setRecords(List.of(
         new QRecord().withValue("fileName", "file2.txt").withValue("contents", "Hi, Bob.")));

      S3InsertAction insertAction = new S3InsertAction();
      insertAction.setS3Utils(getS3Utils());
      InsertOutput insertOutput = insertAction.execute(insertInput);

      assertThat(insertOutput.getRecords())
         .allMatch(record -> record.getBackendDetailString(FilesystemRecordBackendDetailFields.FULL_PATH).contains("blobs"));

      String       fullPath = insertOutput.getRecords().get(0).getBackendDetailString(FilesystemRecordBackendDetailFields.FULL_PATH);
      S3Object     object   = getAmazonS3().getObject(BUCKET_NAME, fullPath);
      List<String> lines    = IOUtils.readLines(object.getObjectContent(), StandardCharsets.UTF_8);
      assertEquals("Hi, Bob.", lines.get(0));

      ObjectMetadata objectMetadata = object.getObjectMetadata();
      assertEquals("text/plain", objectMetadata.getContentType());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testContentTypeFromField() throws QException
   {
      ((S3TableBackendDetails) QContext.getQInstance().getTable(TestUtils.TABLE_NAME_BLOB_S3)
         .getBackendDetails())
         .withContentTypeStrategy(S3TableBackendDetails.ContentTypeStrategy.FROM_FIELD)
         .withContentTypeFieldName("contentType");

      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.TABLE_NAME_BLOB_S3);
      insertInput.setRecords(List.of(
         new QRecord().withValue("fileName", "file2.txt").withValue("contentType", "myContentType/fake").withValue("contents", "Hi, Bob.")));

      S3InsertAction insertAction = new S3InsertAction();
      insertAction.setS3Utils(getS3Utils());
      InsertOutput insertOutput = insertAction.execute(insertInput);

      String   fullPath = insertOutput.getRecords().get(0).getBackendDetailString(FilesystemRecordBackendDetailFields.FULL_PATH);
      S3Object object   = getAmazonS3().getObject(BUCKET_NAME, fullPath);
      ObjectMetadata objectMetadata = object.getObjectMetadata();
      assertEquals("myContentType/fake", objectMetadata.getContentType());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testContentTypeHardcoded() throws QException
   {
      ((S3TableBackendDetails) QContext.getQInstance().getTable(TestUtils.TABLE_NAME_BLOB_S3)
         .getBackendDetails())
         .withContentTypeStrategy(S3TableBackendDetails.ContentTypeStrategy.HARDCODED)
         .withHardcodedContentType("your-content-type");

      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.TABLE_NAME_BLOB_S3);
      insertInput.setRecords(List.of(
         new QRecord().withValue("fileName", "file2.txt").withValue("contents", "Hi, Bob.")));

      S3InsertAction insertAction = new S3InsertAction();
      insertAction.setS3Utils(getS3Utils());
      InsertOutput insertOutput = insertAction.execute(insertInput);

      String   fullPath = insertOutput.getRecords().get(0).getBackendDetailString(FilesystemRecordBackendDetailFields.FULL_PATH);
      S3Object object   = getAmazonS3().getObject(BUCKET_NAME, fullPath);
      ObjectMetadata objectMetadata = object.getObjectMetadata();
      assertEquals("your-content-type", objectMetadata.getContentType());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testCardinalityMany() throws QException, IOException
   {
      QInstance   qInstance   = TestUtils.defineInstance();
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.TABLE_NAME_PERSON_S3);
      insertInput.setRecords(List.of(
         new QRecord().withValue("id", "1").withValue("firstName", "Bob")
      ));

      S3InsertAction insertAction = new S3InsertAction();
      insertAction.setS3Utils(getS3Utils());

      assertThatThrownBy(() -> insertAction.execute(insertInput))
         .hasRootCauseInstanceOf(NotImplementedException.class);
   }
}