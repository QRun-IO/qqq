/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.middleware.javalin;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerAction;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.AssociatedRecordDiscovery;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.AssociatedRecordUpdate;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.AssociationJoin;
import com.kingsrook.qqq.backend.core.actions.values.ValueBehaviorApplier;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.actions.AbstractTableActionInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/*******************************************************************************
 ** Preflight the association writes requested by HTTP before changing the parent.
 ** Omitted members of a named relationship require DELETE even when record
 ** locks hide them. Key-only discovery is internal; actual DML retains its locks.
 *******************************************************************************/
public final class AssociatedWritePermissions
{
   private final AbstractTableActionInput originalInput;
   private final Map<String, Set<Object>> visitedDeleteKeys = new HashMap<>();



   /*******************************************************************************
    ** Preserve the original HTTP action for application permission checkers.
    *******************************************************************************/
   private AssociatedWritePermissions(AbstractTableActionInput originalInput)
   {
      this.originalInput = originalInput;
   }



   /*******************************************************************************
    ** The caller has already authorized INSERT on the parent table.
    *******************************************************************************/
   public static void check(InsertInput input) throws QException
   {
      new AssociatedWritePermissions(input).checkInserts(input);
   }



   /*******************************************************************************
    ** The caller has already authorized EDIT on the parent table.
    *******************************************************************************/
   public static void check(UpdateInput input) throws QException
   {
      new AssociatedWritePermissions(input).checkUpdates(input);
   }



   /*******************************************************************************
    ** The caller has already authorized DELETE on the parent table.
    *******************************************************************************/
   public static void check(DeleteInput input) throws QException
   {
      new AssociatedWritePermissions(input).checkDeletes(input);
   }



   /*******************************************************************************
    ** Every supplied record under an INSERT is another INSERT, even with an id.
    *******************************************************************************/
   private void checkInserts(InsertInput input) throws QException
   {
      QTableMetaData table = TableMetaDataPersonalizerAction.execute(input);
      List<QRecord> records = normalizedRecords(table, input.getRecords(), ValueBehaviorApplier.Action.INSERT);
      for(Association association : CollectionUtils.nonNullList(table.getAssociations()))
      {
         List<QRecord> children = new ArrayList<>();
         for(QRecord record : records)
         {
            children.addAll(getChildren(table, association, record));
         }
         if(!children.isEmpty())
         {
            checkPermission(association.getAssociatedTableName(), TablePermissionSubType.INSERT);
            checkInserts(new InsertInput().withTableName(association.getAssociatedTableName()).withRecords(children)
               .withInputSource(input.getInputSource()).withTransaction(input.getTransaction()));
         }
      }
   }



   /*******************************************************************************
    ** A supplied association is a replacement set: ids are edits, missing ids
    ** are inserts, and existing rows omitted from that set are deletes.
    *******************************************************************************/
   private void checkUpdates(UpdateInput input) throws QException
   {
      QTableMetaData table = TableMetaDataPersonalizerAction.execute(input);
      List<QRecord> records = normalizedRecords(table, input.getRecords(), ValueBehaviorApplier.Action.UPDATE);
      for(Association association : CollectionUtils.nonNullList(table.getAssociations()))
      {
         QTableMetaData childTable = QContext.getQInstance().getTable(association.getAssociatedTableName());
         for(QRecord record : records)
         {
            if(CollectionUtils.nullSafeHasContents(record.getErrors()) || record.getAssociatedRecords() == null)
            {
               continue;
            }
            for(QRecord child : CollectionUtils.nonNullList(record.getAssociatedRecords().get(association.getName())))
            {
               checkPermission(childTable.getName(), child.getValue(childTable.getPrimaryKeyField()) == null ? TablePermissionSubType.INSERT : TablePermissionSubType.EDIT);
            }
         }
      }
      Map<QRecord, AssociatedRecordUpdate.Values> associationValues = AssociatedRecordUpdate.prepare(table, records, input.getTransaction());
      if(associationValues.isEmpty())
      {
         return;
      }
      table = AssociatedRecordDiscovery.physicalParentTable(table);
      for(Association association : CollectionUtils.nonNullList(table.getAssociations()))
      {
         QTableMetaData childTable = QContext.getQInstance().getTable(association.getAssociatedTableName());
         for(List<QRecord> page : CollectionUtils.getPages(records, 500))
         {
            List<QRecord> inserts = new ArrayList<>();
            List<QRecord> updates = new ArrayList<>();
            List<AssociatedRecordDiscovery.Parent> parents = new ArrayList<>();
            for(QRecord record : page)
            {
               AssociatedRecordUpdate.Values values = associationValues.get(record);
               if(values == null || CollectionUtils.nullSafeHasContents(record.getErrors()) || record.getAssociatedRecords() == null
                  || !record.getAssociatedRecords().containsKey(association.getName()))
               {
                  continue;
               }

               List<Serializable> retainedKeys = new ArrayList<>();
               AssociationJoin join = AssociationJoin.resolve(table, association);
               for(QRecord originalChild : CollectionUtils.nonNullList(record.getAssociatedRecords().get(association.getName())))
               {
                  Serializable key = originalChild.getValue(childTable.getPrimaryKeyField());
                  QRecord child = copyForPreflight(originalChild);
                  join.assignParentValues(values.after(), child);
                  if(key == null)
                  {
                     inserts.add(child);
                  }
                  else
                  {
                     retainedKeys.add(key);
                     updates.add(child);
                  }
               }
               if(!join.parentValues(values.before()).contains(null))
               {
                  parents.add(new AssociatedRecordDiscovery.Parent(values.before().getValues(), retainedKeys));
               }
            }

            if(!parents.isEmpty() && !canDeleteLeaf(childTable))
            {
               List<Serializable> omittedKeys = AssociatedRecordDiscovery.findPrimaryKeys(table, association, parents, input.getTransaction());
               if(!omittedKeys.isEmpty())
               {
                  checkPermission(childTable.getName(), TablePermissionSubType.DELETE);
                  checkDeletes(new DeleteInput().withTableName(childTable.getName()).withTransaction(input.getTransaction())
                     .withInputSource(input.getInputSource()).withPrimaryKeys(omittedKeys));
               }
            }
            if(!updates.isEmpty())
            {
               checkUpdates(new UpdateInput().withTableName(childTable.getName()).withRecords(updates)
                  .withInputSource(input.getInputSource()).withTransaction(input.getTransaction()));
            }
            if(!inserts.isEmpty())
            {
               checkInserts(new InsertInput().withTableName(childTable.getName()).withRecords(inserts)
                  .withInputSource(input.getInputSource()).withTransaction(input.getTransaction()));
            }
         }
      }
   }



   /*******************************************************************************
    ** Delete cascades belong to the raw schema, even if USER metadata hides an
    ** association. Track actual table/key pairs so cyclic graphs terminate.
    *******************************************************************************/
   private void checkDeletes(DeleteInput input) throws QException
   {
      QTableMetaData table = QContext.getQInstance().getTable(input.getTableName());
      Set<Object> visited = visitedDeleteKeys.computeIfAbsent(table.getName(), key -> new HashSet<>());
      List<Serializable> keys = new ArrayList<>();
      for(Serializable key : CollectionUtils.nonNullList(input.getPrimaryKeys()))
      {
         Serializable typedKey = ValueUtils.getValueAsFieldType(table.getField(table.getPrimaryKeyField()).getType(), key);
         if(visited.add(AssociatedRecordUpdate.primaryKey(table, new QRecord().withValue(table.getPrimaryKeyField(), typedKey))))
         {
            keys.add(typedKey);
         }
      }
      if(keys.isEmpty())
      {
         return;
      }

      for(Association association : CollectionUtils.nonNullList(table.getAssociations()))
      {
         AssociationJoin join = AssociationJoin.resolve(table, association);
         QTableMetaData childTable = QContext.getQInstance().getTable(association.getAssociatedTableName());
         if(canDeleteLeaf(childTable))
         {
            continue;
         }
         boolean needsStoredValues = join.getJoinOns().stream().anyMatch(joinOn -> !joinOn.getLeftField().equals(table.getPrimaryKeyField()));
         for(List<Serializable> page : CollectionUtils.getPages(keys, 500))
         {
            List<QRecord> parentRecords = needsStoredValues
               ? AssociatedRecordDiscovery.readParentValues(table, List.of(association), page, input.getTransaction())
               : page.stream().map(key -> new QRecord().withValue(table.getPrimaryKeyField(), key)).toList();
            List<AssociatedRecordDiscovery.Parent> parents = new ArrayList<>();
            for(QRecord record : parentRecords)
            {
               if(!join.parentValues(record).contains(null))
               {
                  parents.add(new AssociatedRecordDiscovery.Parent(record.getValues(), List.of()));
               }
            }
            if(parents.isEmpty())
            {
               continue;
            }
            List<Serializable> childKeys = AssociatedRecordDiscovery.findPrimaryKeys(table, association, parents, input.getTransaction());
            if(!childKeys.isEmpty())
            {
               checkPermission(childTable.getName(), TablePermissionSubType.DELETE);
               checkDeletes(new DeleteInput().withTableName(childTable.getName()).withTransaction(input.getTransaction())
                  .withInputSource(input.getInputSource()).withPrimaryKeys(childKeys));
            }
         }
      }
   }



   /*******************************************************************************
    ** Propagate prospective join values on copies, matching core child writes.
    *******************************************************************************/
   private List<QRecord> getChildren(QTableMetaData table, Association association, QRecord record) throws QException
   {
      List<QRecord> children = new ArrayList<>();
      if(CollectionUtils.nullSafeHasContents(record.getErrors()) || record.getAssociatedRecords() == null)
      {
         return children;
      }
      AssociationJoin join = AssociationJoin.resolve(table, association);
      for(QRecord originalChild : CollectionUtils.nonNullList(record.getAssociatedRecords().get(association.getName())))
      {
         QRecord child = copyForPreflight(originalChild);
         join.assignParentValues(record, child);
         children.add(child);
      }
      return children;
   }



   /*******************************************************************************
    ** Use the same standard value normalization as the pending core write.
    ** Copies keep validation and prospective join propagation out of the request.
    *******************************************************************************/
   private List<QRecord> normalizedRecords(QTableMetaData table, List<QRecord> records, ValueBehaviorApplier.Action action)
   {
      List<QRecord> copies = new ArrayList<>();
      for(QRecord record : CollectionUtils.nonNullList(records))
      {
         copies.add(copyForPreflight(record));
      }
      ValueBehaviorApplier.applyFieldBehaviors(action, QContext.getQInstance(), table, copies, null);
      return copies;
   }



   /*******************************************************************************
    ** Only this level's values are changed. Descendant maps are read-only here;
    ** each child gets its own copy before recursion, including null child lists.
    *******************************************************************************/
   private QRecord copyForPreflight(QRecord record)
   {
      QRecord copy = new QRecord();
      copy.setTableName(record.getTableName());
      copy.setValues(new HashMap<>(record.getValues()));
      copy.setErrors(new ArrayList<>(CollectionUtils.nonNullList(record.getErrors())));
      copy.setAssociatedRecords(record.getAssociatedRecords());
      return copy;
   }



   /*******************************************************************************
    ** A granted DELETE on a raw schema leaf covers every possible omitted row.
    ** Discovery cannot reveal another permission requirement in that case.
    *******************************************************************************/
   private boolean canDeleteLeaf(QTableMetaData table)
   {
      return CollectionUtils.nullSafeIsEmpty(table.getAssociations())
         && PermissionsHelper.hasTablePermission(originalInput, table.getName(), TablePermissionSubType.DELETE);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void checkPermission(String tableName, TablePermissionSubType operation) throws QPermissionDeniedException
   {
      if(!PermissionsHelper.hasTablePermission(originalInput, tableName, operation))
      {
         throw new QPermissionDeniedException("Permission denied.");
      }
   }
}
