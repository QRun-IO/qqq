/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.processes.implementations.audits;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;
import com.google.gson.reflect.TypeToken;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerAction;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.actions.permissions.UseTablePermissionCustomPermissionChecker;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.metadata.personalization.TableMetaDataPersonalizerInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
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
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFunctionInputMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import org.apache.commons.lang3.BooleanUtils;


/*******************************************************************************
 ** MetaDataProducer and BackendStep for GetAuditsForRecordProcess
 *******************************************************************************/
public class GetAuditsForRecordProcess implements MetaDataProducerInterface<QProcessMetaData>, BackendStep
{
   private static final QLogger LOG  = QLogger.getLogger(GetAuditsForRecordProcess.class);
   public static final  String  NAME = "GetAuditsForRecord";



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QProcessMetaData produce(QInstance qInstance) throws QException
   {
      return (new QProcessMetaData()
         .withName(NAME)
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
    * Rather than having its own permission, set this process to use the
    * audit table's (effective) read permission (e.g., based on if that table's
    * permission level (hasAccess vs. readWrite vs. readInsertEditDelete).
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

      QQueryFilter filter = new QQueryFilter()
         .withCriteria(new QFilterCriteria(AuditsMetaDataProvider.TABLE_NAME_AUDIT_TABLE + ".name", QCriteriaOperator.EQUALS, tableName))
         .withCriteria(new QFilterCriteria("recordId", QCriteriaOperator.EQUALS, runBackendStepInput.getValueString("recordId")))
         .withOrderBy(new QFilterOrderBy("timestamp", sortDirection))
         .withOrderBy(new QFilterOrderBy("id", sortDirection))
         .withOrderBy(new QFilterOrderBy("auditDetail.id", true));
      QueryJoin auditDetailJoin = new QueryJoin(AuditsMetaDataProvider.TABLE_NAME_AUDIT_DETAIL).withSelect(true).withType(QueryJoin.Type.LEFT);

      ArrayList<QRecord> auditRecords = CollectionUtils.useOrWrap(new QueryAction().execute(new QueryInput(AuditsMetaDataProvider.TABLE_NAME_AUDIT)
         .withShouldGenerateDisplayValues(true)
         .withShouldTranslatePossibleValues(true)
         .withQueryJoin(auditDetailJoin)
         .withFilter(filter.withLimit(limit))).getRecords(), new TypeToken<>() {});

      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // in case the limit was hit, do a count so it can be shown to the user (to give a clue that there are more records) //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      if(limit.equals(auditRecords.size()))
      {
         Integer distinctCount = new CountAction().execute(new CountInput(AuditsMetaDataProvider.TABLE_NAME_AUDIT).withFilter(filter).withQueryJoin(auditDetailJoin)).getCount();
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

      runBackendStepOutput.addValue("audits", auditRecords);
   }

}
