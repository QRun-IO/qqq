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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import com.google.gson.reflect.TypeToken;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.values.QValueFormatter;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.instances.validation.plugins.QInstanceValidatorPluginInterface;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetOutput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.RowBuilderData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.WidgetType;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.AbstractWidgetMetaDataBuilder;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaDataInterface;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.frontend.QFrontendFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import org.apache.commons.lang3.BooleanUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 * Generic widget for a form that lets a user add rows.
 *
 * Setup is managed by default values in the widgetMetaData, codified by methods
 * on the inner {@link Builder} class.
 *
 * <p>
 * Use cases:
 * <ul>
 * <li>On table view and insert/edit screens:
 *    <ul>
 *    <li>enabled by setting isForRecordViewAndEditScreen = true</li>
 *    <li>will submit records as json in a field identified as outputFieldName</li>
 *    <li>OR - with associated child records named: associationName</li>
 *    </ul>
 * </li>
 * <li>In processes:
 *    <ul>
 *    <li>using outputFieldName to define the process value that'll be the json encoded
 *    records.</li>
 *    </ul>
 * </ul>
 * </p>
 *
 * General properties are:
 * <dl>
 * <dt>fields</dt><dd>list of QFieldMetaData that define what fields make up a row.</dd>
 * <dt>isEditable</dt><dd>does not apply if isForRecordViewAndEditScreen - in which case,
 * edit-ability is driven by whether on view or edit screen) - determine if rows
 * are editable or not (e.g., can be used as just a view-only list of rows).</dd>
 * <dt>useModalEditor</dt><dd>only applies if isEditable or isForRecordViewAndEditScreen
 * (and then only on record edit screen) - by default, records can be edited
 * inline on the form - but with this property set to true, a modal editor is used.</dd>
 * <dt>mayReorderRows</dt><dd>control if user is shown drag handle to reorder rows when
 * editing.  Requires an orderByFieldName to be specified.</dd>
 * <dt>orderByFieldName</dt><dd>if using mayReorderRows = true, then this field receives
 * the ordering - e.g., 1, 2, 3.</dd>
 * <dt>requiresFormWrapper</dt><dd>In material dashboard, if an error is shown
 * referencing a FormikContext, set this property to true to provide one.</dd>
 * </dl>
 *******************************************************************************/
public class RowBuilderWidgetRenderer extends AbstractWidgetRenderer
{
   private static final QLogger LOG = QLogger.getLogger(RowBuilderWidgetRenderer.class);



   /*******************************************************************************
    * Factory to make a new Builder for widgetMetaData of this type.
    * @param widgetName unique name for the widget with your QInstance
    * @return Builder object, where additional properties can be set to control
    * widget behavior.  Ultimately to have
    * {@link AbstractWidgetMetaDataBuilder#getWidgetMetaData()} called, to put
    * the meta-data for the widget in a QInstance.
    *******************************************************************************/
   public static Builder widgetMetaDataBuilder(String widgetName)
   {
      QWidgetMetaData baseWidgetMetaData = new QWidgetMetaData()
         .withName(widgetName)
         .withType(WidgetType.ROW_BUILDER.getType())
         .withIsCard(true)
         .withCodeReference(new QCodeReference(RowBuilderWidgetRenderer.class))
         .withValidatorPlugin(new RowBuilderWidgetValidator())

         /////////////////////////////////////////////////////////////////////////////////
         // used in EntityForm.tsx to make this widget available on record edit screens //
         /////////////////////////////////////////////////////////////////////////////////
         .withDefaultValue("includeOnRecordEditScreen", true);

      Builder builder = new Builder(baseWidgetMetaData);

      //////////////////////////////////////////////////////////////////
      // set default values for these widget-type-specific properties //
      //////////////////////////////////////////////////////////////////
      builder.withMayReorderRows(false)
         .withIsForRecordViewAndEditScreen(false)
         .withIsEditable(false)
         .withUseModalEditor(false);

      return (builder);
   }



   /*******************************************************************************
    * Widget Meta Data Builder for this widget type.
    *******************************************************************************/
   public static class Builder extends AbstractWidgetMetaDataBuilder
   {

      /*******************************************************************************
       * Constructor
       *
       *******************************************************************************/
      public Builder(QWidgetMetaData widgetMetaData)
      {
         super(widgetMetaData);
      }



      /*******************************************************************************
       * set name for the widget, though generally would
       *******************************************************************************/
      public Builder withName(String name)
      {
         widgetMetaData.setName(name);
         return (this);
      }



      /*******************************************************************************
       * set the label shown in UI for the widget
       *******************************************************************************/
      public Builder withLabel(String label)
      {
         widgetMetaData.setLabel(label);
         return (this);
      }



      /*******************************************************************************
       * set the fields for the widget
       *******************************************************************************/
      public Builder withFields(List<QFieldMetaData> fields)
      {
         ArrayList<QFieldMetaData> arrayList = CollectionUtils.useOrWrap(fields, new TypeToken<>() {});
         widgetMetaData.withDefaultValue("fields", arrayList);
         return (this);
      }



      /*******************************************************************************
       * set whether edits happen inline, or in modal editor.
       *******************************************************************************/
      public Builder withUseModalEditor(Boolean useModalEditor)
      {
         widgetMetaData.withDefaultValue("useModalEditor", useModalEditor);
         return (this);
      }



      /*******************************************************************************
       * set if the data is editable or not.  note that, if isForRecordViewAndEditScreen,
       * this is ignored, as its whether you're on the view or edit screen that determines
       * isEditable.
       *******************************************************************************/
      public Builder withIsEditable(Boolean isEditable)
      {
         widgetMetaData.withDefaultValue("isEditable", isEditable);
         return (this);
      }



      /*******************************************************************************
       * set if user can reorder rows.  if set, then orderByFieldName must also be set.
       *******************************************************************************/
      public Builder withMayReorderRows(Boolean mayReorderRows)
      {
         widgetMetaData.withDefaultValue("mayReorderRows", mayReorderRows);
         return (this);
      }



      /*******************************************************************************
       * set the field in which the sequence/order-by value is stored. only applies if
       * mayReorderRows is true.
       *******************************************************************************/
      public Builder withOrderByFieldName(String orderByFieldName)
      {
         widgetMetaData.withDefaultValue("orderByFieldName", orderByFieldName);
         return (this);
      }



      /*******************************************************************************
       * set if the widget it meant for use on record view/edit screens, in which case,
       * that determines if editable or not.
       *******************************************************************************/
      public Builder withIsForRecordViewAndEditScreen(Boolean isForRecordViewAndEditScreen)
      {
         widgetMetaData.withDefaultValue("isForRecordViewAndEditScreen", isForRecordViewAndEditScreen);
         return (this);
      }



      /*******************************************************************************
       * set if the row builder form requires a "wrapper" form to be output in the
       * material dashboard frontend (use if FormikContext error appears)
       *******************************************************************************/
      public Builder withRequiresFormWrapper(Boolean requiresFormWrapper)
      {
         widgetMetaData.withDefaultValue("requiresFormWrapper", requiresFormWrapper);
         return (this);
      }



      /*******************************************************************************
       * set the fieldName that the records are written to when data comes out of the
       * widget, e.g., into process values, or a parent-record.
       *******************************************************************************/
      public Builder withOutputFieldName(String outputFieldName)
      {
         widgetMetaData.withDefaultValue("outputFieldName", outputFieldName);
         return (this);
      }



      /*******************************************************************************
       * for the use-case on a record view/edit screen, set the association name
       * that the records are for.
       *******************************************************************************/
      public Builder withAssociationName(String associationName)
      {
         widgetMetaData.withDefaultValue("associationName", associationName);
         return (this);
      }



      /*******************************************************************************
       * for the use-case on a record view/edit screen, set the name of the "parent"
       * table - e.g., the one that has the association.
       *******************************************************************************/
      public Builder withParentTableName(String parentTableName)
      {
         widgetMetaData.withDefaultValue("parentTableName", parentTableName);
         return (this);
      }



      /*******************************************************************************
       * for the use-case on a record view/edit screen, set the name of the "parent"
       * table - e.g., the one that has the association.
       *
       * @param defaultValuesForNewRowsFromParentRecord map with keys of 'fromFieldName'
       * (e.g., a field name in the parent record) →
       *******************************************************************************/
      public Builder withDefaultValuesForNewRowsFromParentRecord(Map<String, Serializable> defaultValuesForNewRowsFromParentRecord)
      {
         HashMap<String, Serializable> hashMap = CollectionUtils.useOrWrap(defaultValuesForNewRowsFromParentRecord, new TypeToken<>() {});
         widgetMetaData.withDefaultValue("defaultValuesForNewRowsFromParentRecord", hashMap);
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
         String recordId        = input.getQueryParams().get("id");
         String associationName = input.getQueryParams().get("associationName");
         String parentTableName = input.getQueryParams().get("parentTableName");

         ArrayList<QRecord>        records                    = new ArrayList<>();
         Map<String, Serializable> defaultValuesForNewRecords = new HashMap<>();

         ///////////////////////////////////////////////////////
         // handle the use-case of loading associated records //
         ///////////////////////////////////////////////////////
         if(recordId != null && associationName != null && parentTableName != null)
         {
            QRecord record = new GetAction().executeForRecord(new GetInput(parentTableName)
               .withPrimaryKey(recordId)
               .withIncludeAssociations(true)
               .withAssociationNamesToInclude(Set.of(associationName)));
            if(record != null)
            {
               List<QRecord> associatedRecords = CollectionUtils.nonNullMap(record.getAssociatedRecords()).get(associationName);
               if(associatedRecords != null)
               {
                  QTableMetaData        mainTable   = QContext.getQInstance().getTable(parentTableName);
                  Optional<Association> association = mainTable.getAssociationByName(associationName);
                  if(association.isPresent())
                  {
                     QTableMetaData associatedTable = QContext.getQInstance().getTable(association.get().getAssociatedTableName());
                     QValueFormatter.setDisplayValuesInRecordsIncludingPossibleValueTranslations(associatedTable, associatedRecords);
                  }
                  records = CollectionUtils.useOrWrap(associatedRecords, new TypeToken<>() {});
               }

               ////////////////////////////////////////////////////////////////////////////////////////////////////
               // if the widget meta data says we need to make values from the parent record available for child //
               // records, then copy values from the parent record into the defaultValuesForNewRecords map       //
               ////////////////////////////////////////////////////////////////////////////////////////////////////
               Serializable defaultValuesForNewRowsFromParentRecord = input.getWidgetMetaData().getDefaultValues().get("defaultValuesForNewRowsFromParentRecord");
               if(defaultValuesForNewRowsFromParentRecord instanceof Map defaultValuesForNewRowsFromBaseRecordMap)
               {
                  for(Object key : defaultValuesForNewRowsFromBaseRecordMap.keySet())
                  {
                     String       fromFieldName = ValueUtils.getValueAsString(key);
                     String       toFieldName   = ValueUtils.getValueAsString(defaultValuesForNewRowsFromBaseRecordMap.get(key));
                     Serializable value         = record.getValue(fromFieldName);
                     defaultValuesForNewRecords.put(toFieldName, value);
                  }
               }
            }
         }

         RowBuilderData widgetData = new RowBuilderData(records);
         widgetData.setDefaultValuesForNewRecords(defaultValuesForNewRecords);

         return (new RenderWidgetOutput(widgetData));
      }
      catch(Exception e)
      {
         LOG.warn("Error rendering row builder widget", e, logPair("widgetName", () -> input.getWidgetMetaData().getName()));
         throw (e);
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static class RowBuilderWidgetValidator implements QInstanceValidatorPluginInterface<QWidgetMetaDataInterface>
   {

      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public void validate(QWidgetMetaDataInterface widgetMetaData, QInstance qInstance, QInstanceValidator qInstanceValidator)
      {
         String prefix = "Widget " + widgetMetaData.getName() + ": ";

         Map<String, Serializable> defaultValues = widgetMetaData.getDefaultValues();

         Set<String> fieldNames = new HashSet<>();
         ArrayList<QFrontendFieldMetaData> frontendFields = new ArrayList<>();
         try
         {
            List<QFieldMetaData> fields = (List<QFieldMetaData>) defaultValues.get("fields");

            if(qInstanceValidator.assertCondition(CollectionUtils.nullSafeHasContents(fields), prefix + "Must have a fields list with 1 or more QFieldMetaData"))
            {
               for(QFieldMetaData fieldMetaData : fields)
               {
                  if(fieldNames.contains(fieldMetaData.getName()))
                  {
                     qInstanceValidator.getErrors().add(prefix + "Has more than 1 field named [" + fieldMetaData.getName() + "]");
                  }
                  fieldNames.add(fieldMetaData.getName());
                  qInstanceValidator.validateField(qInstance, prefix + " Field " + fieldMetaData.getName() + ": ", Optional.empty(), fieldMetaData);

                  frontendFields.add(new QFrontendFieldMetaData(fieldMetaData));
               }
            }
         }
         catch(Exception e)
         {
            qInstanceValidator.getErrors().add(prefix + "Error validating fields: " + e.getMessage());
         }

         widgetMetaData.withDefaultValue("frontendFields", frontendFields);

         //////////////////////////////
         // validate table use-cases //
         //////////////////////////////
         Boolean isForRecordViewAndEditScreen = ValueUtils.getValueAsBoolean(defaultValues.get("isForRecordViewAndEditScreen"));
         String  associationName              = ValueUtils.getValueAsString(defaultValues.get("associationName"));
         String  parentTableName              = ValueUtils.getValueAsString(defaultValues.get("parentTableName"));

         int noOfTableUseCaseFieldsTruthy = 0;
         noOfTableUseCaseFieldsTruthy += BooleanUtils.isTrue(isForRecordViewAndEditScreen) ? 1 : 0;
         noOfTableUseCaseFieldsTruthy += StringUtils.hasContent(associationName) ? 1 : 0;
         noOfTableUseCaseFieldsTruthy += StringUtils.hasContent(parentTableName) ? 1 : 0;

         if(noOfTableUseCaseFieldsTruthy == 3)
         {
            ////////////////////////////////////////////////////////
            // if all table attributes are set - so validate them //
            ////////////////////////////////////////////////////////
            QTableMetaData parentTable = qInstance.getTable(parentTableName);
            if(qInstanceValidator.assertCondition(parentTable != null, prefix + "Specified an unrecognized value for parentTableName [" + parentTableName + "]"))
            {
               qInstanceValidator.assertCondition(parentTable.getAssociationByName(associationName).isPresent(), prefix + "Specified an unrecognized association name [" + associationName + "] within table [" + parentTableName + "]");
            }
         }
         else if(noOfTableUseCaseFieldsTruthy > 0)
         {
            /////////////////////////////////////////////////////////////////////////
            // this indicates some, but incomplete config for doing table use case //
            /////////////////////////////////////////////////////////////////////////
            qInstanceValidator.getErrors().add(prefix + "Has some attributes set for use on a table, but not all.  Must have isForRecordViewAndEditScreen=true, plus parentTableName and associationName");
         }

         ////////////////////////////////
         // validate reorder use cases //
         ////////////////////////////////
         Boolean mayReorderRows   = ValueUtils.getValueAsBoolean(defaultValues.get("mayReorderRows"));
         String  orderByFieldName = ValueUtils.getValueAsString(defaultValues.get("orderByFieldName"));

         if(BooleanUtils.isTrue(mayReorderRows))
         {
            if(qInstanceValidator.assertCondition(StringUtils.hasContent(orderByFieldName), prefix + " missing orderByFieldName (which is required if mayReorderRows=true)"))
            {
               qInstanceValidator.assertCondition(fieldNames.contains(orderByFieldName), prefix + " specified an unrecognized value for orderByFieldName [" + orderByFieldName + "]");
            }
         }
         else
         {
            qInstanceValidator.assertCondition(!StringUtils.hasContent(orderByFieldName), prefix + " should not specify orderByFieldName unless mayReorderRows=true");
         }
      }



      /***************************************************************************
       **
       ***************************************************************************/
      private void validateAssociationName(String prefix, String associationName, QJoinMetaData join, QInstance qInstance, QInstanceValidator qInstanceValidator)
      {
         ///////////////////////////////////
         // make sure join's table exists //
         ///////////////////////////////////
         QTableMetaData table = qInstance.getTable(join.getLeftTable());
         if(table == null)
         {
            qInstanceValidator.getErrors().add(prefix + "Unable to validate associationName, as table [" + join.getLeftTable() + "] on left-side table of join [" + join.getName() + "] does not exist.");
         }
         else
         {
            if(CollectionUtils.nonNullList(table.getAssociations()).stream().noneMatch(a -> associationName.equals(a.getName())))
            {
               qInstanceValidator.getErrors().add(prefix + "an association named [" + associationName + "] does not exist on table [" + join.getLeftTable() + "]");
            }
         }
      }
   }
}
