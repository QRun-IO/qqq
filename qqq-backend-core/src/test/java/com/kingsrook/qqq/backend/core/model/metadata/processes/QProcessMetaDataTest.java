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

}