/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.reporting;


import com.kingsrook.qqq.backend.core.exceptions.QException;


/*******************************************************************************
 ** Subclass of BufferedRecordPipe, which ultimately sends records down to an
 ** original RecordPipe.
 **
 ** Meant to be used where: someone passed in a RecordPipe (so they have a reference
 ** to it, and they are waiting to read from it), but the producer knows that
 ** it will be better to buffer the records, so they want to use a buffered pipe
 ** (but they still need the records to end up in the original pipe - thus -
 ** it gets wrapped by an object of this class).
 *******************************************************************************/
public class RecordPipeBufferedWrapper extends BufferedRecordPipe
{
   private RecordPipe wrappedPipe;



   /*******************************************************************************
    ** Constructor - uses default buffer size
    **
    *******************************************************************************/
   public RecordPipeBufferedWrapper(RecordPipe wrappedPipe)
   {
      this.wrappedPipe = wrappedPipe;
   }



   /*******************************************************************************
    ** Constructor - customize buffer size.
    **
    *******************************************************************************/
   public RecordPipeBufferedWrapper(Integer bufferSize, RecordPipe wrappedPipe)
   {
      super(bufferSize);
      this.wrappedPipe = wrappedPipe;
   }



   /*******************************************************************************
    ** when it's time to actually add records into the pipe (flushing from buffer int
    * pipe), actually add them into the wrapped pipe!
    *******************************************************************************/
   @Override
   protected void flush() throws QException
   {
      wrappedPipe.addRecords(buffer);
      buffer.clear();
   }



   /***************************************************************************
    *
    ***************************************************************************/
   public RecordPipe getWrappedPipe()
   {
      return wrappedPipe;
   }

}
