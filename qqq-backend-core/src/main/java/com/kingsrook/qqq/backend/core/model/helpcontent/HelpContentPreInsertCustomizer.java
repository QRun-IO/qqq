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
import com.kingsrook.qqq.backend.core.actions.customizers.AbstractPreInsertCustomizer;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** Record user authorship without replacing a bundle row's provenance.
 *******************************************************************************/
public class HelpContentPreInsertCustomizer extends AbstractPreInsertCustomizer
{
   /*******************************************************************************
    ** Attribute rows created through normal table actions.
    *******************************************************************************/
   @Override
   public List<QRecord> apply(List<QRecord> records)
   {
      if(records != null)
      {
         records.forEach(record -> attribute(record, null));
      }
      return (records);
   }



   /*******************************************************************************
    ** Shared insert/update attribution; old rows supply omitted provenance fields.
    *******************************************************************************/
   static void attribute(QRecord record, QRecord old)
   {
      if(HelpContentSyncContext.isActive() || !QContext.getQInstance().getTable(HelpContent.TABLE_NAME).getFields().containsKey("source"))
      {
         return;
      }
      if(QContext.getQSession() != null && QContext.getQSession().getUser() != null)
      {
         record.setValue("modifiedBy", QContext.getQSession().getUser().getIdReference());
      }
      String source = old == null ? record.getValueString("source") : old.getValueString("source");
      String bundle = old == null ? record.getValueString("bundleId") : old.getValueString("bundleId");
      if(old == null && (bundle == null || bundle.isBlank()))
      {
         record.setValue("source", HelpContentSource.USER.name());
      }
      else if(source != null)
      {
         record.setValue("source", source);
      }
      else if(bundle == null || bundle.isBlank())
      {
         record.setValue("source", HelpContentSource.USER.name());
      }
   }
}
