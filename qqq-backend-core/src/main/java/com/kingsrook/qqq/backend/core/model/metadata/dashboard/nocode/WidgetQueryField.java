/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.metadata.dashboard.nocode;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 **
 *******************************************************************************/
public class WidgetQueryField extends AbstractWidgetValueSourceWithFilter
{
   private String selectFieldName;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public WidgetQueryField()
   {
      setType(getClass().getSimpleName());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Object evaluate(Map<String, Object> context, RenderWidgetInput input) throws QException
   {
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(tableName);
      queryInput.setInputSource(input.getInputSource());
      QQueryFilter effectiveFilter = getEffectiveFilter(input);
      queryInput.setFilter(effectiveFilter);
      effectiveFilter.setLimit(1);
      if(input.getInputSource() == QInputSource.USER)
      {
         PermissionsHelper.checkTablePermissionThrowing(queryInput, TablePermissionSubType.READ);
         PermissionsHelper.checkJoinedTableReadPermissions(queryInput, queryInput.getQueryJoins(), queryInput.getFilter());
      }
      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      if(CollectionUtils.nullSafeHasContents(queryOutput.getRecords()))
      {
         return (queryOutput.getRecords().get(0).getValue(selectFieldName));
      }

      return (null);
   }



   /*******************************************************************************
    ** Fluent setter for name
    *******************************************************************************/
   public WidgetQueryField withName(String name)
   {
      setName(name);
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for tableName
    *******************************************************************************/
   public WidgetQueryField withTableName(String tableName)
   {
      this.tableName = tableName;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for filter
    *******************************************************************************/
   public WidgetQueryField withFilter(QQueryFilter filter)
   {
      this.filter = filter;
      return (this);
   }



   /*******************************************************************************
    ** Getter for selectFieldName
    *******************************************************************************/
   public String getSelectFieldName()
   {
      return (this.selectFieldName);
   }



   /*******************************************************************************
    ** Setter for selectFieldName
    *******************************************************************************/
   public void setSelectFieldName(String selectFieldName)
   {
      this.selectFieldName = selectFieldName;
   }



   /*******************************************************************************
    ** Fluent setter for selectFieldName
    *******************************************************************************/
   public WidgetQueryField withSelectFieldName(String selectFieldName)
   {
      this.selectFieldName = selectFieldName;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for conditionalFilterList
    *******************************************************************************/
   @Override
   public WidgetQueryField withConditionalFilterList(List<AbstractConditionalFilter> conditionalFilterList)
   {
      this.conditionalFilterList = conditionalFilterList;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter to add a single conditionalFilter
    *******************************************************************************/
   public WidgetQueryField withConditionalFilter(AbstractConditionalFilter conditionalFilter)
   {
      if(this.conditionalFilterList == null)
      {
         this.conditionalFilterList = new ArrayList<>();
      }
      this.conditionalFilterList.add(conditionalFilter);
      return (this);
   }

}
