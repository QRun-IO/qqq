/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.metadata.tables;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionCheckResult;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.values.BasicCustomPossibleValueProvider;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValue;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/*******************************************************************************
 ** possible-value source provider for the `Tables` PVS - a list of all tables
 ** in an application/qInstance.
 *******************************************************************************/
public class TablesCustomPossibleValueProvider extends BasicCustomPossibleValueProvider<QTableMetaData, String>
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected QPossibleValue<String> makePossibleValue(QTableMetaData sourceObject)
   {
      return (new QPossibleValue<>(sourceObject.getName(), sourceObject.getLabel()));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected QTableMetaData getSourceObject(Serializable id)
   {
      QTableMetaData table = QContext.getQInstance().getTable(ValueUtils.getValueAsString(id));
      return isTableAllowed(table) ? table : null;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected List<QTableMetaData> getAllSourceObjects()
   {
      ArrayList<QTableMetaData> rs = new ArrayList<>();
      for(QTableMetaData table : QContext.getQInstance().getTables().values())
      {
         if(isTableAllowed(table))
         {
            rs.add(table);
         }
      }
      return rs;
   }


   /***************************************************************************
    **
    ***************************************************************************/
   protected boolean isTableAllowed(QTableMetaData table)
   {
      if(table == null)
      {
         return (false);
      }

      if(table.getIsHidden())
      {
         return (false);
      }

      PermissionCheckResult permissionCheckResult = PermissionsHelper.getPermissionCheckResult(new QueryInput(table.getName()), table);
      if(!PermissionCheckResult.ALLOW.equals(permissionCheckResult))
      {
         return (false);
      }

      return (true);
   }

}
