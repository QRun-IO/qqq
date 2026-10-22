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


package com.kingsrook.qqq.backend.core.modules.backend.implementations.memory;


import java.time.Instant;
import com.kingsrook.qqq.backend.core.actions.interfaces.ConditionalUpdateInterface;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.ConditionalUpdateInput;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;


/*******************************************************************************
 ** In-memory conditional update (for tests and single-JVM use): atomic under the
 ** memory store's per-table lock; the clock is this JVM's clock.
 *******************************************************************************/
public class MemoryConditionalUpdateAction implements ConditionalUpdateInterface
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public int execute(ConditionalUpdateInput input) throws QException
   {
      return (MemoryRecordStore.getInstance().conditionalUpdate(input));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Instant databaseNow(QBackendMetaData backend)
   {
      return (Instant.now());
   }

}
