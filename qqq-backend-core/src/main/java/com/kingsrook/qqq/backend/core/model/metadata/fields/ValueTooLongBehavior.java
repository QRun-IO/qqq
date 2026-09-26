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
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Behaviors for string fields, if their value is too long.
 **
 ** Note:  This was the first implementation of a FieldBehavior, so its test
 ** coverage is provided in ValueBehaviorApplierTest.
 *******************************************************************************/
public enum ValueTooLongBehavior implements FieldBehavior<ValueTooLongBehavior>
{
   TRUNCATE,
   TRUNCATE_ELLIPSIS,
   ERROR,
   PASS_THROUGH;

   private static final QLogger LOG = QLogger.getLogger(ValueTooLongBehavior.class);



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public ValueTooLongBehavior getDefault()
   {
      return (PASS_THROUGH);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void apply(ValueBehaviorApplier.Action action, List<QRecord> recordList, QInstance instance, QTableMetaData table, QFieldMetaData field)
   {
      if(this.equals(PASS_THROUGH))
      {
         return;
      }

      String fieldName = field.getName();
      if(!QFieldType.STRING.equals(field.getType()))
      {
         LOG.debug("Request to apply a ValueTooLongBehavior to a non-string field", logPair("table", table.getName()), logPair("field", fieldName));
         return;
      }

      if(field.getMaxLength() == null)
      {
         LOG.debug("Request to apply a ValueTooLongBehavior to string field without a maxLength", logPair("table", table.getName()), logPair("field", fieldName));
         return;
      }

      for(QRecord record : recordList)
      {
         String value = record.getValueString(fieldName);
         if(value != null && value.length() > field.getMaxLength())
         {
            switch(this)
            {
               case TRUNCATE -> record.setValue(fieldName, StringUtils.safeTruncate(value, field.getMaxLength()));
               case TRUNCATE_ELLIPSIS -> record.setValue(fieldName, StringUtils.safeTruncate(value, field.getMaxLength(), "..."));
               case ERROR -> record.addError(new BadInputStatusMessage("The value for " + field.getLabel() + " is too long (max allowed length=" + field.getMaxLength() + ")"));
               ///////////////////////////////////
               // PASS_THROUGH is handled above //
               ///////////////////////////////////
               default -> throw new IllegalStateException("Unexpected enum value: " + this);
            }
         }
      }
   }
}
