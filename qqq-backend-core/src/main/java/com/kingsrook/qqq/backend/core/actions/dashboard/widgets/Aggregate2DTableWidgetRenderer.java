/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.dashboard.widgets;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerAction;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.actions.tables.AggregateAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.Aggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateResult;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.GroupBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.QFilterOrderByAggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.QFilterOrderByGroupBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetOutput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.TableData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/*******************************************************************************
 ** Generic widget that does an aggregate query, and presents its results
 ** as a table, using group-by values as both row & column labels.
 *******************************************************************************/
public class Aggregate2DTableWidgetRenderer extends AbstractWidgetRenderer
{
   private static final QLogger LOG = QLogger.getLogger(Aggregate2DTableWidgetRenderer.class);



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public RenderWidgetOutput render(RenderWidgetInput input) throws QException
   {
      Map<String, Serializable> values = input.getWidgetMetaData().getDefaultValues();

      String         tableName   = ValueUtils.getValueAsString(values.get("tableName"));
      String         valueField  = ValueUtils.getValueAsString(values.get("valueField"));
      String         rowField    = ValueUtils.getValueAsString(values.get("rowField"));
      String         columnField = ValueUtils.getValueAsString(values.get("columnField"));

      AggregateInput aggregateInput = new AggregateInput();
      aggregateInput.setTableName(tableName);
      aggregateInput.setInputSource(input.getInputSource());
      if(input.getInputSource() == QInputSource.USER)
      {
         PermissionsHelper.checkTablePermissionThrowing(aggregateInput, TablePermissionSubType.READ);
      }
      QTableMetaData table = TableMetaDataPersonalizerAction.execute(aggregateInput);
      if(table == null || !table.getFields().containsKey(rowField) || !table.getFields().containsKey(columnField))
      {
         throw (new QException("Aggregate widget grouping fields are not available"));
      }
      aggregateInput.setTableMetaData(table);

      // todo - allow input of "list of columns" (e.g., in case some miss sometimes, or as a version of filter)
      // todo - max rows, max cols?

      // todo - from input map
      QQueryFilter filter = new QQueryFilter();
      aggregateInput.setFilter(filter);

      Aggregate aggregate = new Aggregate(valueField, AggregateOperator.COUNT);
      aggregateInput.withAggregate(aggregate);

      GroupBy rowGroupBy    = new GroupBy(table.getField(rowField));
      GroupBy columnGroupBy = new GroupBy(table.getField(columnField));
      aggregateInput.withGroupBy(rowGroupBy);
      aggregateInput.withGroupBy(columnGroupBy);

      String orderBys = ValueUtils.getValueAsString(values.get("orderBys"));
      if(StringUtils.hasContent(orderBys))
      {
         for(String orderBy : orderBys.split(","))
         {
            switch(orderBy)
            {
               case "row" -> filter.addOrderBy(new QFilterOrderByGroupBy(rowGroupBy));
               case "column" -> filter.addOrderBy(new QFilterOrderByGroupBy(columnGroupBy));
               case "value" -> filter.addOrderBy(new QFilterOrderByAggregate(aggregate));
               default -> LOG.warn("Unrecognized orderBy: " + orderBy);
            }
         }
      }

      AggregateOutput aggregateOutput = new AggregateAction().execute(aggregateInput);

      Map<Serializable, Map<Serializable, Serializable>> data       = new LinkedHashMap<>();
      Set<Serializable>                                  columnsSet = new LinkedHashSet<>();

      for(AggregateResult result : aggregateOutput.getResults())
      {
         Serializable column = result.getGroupByValue(columnGroupBy);
         Serializable row    = result.getGroupByValue(rowGroupBy);
         Serializable value  = result.getAggregateValue(aggregate);

         Map<Serializable, Serializable> rowMap = data.computeIfAbsent(row, (k) -> new LinkedHashMap<>());
         rowMap.put(column, value);
         columnsSet.add(column);
      }

      // todo - possible values from rows, cols

      ////////////////////////////////////
      // setup datastructures for table //
      ////////////////////////////////////
      List<Map<String, Object>> tableRows    = new ArrayList<>();
      List<TableData.Column>    tableColumns = new ArrayList<>();
      tableColumns.add(new TableData.Column("default", table.getField(rowField).getLabel(), "_row", "2fr", "left"));

      for(Serializable column : columnsSet)
      {
         tableColumns.add(new TableData.Column("default", String.valueOf(column) /* todo display value */, String.valueOf(column), "1fr", "right"));
      }

      tableColumns.add(new TableData.Column("default", "Total", "_total", "1fr", "right"));

      TableData tableData = new TableData(null, tableColumns, tableRows)
         .withRowsPerPage(100)
         .withFixedStickyLastRow(false)
         .withHidePaginationDropdown(true);

      Map<Serializable, Integer> columnSums = new HashMap<>();
      int                        grandTotal = 0;
      for(Map.Entry<Serializable, Map<Serializable, Serializable>> rowEntry : data.entrySet())
      {
         Map<String, Object> rowMap = new HashMap<>();
         tableRows.add(rowMap);

         rowMap.put("_row", rowEntry.getKey() /* todo display value */);
         int rowTotal = 0;
         for(Serializable column : columnsSet)
         {
            Serializable value = rowEntry.getValue().get(column);
            if(value == null)
            {
               value = 0; // todo?
            }

            Integer valueAsInteger = Objects.requireNonNullElse(ValueUtils.getValueAsInteger(value), 0);
            rowTotal += valueAsInteger;
            columnSums.putIfAbsent(column, 0);
            columnSums.put(column, columnSums.get(column) + valueAsInteger);

            rowMap.put(String.valueOf(column), value); // todo format commas?
         }

         rowMap.put("_total", rowTotal);
         grandTotal += rowTotal;
      }

      ///////////////
      // total row //
      ///////////////
      Map<String, Object> totalRowMap = new HashMap<>();
      tableRows.add(totalRowMap);

      totalRowMap.put("_row", "Total");
      int rowTotal = 0;
      for(Serializable column : columnsSet)
      {
         Serializable value = columnSums.get(column);
         if(value == null)
         {
            value = 0; // todo?
         }

         totalRowMap.put(String.valueOf(column), value); // todo format commas?
      }

      totalRowMap.put("_total", grandTotal);

      return (new RenderWidgetOutput(tableData));
   }

}
