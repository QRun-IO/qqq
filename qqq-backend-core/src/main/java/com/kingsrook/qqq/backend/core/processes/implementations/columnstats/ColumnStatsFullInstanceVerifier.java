/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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
