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

package com.kingsrook.qqq.backend.core.processes.implementations.sharing;


import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;


/*******************************************************************************
 **
 *******************************************************************************/
public class SharingMetaDataProvider
{

   /*******************************************************************************
    **
    *******************************************************************************/
   public void defineAll(QInstance instance, Consumer<QProcessMetaData> processEnricher) throws QException
   {
      List<QProcessMetaData> processes = new ArrayList<>();
      processes.add(new GetSharedRecordsProcess().produce(instance));
      processes.add(new InsertSharedRecordProcess().produce(instance));
      processes.add(new EditSharedRecordProcess().produce(instance));
      processes.add(new DeleteSharedRecordProcess().produce(instance));

      for(QProcessMetaData process : processes)
      {
         if(processEnricher != null)
         {
            processEnricher.accept(process);
         }

         instance.addProcess(process);
      }
   }

}
