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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.kingsrook.qqq.backend.core.actions.scripts.logging.Log4jCodeExecutionLogger;
import com.kingsrook.qqq.backend.core.actions.scripts.logging.QCodeExecutionLoggerInterface;
import com.kingsrook.qqq.backend.core.actions.scripts.logging.ScriptExecutionLoggerInterface;
import com.kingsrook.qqq.backend.core.actions.scripts.logging.StoreScriptLogAndScriptLogLineExecutionLogger;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QCodeException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.scripts.AbstractRunScriptInput;
import com.kingsrook.qqq.backend.core.model.actions.scripts.ExecuteCodeInput;
import com.kingsrook.qqq.backend.core.model.actions.scripts.ExecuteCodeOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeType;
import com.kingsrook.qqq.backend.core.model.scripts.ScriptRevision;
import com.kingsrook.qqq.backend.core.model.scripts.ScriptRevisionFile;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.ObjectUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** Action to execute user/runtime defined code.
 **
 ** This action is designed to support code in multiple languages, by using
 ** executors, e.g., provided by additional runtime qqq dependencies.  Initially
 ** we are building qqq-language-support-javascript.
 **
 ** We also have a Java executor, to provide at least a little bit of testability
 ** within qqq-backend-core.  This executor is a candidate to be replaced in the
 ** future with something that would do actual dynamic java (whether that's compiled
 ** at runtime, or loaded from a plugin jar at runtime).  In other words, the java
 ** executor in place today is just meant to be a placeholder.
 *******************************************************************************/
public class ExecuteCodeAction
{
   private static final QLogger LOG = QLogger.getLogger(ExecuteCodeAction.class);



   /*******************************************************************************
    **
    *******************************************************************************/
   public void run(ExecuteCodeInput input, ExecuteCodeOutput output) throws QException, QCodeException
   {
      QCodeReference codeReference = input.getCodeReference();

      QCodeExecutionLoggerInterface executionLogger = input.getExecutionLogger();
      if(executionLogger == null)
      {
         executionLogger = getDefaultExecutionLogger();
      }
      executionLogger.acceptExecutionStart(input);

      try
      {
         String languageExecutor = switch(codeReference.getCodeType())
         {
            case JAVA -> "com.kingsrook.qqq.backend.core.actions.scripts.QJavaExecutor";
            case JAVA_SCRIPT -> "com.kingsrook.qqq.languages.javascript.QJavaScriptExecutor";
         };

         @SuppressWarnings("unchecked")
         Class<? extends QCodeExecutor> executorClass = (Class<? extends QCodeExecutor>) Class.forName(languageExecutor);
         QCodeExecutor qCodeExecutor = executorClass.getConstructor().newInstance();

         ////////////////////////////////////////////////////////////////////////////////////////////////////
         // merge all of the input context, plus the input... input - into a context for the code executor //
         ////////////////////////////////////////////////////////////////////////////////////////////////////
         Map<String, Serializable> context = new HashMap<>();
         if(input.getContext() != null)
         {
            context.putAll(input.getContext());
         }
         if(input.getInput() != null)
         {
            context.putAll(input.getInput());
         }

         //////////////////////////////////////////
         // safely always set the deploymentMode //
         //////////////////////////////////////////
         context.put("deploymentMode", ObjectUtils.tryAndRequireNonNullElse(() -> QContext.getQInstance().getDeploymentMode(), null));

         /////////////////////////////////////////////////////////////////////////////////
         // set the qCodeExecutor into any context objects which are QCodeExecutorAware //
         /////////////////////////////////////////////////////////////////////////////////
         for(Serializable value : context.values())
         {
            if(value instanceof QCodeExecutorAware qCodeExecutorAware)
            {
               qCodeExecutorAware.setQCodeExecutor(qCodeExecutor);
            }
         }

         Serializable codeOutput = qCodeExecutor.execute(codeReference, context, executionLogger);
         output.setOutput(codeOutput);
         executionLogger.acceptExecutionEnd(codeOutput);
      }
      catch(QCodeException qCodeException)
      {
         executionLogger.acceptException(qCodeException);
         throw (qCodeException);
      }
      catch(Exception e)
      {
         executionLogger.acceptException(e);
         throw (new QException("Error executing code [" + codeReference + "]", e));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static ExecuteCodeInput setupExecuteCodeInput(AbstractRunScriptInput<?> input, ScriptRevision scriptRevision) throws QException
   {
      return setupExecuteCodeInput(input, scriptRevision, null);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static ExecuteCodeInput setupExecuteCodeInput(AbstractRunScriptInput<?> input, ScriptRevision scriptRevision, String fileName) throws QException
   {
      ExecuteCodeInput executeCodeInput = new ExecuteCodeInput();
      executeCodeInput.setInput(new HashMap<>(Objects.requireNonNullElseGet(input.getInputValues(), HashMap::new)));
      executeCodeInput.setContext(new HashMap<>());

      Map<String, Serializable> context = executeCodeInput.getContext();
      if(input.getOutputObject() != null)
      {
         context.put("output", input.getOutputObject());
      }

      if(input.getScriptUtils() != null)
      {
         context.put("scriptUtils", input.getScriptUtils());
      }

      if(CollectionUtils.nullSafeIsEmpty(scriptRevision.getFiles()))
      {
         QueryInput queryInput = new QueryInput();
         queryInput.setTableName(ScriptRevisionFile.TABLE_NAME);
         queryInput.setFilter(new QQueryFilter(new QFilterCriteria("scriptRevisionId", QCriteriaOperator.EQUALS, scriptRevision.getId())));
         QueryOutput queryOutput = new QueryAction().execute(queryInput);

         scriptRevision.setFiles(new ArrayList<>());
         for(QRecord record : queryOutput.getRecords())
         {
            scriptRevision.getFiles().add(new ScriptRevisionFile(record));
         }
      }

      List<ScriptRevisionFile> files = scriptRevision.getFiles();
      if(files == null || files.isEmpty())
      {
         throw (new QException("Script Revision " + scriptRevision.getId() + " did not have an associated ScriptRevisionFile."));
      }
      else
      {
         String contents = null;
         if(fileName == null || files.size() == 1)
         {
            contents = files.get(0).getContents();
         }
         else
         {
            for(ScriptRevisionFile file : files)
            {
               if(file.getFileName().equals(fileName))
               {
                  contents = file.getContents();
               }
            }
            if(contents == null)
            {
               throw (new QException("Could not find file named " + fileName + " for Script Revision " + scriptRevision.getId()));
            }
         }

         executeCodeInput.setCodeReference(new QCodeReference().withInlineCode(contents).withCodeType(QCodeType.JAVA_SCRIPT)); // todo - code type as attribute of script!!
      }

      ExecuteCodeAction.addApiUtilityToContext(context, scriptRevision);
      context.put("qqq", new QqqScriptUtils());
      ExecuteCodeAction.setExecutionLoggerInExecuteCodeInput(input, scriptRevision, executeCodeInput);

      return (executeCodeInput);
   }



   /*******************************************************************************
    ** Try to (dynamically) load the ApiScriptUtils object from the api middleware
    ** module -- in case the runtime doesn't have that module deployed (e.g, not in
    ** the project pom).
    *******************************************************************************/
   public static void addApiUtilityToContext(Map<String, Serializable> context, ScriptRevision scriptRevision)
   {
      addApiUtilityToContext(context, scriptRevision.getApiName(), scriptRevision.getApiVersion());
   }



   /*******************************************************************************
    ** Try to (dynamically) load the ApiScriptUtils object from the api middleware
    ** module -- in case the runtime doesn't have that module deployed (e.g, not in
    ** the project pom).
    *******************************************************************************/
   public static void addApiUtilityToContext(Map<String, Serializable> context, String apiName, String apiVersion)
   {
      if(!StringUtils.hasContent(apiName) || !StringUtils.hasContent(apiVersion))
      {
         return;
      }

      try
      {
         Class<?> apiScriptUtilsClass  = Class.forName("com.kingsrook.qqq.api.utils.ApiScriptUtils");
         Object   apiScriptUtilsObject = apiScriptUtilsClass.getConstructor(String.class, String.class).newInstance(apiName, apiVersion);
         context.put("api", (Serializable) apiScriptUtilsObject);
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
         LOG.warn("Error adding api utility to script context", e);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void setExecutionLoggerInExecuteCodeInput(AbstractRunScriptInput<?> input, ScriptRevision scriptRevision, ExecuteCodeInput executeCodeInput)
   {
      /////////////////////////////////////////////////////////////////////////////////////////////////
      // let caller supply a logger, or by default use StoreScriptLogAndScriptLogLineExecutionLogger //
      /////////////////////////////////////////////////////////////////////////////////////////////////
      QCodeExecutionLoggerInterface executionLogger = Objects.requireNonNullElseGet(input.getLogger(), () -> new StoreScriptLogAndScriptLogLineExecutionLogger(scriptRevision.getScriptId(), scriptRevision.getId()));
      executeCodeInput.setExecutionLogger(executionLogger);
      if(executionLogger instanceof ScriptExecutionLoggerInterface scriptExecutionLoggerInterface)
      {
         ////////////////////////////////////////////////////////////////////////////////////////////////////
         // if logger is aware of scripts (as opposed to a generic CodeExecution logger), give it the ids. //
         ////////////////////////////////////////////////////////////////////////////////////////////////////
         scriptExecutionLoggerInterface.setScriptId(scriptRevision.getScriptId());
         scriptExecutionLoggerInterface.setScriptRevisionId(scriptRevision.getId());
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QCodeExecutionLoggerInterface getDefaultExecutionLogger()
   {
      return (new Log4jCodeExecutionLogger());
   }

}
