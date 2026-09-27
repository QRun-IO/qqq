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

package com.kingsrook.qqq.backend.core.processes.implementations.columnstats;


import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Capability;
import com.kingsrook.qqq.backend.core.model.metadata.tables.ExposedJoin;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.ExceptionUtils;
import com.kingsrook.qqq.backend.core.utils.Pair;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Utility for verifying that the ColumnStats process works for all fields,
 ** on all tables, and all exposed joins.
 **
 ** Meant for use within a unit test, or maybe as part of an instance's boot-up/
 ** validation.
 *******************************************************************************/
public class ColumnStatsFullInstanceVerifier
{
   private static final QLogger LOG = QLogger.getLogger(ColumnStatsFullInstanceVerifier.class);



   /*******************************************************************************
    **
    *******************************************************************************/
   public void verify(Collection<QTableMetaData> tables) throws QException
   {
      Map<Pair<String, String>, Exception> caughtExceptions = new LinkedHashMap<>();
      for(QTableMetaData table : tables)
      {
         if(table.isCapabilityEnabled(QContext.getQInstance().getBackendForTable(table.getName()), Capability.QUERY_STATS))
         {
            LOG.info("Verifying ColumnStats on table", logPair("tableName", table.getName()));
            for(QFieldMetaData field : table.getFields().values())
            {
               runColumnStats(table.getName(), field.getName(), caughtExceptions);
            }

            for(ExposedJoin exposedJoin : CollectionUtils.nonNullList(table.getExposedJoins()))
            {
               QTableMetaData joinTable = QContext.getQInstance().getTable(exposedJoin.getJoinTable());
               for(QFieldMetaData field : joinTable.getFields().values())
               {
                  runColumnStats(table.getName(), joinTable.getName() + "." + field.getName(), caughtExceptions);
               }
            }
         }
      }

      // log out an exceptions caught
      if(!caughtExceptions.isEmpty())
      {
         for(Map.Entry<Pair<String, String>, Exception> entry : caughtExceptions.entrySet())
         {
            LOG.info("Caught an exception verifying column stats", entry.getValue(), logPair("tableName", entry.getKey().getA()), logPair("fieldName", entry.getKey().getB()));
         }
         throw (new QException("Column Stats Verification failed with " + caughtExceptions.size() + " exception" + StringUtils.plural(caughtExceptions.size())));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void runColumnStats(String tableName, String fieldName, Map<Pair<String, String>, Exception> caughtExceptions) throws QException
   {
      try
      {
         RunBackendStepInput input = new RunBackendStepInput();
         input.addValue("tableName", tableName);
         input.addValue("fieldName", fieldName);
         RunBackendStepOutput output = new RunBackendStepOutput();
         new ColumnStatsStep().run(input, output);
      }
      catch(QException e)
      {
         Throwable rootException = ExceptionUtils.getRootException(e);
         if(rootException instanceof QException && rootException.getMessage().contains("not supported for this field's data type"))
         {
            ////////////////////////////////////////////////
            // ignore this exception, it's kinda expected //
            ////////////////////////////////////////////////
            LOG.debug("Caught an expected-exception in column stats", e, logPair("tableName", tableName), logPair("fieldName", fieldName));
         }
         else
         {
            caughtExceptions.put(Pair.of(tableName, fieldName), e);
         }
      }
   }
}
