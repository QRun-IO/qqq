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

package com.kingsrook.qqq.backend.core.instances;


import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.actions.scripts.ScriptTableCustomizer;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QInstanceValidationException;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.scripts.Script;
import com.kingsrook.qqq.backend.core.model.scripts.ScriptsMetaDataProvider;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** A script table with the execution-marker field must have the marker guard
 ** registered, or the instance fails initialization with an actionable message.
 *******************************************************************************/
class ScriptTableCustomizerValidationTest extends BaseTest
{
   /*******************************************************************************
    ** An instance with the standard provider tables.
    *******************************************************************************/
   private QInstance instanceWithScripts() throws QException
   {
      QInstance qInstance = TestUtils.defineInstance();
      new ScriptsMetaDataProvider().defineAll(qInstance, TestUtils.MEMORY_BACKEND_NAME, null);
      return (qInstance);
   }



   /*******************************************************************************
    ** Validate; the only thing asserted is that the marker-guard rule has no complaint
    ** (the provider tables need other instance content that is not under test here).
    *******************************************************************************/
   private void assertNoMarkerGuardReason(QInstance qInstance)
   {
      try
      {
         new QInstanceValidator().validate(qInstance);
      }
      catch(QInstanceValidationException e)
      {
         assertTrue(e.getReasons().stream().noneMatch(reason -> reason.contains("ScriptTableCustomizer")), String.valueOf(e.getReasons()));
      }
   }



   /*******************************************************************************
    ** The standard provider composition is valid.
    *******************************************************************************/
   @Test
   void standardProviderPasses() throws QException
   {
      QInstance qInstance = instanceWithScripts();
      assertNoMarkerGuardReason(qInstance);
   }



   /*******************************************************************************
    ** The marker field without the customizer is rejected, once per missing role.
    *******************************************************************************/
   @Test
   void missingCustomizerFails() throws QException
   {
      QInstanceValidatorTest.assertValidationFailureReasonsAllowingExtraReasons(qInstance ->
      {
         try
         {
            new ScriptsMetaDataProvider().defineAll(qInstance, TestUtils.MEMORY_BACKEND_NAME, null);
         }
         catch(QException e)
         {
            throw (new RuntimeException(e));
         }
         qInstance.getTable(Script.TABLE_NAME).getCustomizers().clear();
      },
         "Table script has the field executionMode but no ScriptTableCustomizer (or subclass) registered for preInsertRecord",
         "Table script has the field executionMode but no ScriptTableCustomizer (or subclass) registered for preUpdateRecord",
         "Table script has the field executionMode but no ScriptTableCustomizer (or subclass) registered for preDeleteRecord");
   }



   /*******************************************************************************
    ** A subclass, or instance-level registration, satisfies the rule.
    *******************************************************************************/
   @Test
   void subclassAndInstanceLevelRegistrationPass() throws QException
   {
      QInstance qInstance = instanceWithScripts();
      qInstance.getTable(Script.TABLE_NAME).getCustomizers().remove(TableCustomizers.PRE_INSERT_RECORD.getRole());
      qInstance.getTable(Script.TABLE_NAME).withCustomizer(TableCustomizers.PRE_INSERT_RECORD, new QCodeReference(CustomScriptCustomizer.class));
      qInstance.getTable(Script.TABLE_NAME).getCustomizers().remove(TableCustomizers.PRE_UPDATE_RECORD.getRole());
      qInstance.withTableCustomizer(TableCustomizers.PRE_UPDATE_RECORD, new QCodeReference(ScriptTableCustomizer.class));
      assertNoMarkerGuardReason(qInstance);
   }



   /*******************************************************************************
    ** Unrelated tables, including one that merely has a same-named field, are not affected.
    *******************************************************************************/
   @Test
   void unrelatedTablesAreUnaffected() throws QException
   {
      QInstance qInstance = TestUtils.defineInstance();
      qInstance.addTable(new QTableMetaData().withName("notScripts").withBackendName(TestUtils.MEMORY_BACKEND_NAME).withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER)).withField(new QFieldMetaData("executionMode", QFieldType.STRING)));
      assertDoesNotThrow(() -> new QInstanceValidator().validate(qInstance));
   }



   /*******************************************************************************
    ** A customizer that extends the standard one.
    *******************************************************************************/
   public static class CustomScriptCustomizer extends ScriptTableCustomizer
   {
   }
}
