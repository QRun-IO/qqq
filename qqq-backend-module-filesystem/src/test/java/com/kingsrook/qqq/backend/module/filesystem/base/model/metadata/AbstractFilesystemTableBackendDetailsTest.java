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

package com.kingsrook.qqq.backend.module.filesystem.base.model.metadata;


import java.util.function.Consumer;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QInstanceValidationException;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.module.filesystem.BaseTest;
import com.kingsrook.qqq.backend.module.filesystem.TestUtils;
import com.kingsrook.qqq.backend.module.filesystem.local.model.metadata.FilesystemTableBackendDetails;
import com.kingsrook.qqq.backend.module.filesystem.s3.model.metadata.S3TableBackendDetails;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;


/*******************************************************************************
 ** Unit test for AbstractFilesystemTableBackendDetails 
 *******************************************************************************/
class AbstractFilesystemTableBackendDetailsTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testValidInstancePasses() throws QInstanceValidationException
   {
      new QInstanceValidator().validate(QContext.getQInstance());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testMissingCardinality() throws QException
   {
      assertValidationFailureReasons((QInstance qInstance) ->
      {
         qInstance.getTable(TestUtils.TABLE_NAME_PERSON_S3).withBackendDetails(new FilesystemTableBackendDetails());
      }, false, "missing cardinality");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testCardinalityOneIssues() throws QException
   {
      assertValidationFailureReasons((QInstance qInstance) ->
      {
         qInstance.getTable(TestUtils.TABLE_NAME_BLOB_LOCAL_FS).withBackendDetails(new FilesystemTableBackendDetails()
            .withCardinality(Cardinality.ONE)
         );
      }, false, "missing contentsFieldName", "missing fileNameFieldName");

      assertValidationFailureReasons((QInstance qInstance) ->
      {
         qInstance.getTable(TestUtils.TABLE_NAME_BLOB_LOCAL_FS).withBackendDetails(new FilesystemTableBackendDetails()
            .withCardinality(Cardinality.ONE)
            .withContentsFieldName("foo")
            .withFileNameFieldName("bar")
         );
      }, false, "contentsFieldName [foo] is not a field", "fileNameFieldName [bar] is not a field");

      assertValidationFailureReasons((QInstance qInstance) ->
      {
         qInstance.getTable(TestUtils.TABLE_NAME_BLOB_LOCAL_FS).withBackendDetails(new FilesystemTableBackendDetails()
            .withCardinality(Cardinality.ONE)
            .withContentsFieldName("contents")
            .withFileNameFieldName("fileName")
            .withRecordFormat(RecordFormat.CSV)
         );
      }, false, "has a recordFormat");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testCardinalityManyIssues() throws QException
   {
      assertValidationFailureReasons((QInstance qInstance) ->
      {
         qInstance.getTable(TestUtils.TABLE_NAME_PERSON_LOCAL_FS_CSV).withBackendDetails(new FilesystemTableBackendDetails()
            .withCardinality(Cardinality.MANY)
         );
      }, false, "missing recordFormat");

      assertValidationFailureReasons((QInstance qInstance) ->
      {
         qInstance.getTable(TestUtils.TABLE_NAME_PERSON_LOCAL_FS_CSV).withBackendDetails(new FilesystemTableBackendDetails()
            .withCardinality(Cardinality.MANY)
            .withRecordFormat(RecordFormat.CSV)
            .withContentsFieldName("foo")
            .withFileNameFieldName("bar")
         );
      }, false, "has a contentsFieldName", "has a fileNameFieldName");
   }



   /*******************************************************************************
    ** Implementation for the overloads of this name.
    *******************************************************************************/
   private void assertValidationFailureReasons(Consumer<QInstance> setup, boolean allowExtraReasons, String... reasons) throws QException
   {
      try
      {
         QInstance qInstance = TestUtils.defineInstance();
         setup.accept(qInstance);
         new QInstanceValidator().validate(qInstance);
         fail("Should have thrown validationException");
      }
      catch(QInstanceValidationException e)
      {
         if(!allowExtraReasons)
         {
            int noOfReasons = e.getReasons() == null ? 0 : e.getReasons().size();
            assertEquals(reasons.length, noOfReasons, "Expected number of validation failure reasons.\nExpected reasons: " + String.join(",", reasons)
               + "\nActual reasons: " + (noOfReasons > 0 ? String.join("\n", e.getReasons()) : "--"));
         }

         for(String reason : reasons)
         {
            assertReason(reason, e);
         }
      }
   }



   /*******************************************************************************
    ** Assert that an instance is valid!
    *******************************************************************************/
   private void assertValidationSuccess(Consumer<QInstance> setup) throws QException
   {
      try
      {
         QInstance qInstance = TestUtils.defineInstance();
         setup.accept(qInstance);
         new QInstanceValidator().validate(qInstance);
      }
      catch(QInstanceValidationException e)
      {
         fail("Expected no validation errors, but received: " + e.getMessage());
      }
   }



   /*******************************************************************************
    ** utility method for asserting that a specific reason string is found within
    ** the list of reasons in the QInstanceValidationException.
    **
    *******************************************************************************/
   private void assertReason(String reason, QInstanceValidationException e)
   {
      assertNotNull(e.getReasons(), "Expected there to be a reason for the failure (but there was not)");
      assertThat(e.getReasons())
         .withFailMessage("Expected any of:\n%s\nTo match: [%s]", e.getReasons(), reason)
         .anyMatch(s -> s.contains(reason));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testClone()
   {
      QTableMetaData table = new QTableMetaData();
      table.withBackendDetails(new S3TableBackendDetails()
         .withContentTypeStrategy(S3TableBackendDetails.ContentTypeStrategy.HARDCODED)
         .withHardcodedContentType("a")
         .withBasePath("b")
         .withGlob("c")
         .withRecordFormat(RecordFormat.CSV)
         .withCardinality(Cardinality.ONE)
         .withContentsFieldName("d"));

      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // make a clone - change things - make sure some stay the same, and some change (but only in the clone (showing it is deep)) //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      QTableMetaData clone = table.clone();
      S3TableBackendDetails cloneBackendDetails = (S3TableBackendDetails) clone.getBackendDetails();
      cloneBackendDetails.setContentTypeStrategy(S3TableBackendDetails.ContentTypeStrategy.FROM_FIELD);
      cloneBackendDetails.setContentTypeFieldName("z");
      cloneBackendDetails.setBasePath("y");
      cloneBackendDetails.setCardinality(Cardinality.MANY);
      // leave glob and record format as they were

      S3TableBackendDetails originalBackendDetails = (S3TableBackendDetails) table.getBackendDetails();
      assertEquals("a", originalBackendDetails.getHardcodedContentType());
      assertNull(originalBackendDetails.getContentTypeFieldName());
      assertEquals("b", originalBackendDetails.getBasePath());
      assertEquals("c", originalBackendDetails.getGlob());
      assertEquals(RecordFormat.CSV, originalBackendDetails.getRecordFormat());
      assertEquals(Cardinality.ONE, originalBackendDetails.getCardinality());

      assertEquals("a", originalBackendDetails.getHardcodedContentType());
      assertEquals("z", cloneBackendDetails.getContentTypeFieldName());
      assertEquals("y", cloneBackendDetails.getBasePath());
      assertEquals("c", cloneBackendDetails.getGlob());
      assertEquals(RecordFormat.CSV, cloneBackendDetails.getRecordFormat());
      assertEquals(Cardinality.MANY, cloneBackendDetails.getCardinality());
   }

}