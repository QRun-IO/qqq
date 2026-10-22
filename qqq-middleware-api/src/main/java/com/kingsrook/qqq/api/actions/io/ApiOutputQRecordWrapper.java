/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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
