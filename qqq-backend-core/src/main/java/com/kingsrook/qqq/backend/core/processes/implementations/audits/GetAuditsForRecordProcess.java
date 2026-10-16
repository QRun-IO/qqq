/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.processes.implementations.audits;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import com.google.gson.reflect.TypeToken;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerAction;
import com.kingsrook.qqq.backend.core.actions.permissions.CorePermissionEnforcer;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.actions.permissions.UseTablePermissionCustomPermissionChecker;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessHelper;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.metadata.personalization.TableMetaDataPersonalizerInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;
import com.kingsrook.qqq.backend.core.model.audits.AuditsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerInterface;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFunctionInputMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import org.apache.commons.lang3.BooleanUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** MetaDataProducer and BackendStep for GetAuditsForRecordProcess
 *******************************************************************************/
public class GetAuditsForRecordProcess implements MetaDataProducerInterface<QProcessMetaData>, BackendStep
{
   private static final QLogger LOG  = QLogger.getLogger(GetAuditsForRecordProcess.class);
   public static final  String  NAME = "GetAuditsForRecord";

   private static final String HISTORY_READ_REASON = "record history: the caller may read the audited record";



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QProcessMetaData produce(QInstance qInstance) throws QException
   {
      ////////////////////////////////////////////////////////////////////////
      // not protected by a permission of its own: run() lets the caller see //
      // a record's history only if they may read that record                //
      ////////////////////////////////////////////////////////////////////////
      return (new QProcessMetaData()
         .withName(NAME)
         .withPermissionRules(new QPermissionRules().withLevel(PermissionLevel.NOT_PROTECTED))
         .withStep(new QBackendStepMetaData()
            .withName("execute")
            .withCode(new QCodeReference(getClass()))
            .withInputData(new QFunctionInputMetaData()
               .withField(new QFieldMetaData("tableName", QFieldType.STRING).withIsRequired(true))
               .withField(new QFieldMetaData("recordId", QFieldType.STRING).withIsRequired(true))
               .withField(new QFieldMetaData("isSortAscending", QFieldType.BOOLEAN).withDefaultValue("false"))
               .withField(new QFieldMetaData("limit", QFieldType.INTEGER).withDefaultValue("1000"))
               .withField(new QFieldMetaData("includeChildren", QFieldType.BOOLEAN).withDefaultValue("false"))
            )
         ));
   }



   /***************************************************************************
    * Opt in to a stricter rule: only users who may read the audit table
    * directly (audit.read - see SystemTablePermissions) may see any record's
    * history.  By default the process is not protected, and shows a record's
    * history to whoever may read that record.
    ***************************************************************************/
   public static void setProcessPermissionToBeBasedOnAuditTableReadPermission(QProcessMetaData process)
   {
      process.setPermissionRules(new QPermissionRules()
         .withCustomPermissionChecker(UseTablePermissionCustomPermissionChecker.build(AuditsMetaDataProvider.TABLE_NAME_AUDIT, TablePermissionSubType.READ)));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void run(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
   {
      boolean sortDirection = BooleanUtils.isTrue(runBackendStepInput.getValueBoolean("isSortAscending"));
      Integer limit         = Objects.requireNonNullElse(runBackendStepInput.getValueInteger("limit"), 1000);
      String  tableName     = runBackendStepInput.getValueString("tableName");

      /////////////////////////////////////////////////////////////////////////////
      // a record's audits are visible only to users who may read its table:     //
      // the audit rows name the table and record, and carry its old/new values  //
      /////////////////////////////////////////////////////////////////////////////
      if(!StringUtils.hasContent(tableName) || QContext.getQInstance().getTable(tableName) == null
         || !PermissionsHelper.hasTablePermission(runBackendStepInput, tableName, TablePermissionSubType.READ))
      {
         throw (new QPermissionDeniedException("Permission denied."));
      }

      ////////////////////////////////////////////////////////////////////////////
      // on a table with record security locks, the record itself must also be //
      // readable by the caller (their keys apply to this get), so a record     //
      // they can't see has no visible history either                           //
      ////////////////////////////////////////////////////////////////////////////
      if(!isRecordReadable(tableName, runBackendStepInput.getValue("recordId")))
      {
         throw (new QPermissionDeniedException("Permission denied."));
      }

      QQueryFilter filter = new QQueryFilter()
         .withCriteria(new QFilterCriteria(AuditsMetaDataProvider.TABLE_NAME_AUDIT_TABLE + ".name", QCriteriaOperator.EQUALS, tableName))
         .withCriteria(new QFilterCriteria("recordId", QCriteriaOperator.EQUALS, runBackendStepInput.getValueString("recordId")))
         .withOrderBy(new QFilterOrderBy("timestamp", sortDirection))
         .withOrderBy(new QFilterOrderBy("id", sortDirection))
         .withOrderBy(new QFilterOrderBy("auditDetail.id", true));
      QueryJoin auditDetailJoin = new QueryJoin(AuditsMetaDataProvider.TABLE_NAME_AUDIT_DETAIL).withSelect(true).withType(QueryJoin.Type.LEFT);

      /////////////////////////////////////////////////////////////////////////////
      // the caller may read the record, so they may see its history - but they  //
      // need not hold audit.read: these two reads (of only this record's audit  //
      // rows) bypass the audit tables' own permission rules, and nothing else   //
      /////////////////////////////////////////////////////////////////////////////
      QueryInput auditQueryInput = new QueryInput(AuditsMetaDataProvider.TABLE_NAME_AUDIT)
         .withShouldGenerateDisplayValues(true)
         .withShouldTranslatePossibleValues(true)
         .withQueryJoin(auditDetailJoin)
         .withFilter(filter.withLimit(limit));
      auditQueryInput.withPermissionBypass(HISTORY_READ_REASON);
      ArrayList<QRecord> auditRecords = CollectionUtils.useOrWrap(new QueryAction().execute(auditQueryInput).getRecords(), new TypeToken<>() {});

      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // in case the limit was hit, do a count so it can be shown to the user (to give a clue that there are more records) //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      if(limit.equals(auditRecords.size()))
      {
         CountInput countInput = new CountInput(AuditsMetaDataProvider.TABLE_NAME_AUDIT).withFilter(filter).withQueryJoin(auditDetailJoin);
         countInput.withPermissionBypass(HISTORY_READ_REASON);
         Integer distinctCount = new CountAction().execute(countInput).getCount();
         runBackendStepOutput.addValue("distinctCount", distinctCount);
      }

      ////////////////////////////////////////////////////////////////////////////
      // make sure user personalization is applied to the table - e.g., so user //
      // doesn't see values for changes to fields they aren't allowed to see    //
      ////////////////////////////////////////////////////////////////////////////
      QTableMetaData table = TableMetaDataPersonalizerAction.execute(new TableMetaDataPersonalizerInput().withTableName(tableName).withInputSource(QInputSource.USER));

      Iterator<QRecord> iterator = CollectionUtils.nonNullList(auditRecords).iterator();
      while(iterator.hasNext())
      {
         QRecord record = iterator.next();

         String fieldName = record.getValueString(AuditsMetaDataProvider.TABLE_NAME_AUDIT_DETAIL + ".fieldName");
         if(StringUtils.hasContent(fieldName))
         {
            if(table == null || !table.getFields().containsKey(fieldName))
            {
               ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
               // if user doesn't have this field (or if it's just a non-existing field), remove this record from the result set //
               ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
               iterator.remove();
            }
         }
      }

      if(runBackendStepInput.isFieldAccessEnforced() || CorePermissionEnforcer.shouldEnforceFieldAccess(runBackendStepInput))
      {
         FieldAccessHelper.applyToRecords(AuditsMetaDataProvider.TABLE_NAME_AUDIT, List.of(auditDetailJoin), auditRecords, "process");
      }
      runBackendStepOutput.addValue("audits", auditRecords);
   }


   /*******************************************************************************
    ** Whether the caller may read the record whose history is asked for.  Table
    ** READ was already checked; on a table without record security locks that is
    ** enough (so the history of a deleted record stays visible), otherwise the
    ** record must come back from a get under the caller's session.
    *******************************************************************************/
   private static boolean isRecordReadable(String tableName, Serializable recordId) throws QException
   {
      QTableMetaData table = QContext.getQInstance().getTable(tableName);
      if(CollectionUtils.nullSafeIsEmpty(table.getRecordSecurityLocks()))
      {
         return (true);
      }

      if(recordId == null)
      {
         return (false);
      }

      Serializable primaryKey = recordId;
      try
      {
         QFieldMetaData primaryKeyField = table.getField(table.getPrimaryKeyField());
         primaryKey = ValueUtils.getValueAsFieldType(primaryKeyField.getType(), recordId);
      }
      catch(Exception e)
      {
         LOG.debug("Could not convert a record id to its table's primary key type", e, logPair("tableName", tableName));
      }

      return (new GetAction().executeForRecord(new GetInput(tableName).withPrimaryKey(primaryKey)) != null);
   }

}
