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
import com.kingsrook.qqq.backend.core.actions.tables.AggregateAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.Aggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateResult;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;


/*******************************************************************************
 **
 *******************************************************************************/
public class WidgetAggregate extends AbstractWidgetValueSourceWithFilter
{
   private Aggregate aggregate;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public WidgetAggregate()
   {
      setType(getClass().getSimpleName());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Object evaluate(Map<String, Object> context, RenderWidgetInput input) throws QException
   {
      AggregateInput aggregateInput = new AggregateInput();
      aggregateInput.setTableName(tableName);
      aggregateInput.setInputSource(input.getInputSource());
      if(input.getInputSource() == QInputSource.USER)
      {
         PermissionsHelper.checkTablePermissionThrowing(aggregateInput, TablePermissionSubType.READ);
      }
      aggregateInput.setAggregates(List.of(aggregate));
      aggregateInput.setFilter(getEffectiveFilter(input));
      if(input.getInputSource() == QInputSource.USER)
      {
         PermissionsHelper.checkJoinedTableReadPermissions(aggregateInput, aggregateInput.getQueryJoins(), aggregateInput.getFilter());
      }

      AggregateOutput       aggregateOutput = new AggregateAction().execute(aggregateInput);
      List<AggregateResult> results         = aggregateOutput.getResults();
      if(results.isEmpty())
      {
         return (null);
      }
      else
      {
         AggregateResult aggregateResult = results.get(0);
         return (aggregateResult.getAggregateValue(aggregate));
      }
   }



   /*******************************************************************************
    ** Fluent setter for name
    *******************************************************************************/
   public WidgetAggregate withName(String name)
   {
      setName(name);
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for tableName
    *******************************************************************************/
   @Override
   public WidgetAggregate withTableName(String tableName)
   {
      this.tableName = tableName;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for filter
    *******************************************************************************/
   @Override
   public WidgetAggregate withFilter(QQueryFilter filter)
   {
      this.filter = filter;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for conditionalFilterList
    *******************************************************************************/
   @Override
   public WidgetAggregate withConditionalFilterList(List<AbstractConditionalFilter> conditionalFilterList)
   {
      this.conditionalFilterList = conditionalFilterList;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter to add a single conditionalFilter
    *******************************************************************************/
   public WidgetAggregate withConditionalFilter(AbstractConditionalFilter conditionalFilter)
   {
      if(this.conditionalFilterList == null)
      {
         this.conditionalFilterList = new ArrayList<>();
      }
      this.conditionalFilterList.add(conditionalFilter);
      return (this);
   }



   /*******************************************************************************
    ** Getter for aggregate
    *******************************************************************************/
   public Aggregate getAggregate()
   {
      return (this.aggregate);
   }



   /*******************************************************************************
    ** Setter for aggregate
    *******************************************************************************/
   public void setAggregate(Aggregate aggregate)
   {
      this.aggregate = aggregate;
   }



   /*******************************************************************************
    ** Fluent setter for aggregate
    *******************************************************************************/
   public WidgetAggregate withAggregate(Aggregate aggregate)
   {
      this.aggregate = aggregate;
      return (this);
   }

}
