/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.metadata.sharing;


import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static com.kingsrook.qqq.backend.core.instances.QInstanceValidatorTest.assertValidationFailureReasons;
import static com.kingsrook.qqq.backend.core.instances.QInstanceValidatorTest.assertValidationFailureReasonsAllowingExtraReasons;
import static com.kingsrook.qqq.backend.core.instances.QInstanceValidatorTest.assertValidationSuccess;


/*******************************************************************************
 ** Unit test for ShareableTableMetaData 
 *******************************************************************************/
class ShareableTableMetaDataTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testValidation()
   {
      assertValidationFailureReasonsAllowingExtraReasons(qInstance -> qInstance.addTable(newTable().withShareableTableMetaData(new ShareableTableMetaData())),
         "missing sharedRecordTableName");

      assertValidationFailureReasonsAllowingExtraReasons(qInstance -> qInstance.addTable(newTable().withShareableTableMetaData(new ShareableTableMetaData()
         .withSharedRecordTableName("notATable")
      )), "unrecognized sharedRecordTableName");

      assertValidationFailureReasonsAllowingExtraReasons(qInstance -> qInstance.addTable(newTable().withShareableTableMetaData(new ShareableTableMetaData()
         .withAudienceTypesPossibleValueSourceName("notAPVS")
      )), "unrecognized audienceTypesPossibleValueSourceName");

      assertValidationFailureReasons(qInstance -> qInstance.addTable(newTable().withShareableTableMetaData(new ShareableTableMetaData()
            .withSharedRecordTableName(TestUtils.TABLE_NAME_PERSON_MEMORY)
         )), "missing assetIdFieldName",
         "missing scopeFieldName",
         "missing audienceTypes");

      assertValidationFailureReasonsAllowingExtraReasons(qInstance -> qInstance.addTable(newTable().withShareableTableMetaData(new ShareableTableMetaData()
         .withSharedRecordTableName(TestUtils.TABLE_NAME_PERSON_MEMORY)
         .withAssetIdFieldName("notAField")
      )), "unrecognized assertIdFieldName");

      assertValidationFailureReasonsAllowingExtraReasons(qInstance -> qInstance.addTable(newTable().withShareableTableMetaData(new ShareableTableMetaData()
         .withSharedRecordTableName(TestUtils.TABLE_NAME_PERSON_MEMORY)
         .withScopeFieldName("notAField")
      )), "unrecognized scopeFieldName");

      assertValidationFailureReasonsAllowingExtraReasons(qInstance -> qInstance.addTable(newTable().withShareableTableMetaData(new ShareableTableMetaData()
         .withSharedRecordTableName(TestUtils.TABLE_NAME_PERSON_MEMORY)
         .withAudienceType(new ShareableAudienceType().withName("myType"))
      )), "missing fieldName for shareableAudienceType");

      assertValidationFailureReasonsAllowingExtraReasons(qInstance -> qInstance.addTable(newTable().withShareableTableMetaData(new ShareableTableMetaData()
         .withSharedRecordTableName(TestUtils.TABLE_NAME_PERSON_MEMORY)
         .withAudienceType(new ShareableAudienceType().withName("myType").withFieldName("notAField"))
      )), "unrecognized fieldName");

      /* todo - corresponding todo in main class
      assertValidationFailureReasonsAllowingExtraReasons(qInstance -> qInstance.addTable(newTable().withShareableTableMetaData(new ShareableTableMetaData()
         .withSharedRecordTableName(TestUtils.TABLE_NAME_PERSON_MEMORY)
         .withAudienceType(new ShareableAudienceType().withName("myType").withFieldName("firstName").withSourceTableName("notATable"))
      )), "unrecognized sourceTableName");

      assertValidationFailureReasonsAllowingExtraReasons(qInstance -> qInstance.addTable(newTable().withShareableTableMetaData(new ShareableTableMetaData()
         .withSharedRecordTableName(TestUtils.TABLE_NAME_PERSON_MEMORY)
         .withAudienceType(new ShareableAudienceType().withName("myType").withFieldName("firstName").withSourceTableName(TestUtils.TABLE_NAME_SHAPE).withSourceTableKeyFieldName("notAField"))
      )), "unrecognized sourceTableKeyFieldName");
      */

      assertValidationFailureReasonsAllowingExtraReasons(qInstance -> qInstance.addTable(newTable().withShareableTableMetaData(new ShareableTableMetaData()
         .withThisTableOwnerIdFieldName("notAField")
      )), "unrecognized thisTableOwnerIdFieldName");

      assertValidationSuccess(qInstance -> qInstance.addTable(newTable()
         .withField(new QFieldMetaData("userId", QFieldType.INTEGER))
         .withShareableTableMetaData(new ShareableTableMetaData()
            .withSharedRecordTableName(TestUtils.TABLE_NAME_PERSON_MEMORY)
            .withAssetIdFieldName("firstName")
            .withScopeFieldName("firstName")
            .withThisTableOwnerIdFieldName("userId")
            .withAudienceTypesPossibleValueSourceName(TestUtils.POSSIBLE_VALUE_SOURCE_STATE)
            .withAudienceType(new ShareableAudienceType().withName("myType").withFieldName("lastName").withSourceTableName(TestUtils.TABLE_NAME_SHAPE).withSourceTableKeyFieldName("id"))
         )));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   protected QTableMetaData newTable()
   {
      QTableMetaData tableMetaData = new QTableMetaData()
         .withName("A")
         .withBackendName(TestUtils.DEFAULT_BACKEND_NAME)
         .withPrimaryKeyField("id");

      tableMetaData.addField(new QFieldMetaData("id", QFieldType.INTEGER));

      return (tableMetaData);
   }

}