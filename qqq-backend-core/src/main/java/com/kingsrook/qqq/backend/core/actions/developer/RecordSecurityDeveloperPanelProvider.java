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

package com.kingsrook.qqq.backend.core.actions.developer;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.developer.DeveloperPanel;
import com.kingsrook.qqq.backend.core.model.developer.DeveloperPanelBlock;
import com.kingsrook.qqq.backend.core.model.developer.DeveloperPanelContext;
import com.kingsrook.qqq.backend.core.model.developer.DeveloperPanelProviderInterface;
import com.kingsrook.qqq.backend.core.model.developer.DeveloperPanelTone;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.security.MultiRecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** Built-in developer panel: the table's record security locks, and for each,
 ** why the current user can see this record (an all-access key, a matching key
 ** value, or the lock's null-value behavior).  Only the user's own access to
 ** this record is described - never the full list of their key values.
 *******************************************************************************/
public class RecordSecurityDeveloperPanelProvider implements DeveloperPanelProviderInterface
{
   public static final String ID = "recordSecurity";



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String getId()
   {
      return (ID);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String getTitle()
   {
      return ("Record Security");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Integer getOrder()
   {
      return (150);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String getIcon()
   {
      return ("lock");
   }



   /*******************************************************************************
    ** Shown for every record view: a table without locks says so, which is
    ** itself useful to a developer.
    *******************************************************************************/
   @Override
   public boolean isAvailable(DeveloperPanelContext context)
   {
      return (context.getTable() != null && context.getRecord() != null);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public DeveloperPanel build(DeveloperPanelContext context)
   {
      QTableMetaData           table = context.getTable();
      List<RecordSecurityLock> locks = CollectionUtils.nonNullList(table.getRecordSecurityLocks());

      DeveloperPanel panel = new DeveloperPanel().withDescription("Record security locks on this table, and why you can see this record.");
      if(locks.isEmpty())
      {
         return (panel.withStatusLabel("No locks").withStatusTone(DeveloperPanelTone.INFO)
            .withBlock(DeveloperPanelBlock.text("This table has no record security locks: any user with read permission on the table can see all of its records.", DeveloperPanelTone.INFO)));
      }

      DeveloperPanelBlock lockTable = DeveloperPanelBlock.table("Locks", List.of("Security key", "Field", "Scope", "Null values", "This record's value", "Your access"));
      for(RecordSecurityLock lock : locks)
      {
         addLockRows(lockTable, table, context, lock, "");
      }

      return (panel.withStatusLabel(locks.size() + (locks.size() == 1 ? " lock" : " locks")).withStatusTone(DeveloperPanelTone.SUCCESS)
         .withBlock(DeveloperPanelBlock.text("You can see this record because your session satisfies its read locks - see Your access for each lock.", DeveloperPanelTone.SUCCESS))
         .withBlock(lockTable));
   }



   /*******************************************************************************
    ** Add a row per lock (recursing into multi-locks, with their operator as a
    ** prefix).
    *******************************************************************************/
   private void addLockRows(DeveloperPanelBlock lockTable, QTableMetaData table, DeveloperPanelContext context, RecordSecurityLock lock, String prefix)
   {
      if(lock instanceof MultiRecordSecurityLock multiLock)
      {
         String operator = multiLock.getOperator() == null ? "AND" : multiLock.getOperator().name();
         for(RecordSecurityLock subLock : CollectionUtils.nonNullList(multiLock.getLocks()))
         {
            addLockRows(lockTable, table, context, subLock, prefix + "(" + operator + ") ");
         }
         return;
      }

      boolean      viaJoin    = CollectionUtils.nullSafeHasContents(lock.getJoinNameChain());
      String       fieldLabel = viaJoin ? lock.getFieldName() + " (via " + String.join(" > ", lock.getJoinNameChain()) + ")" : lock.getFieldName();
      Serializable value      = viaJoin ? null : context.getRecord().getValue(lock.getFieldName());
      String       valueText  = viaJoin ? "(on a joined record)" : (value == null ? "null" : String.valueOf(value));

      List<String> row = new ArrayList<>();
      row.add(prefix + lock.getSecurityKeyType());
      row.add(fieldLabel);
      row.add(lock.getLockScope() == null ? "" : lock.getLockScope().name());
      row.add(lock.getNullValueBehavior() == null ? "" : lock.getNullValueBehavior().name());
      row.add(valueText);
      row.add(describeAccess(table, lock, value, viaJoin));
      lockTable.withRow(row);
   }



   /*******************************************************************************
    ** Why the session passes this lock (or that it does not, for write-only
    ** scopes, which do not affect reading).
    *******************************************************************************/
   private String describeAccess(QTableMetaData table, RecordSecurityLock lock, Serializable value, boolean viaJoin)
   {
      QSession         session = QContext.getQSession();
      QSecurityKeyType keyType = QContext.getQInstance().getSecurityKeyType(lock.getSecurityKeyType());
      if(session == null)
      {
         return ("No session");
      }

      if(keyType != null && StringUtils.hasContent(keyType.getAllAccessKeyName()) && session.hasSecurityKeyValue(keyType.getAllAccessKeyName(), true, QFieldType.BOOLEAN))
      {
         return ("All access (" + keyType.getAllAccessKeyName() + ")");
      }

      if(viaJoin)
      {
         return ("Checked on the joined record");
      }

      if(value == null)
      {
         return switch(lock.getNullValueBehavior())
         {
            case ALLOW -> "Allowed (null values are allowed)";
            case ALLOW_WRITE_ONLY -> "Null value (allowed for writes only)";
            case DENY -> "Null value (denied unless your session overrides it)";
         };
      }

      QFieldMetaData field     = table.getFields().get(lock.getFieldName());
      QFieldType     fieldType = field == null ? null : field.getType();
      if(session.hasSecurityKeyValue(lock.getSecurityKeyType(), value, fieldType))
      {
         return ("Your session has key value " + value);
      }

      if(lock.getLockScope() == RecordSecurityLock.LockScope.WRITE)
      {
         return ("No matching key - you cannot write this record");
      }

      return ("No matching key");
   }

}
