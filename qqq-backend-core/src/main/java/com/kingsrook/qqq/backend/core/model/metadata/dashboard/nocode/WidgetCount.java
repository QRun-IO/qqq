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

package com.kingsrook.qqq.backend.core.model.metadata.dashboard.nocode;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;


/*******************************************************************************
 **
 *******************************************************************************/
public class WidgetCount extends AbstractWidgetValueSourceWithFilter
{


   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public WidgetCount()
   {
      setType(getClass().getSimpleName());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Object evaluate(Map<String, Object> context, RenderWidgetInput input) throws QException
   {
      CountInput countInput = new CountInput();
      countInput.setTableName(tableName);
      countInput.setInputSource(input.getInputSource());
      countInput.setFilter(getEffectiveFilter(input));
      if(input.getInputSource() == QInputSource.USER)
      {
         PermissionsHelper.checkTablePermissionThrowing(countInput, TablePermissionSubType.READ);
         PermissionsHelper.checkJoinedTableReadPermissions(countInput, countInput.getQueryJoins(), countInput.getFilter());
      }

      CountOutput countOutput = new CountAction().execute(countInput);
      return (countOutput.getCount());
   }



   /*******************************************************************************
    ** Fluent setter for name
    *******************************************************************************/
   public WidgetCount withName(String name)
   {
      setName(name);
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for tableName
    *******************************************************************************/
   @Override
   public WidgetCount withTableName(String tableName)
   {
      this.tableName = tableName;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for filter
    *******************************************************************************/
   @Override
   public WidgetCount withFilter(QQueryFilter filter)
   {
      this.filter = filter;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for conditionalFilterList
    *******************************************************************************/
   @Override
   public WidgetCount withConditionalFilterList(List<AbstractConditionalFilter> conditionalFilterList)
   {
      this.conditionalFilterList = conditionalFilterList;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter to add a single conditionalFilter
    *******************************************************************************/
   public WidgetCount withConditionalFilter(AbstractConditionalFilter conditionalFilter)
   {
      if(this.conditionalFilterList == null)
      {
         this.conditionalFilterList = new ArrayList<>();
      }
      this.conditionalFilterList.add(conditionalFilter);
      return (this);
   }

}
