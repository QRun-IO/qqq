/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.tables.picker;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QNotFoundException;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.picker.RelatedRecordPickerInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.picker.RelatedRecordPickerOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.CriteriaMissingInputValueBehavior;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.CriteriaOption;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSourceType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QFieldSection;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Tier;
import com.kingsrook.qqq.backend.core.model.metadata.tables.picker.QRelatedRecordPicker;
import com.kingsrook.qqq.backend.core.model.metadata.tables.picker.RelatedRecordPickerMode;
import com.kingsrook.qqq.backend.core.model.statusmessages.QStatusMessage;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/*******************************************************************************
 ** Serves related-record pickers (QRelatedRecordPicker): query the target
 ** table, prepare child rows, add them, and remove links.  Every read and write
 ** runs as the user through the standard actions (permissions, record security
 ** locks, validation, audit).  See docs/RELATED-RECORD-PICKER.md.
 *******************************************************************************/
public class RelatedRecordPickerAction
{
   public static final int DEFAULT_LIMIT = 50;
   public static final int MAX_LIMIT     = 200;

   private static final int DEFAULT_COLUMN_COUNT = 5;



   /*******************************************************************************
    ** A picker with its tables and join fields worked out.
    *******************************************************************************/
   public static class Resolved
   {
      private final QTableMetaData       table;
      private final QRelatedRecordPicker picker;
      private final QTableMetaData       targetTable;
      private final QTableMetaData       childTable;
      private final Map<String, String>  parentToChildFields;



      /*******************************************************************************
       ** Constructor
       *******************************************************************************/
      Resolved(QTableMetaData table, QRelatedRecordPicker picker, QTableMetaData targetTable, QTableMetaData childTable, Map<String, String> parentToChildFields)
      {
         this.table = table;
         this.picker = picker;
         this.targetTable = targetTable;
         this.childTable = childTable;
         this.parentToChildFields = parentToChildFields;
      }



      /*******************************************************************************
       ** The table that owns the picker.
       *******************************************************************************/
      public QTableMetaData getTable()
      {
         return (table);
      }



      /*******************************************************************************
       ** The picker.
       *******************************************************************************/
      public QRelatedRecordPicker getPicker()
      {
         return (picker);
      }



      /*******************************************************************************
       ** The table the picker chooses records of.
       *******************************************************************************/
      public QTableMetaData getTargetTable()
      {
         return (targetTable);
      }



      /*******************************************************************************
       ** The child (or join) table of the association (null for SINGLE).
       *******************************************************************************/
      public QTableMetaData getChildTable()
      {
         return (childTable);
      }



      /*******************************************************************************
       ** Parent field name to child field name, from the association's join.
       *******************************************************************************/
      public Map<String, String> getParentToChildFields()
      {
         return (parentToChildFields);
      }



      /*******************************************************************************
       ** The child fields set by the server on every new row (the parent join
       ** fields and the target key).
       *******************************************************************************/
      public Set<String> getForcedChildFields()
      {
         Set<String> forced = new LinkedHashSet<>(parentToChildFields.values());
         if(picker.getChildTargetFieldName() != null)
         {
            forced.add(picker.getChildTargetFieldName());
         }
         return (forced);
      }



      /*******************************************************************************
       ** The result columns (declared, or the target's label and T1 fields).
       *******************************************************************************/
      public List<String> getColumns()
      {
         if(CollectionUtils.nullSafeHasContents(picker.getColumns()))
         {
            return (picker.getColumns());
         }
         Set<String> columns = new LinkedHashSet<>(CollectionUtils.nonNullList(targetTable.getRecordLabelFields()));
         columns.addAll(t1FieldNames(targetTable));
         return (columns.stream().filter(name -> targetTable.getFields().containsKey(name)).limit(DEFAULT_COLUMN_COUNT).toList());
      }



      /*******************************************************************************
       ** The preview fields (declared, or the target's T1 fields).
       *******************************************************************************/
      public List<String> getPreviewFields()
      {
         if(CollectionUtils.nullSafeHasContents(picker.getPreviewFields()))
         {
            return (picker.getPreviewFields());
         }
         return (t1FieldNames(targetTable));
      }



      /*******************************************************************************
       ** The child fields of the review step (declared, or the child's required,
       ** editable fields other than the forced ones).
       *******************************************************************************/
      public List<String> getReviewFieldNames()
      {
         if(picker.getMode() != RelatedRecordPickerMode.MULTI_CHILD || childTable == null)
         {
            return (new ArrayList<>());
         }
         if(picker.getReviewFieldNames() != null)
         {
            return (picker.getReviewFieldNames());
         }
         Set<String> forced = getForcedChildFields();
         return (childTable.getFields().values().stream()
            .filter(f -> f.getIsRequired() && f.getIsEditable() && !f.getIsHidden() && !forced.contains(f.getName()) && !f.getName().equals(childTable.getPrimaryKeyField()))
            .map(QFieldMetaData::getName)
            .toList());
      }
   }



   /*******************************************************************************
    ** Find a table's picker and resolve its tables and join fields.
    **
    ** @throws QNotFoundException for an unknown table or picker
    *******************************************************************************/
   public static Resolved resolve(String tableName, String pickerName) throws QException
   {
      QInstance      qInstance = QContext.getQInstance();
      QTableMetaData table     = qInstance.getTable(tableName);
      if(table == null)
      {
         throw (new QNotFoundException("Could not find table named " + tableName + " in this instance."));
      }
      QRelatedRecordPicker picker = table.getRelatedRecordPicker(pickerName);
      if(picker == null)
      {
         throw (new QNotFoundException("Could not find related record picker named " + pickerName + " on table " + tableName + "."));
      }
      Resolved resolved = resolve(qInstance, table, picker);
      if(resolved == null)
      {
         throw (new QException("Related record picker " + pickerName + " on table " + tableName + " is not resolvable."));
      }
      return (resolved);
   }



   /*******************************************************************************
    ** Resolve a picker's tables and join fields, or null when the metadata does
    ** not resolve (the validator reports why).
    *******************************************************************************/
   public static Resolved resolve(QInstance qInstance, QTableMetaData table, QRelatedRecordPicker picker)
   {
      if(picker.getMode() == null)
      {
         return (null);
      }

      if(picker.getMode() == RelatedRecordPickerMode.SINGLE)
      {
         String         targetTableName = picker.getTargetTableName() != null ? picker.getTargetTableName() : targetFromField(qInstance, table, picker.getFieldName());
         QTableMetaData targetTable     = targetTableName == null ? null : qInstance.getTable(targetTableName);
         return (targetTable == null ? null : new Resolved(table, picker, targetTable, null, new LinkedHashMap<>()));
      }

      Association association = CollectionUtils.nonNullList(table.getAssociations()).stream()
         .filter(a -> Objects.equals(a.getName(), picker.getAssociationName()))
         .findFirst().orElse(null);
      if(association == null)
      {
         return (null);
      }
      QTableMetaData childTable = qInstance.getTable(association.getAssociatedTableName());
      QJoinMetaData  join       = qInstance.getJoin(association.getJoinName());
      if(childTable == null || join == null)
      {
         return (null);
      }

      Map<String, String> parentToChild = new LinkedHashMap<>();
      boolean             leftIsParent  = join.getLeftTable().equals(table.getName());
      for(JoinOn joinOn : CollectionUtils.nonNullList(join.getJoinOns()))
      {
         if(leftIsParent)
         {
            parentToChild.put(joinOn.getLeftField(), joinOn.getRightField());
         }
         else
         {
            parentToChild.put(joinOn.getRightField(), joinOn.getLeftField());
         }
      }

      String         targetTableName = picker.getTargetTableName() != null ? picker.getTargetTableName() : targetFromField(qInstance, childTable, picker.getChildTargetFieldName());
      QTableMetaData targetTable     = targetTableName == null ? null : qInstance.getTable(targetTableName);
      return (targetTable == null ? null : new Resolved(table, picker, targetTable, childTable, parentToChild));
   }



   /*******************************************************************************
    ** The table of a field's TABLE possible value source, or null.
    *******************************************************************************/
   private static String targetFromField(QInstance qInstance, QTableMetaData table, String fieldName)
   {
      QFieldMetaData field = fieldName == null ? null : table.getFields().get(fieldName);
      if(field == null || field.getPossibleValueSourceName() == null)
      {
         return (null);
      }
      QPossibleValueSource possibleValueSource = qInstance.getPossibleValueSource(field.getPossibleValueSourceName());
      if(possibleValueSource == null || possibleValueSource.getType() != QPossibleValueSourceType.TABLE)
      {
         return (null);
      }
      return (possibleValueSource.getTableName());
   }



   /*******************************************************************************
    ** The names of a table's T1 section fields.
    *******************************************************************************/
   static List<String> t1FieldNames(QTableMetaData table)
   {
      List<String> names = new ArrayList<>();
      for(QFieldSection section : CollectionUtils.nonNullList(table.getSections()))
      {
         if(section.getTier() == Tier.T1 && section.getFieldNames() != null)
         {
            section.getFieldNames().stream().filter(name -> table.getFields().containsKey(name)).forEach(names::add);
         }
      }
      return (names);
   }



   /*******************************************************************************
    ** Query the target table: the user's filter AND the picker's filter AND the
    ** search, one page, with hasMore (no count) and the already-linked targets.
    *******************************************************************************/
   public RelatedRecordPickerOutput query(RelatedRecordPickerInput input) throws QException
   {
      Resolved                  resolved = resolve(input.getTableName(), input.getPickerName());
      QTableMetaData            target   = resolved.getTargetTable();
      Map<String, Serializable> values   = parentValues(resolved, input);
      checkTargetRead(target);

      int limit = input.getLimit() == null ? DEFAULT_LIMIT : Math.max(1, Math.min(MAX_LIMIT, input.getLimit()));
      int skip  = input.getSkip() == null ? 0 : Math.max(0, input.getSkip());

      QQueryFilter filter = new QQueryFilter();
      if(input.getFilter() != null)
      {
         QQueryFilter userFilter = input.getFilter().clone();
         assertFieldsOnTable(userFilter, target);
         if(CollectionUtils.nullSafeHasContents(userFilter.getOrderBys()))
         {
            userFilter.getOrderBys().forEach(filter::withOrderBy);
            userFilter.setOrderBys(new ArrayList<>());
         }
         filter.addSubFilter(userFilter);
      }
      QQueryFilter pickerFilter = interpolated(resolved.getPicker().getFilter(), values);
      if(pickerFilter != null)
      {
         filter.addSubFilter(pickerFilter);
      }
      QQueryFilter searchFilter = searchFilter(target, input.getSearchTerm());
      if(searchFilter != null)
      {
         filter.addSubFilter(searchFilter);
      }
      if(CollectionUtils.nullSafeIsEmpty(filter.getOrderBys()))
      {
         filter.withOrderBy(defaultOrderBy(target));
      }
      filter.withSkip(skip).withLimit(limit + 1);

      QueryInput queryInput = new QueryInput(target.getName());
      queryInput.setFilter(filter);
      queryInput.setShouldGenerateDisplayValues(true);
      queryInput.setShouldTranslatePossibleValues(true);
      List<QRecord> records = new QueryAction().execute(queryInput).getRecords();

      RelatedRecordPickerOutput output = new RelatedRecordPickerOutput();
      output.setHasMore(records.size() > limit);
      output.setRecords(new ArrayList<>(records.subList(0, Math.min(limit, records.size()))));

      if(input.getPrimaryKey() != null && resolved.getChildTable() != null && !output.getRecords().isEmpty())
      {
         List<Serializable> ids    = output.getRecords().stream().map(r -> r.getValue(target.getPrimaryKeyField())).toList();
         Set<String>        linked = new LinkedHashSet<>();
         for(QRecord child : queryChildren(resolved, values, ids))
         {
            linked.add(String.valueOf(child.getValue(resolved.getPicker().getChildTargetFieldName())));
         }
         output.setLinkedIds(ids.stream().filter(id -> linked.contains(String.valueOf(id))).toList());
      }

      if(skip == 0 && resolved.getPicker().getDefaultFilter() != null)
      {
         output.setResolvedDefaultFilter(interpolated(resolved.getPicker().getDefaultFilter(), values));
      }
      return (output);
   }



   /*******************************************************************************
    ** Build (without saving) one child row per target, with defaults, values
    ** copied from the target, and the forced join values.
    *******************************************************************************/
   public RelatedRecordPickerOutput prepare(RelatedRecordPickerInput input) throws QException
   {
      Resolved                  resolved = resolve(input.getTableName(), input.getPickerName());
      Map<String, Serializable> values   = requireChildMode(resolved, input);
      checkChildPermission(resolved, TablePermissionSubType.INSERT);
      Map<Serializable, QRecord> targets = loadTargets(resolved, values, input.getTargetIds());

      List<QRecord> drafts = new ArrayList<>();
      for(Serializable targetId : input.getTargetIds())
      {
         drafts.add(buildChild(resolved, values, targets.get(normalizeId(resolved, targetId)), null));
      }

      RelatedRecordPickerOutput output = new RelatedRecordPickerOutput();
      output.setRecords(drafts);
      output.setReviewFieldNames(resolved.getReviewFieldNames());
      return (output);
   }



   /*******************************************************************************
    ** Insert one child row per target (or per given record), all or nothing.
    *******************************************************************************/
   public RelatedRecordPickerOutput add(RelatedRecordPickerInput input) throws QException
   {
      Resolved                  resolved    = resolve(input.getTableName(), input.getPickerName());
      Map<String, Serializable> values      = requireChildMode(resolved, input);
      String                    targetField = resolved.getPicker().getChildTargetFieldName();
      QTableMetaData            child       = resolved.getChildTable();

      List<Serializable>              targetIds  = new ArrayList<>();
      List<Map<String, Serializable>> clientRows = new ArrayList<>();
      if(CollectionUtils.nullSafeHasContents(input.getRecords()))
      {
         for(Map<String, Serializable> row : input.getRecords())
         {
            Serializable targetId = row == null ? null : row.get(targetField);
            if(targetId == null)
            {
               throw (new QBadRequestException("Every record must name its " + child.getField(targetField).getLabel() + " (" + targetField + ")."));
            }
            targetIds.add(targetId);
            clientRows.add(row);
         }
      }
      else
      {
         for(Serializable targetId : CollectionUtils.nonNullList(input.getTargetIds()))
         {
            targetIds.add(targetId);
            clientRows.add(null);
         }
      }

      checkChildPermission(resolved, TablePermissionSubType.INSERT);
      Map<Serializable, QRecord> targets = loadTargets(resolved, values, targetIds);

      List<QRecord> rows = new ArrayList<>();
      for(int i = 0; i < targetIds.size(); i++)
      {
         rows.add(buildChild(resolved, values, targets.get(normalizeId(resolved, targetIds.get(i))), clientRows.get(i)));
      }

      ////////////////////////////////////////////////////////////////////////////
      // validate every row first (required fields, record locks, unique keys), //
      // so a bad row stops the whole add before anything is written            //
      ////////////////////////////////////////////////////////////////////////////
      InsertInput validationInput = new InsertInput(child.getName()).withInputSource(QInputSource.USER);
      validationInput.setRecords(rows.stream().map(QRecord::new).toList());
      new InsertAction().performValidations(validationInput, true, false);
      throwIfErrors(validationInput.getRecords());

      InsertInput insertInput = new InsertInput(child.getName()).withInputSource(QInputSource.USER);
      insertInput.setRecords(rows);
      try(QBackendTransaction transaction = QBackendTransaction.openFor(insertInput))
      {
         insertInput.setTransaction(transaction);
         InsertOutput insertOutput = new InsertAction().execute(insertInput);
         if(insertOutput.getRecords().stream().anyMatch(r -> CollectionUtils.nullSafeHasContents(r.getErrors())))
         {
            transaction.rollback();
            throwIfErrors(insertOutput.getRecords());
         }
         transaction.commit();

         RelatedRecordPickerOutput output = new RelatedRecordPickerOutput();
         output.setRecords(insertOutput.getRecords());
         List<String> warnings = insertOutput.getRecords().stream()
            .flatMap(r -> CollectionUtils.nonNullList(r.getWarnings()).stream())
            .map(QStatusMessage::getMessage).toList();
         output.setWarnings(warnings.isEmpty() ? null : warnings);
         return (output);
      }
   }



   /*******************************************************************************
    ** Delete this parent's join rows for the given targets (LINK only).
    *******************************************************************************/
   public RelatedRecordPickerOutput remove(RelatedRecordPickerInput input) throws QException
   {
      Resolved resolved = resolve(input.getTableName(), input.getPickerName());
      if(resolved.getPicker().getMode() != RelatedRecordPickerMode.LINK)
      {
         throw (new QBadRequestException("Only a LINK picker removes links; delete child rows with the table's delete route."));
      }
      Map<String, Serializable> values = requireChildMode(resolved, input);
      checkChildPermission(resolved, TablePermissionSubType.DELETE);

      QTableMetaData     child = resolved.getChildTable();
      List<Serializable> keys  = queryChildren(resolved, values, input.getTargetIds()).stream().map(r -> r.getValue(child.getPrimaryKeyField())).toList();

      RelatedRecordPickerOutput output = new RelatedRecordPickerOutput();
      output.setDeletedCount(0);
      if(!keys.isEmpty())
      {
         DeleteInput deleteInput = new DeleteInput(child.getName()).withPrimaryKeys(new ArrayList<>(keys));
         deleteInput.setInputSource(QInputSource.USER);
         DeleteOutput deleteOutput = new DeleteAction().execute(deleteInput);
         throwIfErrors(deleteOutput.getRecordsWithErrors());
         output.setDeletedCount(deleteOutput.getDeletedRecordCount());
      }
      return (output);
   }



   /*******************************************************************************
    ** The parent's values for MULTI_CHILD/LINK, after the request checks.
    *******************************************************************************/
   private Map<String, Serializable> requireChildMode(Resolved resolved, RelatedRecordPickerInput input) throws QException
   {
      if(resolved.getChildTable() == null)
      {
         throw (new QBadRequestException("Related record picker " + resolved.getPicker().getName() + " is a " + resolved.getPicker().getMode() + " picker; this operation needs MULTI_CHILD or LINK."));
      }
      if(input.getPrimaryKey() == null)
      {
         throw (new QBadRequestException("primaryKey (the parent record) is required."));
      }
      if(CollectionUtils.nullSafeIsEmpty(input.getTargetIds()) && CollectionUtils.nullSafeIsEmpty(input.getRecords()))
      {
         throw (new QBadRequestException("No target records were given."));
      }
      int count = Math.max(CollectionUtils.nonNullList(input.getTargetIds()).size(), CollectionUtils.nonNullList(input.getRecords()).size());
      int max   = Objects.requireNonNullElse(resolved.getPicker().getMaxSelections(), QRelatedRecordPicker.DEFAULT_MAX_SELECTIONS);
      if(count > max)
      {
         throw (new QBadRequestException("At most " + max + " records can be picked at once."));
      }
      return (parentValues(resolved, input));
   }



   /*******************************************************************************
    ** The parent record's values (read as the user; 404 if not visible), else
    ** the form values given with the request.
    *******************************************************************************/
   private Map<String, Serializable> parentValues(Resolved resolved, RelatedRecordPickerInput input) throws QException
   {
      if(input.getPrimaryKey() == null)
      {
         return (input.getValues() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(input.getValues()));
      }
      QTableMetaData table = resolved.getTable();
      PermissionsHelper.checkTablePermissionThrowing(new GetInput(table.getName()), TablePermissionSubType.READ);
      GetInput getInput = new GetInput(table.getName());
      getInput.setPrimaryKey(input.getPrimaryKey());
      QRecord parent = new GetAction().executeForRecord(getInput);
      if(parent == null)
      {
         throw (new QNotFoundException("Could not find " + table.getLabel() + " " + input.getPrimaryKey() + "."));
      }
      return (new LinkedHashMap<>(parent.getValues()));
   }



   /*******************************************************************************
    ** READ on the target table is required to list its records.
    *******************************************************************************/
   private void checkTargetRead(QTableMetaData target) throws QException
   {
      PermissionsHelper.checkTablePermissionThrowing(new QueryInput(target.getName()), TablePermissionSubType.READ);
   }



   /*******************************************************************************
    ** A permission on the child (or join) table.
    *******************************************************************************/
   private void checkChildPermission(Resolved resolved, TablePermissionSubType permission) throws QException
   {
      PermissionsHelper.checkTablePermissionThrowing(new InsertInput(resolved.getChildTable().getName()), permission);
   }



   /*******************************************************************************
    ** The targets, read through the picker query (its filter, the user's
    ** security); any id not found is a bad request.
    *******************************************************************************/
   private Map<Serializable, QRecord> loadTargets(Resolved resolved, Map<String, Serializable> values, List<Serializable> targetIds) throws QException
   {
      QTableMetaData target = resolved.getTargetTable();
      checkTargetRead(target);
      QFieldMetaData primaryKey = target.getField(target.getPrimaryKeyField());

      List<Serializable> ids          = targetIds.stream().map(id -> ValueUtils.getValueAsFieldType(primaryKey.getType(), id)).distinct().toList();
      QQueryFilter       filter       = new QQueryFilter(new QFilterCriteria(primaryKey.getName(), QCriteriaOperator.IN, ids));
      QQueryFilter       pickerFilter = interpolated(resolved.getPicker().getFilter(), values);
      if(pickerFilter != null)
      {
         filter.addSubFilter(pickerFilter);
      }
      QueryInput queryInput = new QueryInput(target.getName());
      queryInput.setFilter(filter);
      queryInput.setShouldGenerateDisplayValues(true);
      queryInput.setShouldTranslatePossibleValues(true);

      Map<Serializable, QRecord> found = new LinkedHashMap<>();
      for(QRecord record : new QueryAction().execute(queryInput).getRecords())
      {
         found.put(ValueUtils.getValueAsFieldType(primaryKey.getType(), record.getValue(primaryKey.getName())), record);
      }
      List<Serializable> missing = ids.stream().filter(id -> !found.containsKey(id)).toList();
      if(!missing.isEmpty())
      {
         throw (new QBadRequestException("These " + target.getLabel() + " records are not available to pick: " + StringUtils.join(", ", missing) + "."));
      }
      return (found);
   }



   /*******************************************************************************
    ** A target id as the target's primary-key type.
    *******************************************************************************/
   private Serializable normalizeId(Resolved resolved, Serializable id)
   {
      QTableMetaData target = resolved.getTargetTable();
      return (ValueUtils.getValueAsFieldType(target.getField(target.getPrimaryKeyField()).getType(), id));
   }



   /*******************************************************************************
    ** One child row: defaults, then values copied from the target, then the
    ** client's review values (review fields only), then the forced values.
    *******************************************************************************/
   private QRecord buildChild(Resolved resolved, Map<String, Serializable> parentValues, QRecord target, Map<String, Serializable> clientValues)
   {
      QRelatedRecordPicker picker      = resolved.getPicker();
      QTableMetaData       child       = resolved.getChildTable();
      QTableMetaData       targetTable = resolved.getTargetTable();
      QRecord              row         = new QRecord().withTableName(child.getName());

      CollectionUtils.nonNullMap(picker.getChildFieldDefaults()).forEach(row::setValue);
      CollectionUtils.nonNullMap(picker.getChildValuesFromTarget()).forEach((childField, targetField) -> row.setValue(childField, target.getValue(targetField)));

      if(clientValues != null)
      {
         for(String fieldName : resolved.getReviewFieldNames())
         {
            if(clientValues.containsKey(fieldName))
            {
               QFieldType type = child.getField(fieldName).getType();
               row.setValue(fieldName, ValueUtils.getValueAsFieldType(type, clientValues.get(fieldName)));
            }
         }
      }

      resolved.getParentToChildFields().forEach((parentField, childField) -> row.setValue(childField, parentValues.get(parentField)));
      row.setValue(picker.getChildTargetFieldName(), target.getValue(targetTable.getPrimaryKeyField()));
      row.setDisplayValue(picker.getChildTargetFieldName(), target.getRecordLabel());
      return (row);
   }



   /*******************************************************************************
    ** This parent's child rows for the given targets (as the user).
    *******************************************************************************/
   private List<QRecord> queryChildren(Resolved resolved, Map<String, Serializable> parentValues, List<Serializable> targetIds) throws QException
   {
      if(CollectionUtils.nullSafeIsEmpty(targetIds))
      {
         return (new ArrayList<>());
      }
      QTableMetaData child  = resolved.getChildTable();
      QQueryFilter   filter = new QQueryFilter();
      for(Map.Entry<String, String> entry : resolved.getParentToChildFields().entrySet())
      {
         filter.addCriteria(new QFilterCriteria(entry.getValue(), QCriteriaOperator.EQUALS, parentValues.get(entry.getKey())));
      }
      QFieldType targetType = child.getField(resolved.getPicker().getChildTargetFieldName()).getType();
      filter.addCriteria(new QFilterCriteria(resolved.getPicker().getChildTargetFieldName(), QCriteriaOperator.IN,
         targetIds.stream().map(id -> ValueUtils.getValueAsFieldType(targetType, id)).toList()));
      QueryInput queryInput = new QueryInput(child.getName());
      queryInput.setFilter(filter);
      return (new QueryAction().execute(queryInput).getRecords());
   }



   /*******************************************************************************
    ** A clone of a metadata filter with ${input.x} replaced from the values;
    ** criteria whose value is missing are dropped.
    *******************************************************************************/
   static QQueryFilter interpolated(QQueryFilter filter, Map<String, Serializable> values) throws QException
   {
      if(filter == null)
      {
         return (null);
      }
      QQueryFilter clone = filter.clone();
      clone.interpretValues(Map.of("input", values == null ? new LinkedHashMap<>() : values), () -> CriteriaMissingInputValueBehavior.REMOVE_FROM_FILTER);
      return (clone);
   }



   /*******************************************************************************
    ** OR of CONTAINS over the target's search fields (else its record-label
    ** fields); a numeric term also matches the primary key.
    *******************************************************************************/
   private QQueryFilter searchFilter(QTableMetaData target, String searchTerm)
   {
      if(!StringUtils.hasContent(searchTerm))
      {
         return (null);
      }
      String       term   = searchTerm.trim();
      List<String> fields = CollectionUtils.nullSafeHasContents(target.getSearchFields()) ? target.getSearchFields() : CollectionUtils.nonNullList(target.getRecordLabelFields());
      QQueryFilter filter = new QQueryFilter().withBooleanOperator(QQueryFilter.BooleanOperator.OR);
      for(String fieldName : fields)
      {
         QFieldMetaData field = target.getFields().get(fieldName);
         if(field != null && field.getType().isStringLike())
         {
            filter.addCriteria(new QFilterCriteria(fieldName, QCriteriaOperator.CONTAINS, term).withOption(CriteriaOption.CASE_INSENSITIVE));
         }
      }
      QFieldMetaData primaryKey = target.getField(target.getPrimaryKeyField());
      if(primaryKey.getType().isNumeric() && term.matches("\\d{1,18}"))
      {
         filter.addCriteria(new QFilterCriteria(primaryKey.getName(), QCriteriaOperator.EQUALS, ValueUtils.getValueAsFieldType(primaryKey.getType(), term)));
      }
      if(CollectionUtils.nullSafeIsEmpty(filter.getCriteria()))
      {
         filter.addCriteria(new QFilterCriteria(primaryKey.getName(), QCriteriaOperator.FALSE));
      }
      return (filter);
   }



   /*******************************************************************************
    ** Default order: the first record-label field, else newest first.
    *******************************************************************************/
   private QFilterOrderBy defaultOrderBy(QTableMetaData target)
   {
      List<String> labelFields = CollectionUtils.nonNullList(target.getRecordLabelFields());
      if(!labelFields.isEmpty() && target.getFields().containsKey(labelFields.get(0)))
      {
         return (new QFilterOrderBy(labelFields.get(0), true));
      }
      return (new QFilterOrderBy(target.getPrimaryKeyField(), false));
   }



   /*******************************************************************************
    ** The user's filter may only name fields of the target table.
    *******************************************************************************/
   private void assertFieldsOnTable(QQueryFilter filter, QTableMetaData target) throws QBadRequestException
   {
      for(QFilterCriteria criteria : CollectionUtils.nonNullList(filter.getCriteria()))
      {
         if(criteria.getFieldName() != null && !target.getFields().containsKey(criteria.getFieldName()))
         {
            throw (new QBadRequestException("Unknown field in filter: " + criteria.getFieldName()));
         }
         if(criteria.getOtherFieldName() != null && !target.getFields().containsKey(criteria.getOtherFieldName()))
         {
            throw (new QBadRequestException("Unknown field in filter: " + criteria.getOtherFieldName()));
         }
      }
      for(QFilterOrderBy orderBy : CollectionUtils.nonNullList(filter.getOrderBys()))
      {
         if(!target.getFields().containsKey(orderBy.getFieldName()))
         {
            throw (new QBadRequestException("Unknown field in order by: " + orderBy.getFieldName()));
         }
      }
      for(QQueryFilter subFilter : CollectionUtils.nonNullList(filter.getSubFilters()))
      {
         assertFieldsOnTable(subFilter, target);
      }
   }



   /*******************************************************************************
    ** Throw a bad request listing each record's errors, if any has errors.
    *******************************************************************************/
   private void throwIfErrors(List<QRecord> records) throws QBadRequestException
   {
      List<String>  messages = new ArrayList<>();
      List<QRecord> list     = CollectionUtils.nonNullList(records);
      for(int i = 0; i < list.size(); i++)
      {
         for(QStatusMessage error : CollectionUtils.nonNullList(list.get(i).getErrors()))
         {
            messages.add("Row " + (i + 1) + ": " + error.getMessage());
         }
      }
      if(!messages.isEmpty())
      {
         throw (new QBadRequestException(String.join("\n", messages)));
      }
   }
}
