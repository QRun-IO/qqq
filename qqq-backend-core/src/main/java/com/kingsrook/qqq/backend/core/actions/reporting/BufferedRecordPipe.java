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

package com.kingsrook.qqq.backend.core.actions.reporting;


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** Subclass of RecordPipe, which uses a buffer in the addRecord method, to avoid
 ** sending single-records at a time through postRecordActions and to consumers.
 *******************************************************************************/
public class BufferedRecordPipe extends RecordPipe
{
   protected List<QRecord> buffer     = new ArrayList<>();
   private   Integer       bufferSize = 100;



   /*******************************************************************************
    ** Constructor - uses default buffer size
    **
    *******************************************************************************/
   public BufferedRecordPipe()
   {
   }



   /*******************************************************************************
    ** Constructor - customize buffer size.
    **
    *******************************************************************************/
   public BufferedRecordPipe(Integer bufferSize)
   {
      this.bufferSize = bufferSize;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void addRecord(QRecord record) throws QException
   {
      buffer.add(record);
      if(buffer.size() >= bufferSize)
      {
         flush();
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void addRecords(List<QRecord> records) throws QException
   {
      buffer.addAll(records);
      if(buffer.size() >= bufferSize)
      {
         flush();
      }
   }



   /***************************************************************************
    *
    ***************************************************************************/
   protected void flush() throws QException
   {
      super.addRecords(buffer);
      buffer.clear();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void finalFlush() throws QException
   {
      if(!buffer.isEmpty())
      {
         flush();
      }
   }
}
