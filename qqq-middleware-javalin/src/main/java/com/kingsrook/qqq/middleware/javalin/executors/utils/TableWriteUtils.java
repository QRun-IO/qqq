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

package com.kingsrook.qqq.middleware.javalin.executors.utils;


import java.io.Serializable;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage;
import com.kingsrook.qqq.backend.core.model.statusmessages.QStatusMessage;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** Shared behavior of the v1 table insert and update executors, matching the
 ** legacy data routes.
 *******************************************************************************/
public class TableWriteUtils
{

   /*******************************************************************************
    ** The record to write: the one the spec read from the request, or one built
    ** from plain field values.
    *******************************************************************************/
   public static QRecord recordToWrite(String tableName, QRecord record, Map<String, Serializable> recordValues)
   {
      QRecord result = record != null ? record : new QRecord();
      result.setTableName(tableName);
      if(recordValues != null)
      {
         recordValues.forEach(result::setValue);
      }
      return (result);
   }



   /*******************************************************************************
    ** Refuse a write whose record came back with errors: a bad request when every
    ** error is bad input (for example a missing required value), otherwise a
    ** user-facing error - with the legacy message ("Error inserting Person: ...").
    *******************************************************************************/
   public static void throwIfRecordErrors(String verb, String tableName, QRecord outputRecord) throws QUserFacingException
   {
      List<? extends QStatusMessage> errors = outputRecord.getErrors();
      if(CollectionUtils.nullSafeIsEmpty(errors))
      {
         return;
      }

      QTableMetaData table   = QContext.getQInstance().getTable(tableName);
      String         label   = table != null ? table.getLabel() : tableName;
      String         message = "Error " + verb + " " + label + ": " + StringUtils.joinWithCommasAndAnd(errors.stream().map(QStatusMessage::getMessage).toList());
      if(errors.stream().allMatch(error -> error instanceof BadInputStatusMessage))
      {
         throw (new QBadRequestException(message));
      }
      throw (new QUserFacingException(message));
   }

}
