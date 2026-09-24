/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.kingsrook.qqq.backend.core.model.metadata.processes;


import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 ** Unit test for QProcessMetaData 
 *******************************************************************************/
class QProcessMetaDataTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetInputField()
   {
      {
         ///////////////////////////
         // empty case, no fields //
         ///////////////////////////
         QProcessMetaData process = new QProcessMetaData().withStep(new QBackendStepMetaData().withName("test"));

         assertThat(process.getInputField("yourField"))
            .isEmpty();
      }

      {
         //////////////////////////
         // simple case, 1 field //
         //////////////////////////
         QProcessMetaData process = new QProcessMetaData().withStep(new QBackendStepMetaData()
            .withName("test")
            .withInputData(new QFunctionInputMetaData().withField(new QFieldMetaData("myField", QFieldType.STRING))));

         assertThat(process.getInputField("myField"))
            .isPresent().get()
            .hasFieldOrPropertyWithValue("type", QFieldType.STRING);

         assertThat(process.getInputField("yourField"))
            .isEmpty();
      }

      {
         //////////////////////////
         // same name in 2 steps //
         //////////////////////////
         QProcessMetaData process = new QProcessMetaData()
            .withStep(new QBackendStepMetaData()
               .withName("first")
               .withInputData(new QFunctionInputMetaData()
                  .withField(new QFieldMetaData("myField", QFieldType.STRING))
                  .withField(new QFieldMetaData("yourField", QFieldType.STRING))))
            .withStep(new QFrontendStepMetaData()
               .withName("second")
               .withFormField(new QFieldMetaData("theirField", QFieldType.BOOLEAN)))
            .withStep(new QBackendStepMetaData()
               .withName("third")
               .withInputData(new QFunctionInputMetaData()
                  .withField(new QFieldMetaData("myField", QFieldType.INTEGER))
                  .withField(new QFieldMetaData("theirField", QFieldType.INTEGER))));

         assertThat(process.getInputField("myField"))
            .isPresent().get()
            .hasFieldOrPropertyWithValue("type", QFieldType.STRING);

         assertThat(process.getInputField("theirField"))
            .isPresent().get()
            .hasFieldOrPropertyWithValue("type", QFieldType.BOOLEAN);
      }

      {
         ///////////////////
         // optional step //
         ///////////////////
         QProcessMetaData process = new QProcessMetaData()
            .withStep(new QBackendStepMetaData()
               .withName("first")
               .withInputData(new QFunctionInputMetaData()
                  .withField(new QFieldMetaData("myField", QFieldType.STRING))
                  .withField(new QFieldMetaData("yourField", QFieldType.STRING))));

         process.withOptionalStep(new QFrontendStepMetaData()
            .withName("optional")
            .withFormField(new QFieldMetaData("myField", QFieldType.INTEGER))
            .withFormField(new QFieldMetaData("theirField", QFieldType.INTEGER)));

         //////////////////////////////////////////////////////////////
         // this field should come from the step in the list (first) //
         //////////////////////////////////////////////////////////////
         assertThat(process.getInputField("myField"))
            .isPresent().get()
            .hasFieldOrPropertyWithValue("type", QFieldType.STRING);

         /////////////////////////////////////////////////////
         // this field should be found in the optional step //
         /////////////////////////////////////////////////////
         assertThat(process.getInputField("theirField"))
            .isPresent().get()
            .hasFieldOrPropertyWithValue("type", QFieldType.INTEGER);
      }
   }



   /*******************************************************************************
    ** toString should include the process name.
    *******************************************************************************/
   @Test
   void testToString_includesName()
   {
      QProcessMetaData process = new QProcessMetaData().withName("myProcess");
      assertThat(process.toString()).contains("myProcess");
   }



   /*******************************************************************************
    ** withStep with a nameless step should throw IllegalArgumentException.
    *******************************************************************************/
   @Test
   void testWithStep_namelessStep_throwsIllegalArgumentException()
   {
      QProcessMetaData process = new QProcessMetaData();

      assertThatThrownBy(() -> process.withStep(new QBackendStepMetaData()))
         .isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("name");
   }



   /*******************************************************************************
    ** withOptionalStep with a nameless step should throw IllegalArgumentException.
    *******************************************************************************/
   @Test
   void testWithOptionalStep_namelessStep_throwsIllegalArgumentException()
   {
      QProcessMetaData process = new QProcessMetaData();

      assertThatThrownBy(() -> process.withOptionalStep(new QFrontendStepMetaData()))
         .isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("name");
   }



   /*******************************************************************************
    ** getStep should find sub-steps nested inside a QStateMachineStep.
    *******************************************************************************/
   @Test
   void testGetStep_stateMachineSubStep_foundBySubStepName()
   {
      QFrontendStepMetaData frontendSubStep = new QFrontendStepMetaData().withName("wizard.frontend");
      QBackendStepMetaData  backendSubStep  = new QBackendStepMetaData().withName("wizard.backend");

      QStateMachineStep stateMachineStep = QStateMachineStep.frontendThenBackend("wizard", frontendSubStep, backendSubStep);

      QProcessMetaData process = new QProcessMetaData().withStep(stateMachineStep);

      // Top-level name resolves directly from map
      assertNotNull(process.getStep("wizard"));
      assertThat(process.getStep("wizard")).isInstanceOf(QStateMachineStep.class);

      // Sub-step names are found via traversal
      assertNotNull(process.getStep("wizard.frontend"));
      assertNotNull(process.getStep("wizard.backend"));
   }



   /*******************************************************************************
    ** getStep should return null for an unknown step name.
    *******************************************************************************/
   @Test
   void testGetStep_unknownName_returnsNull()
   {
      QProcessMetaData process = new QProcessMetaData()
         .withStep(new QBackendStepMetaData().withName("existing"));

      assertNull(process.getStep("nonExistent"));
   }



   /*******************************************************************************
    ** getBackendStep should cast and return the step.
    *******************************************************************************/
   @Test
   void testGetBackendStep_namedStep_returnsTypedResult()
   {
      QProcessMetaData process = new QProcessMetaData()
         .withStep(new QBackendStepMetaData().withName("doWork"));

      QBackendStepMetaData step = process.getBackendStep("doWork");
      assertNotNull(step);
      assertEquals("doWork", step.getName());
   }



   /*******************************************************************************
    ** getOutputFields collects unique output fields across all steps.
    *******************************************************************************/
   @Test
   void testGetOutputFields_multipleSteps_uniqueFieldsReturned()
   {
      QProcessMetaData process = new QProcessMetaData()
         .withStep(new QBackendStepMetaData()
            .withName("step1")
            .withOutputMetaData(new QFunctionOutputMetaData()
               .withField(new QFieldMetaData("result", QFieldType.STRING))
               .withField(new QFieldMetaData("count", QFieldType.INTEGER))))
         .withStep(new QBackendStepMetaData()
            .withName("step2")
            .withOutputMetaData(new QFunctionOutputMetaData()
               .withField(new QFieldMetaData("result", QFieldType.STRING))   // duplicate — must be deduplicated
               .withField(new QFieldMetaData("status", QFieldType.STRING))));

      assertThat(process.getOutputFields())
         .hasSize(3)
         .extracting("name")
         .containsExactlyInAnyOrder("result", "count", "status");
   }



   /*******************************************************************************
    ** getOutputFields on a process with no steps should return an empty list.
    *******************************************************************************/
   @Test
   void testGetOutputFields_noSteps_emptyList()
   {
      QProcessMetaData process = new QProcessMetaData();

      assertThat(process.getOutputFields()).isEmpty();
   }



   /*******************************************************************************
    ** setStepList(null) should clear both the list and the step map.
    *******************************************************************************/
   @Test
   void testSetStepList_null_clearsListAndMap()
   {
      QProcessMetaData process = new QProcessMetaData()
         .withStep(new QBackendStepMetaData().withName("first"));

      process.setStepList(null);

      assertNull(process.getStepList());
      assertNull(process.getAllSteps());
   }



   /*******************************************************************************
    ** withStep(int, step) should insert the step at the specified index.
    *******************************************************************************/
   @Test
   void testWithStep_indexed_insertsAtCorrectPosition()
   {
      QProcessMetaData process = new QProcessMetaData()
         .withStep(new QBackendStepMetaData().withName("first"))
         .withStep(new QBackendStepMetaData().withName("third"));

      process.withStep(1, new QBackendStepMetaData().withName("second"));

      assertThat(process.getStepList())
         .extracting("name")
         .containsExactly("first", "second", "third");
   }

}