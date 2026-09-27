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

package com.kingsrook.qqq.backend.core.actions.scripts;


import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.kingsrook.qqq.backend.core.actions.ActionHelper;
import com.kingsrook.qqq.backend.core.actions.audits.DMLAuditAction;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.scripts.ExecuteCodeInput;
import com.kingsrook.qqq.backend.core.model.actions.scripts.ExecuteCodeOutput;
import com.kingsrook.qqq.backend.core.model.actions.scripts.RunAdHocRecordScriptInput;
import com.kingsrook.qqq.backend.core.model.actions.scripts.RunAdHocRecordScriptOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.RunAdHocScriptCustomizers;
import com.kingsrook.qqq.backend.core.model.metadata.code.AdHocScriptCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.scripts.Script;
import com.kingsrook.qqq.backend.core.model.scripts.ScriptRevision;
import com.kingsrook.qqq.backend.core.model.scripts.ScriptsMetaDataProvider;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.memoization.Memoization;


/*******************************************************************************
 **
 *******************************************************************************/
public class RunAdHocRecordScriptAction
{
   private static final QLogger LOG = QLogger.getLogger(RunAdHocRecordScriptAction.class);

   private Map<Integer, ScriptRevision> scriptRevisionCacheByScriptRevisionId = new HashMap<>();
   private Map<Integer, ScriptRevision> scriptRevisionCacheByScriptId         = new HashMap<>();

   private static Memoization<Integer, Script> scriptMemoizationById = new Memoization<>();



   /*******************************************************************************
    **
    *******************************************************************************/
   public void run(RunAdHocRecordScriptInput input, RunAdHocRecordScriptOutput output) throws QException
   {
      try
      {
         ActionHelper.validateSession(input);

         /////////////////////////
         // figure out the code //
         /////////////////////////
         ScriptRevision scriptRevision = getScriptRevision(input);
         if(scriptRevision == null)
         {
            throw (new QException("Script revision was not found."));
         }

         Optional<Script> script = getScript(scriptRevision);

         QContext.getQSession().setValue(DMLAuditAction.AUDIT_CONTEXT_FIELD_NAME, script.isPresent()
            ? "via Script \"%s\"".formatted(script.get().getName())
            : "via Script id " + scriptRevision.getScriptId());

         ////////////////////////////
         // figure out the records //
         ////////////////////////////
         QTableMetaData table = QContext.getQInstance().getTable(input.getTableName());
         if(CollectionUtils.nullSafeIsEmpty(input.getRecordList()))
         {
            QueryInput queryInput = new QueryInput();
            queryInput.setTableName(input.getTableName());
            queryInput.setFilter(new QQueryFilter(new QFilterCriteria(table.getPrimaryKeyField(), QCriteriaOperator.IN, input.getRecordPrimaryKeyList())));
            queryInput.setIncludeAssociations(true);
            QueryOutput queryOutput = new QueryAction().execute(queryInput);
            input.setRecordList(queryOutput.getRecords());
         }

         if(CollectionUtils.nullSafeIsEmpty(input.getRecordList()))
         {
            ////////////////////////////////////////
            // just return if nothing found?  idk //
            ////////////////////////////////////////
            LOG.info("No records supplied as input (or found via primary keys); exiting with noop");
            return;
         }

         ////////////////////////////////////////////////////
         // if a customizer was provided, handle that here //
         ////////////////////////////////////////////////////
         QCodeReference customizerReference = QContext.getQInstance().getSupplementalCustomizer(RunAdHocScriptCustomizers.PRE_EXECUTE);
         if(customizerReference != null)
         {
            RunAdHocRecordScriptCustomizerInterface customizer = QCodeLoader.getAdHoc(RunAdHocRecordScriptCustomizerInterface.class, customizerReference);
            customizer.preExecuteCode(input);
         }

         /////////////
         // run it! //
         /////////////
         ExecuteCodeInput executeCodeInput = ExecuteCodeAction.setupExecuteCodeInput(input, scriptRevision);
         executeCodeInput.getInput().put("records", getRecordsForScript(input, scriptRevision));

         ExecuteCodeOutput executeCodeOutput = new ExecuteCodeOutput();
         new ExecuteCodeAction().run(executeCodeInput, executeCodeOutput);

         output.setOutput(executeCodeOutput.getOutput());
         output.setLogger(executeCodeInput.getExecutionLogger());
      }
      catch(Exception e)
      {
         output.setException(Optional.of(e));
      }
      finally
      {
         QContext.getQSession().removeValue(DMLAuditAction.AUDIT_CONTEXT_FIELD_NAME);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static ArrayList<? extends Serializable> getRecordsForScript(RunAdHocRecordScriptInput input, ScriptRevision scriptRevision)
   {
      try
      {
         Class<?> apiScriptUtilsClass        = Class.forName("com.kingsrook.qqq.api.utils.ApiScriptUtils");
         Method   qRecordListToApiRecordList = apiScriptUtilsClass.getMethod("qRecordListToApiRecordList", List.class, String.class, String.class, String.class);
         Object   apiRecordList              = qRecordListToApiRecordList.invoke(null, input.getRecordList(), input.getTableName(), scriptRevision.getApiName(), scriptRevision.getApiVersion());

         @SuppressWarnings("unchecked")
         ArrayList<? extends Serializable> rs = (ArrayList<? extends Serializable>) apiRecordList;
         return rs;
      }
      catch(ClassNotFoundException e)
      {
         /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // this is the only exception we're kinda expecting here - so catch for it specifically, and just log.trace - others, warn //
         /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         LOG.trace("Couldn't load ApiScriptUtils class - qqq-middleware-api not on the classpath?");
      }
      catch(Exception e)
      {
         LOG.warn("Error converting QRecord list to api record list", e);
      }

      return (new ArrayList<>(input.getRecordList()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private ScriptRevision getScriptRevision(RunAdHocRecordScriptInput input) throws QException
   {
      AdHocScriptCodeReference codeReference = input.getCodeReference();
      if(codeReference.getScriptRevisionRecord() != null)
      {
         return (new ScriptRevision(codeReference.getScriptRevisionRecord()));
      }

      if(codeReference.getScriptRevisionId() != null)
      {
         if(!scriptRevisionCacheByScriptRevisionId.containsKey(codeReference.getScriptRevisionId()))
         {
            GetInput getInput = new GetInput();
            getInput.setTableName(ScriptRevision.TABLE_NAME);
            getInput.setPrimaryKey(codeReference.getScriptRevisionId());
            GetOutput getOutput = new GetAction().execute(getInput);
            if(getOutput.getRecord() != null)
            {
               scriptRevisionCacheByScriptRevisionId.put(codeReference.getScriptRevisionId(), new ScriptRevision(getOutput.getRecord()));
            }
            else
            {
               scriptRevisionCacheByScriptRevisionId.put(codeReference.getScriptRevisionId(), null);
            }
         }

         return (scriptRevisionCacheByScriptRevisionId.get(codeReference.getScriptRevisionId()));
      }

      if(codeReference.getScriptId() != null)
      {
         if(!scriptRevisionCacheByScriptId.containsKey(codeReference.getScriptId()))
         {
            QueryInput queryInput = new QueryInput();
            queryInput.setTableName(ScriptRevision.TABLE_NAME);
            queryInput.setFilter(new QQueryFilter(new QFilterCriteria("script.id", QCriteriaOperator.EQUALS, codeReference.getScriptId())));
            queryInput.withQueryJoin(new QueryJoin(Script.TABLE_NAME).withBaseTableOrAlias(ScriptRevision.TABLE_NAME).withJoinMetaData(QContext.getQInstance().getJoin(ScriptsMetaDataProvider.CURRENT_SCRIPT_REVISION_JOIN_NAME)));
            QueryOutput queryOutput = new QueryAction().execute(queryInput);

            if(CollectionUtils.nullSafeHasContents(queryOutput.getRecords()))
            {
               scriptRevisionCacheByScriptId.put(codeReference.getScriptId(), new ScriptRevision(queryOutput.getRecords().get(0)));
            }
            else
            {
               scriptRevisionCacheByScriptId.put(codeReference.getScriptId(), null);
            }
         }

         return (scriptRevisionCacheByScriptId.get(codeReference.getScriptId()));
      }

      throw (new QException("Code reference did not contain a scriptRevision, scriptRevisionId, or scriptId"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Optional<Script> getScript(ScriptRevision scriptRevision)
   {
      if(scriptRevision == null || scriptRevision.getScriptId() == null)
      {
         return (Optional.empty());
      }

      try
      {
         return scriptMemoizationById.getResult(scriptRevision.getScriptId(), scriptId ->
         {
            try
            {
               QRecord scriptRecord = new GetAction().executeForRecord(new GetInput(Script.TABLE_NAME).withPrimaryKey(scriptRevision.getScriptId()));
               if(scriptRecord != null)
               {
                  Script script = new Script(scriptRecord);
                  scriptMemoizationById.storeResult(scriptRevision.getScriptId(), script);
                  return (script);
               }
            }
            catch(Exception e)
            {
               LOG.info("");
            }

            return (null);
         });
      }
      catch(Exception e)
      {
         return (Optional.empty());
      }
   }
}
