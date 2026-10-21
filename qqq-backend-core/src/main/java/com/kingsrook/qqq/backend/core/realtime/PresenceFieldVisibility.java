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

package com.kingsrook.qqq.backend.core.realtime;

import java.util.LinkedHashMap;
import java.util.Set;
import java.util.stream.Collectors;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;

/*******************************************************************************
 ** Apply recipient field privacy consistently to REST and realtime presence.
 *******************************************************************************/
public class PresenceFieldVisibility
{
   /*******************************************************************************
    ** Resolve on each delivery; shared leases must never retain a recipient's view.
    *******************************************************************************/
   public static Set<String> visibleFieldNames(String tableName) throws QException
   {
      var table = TableMetaDataPersonalizerAction.execute(new GetInput(tableName).withInputSource(QInputSource.USER), true);
      if(table == null)
      {
         return (Set.of());
      }
      var fields = new LinkedHashMap<String, QFieldMetaData>(table.getFields());
      if(table.getVirtualFields() != null)
      {
         fields.putAll(table.getVirtualFields());
      }
      return (fields.entrySet().stream().filter(entry -> !Boolean.TRUE.equals(entry.getValue().getIsHidden())
         && entry.getValue().getType() != QFieldType.PASSWORD).map(java.util.Map.Entry::getKey).collect(Collectors.toSet()));
   }
}
