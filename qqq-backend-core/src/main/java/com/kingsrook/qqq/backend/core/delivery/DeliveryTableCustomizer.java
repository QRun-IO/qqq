/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.delivery;


import java.util.List;
import java.util.Objects;
import java.util.Optional;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;


/*******************************************************************************
 ** Owners cannot be changed, even by delivery administrators.
 *******************************************************************************/
public class DeliveryTableCustomizer implements TableCustomizerInterface
{
   static final ThreadLocal<Boolean> LOG_WRITE = ThreadLocal.withInitial(() -> false);



   /*******************************************************************************
    ** Bind user-created choices to the authenticated owner.
    *******************************************************************************/
   @Override
   public List<QRecord> preInsert(InsertInput input, List<QRecord> records, boolean preview) throws QException
   {
      requireLogWriter(input.getTableName());
      if(!(QContext.getQSession() instanceof QSystemUserSession))
      {
         for(QRecord record : records)
         {
            record.setValue("ownerUserId", QContext.getQSession().getUser().getIdReference());
         }
      }
      return (records);
   }



   /*******************************************************************************
    ** Ownership is immutable; partial updates need not repeat it.
    *******************************************************************************/
   @Override
   public List<QRecord> preUpdate(UpdateInput input, List<QRecord> records, boolean preview, Optional<List<QRecord>> oldRecords) throws QException
   {
      requireLogWriter(input.getTableName());
      for(QRecord record : records)
      {
         if(record.getValues().containsKey("ownerUserId"))
         {
            QRecord old = oldRecords.orElse(List.of()).stream().filter(candidate -> Objects.equals(candidate.getValue("id"), record.getValue("id"))).findFirst().orElse(null);
            if(old == null || !Objects.equals(old.getValue("ownerUserId"), record.getValue("ownerUserId")))
            {
               throw new QPermissionDeniedException("Delivery ownership cannot be changed.");
            }
         }
      }
      return (records);
   }


   /*******************************************************************************
    ** Delivery log records are action-owned, including their stored request envelope.
    *******************************************************************************/
   private void requireLogWriter(String tableName) throws QPermissionDeniedException
   {
      if(DeliveryMetaDataProvider.DELIVERY_LOG.equals(tableName) && !LOG_WRITE.get())
      {
         throw new QPermissionDeniedException("Delivery logs can only be written by the delivery action.");
      }
   }



   /*******************************************************************************
    ** Prevent record UI mutations of action-owned logs.
    *******************************************************************************/
   @Override
   public List<QRecord> preDelete(DeleteInput input, List<QRecord> records, boolean preview) throws QException
   {
      requireLogWriter(input.getTableName());
      return (records);
   }
}
