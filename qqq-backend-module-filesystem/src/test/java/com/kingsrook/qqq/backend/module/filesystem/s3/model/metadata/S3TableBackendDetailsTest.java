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

package com.kingsrook.qqq.backend.module.filesystem.s3.model.metadata;


import java.util.List;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.collections.ListBuilder;
import com.kingsrook.qqq.backend.module.filesystem.BaseTest;
import com.kingsrook.qqq.backend.module.filesystem.base.model.metadata.Cardinality;
import org.assertj.core.api.CollectionAssert;
import org.junit.jupiter.api.Test;


/*******************************************************************************
 ** Unit test for S3TableBackendDetails 
 *******************************************************************************/
class S3TableBackendDetailsTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testValidateContentTypeStrategyBasedOnFileNameOrNone()
   {
      /////////////////////////////////////////////
      // same validation rules for both of these //
      /////////////////////////////////////////////
      for(S3TableBackendDetails.ContentTypeStrategy contentTypeStrategy : ListBuilder.of(null, S3TableBackendDetails.ContentTypeStrategy.BASED_ON_FILE_NAME, S3TableBackendDetails.ContentTypeStrategy.NONE))
      {
         S3TableBackendDetails s3TableBackendDetails = getS3TableBackendDetails()
            .withContentTypeStrategy(contentTypeStrategy);
         QTableMetaData table = getQTableMetaData();

         List<String> errors = runValidation(s3TableBackendDetails, table);
         CollectionAssert.assertThatCollection(errors)
            .isEmpty();

         s3TableBackendDetails.setHardcodedContentType("Test");
         s3TableBackendDetails.setContentTypeFieldName("Test");
         errors = runValidation(s3TableBackendDetails, table);
         CollectionAssert.assertThatCollection(errors)
            .hasSize(2)
            .contains("Table testTable backend details - contentTypeFieldName should not be set when contentTypeStrategy is " + contentTypeStrategy)
            .contains("Table testTable backend details - hardcodedContentType should not be set when contentTypeStrategy is " + contentTypeStrategy);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testValidateContentTypeStrategyFromField()
   {
      S3TableBackendDetails s3TableBackendDetails = getS3TableBackendDetails()
         .withContentTypeStrategy(S3TableBackendDetails.ContentTypeStrategy.FROM_FIELD);
      QTableMetaData table = getQTableMetaData();

      List<String> errors = runValidation(s3TableBackendDetails, table);
      CollectionAssert.assertThatCollection(errors)
         .hasSize(1)
         .contains("Table testTable backend details - contentTypeFieldName must be set when contentTypeStrategy is FROM_FIELD");

      s3TableBackendDetails.setContentTypeFieldName("notAField");
      errors = runValidation(s3TableBackendDetails, table);
      CollectionAssert.assertThatCollection(errors)
         .hasSize(1)
         .contains("Table testTable backend details - contentTypeFieldName must be a valid field name in the table");

      table.addField(new QFieldMetaData("contentType", QFieldType.STRING));
      s3TableBackendDetails.setContentTypeFieldName("contentType");
      errors = runValidation(s3TableBackendDetails, table);
      CollectionAssert.assertThatCollection(errors)
         .isEmpty();

      s3TableBackendDetails.setHardcodedContentType("hard");
      errors = runValidation(s3TableBackendDetails, table);
      CollectionAssert.assertThatCollection(errors)
         .hasSize(1)
         .contains("Table testTable backend details - hardcodedContentType should not be set when contentTypeStrategy is FROM_FIELD");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testValidateContentTypeStrategyHardcoded()
   {
      S3TableBackendDetails s3TableBackendDetails = getS3TableBackendDetails()
         .withContentTypeStrategy(S3TableBackendDetails.ContentTypeStrategy.HARDCODED);
      QTableMetaData table = getQTableMetaData();

      List<String> errors = runValidation(s3TableBackendDetails, table);
      CollectionAssert.assertThatCollection(errors)
         .hasSize(1)
         .contains("Table testTable backend details - hardcodedContentType must be set when contentTypeStrategy is HARDCODED");

      s3TableBackendDetails.setHardcodedContentType("Test");
      errors = runValidation(s3TableBackendDetails, table);
      CollectionAssert.assertThatCollection(errors)
         .isEmpty();

      s3TableBackendDetails.setContentTypeFieldName("aField");
      errors = runValidation(s3TableBackendDetails, table);
      CollectionAssert.assertThatCollection(errors)
         .hasSize(1)
         .contains("Table testTable backend details - contentTypeFieldName should not be set when contentTypeStrategy is HARDCODED");
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static QTableMetaData getQTableMetaData()
   {
      QTableMetaData table = new QTableMetaData()
         .withName("testTable")
         .withField(new QFieldMetaData("contents", QFieldType.BLOB))
         .withField(new QFieldMetaData("fileName", QFieldType.STRING));
      return table;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static S3TableBackendDetails getS3TableBackendDetails()
   {
      S3TableBackendDetails s3TableBackendDetails = new S3TableBackendDetails()
         .withContentsFieldName("contents")
         .withFileNameFieldName("fileName")
         .withCardinality(Cardinality.ONE);
      return s3TableBackendDetails;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private List<String> runValidation(S3TableBackendDetails s3TableBackendDetails, QTableMetaData table)
   {
      QInstanceValidator validator = new QInstanceValidator();
      s3TableBackendDetails.validate(QContext.getQInstance(), table, validator);
      return (validator.getErrors());
   }
}