/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.module.rdbms.actions;


import java.util.List;
import com.kingsrook.qqq.backend.core.actions.ai.AiFieldMetaData;
import com.kingsrook.qqq.backend.core.actions.ai.DataClassification;
import com.kingsrook.qqq.backend.core.exceptions.QValueException;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.module.rdbms.BaseTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;


/*******************************************************************************
 ** JDBC conversion retains classification, including field-function result types.
 *******************************************************************************/
class ClassifiedValueConversionTest extends BaseTest
{
   /*******************************************************************************
    ** The shared insert, update and filter scrubber drops sensitive parser causes.
    *******************************************************************************/
   @Test
   void classifiedScrubbing()
   {
      AbstractRDBMSAction action = new RDBMSQueryAction();
      QFieldMetaData field = new QFieldMetaData("reading", QFieldType.INTEGER).withLabel("Reading");
      QTableMetaData table = new QTableMetaData().withName("readings").withField(field);
      for(DataClassification classification : List.of(DataClassification.PHI, DataClassification.PII))
      {
         field.withSupplementalMetaData(new AiFieldMetaData().withClassification(classification));
         for(QFieldType type : List.of(QFieldType.INTEGER, QFieldType.DATE))
         {
            QValueException exception = assertThrows(QValueException.class, () -> action.scrubValue(table, field, type, "CANARY-PHI-invalid"));
            assertEquals("Value [***] could not be converted for Reading", exception.getMessage());
            assertNull(exception.getCause());
         }
      }
      assertEquals(1234, action.scrubValue(table, field, QFieldType.INTEGER, "1,234"));
      assertNull(action.scrubValue(table, field, QFieldType.INTEGER, ""));
   }



   /*******************************************************************************
    ** Ordinary field diagnostics and permissive JDBC scrubbing stay unchanged.
    *******************************************************************************/
   @Test
   void unclassifiedScrubbing()
   {
      AbstractRDBMSAction action = new RDBMSQueryAction();
      QFieldMetaData field = new QFieldMetaData("reading", QFieldType.INTEGER);
      QTableMetaData table = new QTableMetaData().withName("readings").withField(field);
      QValueException exception = assertThrows(QValueException.class, () -> action.scrubValue(table, field, field.getType(), "invalid-number"));
      assertEquals("Value [invalid-number] could not be converted to an Integer.", exception.getMessage());
      assertEquals(NumberFormatException.class, exception.getCause().getClass());
   }
}
