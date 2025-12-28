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

package com.kingsrook.qqq.api.actions.io;


import java.io.Serializable;
import java.util.List;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/***************************************************************************
 ** implementation of ApiOutputRecordWrapperInterface that wraps a QRecord
 ***************************************************************************/
public class ApiOutputQRecordWrapper implements ApiOutputRecordWrapperInterface<QRecord, ApiOutputQRecordWrapper>
{
   private QRecord record;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public ApiOutputQRecordWrapper(QRecord record)
   {
      this.record = record;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void putValue(String key, Serializable value)
   {
      record.setValue(key, value);
      record.setDisplayValue(key, ValueUtils.getValueAsString(value)); // todo is this useful?
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void putAssociation(String key, List<QRecord> values)
   {
      record.withAssociatedRecords(key, values);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public ApiOutputQRecordWrapper newSibling(String tableName)
   {
      return (new ApiOutputQRecordWrapper(new QRecord().withTableName(tableName)));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public QRecord getContents()
   {
      return this.record;
   }

}
