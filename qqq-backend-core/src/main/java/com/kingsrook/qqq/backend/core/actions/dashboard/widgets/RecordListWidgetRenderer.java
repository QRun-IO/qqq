/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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


import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import com.kingsrook.qqq.backend.core.actions.dashboard.AbstractHTMLWidgetRenderer;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.values.QValueFormatter;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.instances.validation.plugins.QInstanceValidatorPluginInterface;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.FilterUseCase;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetOutput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.ChildRecordListData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.WidgetType;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.AbstractWidgetMetaDataBuilder;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaDataInterface;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import com.kingsrook.qqq.backend.core.utils.collections.MapBuilder;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Generic widget to display a list of records.
 **
 ** Note, closely related to (and copied from ChildRecordListRenderer.
 ** opportunity to share more code with that in the future??
 *******************************************************************************/
public class RecordListWidgetRenderer extends AbstractWidgetRenderer
{
   private static final QLogger LOG = QLogger.getLogger(RecordListWidgetRenderer.class);



   /*******************************************************************************
    **
    *******************************************************************************/
   public static Builder widgetMetaDataBuilder(String widgetName)
   {
      return (new Builder(new QWidgetMetaData()
         .withName(widgetName)
         .withIsCard(true)
         .withCodeReference(new QCodeReference(RecordListWidgetRenderer.class))
         .withType(WidgetType.CHILD_RECORD_LIST.getType())
         .withValidatorPlugin(new RecordListWidgetValidator())
      ));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class Builder extends AbstractWidgetMetaDataBuilder
   {

      /*******************************************************************************
       ** Constructor
       **
       *******************************************************************************/
      public Builder(QWidgetMetaData widgetMetaData)
      {
         super(widgetMetaData);
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      public Builder withLabel(String label)
      {
         widgetMetaData.setLabel(label);
         return (this);
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      public Builder withMaxRows(Integer maxRows)
      {
         widgetMetaData.withDefaultValue("maxRows", maxRows);
         return (this);
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      public Builder withTableName(String tableName)
      {
         widgetMetaData.withDefaultValue("tableName", tableName);
         return (this);
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      public Builder withFilter(QQueryFilter filter)
      {
         widgetMetaData.withDefaultValue("filter", filter);
         return (this);
      }

   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public RenderWidgetOutput render(RenderWidgetInput input) throws QException
   {
      try
      {
         Integer maxRows = null;
         if(StringUtils.hasContent(input.getQueryParams().get("maxRows")))
         {
            maxRows = ValueUtils.getValueAsInteger(input.getQueryParams().get("maxRows"));
         }
         else if(input.getWidgetMetaData().getDefaultValues().containsKey("maxRows"))
         {
            maxRows = ValueUtils.getValueAsInteger(input.getWidgetMetaData().getDefaultValues().get("maxRows"));
         }

         QQueryFilter filter = ((QQueryFilter) input.getWidgetMetaData().getDefaultValues().get("filter")).clone();
         filter.interpretValues(MapBuilder.of("input", new HashMap<>(input.getQueryParams())), FilterUseCase.DEFAULT);
         filter.setLimit(maxRows);

         String         tableName = ValueUtils.getValueAsString(input.getWidgetMetaData().getDefaultValues().get("tableName"));
         QTableMetaData table     = QContext.getQInstance().getTable(tableName);

         QueryInput queryInput = new QueryInput();
         queryInput.setTableName(tableName);
         queryInput.setShouldTranslatePossibleValues(true);
         queryInput.setShouldGenerateDisplayValues(true);
         queryInput.setFilter(filter);
         queryInput.setInputSource(QInputSource.USER);
         QueryOutput queryOutput = new QueryAction().execute(queryInput);

         QValueFormatter.setBlobValuesToDownloadUrls(table, queryOutput.getRecords());

         int totalRows = queryOutput.getRecords().size();
         if(maxRows != null && (queryOutput.getRecords().size() == maxRows))
         {
            /////////////////////////////////////////////////////////////////////////////////////
            // if the input said to only do some max, and the # of results we got is that max, //
            // then do a count query, for displaying 1-n of <count>                            //
            /////////////////////////////////////////////////////////////////////////////////////
            CountInput countInput = new CountInput();
            countInput.setTableName(tableName);
            countInput.setFilter(filter);
            totalRows = new CountAction().execute(countInput).getCount();
         }

         String tablePath = QContext.getQInstance().getTablePath(tableName);
         if(!AbstractHTMLWidgetRenderer.doesHaveTablePermission(tableName))
         {
            tablePath = null;
         }
         String viewAllLink = tablePath == null ? null : (tablePath + "?filter=" + URLEncoder.encode(JsonUtils.toJson(filter), StandardCharsets.UTF_8));

         ChildRecordListData widgetData = new ChildRecordListData(input.getQueryParams().get("widgetLabel"), queryOutput, table, tablePath, viewAllLink, totalRows);

         return (new RenderWidgetOutput(widgetData));
      }
      catch(Exception e)
      {
         LOG.warn("Error rendering record list widget", e, logPair("widgetName", () -> input.getWidgetMetaData().getName()));
         throw (e);
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static class RecordListWidgetValidator implements QInstanceValidatorPluginInterface<QWidgetMetaDataInterface>
   {

      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public void validate(QWidgetMetaDataInterface widgetMetaData, QInstance qInstance, QInstanceValidator qInstanceValidator)
      {
         String prefix = "Widget " + widgetMetaData.getName() + ": ";

         //////////////////////////////////////////////
         // make sure table name is given and exists //
         //////////////////////////////////////////////
         QTableMetaData table     = null;
         String         tableName = ValueUtils.getValueAsString(CollectionUtils.nonNullMap(widgetMetaData.getDefaultValues()).get("tableName"));
         if(qInstanceValidator.assertCondition(StringUtils.hasContent(tableName), prefix + "defaultValue for tableName must be given"))
         {
            ////////////////////////////
            // make sure table exists //
            ////////////////////////////
            table = qInstance.getTable(tableName);
            qInstanceValidator.assertCondition(table != null, prefix + "No table named " + tableName + " exists in the instance");
         }

         ////////////////////////////////////////////////////////////////////////////////////
         // make sure filter is given and is valid (only check that if table is given too) //
         ////////////////////////////////////////////////////////////////////////////////////
         QQueryFilter filter = ((QQueryFilter) widgetMetaData.getDefaultValues().get("filter"));
         if(qInstanceValidator.assertCondition(filter != null, prefix + "defaultValue for filter must be given") && table != null)
         {
            qInstanceValidator.validateQueryFilter(qInstance, prefix, table, filter, null);
         }
      }

   }
}
