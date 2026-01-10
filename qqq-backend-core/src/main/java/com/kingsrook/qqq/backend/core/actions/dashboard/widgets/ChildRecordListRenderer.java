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


import java.io.Serializable;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.google.gson.reflect.TypeToken;
import com.kingsrook.qqq.backend.core.actions.dashboard.AbstractHTMLWidgetRenderer;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.values.QValueFormatter;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QNotFoundException;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.instances.validation.plugins.QInstanceValidatorPluginInterface;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetOutput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.ChildRecordListData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.WidgetType;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.AbstractWidgetMetaDataBuilder;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaDataInterface;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import com.kingsrook.qqq.backend.core.utils.collections.ListBuilder;
import com.kingsrook.qqq.backend.core.utils.collections.MutableList;
import org.apache.commons.lang3.BooleanUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Generic widget for display a list of child records.
 *******************************************************************************/
public class ChildRecordListRenderer extends AbstractWidgetRenderer
{
   private static final QLogger LOG = QLogger.getLogger(ChildRecordListRenderer.class);



   /*******************************************************************************
    **
    *******************************************************************************/
   public static Builder widgetMetaDataBuilder(QJoinMetaData join)
   {
      return (new Builder(new QWidgetMetaData()
         .withName(join.getName())
         .withIsCard(true)
         .withCodeReference(new QCodeReference(ChildRecordListRenderer.class))
         .withType(WidgetType.CHILD_RECORD_LIST.getType())
         .withDefaultValue("joinName", join.getName())
         .withValidatorPlugin(new ChildRecordListWidgetValidator())
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
       * set internal name for the widget in the QInstance.
       *******************************************************************************/
      public Builder withName(String name)
      {
         widgetMetaData.setName(name);
         return (this);
      }



      /*******************************************************************************
       * set user-facing label for the widget
       *******************************************************************************/
      public Builder withLabel(String label)
      {
         widgetMetaData.setLabel(label);
         return (this);
      }



      /*******************************************************************************
       * set the max rows to be included in the widget.  Wise to always set this, to
       * avoid unbounded query (todo - add a system or instance wide default)
       *******************************************************************************/
      public Builder withMaxRows(Integer maxRows)
      {
         widgetMetaData.withDefaultValue("maxRows", maxRows);
         return (this);
      }



      /*******************************************************************************
       * Tell the UI that it should show an add-new button, to create records in the
       * child table.
       *******************************************************************************/
      public Builder withCanAddChildRecord(boolean b)
      {
         widgetMetaData.withDefaultValue("canAddChildRecord", b);
         return (this);
      }



      /*******************************************************************************
       * For the case where you canAddChildRecord is true, this field is a set of field
       * names in the child table that should be disabled in the add-child-record UI.
       *******************************************************************************/
      public Builder withDisabledFieldsForNewChildRecords(Set<String> disabledFieldsForNewChildRecords)
      {
         widgetMetaData.withDefaultValue("disabledFieldsForNewChildRecords", new HashSet<>(disabledFieldsForNewChildRecords));
         return (this);
      }



      /*******************************************************************************
       * For a widget that is meant to manage a list of associated child records, e.g.,
       * on an insert/edit screen, that association name must be set in this property.
       *******************************************************************************/
      public Builder withManageAssociationName(String manageAssociationName)
      {
         widgetMetaData.withDefaultValue("manageAssociationName", manageAssociationName);
         return (this);
      }



      /*******************************************************************************
       * The default behavior is to show all fields in the table (and any included
       * exposed joins) on the frontend.  This option allows you to specify a subset
       * of fields to omit - to send ot the frontend for not displaying.
       *******************************************************************************/
      public Builder withOmitFieldNames(List<String> omitFieldNames)
      {
         ArrayList<String> arrayList = CollectionUtils.useOrWrap(omitFieldNames, new TypeToken<>() {});
         widgetMetaData.withDefaultValue("omitFieldNames", arrayList);
         return (this);
      }



      /*******************************************************************************
       * The default behavior is to show all fields in the table (and any included
       * exposed joins) on the frontend.  This option allows you to specify a subset
       * of field names, that get set in {@link ChildRecordListData#withOnlyIncludeFieldNames(List)},
       * which a frontend should respect to limit what's shown.
       *******************************************************************************/
      public Builder withOnlyIncludeFieldNames(List<String> onlyIncludeFieldNames)
      {
         ArrayList<String> arrayList = CollectionUtils.useOrWrap(onlyIncludeFieldNames, new TypeToken<>() {});
         widgetMetaData.withDefaultValue("onlyIncludeFieldNames", arrayList);
         return (this);
      }



      /*******************************************************************************
       * The default behavior makes the join-field from the child table be sent to the
       * frontend in the {@link ChildRecordListData#withOmitFieldNames(List)} - e.g.,
       * so that it isn't shown (as it's redundant on all rows).
       *
       * <p>You can set this property to true to reverse that course, and not put
       * the join field in the omit list, so it does display.</p>
       *******************************************************************************/
      public Builder withKeepJoinField(Boolean keepJoinField)
      {
         widgetMetaData.withDefaultValue("keepJoinField", keepJoinField);
         return (this);
      }



      /*******************************************************************************
       * Add 1 or more query joins to the widget.  You'll probably want those to be
       * marked as .withSelect(true), to include their values (unless you just want to
       * cause an inner join to make records not be found maybe).  They'll need to
       * correspond to exposed joins on the table, for a frontend to display them.
       * Any joins listed in here will have their tables added to
       * {@link ChildRecordListData#withIncludeExposedJoinTables(List)}
       *******************************************************************************/
      public Builder withQueryJoins(List<QueryJoin> queryJoins)
      {
         ArrayList<QueryJoin> queryJoinsArrayList = CollectionUtils.useOrWrap(queryJoins, new TypeToken<>() {});
         widgetMetaData.withDefaultValue("queryJoins", queryJoinsArrayList);
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
         String         widgetLabel = input.getQueryParams().get("widgetLabel");
         String         joinName    = input.getQueryParams().get("joinName");
         QJoinMetaData  join        = QContext.getQInstance().getJoin(joinName);
         String         id          = input.getQueryParams().get("id");
         QTableMetaData leftTable   = QContext.getQInstance().getTable(join.getLeftTable());
         QTableMetaData rightTable  = QContext.getQInstance().getTable(join.getRightTable());

         Map<String, Serializable> widgetMetaDataDefaultValues = input.getWidgetMetaData().getDefaultValues();
         List<String>              omitFieldNames              = (List<String>) widgetMetaDataDefaultValues.get("omitFieldNames");
         if(omitFieldNames == null)
         {
            omitFieldNames = new ArrayList<>();
         }
         else
         {
            omitFieldNames = new MutableList<>(omitFieldNames);
         }

         Integer maxRows = null;
         if(StringUtils.hasContent(input.getQueryParams().get("maxRows")))
         {
            maxRows = ValueUtils.getValueAsInteger(input.getQueryParams().get("maxRows"));
         }
         else if(widgetMetaDataDefaultValues.containsKey("maxRows"))
         {
            maxRows = ValueUtils.getValueAsInteger(widgetMetaDataDefaultValues.get("maxRows"));
         }

         //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // fetch the record that we're getting children for. e.g., the left-side of the join, with the input id                                     //
         // but - only try this if we were given an id.  note, this widget could be called for on an INSERT screen, where we don't have a record yet //
         // but we still want to be able to return all the other data in here that otherwise comes from the widget meta data, join, etc.             //
         //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         int          totalRows     = 0;
         QRecord      primaryRecord = null;
         QQueryFilter filter        = null;
         QueryOutput  queryOutput   = new QueryOutput(new QueryInput());

         List<String> includeExposedJoinTables = null;

         if(StringUtils.hasContent(id))
         {
            GetInput getInput = new GetInput();
            getInput.setTableName(join.getLeftTable());
            getInput.setPrimaryKey(id);
            getInput.withShouldOmitHiddenFields(false);
            GetOutput getOutput = new GetAction().execute(getInput);
            primaryRecord = getOutput.getRecord();

            if(primaryRecord == null)
            {
               throw (new QNotFoundException("Could not find " + (leftTable == null ? "" : leftTable.getLabel()) + " with primary key " + id));
            }

            ////////////////////////////////////////////////////////////////////
            // set up the query - for the table on the right side of the join //
            ////////////////////////////////////////////////////////////////////
            filter = new QQueryFilter();
            for(JoinOn joinOn : join.getJoinOns())
            {
               String rightField = joinOn.getRightField();
               filter.addCriteria(new QFilterCriteria(rightField, QCriteriaOperator.EQUALS, ListBuilder.of(primaryRecord.getValue(joinOn.getLeftField()))));

               Boolean keepJoinField = ValueUtils.getValueAsBoolean(widgetMetaDataDefaultValues.get("keepJoinField"));
               if(!BooleanUtils.isTrue(keepJoinField))
               {
                  omitFieldNames.add(rightField);
               }
            }

            Serializable orderBy = widgetMetaDataDefaultValues.get("orderBy");
            if(orderBy instanceof List orderByList && !orderByList.isEmpty() && orderByList.get(0) instanceof QFilterOrderBy)
            {
               filter.setOrderBys(orderByList);
            }
            else
            {
               filter.setOrderBys(join.getOrderBys());
            }

            filter.setLimit(maxRows);

            QueryInput queryInput = new QueryInput();
            queryInput.setTableName(join.getRightTable());
            queryInput.setShouldTranslatePossibleValues(true);
            queryInput.setShouldGenerateDisplayValues(true);
            queryInput.setFilter(filter);
            queryInput.setInputSource(QInputSource.USER);

            Serializable queryJoinsSerializable = widgetMetaDataDefaultValues.get("queryJoins");
            if(queryJoinsSerializable instanceof List queryJoinsList)
            {
               queryInput.withQueryJoins(queryJoinsList);

               includeExposedJoinTables = new ArrayList<>();
               for(QueryJoin queryJoin : queryInput.getQueryJoins())
               {
                  includeExposedJoinTables.add(queryJoin.getJoinTable());
               }
            }

            queryOutput = new QueryAction().execute(queryInput);

            QValueFormatter.setBlobValuesToDownloadUrls(rightTable, queryOutput.getRecords());

            totalRows = queryOutput.getRecords().size();
            if(maxRows != null && (queryOutput.getRecords().size() == maxRows))
            {
               /////////////////////////////////////////////////////////////////////////////////////
               // if the input said to only do some max, and the # of results we got is that max, //
               // then do a count query, for displaying 1-n of <count>                            //
               /////////////////////////////////////////////////////////////////////////////////////
               CountInput countInput = new CountInput();
               countInput.setTableName(join.getRightTable());
               countInput.setFilter(filter);
               totalRows = new CountAction().execute(countInput).getCount();
            }
         }

         String tablePath = QContext.getQInstance().getTablePath(rightTable.getName());
         if(!AbstractHTMLWidgetRenderer.doesHaveTablePermission(rightTable.getName()))
         {
            tablePath = null;
         }
         String viewAllLink = tablePath == null ? null : (tablePath + "?filter=" + URLEncoder.encode(JsonUtils.toJson(filter), StandardCharsets.UTF_8));

         ChildRecordListData widgetData = new ChildRecordListData(widgetLabel, queryOutput, rightTable, tablePath, viewAllLink, totalRows);
         widgetData.setOmitFieldNames(omitFieldNames);

         //////////////////////////////////////////////////////////////////////////////////////////////////////
         // todo - think about - should we check if user has permission on the child table here?             //
         // at first it seems like an obvious "yes" - but - for cases where the child is a managed           //
         // association (e.g., you're adding children on the parent record's insert/edit screen - in that    //
         // case would we want to allow adding children even if the child table wasn't directly available?   //
         // PermissionsHelper.hasTablePermission(input, rightTable.getName(), TablePermissionSubType.INSERT) //
         //////////////////////////////////////////////////////////////////////////////////////////////////////
         if(BooleanUtils.isTrue(ValueUtils.getValueAsBoolean(input.getQueryParams().get("canAddChildRecord"))))
         {
            widgetData.setCanAddChildRecord(true);

            //////////////////////////////////////////////////////////
            // new child records must have values from the join-ons //
            //////////////////////////////////////////////////////////
            Map<String, Serializable> defaultValuesForNewChildRecords = new HashMap<>();
            if(primaryRecord != null)
            {
               for(JoinOn joinOn : join.getJoinOns())
               {
                  defaultValuesForNewChildRecords.put(joinOn.getRightField(), primaryRecord.getValue(joinOn.getLeftField()));
               }
            }

            widgetData.setDefaultValuesForNewChildRecords(defaultValuesForNewChildRecords);

            if(widgetMetaDataDefaultValues.containsKey("disabledFieldsForNewChildRecords"))
            {
               @SuppressWarnings("unchecked")
               Set<String> disabledFieldsForNewChildRecords = (Set<String>) widgetMetaDataDefaultValues.get("disabledFieldsForNewChildRecords");
               widgetData.setDisabledFieldsForNewChildRecords(disabledFieldsForNewChildRecords);
            }
            else
            {
               /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
               // if there are no disabled fields specified - then normally any fields w/ a default value get implicitly disabled //
               // but - if we didn't look-up the primary record, then we'll want to explicit disable fields from joins            //
               /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
               if(primaryRecord == null)
               {
                  Set<String> implicitlyDisabledFields = new HashSet<>();
                  widgetData.setDisabledFieldsForNewChildRecords(implicitlyDisabledFields);
                  for(JoinOn joinOn : join.getJoinOns())
                  {
                     implicitlyDisabledFields.add(joinOn.getRightField());
                  }
               }
            }

            if(widgetMetaDataDefaultValues.containsKey("defaultValuesForNewChildRecordsFromParentFields"))
            {
               @SuppressWarnings("unchecked")
               Map<String, String> defaultValuesForNewChildRecordsFromParentFields = (Map<String, String>) widgetMetaDataDefaultValues.get("defaultValuesForNewChildRecordsFromParentFields");
               widgetData.setDefaultValuesForNewChildRecordsFromParentFields(defaultValuesForNewChildRecordsFromParentFields);
            }
         }

         widgetData.setAllowRecordEdit(BooleanUtils.isTrue(ValueUtils.getValueAsBoolean(input.getQueryParams().get("allowRecordEdit"))));
         widgetData.setAllowRecordDelete(BooleanUtils.isTrue(ValueUtils.getValueAsBoolean(input.getQueryParams().get("allowRecordDelete"))));
         widgetData.setIncludeExposedJoinTables(includeExposedJoinTables);

         List<String> onlyIncludeFieldNames = (List<String>) widgetMetaDataDefaultValues.get("onlyIncludeFieldNames");
         if(onlyIncludeFieldNames != null)
         {
            widgetData.setOnlyIncludeFieldNames(onlyIncludeFieldNames);
         }

         return (new RenderWidgetOutput(widgetData));
      }
      catch(Exception e)
      {
         LOG.warn("Error rendering child record list", e, logPair("widgetName", () -> input.getWidgetMetaData().getName()));
         throw (e);
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static class ChildRecordListWidgetValidator implements QInstanceValidatorPluginInterface<QWidgetMetaDataInterface>
   {

      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public void validate(QWidgetMetaDataInterface widgetMetaData, QInstance qInstance, QInstanceValidator qInstanceValidator)
      {
         String prefix = "Widget " + widgetMetaData.getName() + ": ";

         //////////////////////////////////
         // make sure join name is given //
         //////////////////////////////////
         String joinName = ValueUtils.getValueAsString(CollectionUtils.nonNullMap(widgetMetaData.getDefaultValues()).get("joinName"));
         if(qInstanceValidator.assertCondition(StringUtils.hasContent(joinName), prefix + "defaultValue for joinName must be given"))
         {
            ///////////////////////////
            // make sure join exists //
            ///////////////////////////
            QJoinMetaData join = qInstance.getJoin(joinName);
            if(qInstanceValidator.assertCondition(join != null, prefix + "No join named " + joinName + " exists in the instance"))
            {
               //////////////////////////////////////////////////////////////////////////////////
               // if there's a manageAssociationName, make sure the table has that association //
               //////////////////////////////////////////////////////////////////////////////////
               String manageAssociationName = ValueUtils.getValueAsString(widgetMetaData.getDefaultValues().get("manageAssociationName"));
               if(StringUtils.hasContent(manageAssociationName))
               {
                  validateAssociationName(prefix, manageAssociationName, join, qInstance, qInstanceValidator);
               }
            }
         }
      }



      /***************************************************************************
       **
       ***************************************************************************/
      private void validateAssociationName(String prefix, String manageAssociationName, QJoinMetaData join, QInstance qInstance, QInstanceValidator qInstanceValidator)
      {
         ///////////////////////////////////
         // make sure join's table exists //
         ///////////////////////////////////
         QTableMetaData table = qInstance.getTable(join.getLeftTable());
         if(table == null)
         {
            qInstanceValidator.getErrors().add(prefix + "Unable to validate manageAssociationName, as table [" + join.getLeftTable() + "] on left-side table of join [" + join.getName() + "] does not exist.");
         }
         else
         {
            if(CollectionUtils.nonNullList(table.getAssociations()).stream().noneMatch(a -> manageAssociationName.equals(a.getName())))
            {
               qInstanceValidator.getErrors().add(prefix + "an association named [" + manageAssociationName + "] does not exist on table [" + join.getLeftTable() + "]");
            }
         }
      }
   }
}
