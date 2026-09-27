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

package com.kingsrook.qqq.backend.core.modules.backend.implementations.mock;


import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.UUID;
import com.kingsrook.qqq.backend.core.actions.interfaces.QueryInterface;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.ObjectUtils;


/*******************************************************************************
 ** Mocked up version of query action.
 **
 *******************************************************************************/
public class MockQueryAction implements QueryInterface
{
   private static final QLogger LOG = QLogger.getLogger(MockQueryAction.class);



   /*******************************************************************************
    **
    *******************************************************************************/
   public QueryOutput execute(QueryInput queryInput) throws QException
   {
      try
      {
         QTableMetaData table = queryInput.getTable();

         QueryOutput queryOutput = new QueryOutput(queryInput);

         @SuppressWarnings("UnnecessaryUnboxing") // force an un-boxing, to force an NPE if it's null, to get to the "else 1"
         int rows = ObjectUtils.tryElse(() -> queryInput.getFilter().getLimit().intValue(), 1);
         for(int i = 0; i < rows; i++)
         {
            QRecord record = new QRecord();
            record.setTableName(table.getName());

            for(String field : table.getFields().keySet())
            {
               Serializable value = field.equals("id") ? (i + 1) : getMockValue(table, field);
               record.setValue(field, value);
            }

            queryOutput.addRecord(record);

            if(queryInput.getAsyncJobCallback().wasCancelRequested())
            {
               LOG.info("Breaking query job, as requested.");
               break;
            }
         }

         return (queryOutput);
      }
      catch(Exception e)
      {
         throw new QException("Error executing query", e);
      }
   }



   /*******************************************************************************
    ** Get a mock value to use, based on its type.
    **
    *******************************************************************************/
   public static Serializable getMockValue(QTableMetaData table, String field)
   {
      return switch(table.getField(field).getType())
      {
         case STRING -> UUID.randomUUID().toString();
         case INTEGER -> 42;
         case LONG -> 42L;
         case DECIMAL -> new BigDecimal("3.14159");
         case DATE -> LocalDate.of(1970, Month.JANUARY, 1);
         case DATE_TIME -> LocalDateTime.of(1970, Month.JANUARY, 1, 0, 0);
         case TEXT -> "Four score and seven years ago...";
         case HTML -> "<b>BOLD</b>";
         case PASSWORD -> "abc***234";
         default -> throw new IllegalStateException("Unexpected value: " + table.getField(field).getType());
      };
   }

}
