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
