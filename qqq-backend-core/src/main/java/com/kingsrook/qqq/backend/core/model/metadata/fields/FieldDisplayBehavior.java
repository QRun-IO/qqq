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

package com.kingsrook.qqq.backend.core.model.metadata.fields;


import java.util.List;
import com.kingsrook.qqq.backend.core.actions.values.ValueBehaviorApplier;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;


/*******************************************************************************
 ** Interface to mark a field behavior as one to be used during generating
 ** display values.
 *******************************************************************************/
public interface FieldDisplayBehavior<T extends FieldDisplayBehavior<T>> extends FieldBehavior<T>
{
   NoopFieldDisplayBehavior NOOP = new NoopFieldDisplayBehavior();

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   @SuppressWarnings("unchecked")
   default T getDefault()
   {
      return (T) NOOP;
   }


   /***************************************************************************
    ** a default implementation for this behavior type, which does nothing.
    ***************************************************************************/
   class NoopFieldDisplayBehavior implements FieldDisplayBehavior<NoopFieldDisplayBehavior>
   {

      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public void apply(ValueBehaviorApplier.Action action, List<QRecord> recordList, QInstance instance, QTableMetaData table, QFieldMetaData field)
      {

      }
   }
}
