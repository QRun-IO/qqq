/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.actions.tables.query;


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import org.apache.logging.log4j.Level;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Kinda the standard way that a QueryOutput would store its records - in a
 ** simple list.
 *******************************************************************************/
class QueryOutputList implements QueryOutputStorageInterface
{
   private static final QLogger LOG = QLogger.getLogger(QueryOutputList.class);

   private final String        tableName;
   private       List<QRecord> records = new ArrayList<>();

   private static int LOG_SIZE_INFO_OVER  = 50_000;
   private static int LOG_SIZE_WARN_OVER  = 100_000;
   private static int LOG_SIZE_ERROR_OVER = 250_000;



   /*******************************************************************************
    **
    *******************************************************************************/
   public QueryOutputList(QueryInput queryInput)
   {
      tableName = queryInput.getTableName();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void logSize(int sizeBefore, int sizeAfter)
   {
      Level level = null;
      if(sizeBefore < LOG_SIZE_ERROR_OVER && sizeAfter >= LOG_SIZE_ERROR_OVER)
      {
         level = Level.ERROR;
      }
      else if(sizeBefore < LOG_SIZE_WARN_OVER && sizeAfter >= LOG_SIZE_WARN_OVER)
      {
         level = Level.WARN;
      }
      else if(sizeBefore < LOG_SIZE_INFO_OVER && sizeAfter >= LOG_SIZE_INFO_OVER)
      {
         level = Level.INFO;
      }

      if(level != null)
      {
         LOG.log(level, "Large number of records in QueryOutputList", new Throwable(), logPair("noRecords", sizeAfter), logPair("tableName", tableName));
      }
   }



   /*******************************************************************************
    ** add a record to this output
    *******************************************************************************/
   @Override
   public void addRecord(QRecord record)
   {
      int sizeBefore = this.records.size();
      records.add(record);
      logSize(sizeBefore, this.records.size());
   }



   /*******************************************************************************
    ** add a list of records to this output
    *******************************************************************************/
   @Override
   public void addRecords(List<QRecord> records)
   {
      int sizeBefore = this.records.size();
      this.records.addAll(records);
      logSize(sizeBefore, this.records.size());
   }



   /*******************************************************************************
    ** Get all stored records
    *******************************************************************************/
   @Override
   public List<QRecord> getRecords()
   {
      return (records);
   }



   /*******************************************************************************
    ** Setter for LOG_SIZE_INFO_OVER
    **
    *******************************************************************************/
   public static void setLogSizeInfoOver(int logSizeInfoOver)
   {
      QueryOutputList.LOG_SIZE_INFO_OVER = logSizeInfoOver;
   }



   /*******************************************************************************
    ** Setter for LOG_SIZE_WARN_OVER
    **
    *******************************************************************************/
   public static void setLogSizeWarnOver(int logSizeWarnOver)
   {
      QueryOutputList.LOG_SIZE_WARN_OVER = logSizeWarnOver;
   }



   /*******************************************************************************
    ** Setter for LOG_SIZE_ERROR_OVER
    **
    *******************************************************************************/
   public static void setLogSizeErrorOver(int logSizeErrorOver)
   {
      QueryOutputList.LOG_SIZE_ERROR_OVER = logSizeErrorOver;
   }



   /***************************************************************************
    *
    ***************************************************************************/
   public static void resetDefaultLogSizeOverValues()
   {
      QueryOutputList.LOG_SIZE_INFO_OVER = 50_000;
      QueryOutputList.LOG_SIZE_WARN_OVER = 100_000;
      QueryOutputList.LOG_SIZE_ERROR_OVER = 250_000;
   }
}
