/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.helpcontent;


import java.util.List;
import com.kingsrook.qqq.backend.core.actions.customizers.AbstractPreDeleteCustomizer;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QInstanceHelpContentManager;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** remove existing helpContent from meta-data when a record is deleted
 *******************************************************************************/
public class HelpContentPreDeleteCustomizer extends AbstractPreDeleteCustomizer
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public List<QRecord> apply(List<QRecord> records) throws QException
   {
      if(records != null)
      {
         for(QRecord record : records)
         {
            removeOldRecordFromMetaData(record);
         }
      }

      return (records);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   static void removeOldRecordFromMetaData(QRecord oldRecord)
   {
      ////////////////////////////////////////////////////////////////////////////
      // this (clearing the content) will remove the helpContent under this key //
      ////////////////////////////////////////////////////////////////////////////
      if(oldRecord != null)
      {
         QRecord recordWithoutContent = new QRecord(oldRecord);
         recordWithoutContent.setValue("content", null);
         QInstanceHelpContentManager.processHelpContentRecord(QContext.getQInstance(), recordWithoutContent);
      }
   }
}
