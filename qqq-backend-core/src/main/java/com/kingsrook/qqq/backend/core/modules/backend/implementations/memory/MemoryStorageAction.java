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

package com.kingsrook.qqq.backend.core.modules.backend.implementations.memory;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;
import com.kingsrook.qqq.backend.core.actions.interfaces.QStorageInterface;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QNotFoundException;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.storage.StorageInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** implementation of bulk-storage interface, for the memory backend module.
 **
 ** Requires table to have (at least?) 2 fields - a STRING primary key and a
 ** BLOB to store bytes.
 *******************************************************************************/
public class MemoryStorageAction implements QStorageInterface
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public OutputStream createOutputStream(StorageInput storageInput)
   {
      return new MemoryStorageOutputStream(storageInput.getTableName(), storageInput.getReference());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public InputStream getInputStream(StorageInput storageInput) throws QException
   {
      QRecord record = new GetAction().executeForRecord(new GetInput(storageInput.getTableName()).withPrimaryKey(storageInput.getReference()));
      if(record == null)
      {
         throw (new QNotFoundException("Could not find input stream for [" + storageInput.getTableName() + "][" + storageInput.getReference() + "]"));
      }

      QFieldMetaData blobField = getBlobField(storageInput.getTableName());
      return (new ByteArrayInputStream(record.getValueByteArray(blobField.getName())));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QFieldMetaData getBlobField(String tableName) throws QException
   {
      Optional<QFieldMetaData> firstBlobField = QContext.getQInstance().getTable(tableName).getFields().values().stream().filter(f -> QFieldType.BLOB.equals(f.getType())).findFirst();
      if(firstBlobField.isEmpty())
      {
         throw (new QException("Could not find a blob field in table [" + tableName + "]"));
      }
      return firstBlobField.get();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static class MemoryStorageOutputStream extends ByteArrayOutputStream
   {
      private final String tableName;
      private final String reference;



      /*******************************************************************************
       ** Constructor
       **
       *******************************************************************************/
      public MemoryStorageOutputStream(String tableName, String reference)
      {
         this.tableName = tableName;
         this.reference = reference;
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void close() throws IOException
      {
         super.close();

         try
         {
            QFieldMetaData blobField = getBlobField(tableName);
            InsertOutput insertOutput = new InsertAction().execute(new InsertInput(tableName).withRecord(new QRecord()
               .withValue(QContext.getQInstance().getTable(tableName).getPrimaryKeyField(), reference)
               .withValue(blobField.getName(), toByteArray())));

            if(CollectionUtils.nullSafeHasContents(insertOutput.getRecords().get(0).getErrors()))
            {
               throw(new IOException("Error storing stream into memory storage: " + StringUtils.joinWithCommasAndAnd(insertOutput.getRecords().get(0).getErrors().stream().map(e -> e.getMessage()).toList())));
            }
         }
         catch(Exception e)
         {
            throw new IOException("Wrapped QException", e);
         }
      }
   }
}
