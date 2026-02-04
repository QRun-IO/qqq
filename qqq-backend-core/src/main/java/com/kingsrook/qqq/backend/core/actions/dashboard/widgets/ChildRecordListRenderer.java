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
import com.kingsrook.qqq.backend.core.utils.ObjectUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import com.kingsrook.qqq.backend.core.utils.collections.ListBuilder;
import com.kingsrook.qqq.backend.core.utils.collections.MutableList;
import org.apache.commons.lang3.BooleanUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Generic widget-renderer for display a list of child records.
 *
 * <p>Includes a Builder class ({@see ChildRecordListRenderer.Builder}) for configuring
 * the meta-data corresponding to renderer.</p>
 *
 * <p>The widget is based around a Join, which, in the normal mode of operation,
 * should have the parent-table on its left side, and the child table on its right side.
 * However, the widget supports a "flipJoin" setting, which can be used to treat the
 * right-side of the join as the parent table, and the left-side as the child table.</p>
 *
 * <p>Several configurations can be controlled by setting values in the widgetMetaData's
 * defaultValues map.  There are a series of public static final String keys for those
 * slots in the map.  There are also corresponding fluent-setters for each of these
 * options in the Builder class ({@see ChildRecordListRenderer.Builder}).</p>
 *******************************************************************************/
public class ChildRecordListRenderer extends AbstractWidgetRenderer
{
   private static final QLogger LOG = QLogger.getLogger(ChildRecordListRenderer.class);

   public static final String KEY_JOIN_NAME                                               = "joinName";
   public static final String KEY_FLIP_JOIN                                               = "flipJoin";
   public static final String KEY_MAX_ROWS                                                = "maxRows";
   public static final String KEY_CAN_ADD_CHILD_RECORD                                    = "canAddChildRecord";
   public static final String KEY_DISABLED_FIELDS_FOR_NEW_CHILD_RECORDS                   = "disabledFieldsForNewChildRecords";
   public static final String KEY_DEFAULT_VALUES_FOR_NEW_CHILD_RECORDS_FROM_PARENT_FIELDS = "defaultValuesForNewChildRecordsFromParentFields";
   public static final String KEY_MANAGE_ASSOCIATION_NAME                                 = "manageAssociationName";
   public static final String KEY_OMIT_FIELD_NAMES                                        = "omitFieldNames";
   public static final String KEY_ONLY_INCLUDE_FIELD_NAMES                                = "onlyIncludeFieldNames";
   public static final String KEY_KEEP_JOIN_FIELD                                         = "keepJoinField";
   public static final String KEY_QUERY_JOINS                                             = "queryJoins";
   public static final String KEY_BASE_FILTER                                             = "baseFilter";
   public static final String KEY_ORDER_BY                                                = "orderBy";



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
         .withDefaultValue(KEY_JOIN_NAME, join.getName())
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
       * Indicate that the join being used needs to be "flipped" - e.g., to treat the
       * right-side of the join as the parent table, and the left-side as the child table.
       *
       * <p>internally this method sets the widgetMetaData defaultValue with key:
       * KEY_FLIP_JOIN</p>
       *******************************************************************************/
      public Builder withFlipJoin(Boolean flipJoin)
      {
         widgetMetaData.withDefaultValue(KEY_FLIP_JOIN, flipJoin);
         return (this);
      }



      /*******************************************************************************
       * set the max rows to be included in the widget.  Wise to always set this, to
       * avoid unbounded query (todo - add a system or instance wide default?)
       *
       * <p>internally this method sets the widgetMetaData defaultValue with key:
       * KEY_MAX_ROWS</p>
       *******************************************************************************/
      public Builder withMaxRows(Integer maxRows)
      {
         widgetMetaData.withDefaultValue(KEY_MAX_ROWS, maxRows);
         return (this);
      }



      /*******************************************************************************
       * Tell the UI that it should show an add-new button, to create records in the
       * child table.
       *
       * <p>internally this method sets the widgetMetaData defaultValue with key:
       * KEY_CAN_ADD_CHILD_RECORD</p>
       *******************************************************************************/
      public Builder withCanAddChildRecord(boolean b)
      {
         widgetMetaData.withDefaultValue(KEY_CAN_ADD_CHILD_RECORD, b);
         return (this);
      }



      /*******************************************************************************
       * For the case where you canAddChildRecord is true, this field is a set of field
       * names in the child table that should be disabled in the add-child-record UI.
       *
       * <p>internally this method sets the widgetMetaData defaultValue with key:
       * KEY_DISABLED_FIELDS_FOR_NEW_CHILD_RECORDS</p>
       *******************************************************************************/
      public Builder withDisabledFieldsForNewChildRecords(Set<String> disabledFieldsForNewChildRecords)
      {
         widgetMetaData.withDefaultValue(KEY_DISABLED_FIELDS_FOR_NEW_CHILD_RECORDS, new HashSet<>(disabledFieldsForNewChildRecords));
         return (this);
      }



      /*******************************************************************************
       * For the case where you canAddChildRecord is true, this field is a Map of
       * field names from the parent table to field names in the parent table, that
       * makes a default value for the child record come from the parent record.
       * By default, the join field in the child will get set from the parent, but this
       * map allows additional values to also come from the parent.
       *
       * <p>Useful for, e.g., a security field that should be the same in parent and
       * child (e.g., <pre>Map.of("organizationId", "organizationId")</pre>) or maybe a
       * secondary foreign key(?) like <pre>Map.of("warehouseId", "id")</pre> (to make
       * the child's warehouseId equal the parent's id).</p>
       *
       * <p>Common to use along with withDisabledFieldsForNewChildRecords, if the
       * fields with default values should be disabled (read-only) in the UI.</p>
       *
       * <p>internally this method sets the widgetMetaData defaultValue with key:
       * KEY_DEFAULT_VALUES_FOR_NEW_CHILD_RECORDS_FROM_PARENT_FIELDS</p>
       *******************************************************************************/
      public Builder withDefaultValuesForNewChildRecordsFromParentFields(Map<String, String> defaultValuesForNewChildRecordsFromParentFields)
      {
         widgetMetaData.withDefaultValue(KEY_DEFAULT_VALUES_FOR_NEW_CHILD_RECORDS_FROM_PARENT_FIELDS, new HashMap<>(defaultValuesForNewChildRecordsFromParentFields));
         return (this);
      }



      /*******************************************************************************
       * For a widget that is meant to manage a list of associated child records, e.g.,
       * on an insert/edit screen, that association name must be set in this property.
       *
       * <p>internally this method sets the widgetMetaData defaultValue with key:
       * KEY_MANAGE_ASSOCIATION_NAME</p>
       *******************************************************************************/
      public Builder withManageAssociationName(String manageAssociationName)
      {
         widgetMetaData.withDefaultValue(KEY_MANAGE_ASSOCIATION_NAME, manageAssociationName);
         return (this);
      }



      /*******************************************************************************
       * The default behavior is to show all fields in the table (and any included
       * exposed joins) on the frontend.  This option allows you to specify a subset
       * of fields to omit - to send ot the frontend for not displaying.
       *
       * <p>internally this method sets the widgetMetaData defaultValue with key:
       * KEY_OMIT_FIELD_NAMES</p>
       *******************************************************************************/
      public Builder withOmitFieldNames(List<String> omitFieldNames)
      {
         ArrayList<String> arrayList = CollectionUtils.useOrWrap(omitFieldNames, new TypeToken<>() {});
         widgetMetaData.withDefaultValue(KEY_OMIT_FIELD_NAMES, arrayList);
         return (this);
      }



      /*******************************************************************************
       * The default behavior is to show all fields in the table (and any included
       * exposed joins) on the frontend.  This option allows you to specify a subset
       * of field names, that get set in {@link ChildRecordListData#withOnlyIncludeFieldNames(List)},
       * which a frontend should respect to limit what's shown.
       *
       * <p>internally this method sets the widgetMetaData defaultValue with key:
       * KEY_ONLY_INCLUDE_FIELD_NAMES</p>
       *******************************************************************************/
      public Builder withOnlyIncludeFieldNames(List<String> onlyIncludeFieldNames)
      {
         ArrayList<String> arrayList = CollectionUtils.useOrWrap(onlyIncludeFieldNames, new TypeToken<>() {});
         widgetMetaData.withDefaultValue(KEY_ONLY_INCLUDE_FIELD_NAMES, arrayList);
         return (this);
      }



      /*******************************************************************************
       * The default behavior makes the join-field from the child table be sent to the
       * frontend in the {@link ChildRecordListData#withOmitFieldNames(List)} - e.g.,
       * so that it isn't shown (as it's redundant on all rows).
       *
       * <p>You can set this property to true to reverse that course, and not put
       * the join field in the omit list, so it does display.</p>
       *
       * <p>internally this method sets the widgetMetaData defaultValue with key:
       * KEY_KEEP_JOIN_FIELD</p>
       *******************************************************************************/
      public Builder withKeepJoinField(Boolean keepJoinField)
      {
         widgetMetaData.withDefaultValue(KEY_KEEP_JOIN_FIELD, keepJoinField);
         return (this);
      }



      /*******************************************************************************
       * Add 1 or more query joins to the widget.  You'll probably want those to be
       * marked as .withSelect(true), to include their values (unless you just want to
       * cause an inner join to make records not be found maybe).  They'll need to
       * correspond to exposed joins on the table, for a frontend to display them.
       * Any joins listed in here will have their tables added to
       * {@link ChildRecordListData#withIncludeExposedJoinTables(List)}.
       *
       * <p>internally this method sets the widgetMetaData defaultValue with key:
       * KEY_QUERY_JOINS</p>
       *******************************************************************************/
      public Builder withQueryJoins(List<QueryJoin> queryJoins)
      {
         ArrayList<QueryJoin> queryJoinsArrayList = CollectionUtils.useOrWrap(queryJoins, new TypeToken<>() {});
         widgetMetaData.withDefaultValue(KEY_QUERY_JOINS, queryJoinsArrayList);
         return (this);
      }



      /*******************************************************************************
       * Set a "base filter" to be used by the query that selects the child records.
       * This can be used to add, for example, additional criteria, such as filtering
       * out records of a given status.  Or, for example, if the selection might otherwise
       * include the record being viewed, to exclude it (using: NOT_EQUALS, "${input.id}")
       *
       * <p>internally this method sets the widgetMetaData defaultValue with key:
       * KEY_BASE_FILTER</p>
       *******************************************************************************/
      public Builder withBaseFilter(QQueryFilter baseFilter)
      {
         widgetMetaData.withDefaultValue(KEY_BASE_FILTER, baseFilter);
         return (this);
      }



      /*******************************************************************************
       * Set a list of OrderBys to be used for the query that selects the child records.
       *
       * <p>internally this method sets the widgetMetaData defaultValue with key:
       * KEY_ORDER_BY</p>
       *******************************************************************************/
      public Builder withOrderBys(List<QFilterOrderBy> orderBys)
      {
         ArrayList<QFilterOrderBy> orderBysArrayList = CollectionUtils.useOrWrap(orderBys, new TypeToken<>() {});
         widgetMetaData.withDefaultValue(KEY_ORDER_BY, orderBysArrayList);
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
         String widgetLabel = input.getQueryParams().get("widgetLabel");
         String id          = input.getQueryParams().get("id");

         String        joinName = input.getQueryParams().get(KEY_JOIN_NAME);
         QJoinMetaData join     = QContext.getQInstance().getJoin(joinName);

         ///////////////////////////////////////////////////////////////////////////////
         // flip the join (which takes care of flipping the join-ons) if so requested //
         ///////////////////////////////////////////////////////////////////////////////
         if(BooleanUtils.isTrue(ValueUtils.getValueAsBoolean(input.getQueryParams().get(KEY_FLIP_JOIN))))
         {
            join = join.flip();
         }

         QTableMetaData leftTable   = QContext.getQInstance().getTable(join.getLeftTable());
         QTableMetaData rightTable  = QContext.getQInstance().getTable(join.getRightTable());

         Map<String, Serializable> widgetMetaDataDefaultValues = input.getWidgetMetaData().getDefaultValues();
         List<String> omitFieldNames = (List<String>) widgetMetaDataDefaultValues.get(KEY_OMIT_FIELD_NAMES);
         if(omitFieldNames == null)
         {
            omitFieldNames = new ArrayList<>();
         }
         else
         {
            omitFieldNames = new MutableList<>(omitFieldNames);
         }

         Integer maxRows = null;
         if(StringUtils.hasContent(input.getQueryParams().get(KEY_MAX_ROWS)))
         {
            maxRows = ValueUtils.getValueAsInteger(input.getQueryParams().get(KEY_MAX_ROWS));
         }
         else if(widgetMetaDataDefaultValues.containsKey(KEY_MAX_ROWS))
         {
            maxRows = ValueUtils.getValueAsInteger(widgetMetaDataDefaultValues.get(KEY_MAX_ROWS));
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

            ////////////////////////////////////////////////////////////////////////////////////////////////
            // if widget default values specify a base filter - use clone of it - else start a new filter //
            ////////////////////////////////////////////////////////////////////////////////////////////////
            Serializable baseFilter = widgetMetaDataDefaultValues.get(KEY_BASE_FILTER);
            if(baseFilter instanceof QQueryFilter baseQueryFilter)
            {
               filter = baseQueryFilter.clone();

               ////////////////////////////////////////////////
               // support, e.g., ${input.id} criteria values //
               ////////////////////////////////////////////////
               filter.interpretValues(new HashMap<>(input.getQueryParams()));
            }
            else
            {
               filter = new QQueryFilter();
            }

            //////////////////////////////////////////////////////////////
            // add criteria for the table on the right side of the join //
            //////////////////////////////////////////////////////////////
            for(JoinOn joinOn : join.getJoinOns())
            {
               String rightField = joinOn.getRightField();
               filter.addCriteria(new QFilterCriteria(rightField, QCriteriaOperator.EQUALS, ListBuilder.of(primaryRecord.getValue(joinOn.getLeftField()))));

               Boolean keepJoinField = ValueUtils.getValueAsBoolean(widgetMetaDataDefaultValues.get(KEY_KEEP_JOIN_FIELD));
               if(!BooleanUtils.isTrue(keepJoinField))
               {
                  omitFieldNames.add(rightField);
               }
            }

            Serializable orderBy = widgetMetaDataDefaultValues.get(KEY_ORDER_BY);
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

            Serializable queryJoinsSerializable = widgetMetaDataDefaultValues.get(KEY_QUERY_JOINS);
            if(queryJoinsSerializable instanceof List queryJoinsList)
            {
               queryInput.withQueryJoins(queryJoinsList);

               includeExposedJoinTables = new ArrayList<>();
               for(QueryJoin queryJoin : queryInput.getQueryJoins())
               {
                  includeExposedJoinTables.add(queryJoin.getJoinTable());
               }
            }

            queryOutput = executeQuery(queryInput);

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
         if(BooleanUtils.isTrue(ValueUtils.getValueAsBoolean(input.getQueryParams().get(KEY_CAN_ADD_CHILD_RECORD))))
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

            if(widgetMetaDataDefaultValues.containsKey(KEY_DISABLED_FIELDS_FOR_NEW_CHILD_RECORDS))
            {
               @SuppressWarnings("unchecked")
               Set<String> disabledFieldsForNewChildRecords = (Set<String>) widgetMetaDataDefaultValues.get(KEY_DISABLED_FIELDS_FOR_NEW_CHILD_RECORDS);
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

            if(widgetMetaDataDefaultValues.containsKey(KEY_DEFAULT_VALUES_FOR_NEW_CHILD_RECORDS_FROM_PARENT_FIELDS))
            {
               @SuppressWarnings("unchecked")
               Map<String, String> defaultValuesForNewChildRecordsFromParentFields = (Map<String, String>) widgetMetaDataDefaultValues.get(KEY_DEFAULT_VALUES_FOR_NEW_CHILD_RECORDS_FROM_PARENT_FIELDS);
               widgetData.setDefaultValuesForNewChildRecordsFromParentFields(defaultValuesForNewChildRecordsFromParentFields);
            }
         }

         widgetData.setAllowRecordEdit(BooleanUtils.isTrue(ValueUtils.getValueAsBoolean(input.getQueryParams().get("allowRecordEdit"))));
         widgetData.setAllowRecordDelete(BooleanUtils.isTrue(ValueUtils.getValueAsBoolean(input.getQueryParams().get("allowRecordDelete"))));
         widgetData.setIncludeExposedJoinTables(includeExposedJoinTables);

         List<String> onlyIncludeFieldNames = (List<String>) widgetMetaDataDefaultValues.get(KEY_ONLY_INCLUDE_FIELD_NAMES);
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
    * execute the query input, producing a query output.
    *
    * Note, this method exists as a point where a subclass could override
    * to customize values in the query input or even the rows selected by
    * the query.
    ***************************************************************************/
   protected QueryOutput executeQuery(QueryInput queryInput) throws QException
   {
      return new QueryAction().execute(queryInput);
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

         Map<String, Serializable> widgetMetaDataDefaultValues = CollectionUtils.nonNullMap(widgetMetaData.getDefaultValues());

         //////////////////////////////////
         // make sure join name is given //
         //////////////////////////////////
         String joinName = ValueUtils.getValueAsString(widgetMetaDataDefaultValues.get(KEY_JOIN_NAME));
         if(qInstanceValidator.assertCondition(StringUtils.hasContent(joinName), prefix + "defaultValue for " + KEY_JOIN_NAME + " must be given"))
         {
            ///////////////////////////
            // make sure join exists //
            ///////////////////////////
            QJoinMetaData join = qInstance.getJoin(joinName);
            if(qInstanceValidator.assertCondition(join != null, prefix + "No join named " + joinName + " exists in the instance"))
            {
               ///////////////////////////////////
               // flip the join if so requested //
               ///////////////////////////////////
               if(BooleanUtils.isTrue(ValueUtils.getValueAsBoolean(widgetMetaDataDefaultValues.get(KEY_FLIP_JOIN))))
               {
                  join = join.flip();
               }

               QTableMetaData rightTable = qInstance.getTable(join.getRightTable());

               ///////////////////////////////////////////////////////////////////////////////////////
               // if there's a manageAssociationName, make sure the left-table has that association //
               ///////////////////////////////////////////////////////////////////////////////////////
               String manageAssociationName = ValueUtils.getValueAsString(widgetMetaDataDefaultValues.get(KEY_MANAGE_ASSOCIATION_NAME));
               if(StringUtils.hasContent(manageAssociationName))
               {
                  validateAssociationName(prefix, manageAssociationName, join, qInstance, qInstanceValidator);
               }

               /////////////////////////////////////////////////////////////////////
               // if there are query joins, grab them, to use in later validation //
               /////////////////////////////////////////////////////////////////////
               List<QueryJoin> queryJoinList          = new ArrayList<>();
               Serializable    queryJoinsSerializable = widgetMetaDataDefaultValues.get(KEY_QUERY_JOINS);
               if(queryJoinsSerializable instanceof List l)
               {
                  for(Object o : l)
                  {
                     if(o instanceof QueryJoin queryJoin)
                     {
                        queryJoinList.add(queryJoin);
                     }
                     else
                     {
                        qInstanceValidator.getErrors().add(prefix + KEY_QUERY_JOINS + " has an element which is not a QueryJoin (is: " + (ObjectUtils.tryElse(() -> o.getClass(), "?")) + ")");
                     }
                  }
               }
               else
               {
                  qInstanceValidator.assertCondition(queryJoinsSerializable == null, prefix + KEY_QUERY_JOINS + " is not an instance of List (is: " + (ObjectUtils.tryElse(() -> queryJoinsSerializable.getClass(), "")) + ")");
               }

               ///////////////////////////////////////////////////////////////////////
               // if there's a base filter, validate it for the table & query joins //
               ///////////////////////////////////////////////////////////////////////
               Serializable baseFilter = widgetMetaDataDefaultValues.get(KEY_BASE_FILTER);
               if(baseFilter instanceof QQueryFilter baseQueryFilter)
               {
                  if(rightTable != null)
                  {
                     qInstanceValidator.validateQueryFilter(qInstance, prefix + KEY_BASE_FILTER + ": ", rightTable, baseQueryFilter, queryJoinList);
                  }
               }
               else
               {
                  qInstanceValidator.assertCondition(baseFilter == null, prefix + KEY_BASE_FILTER + " is not an instance of QQueryFilter (is: " + (ObjectUtils.tryElse(() -> baseFilter.getClass(), "?")) + ")");
               }

               ///////////////////////////////////////////
               // if order bys are given, validate them //
               ///////////////////////////////////////////
               Serializable orderBy = widgetMetaDataDefaultValues.get(KEY_ORDER_BY);
               if(orderBy instanceof List orderByList)
               {
                  boolean areObjectTypesCorrect = true;
                  if(!orderByList.isEmpty() && rightTable != null)
                  {
                     QQueryFilter queryFilter = new QQueryFilter();
                     for(Object o : orderByList)
                     {
                        if(o instanceof QFilterOrderBy qFilterOrderBy)
                        {
                           queryFilter.addOrderBy(qFilterOrderBy);
                        }
                        else
                        {
                           qInstanceValidator.getErrors().add(prefix + KEY_ORDER_BY + " has an element which is not a QFilterOrderBy (is: " + (ObjectUtils.tryElse(() -> o.getClass(), "?")) + ")");
                           areObjectTypesCorrect = false;
                        }
                     }

                     if(areObjectTypesCorrect)
                     {
                        qInstanceValidator.validateQueryFilter(qInstance, prefix + KEY_ORDER_BY + ": ", rightTable, queryFilter, queryJoinList);
                     }
                  }
               }
               else
               {
                  qInstanceValidator.assertCondition(orderBy == null, prefix + KEY_ORDER_BY + " is not an instance of List (is: " + (ObjectUtils.tryElse(() -> baseFilter.getClass(), "?")) + ")");
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
