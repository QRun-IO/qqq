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

package com.kingsrook.qqq.middleware.javalin;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.actions.scripts.StoreAssociatedScriptAction;
import com.kingsrook.qqq.backend.core.actions.scripts.TestScriptActionInterface;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QNotFoundException;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.scripts.StoreAssociatedScriptInput;
import com.kingsrook.qqq.backend.core.model.actions.scripts.StoreAssociatedScriptOutput;
import com.kingsrook.qqq.backend.core.model.actions.scripts.TestScriptInput;
import com.kingsrook.qqq.backend.core.model.actions.scripts.TestScriptOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.AssociatedScript;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.scripts.Script;
import com.kingsrook.qqq.backend.core.model.scripts.ScriptType;
import com.kingsrook.qqq.backend.core.processes.utils.GeneralProcessUtils;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;
import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.post;


/*******************************************************************************
 ** endpoints and handlers for deal with record scripts
 *******************************************************************************/
public class QJavalinScriptsHandler
{
   private static final QLogger LOG = QLogger.getLogger(QJavalinScriptsHandler.class);



   /*******************************************************************************
    ** Define routes under the basic /data/${table}/${primaryKey} path - e.g.,
    ** record-specific script routes.
    *******************************************************************************/
   public static void defineRecordRoutes()
   {
      // todo - do we want some generic "developer mode" permission??
      get("/developer", QJavalinScriptsHandler::getRecordDeveloperMode);
      post("/developer/associatedScript/{fieldName}", QJavalinScriptsHandler::storeRecordAssociatedScript);
      get("/developer/associatedScript/{fieldName}/{scriptRevisionId}/logs", QJavalinScriptsHandler::getAssociatedScriptLogs);
      post("/developer/associatedScript/{fieldName}/test", QJavalinScriptsHandler::testAssociatedScript);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void getRecordDeveloperMode(Context context)
   {
      try
      {
         String         tableName  = context.pathParam("table");
         QTableMetaData table      = QJavalinImplementation.qInstance.getTable(tableName);
         String         primaryKey = context.pathParam("primaryKey");
         GetInput       getInput   = new GetInput();

         QJavalinImplementation.setupSession(context, getInput);
         QJavalinAccessLogger.logStart("getRecordDeveloperMode", logPair("table", tableName), logPair("primaryKey", primaryKey));
         getInput.setTableName(tableName);
         getInput.setShouldGenerateDisplayValues(true);
         getInput.setShouldTranslatePossibleValues(true);

         PermissionsHelper.checkTablePermissionThrowing(getInput, TablePermissionSubType.READ);

         // todo - validate that the primary key is of the proper type (e.g,. not a string for an id field)
         //  and throw a 400-series error (tell the user bad-request), rather than, we're doing a 500 (server error)

         getInput.setPrimaryKey(primaryKey);

         GetAction getAction = new GetAction();
         GetOutput getOutput = getAction.execute(getInput);

         ///////////////////////////////////////////////////////
         // throw a not found error if the record isn't found //
         ///////////////////////////////////////////////////////
         QRecord record = getOutput.getRecord();
         if(record == null)
         {
            throw (new QNotFoundException("Could not find " + table.getLabel() + " with "
               + table.getFields().get(table.getPrimaryKeyField()).getLabel() + " of " + primaryKey));
         }

         Map<String, Serializable> rs = new HashMap<>();
         rs.put("record", record);

         ArrayList<HashMap<String, Serializable>> associatedScripts = new ArrayList<>();
         rs.put("associatedScripts", associatedScripts);

         QTableMetaData scriptTypeTable     = QContext.getQInstance().getTable(ScriptType.TABLE_NAME);
         QTableMetaData scriptRevisionTable = QContext.getQInstance().getTable(ScriptType.TABLE_NAME);
         QTableMetaData scriptTable         = QContext.getQInstance().getTable(Script.TABLE_NAME);
         if(scriptTypeTable != null && scriptTable != null && scriptRevisionTable != null)
         {
            Map<Serializable, QRecord> scriptTypeMap = GeneralProcessUtils.loadTableToMap(ScriptType.TABLE_NAME, "id");

            ///////////////////////////////////////////////////////
            // process each associated script type for the table //
            ///////////////////////////////////////////////////////
            QInstanceEnricher qInstanceEnricher = new QInstanceEnricher(QJavalinImplementation.qInstance);
            for(AssociatedScript associatedScript : CollectionUtils.nonNullList(table.getAssociatedScripts()))
            {
               HashMap<String, Serializable> thisScriptData = new HashMap<>();
               associatedScripts.add(thisScriptData);
               thisScriptData.put("associatedScript", associatedScript);
               thisScriptData.put("scriptType", scriptTypeMap.get(associatedScript.getScriptTypeId()));

               /////////////////////////////////////////////////////////////////////
               // load the associated script and current revision from the record //
               /////////////////////////////////////////////////////////////////////
               String       fieldName = associatedScript.getFieldName();
               Serializable scriptId  = record.getValue(fieldName);
               if(scriptId != null)
               {
                  GetInput getScriptInput = new GetInput();
                  QJavalinImplementation.setupSession(context, getScriptInput);
                  getScriptInput.setTableName("script");
                  getScriptInput.setPrimaryKey(scriptId);
                  GetOutput getScriptOutput = new GetAction().execute(getScriptInput);
                  if(getScriptOutput.getRecord() != null)
                  {
                     thisScriptData.put("script", getScriptOutput.getRecord());

                     QueryInput queryInput = new QueryInput();
                     QJavalinImplementation.setupSession(context, queryInput);
                     queryInput.setTableName("scriptRevision");
                     queryInput.setFilter(new QQueryFilter()
                        .withCriteria(new QFilterCriteria("scriptId", QCriteriaOperator.EQUALS, List.of(getScriptOutput.getRecord().getValue("id"))))
                        .withOrderBy(new QFilterOrderBy("id", false))
                     );
                     QueryOutput queryOutput = new QueryAction().execute(queryInput);
                     thisScriptData.put("scriptRevisions", new ArrayList<>(queryOutput.getRecords()));
                  }
               }

               ///////////////////////////////////////////////////////////
               // load testing info about the script type, if available //
               ///////////////////////////////////////////////////////////
               QCodeReference scriptTesterCodeRef = associatedScript.getScriptTester();
               if(scriptTesterCodeRef != null)
               {
                  TestScriptActionInterface scriptTester = QCodeLoader.getAdHoc(TestScriptActionInterface.class, scriptTesterCodeRef);
                  thisScriptData.put("testInputFields", enrichFieldsToArrayList(qInstanceEnricher, scriptTester.getTestInputFields()));
                  thisScriptData.put("testOutputFields", enrichFieldsToArrayList(qInstanceEnricher, scriptTester.getTestOutputFields()));
               }
            }
         }
         else
         {
            LOG.info("One or more script tables was not found in the instance.");
         }

         QJavalinAccessLogger.logEndSuccess();
         context.result(JsonUtils.toJson(rs));
      }
      catch(Exception e)
      {
         QJavalinAccessLogger.logEndFail(e);
         QJavalinImplementation.handleException(context, e);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static Serializable enrichFieldsToArrayList(QInstanceEnricher qInstanceEnricher, List<QFieldMetaData> fields)
   {
      ArrayList<QFieldMetaData> rs = new ArrayList<>();

      if(CollectionUtils.nullSafeIsEmpty(fields))
      {
         return (rs);
      }

      for(QFieldMetaData field : fields)
      {
         qInstanceEnricher.enrichField(field);
         rs.add(field);
      }

      return (rs);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void getAssociatedScriptLogs(Context context)
   {
      try
      {
         String scriptRevisionId = context.pathParam("scriptRevisionId");
         QJavalinAccessLogger.logStart("getAssociatedScriptLogs", logPair("scriptRevisionId", scriptRevisionId));

         QueryInput queryInput = new QueryInput();
         QJavalinImplementation.setupSession(context, queryInput);

         getReferencedRecordToEnsureAccess(context);

         queryInput.setTableName("scriptLog");
         queryInput.setFilter(new QQueryFilter()
            .withCriteria(new QFilterCriteria("scriptRevisionId", QCriteriaOperator.EQUALS, List.of(scriptRevisionId)))
            .withOrderBy(new QFilterOrderBy("id", false))
            .withLimit(100));
         QueryOutput queryOutput = new QueryAction().execute(queryInput);

         if(CollectionUtils.nullSafeHasContents(queryOutput.getRecords()))
         {
            GeneralProcessUtils.addForeignRecordsListToRecordList(queryOutput.getRecords(), "id", "scriptLogLine", "scriptLogId");
         }

         Map<String, Serializable> rs = new HashMap<>();
         rs.put("scriptLogRecords", new ArrayList<>(queryOutput.getRecords()));

         QJavalinAccessLogger.logEndSuccess();
         context.result(JsonUtils.toJson(rs));
      }
      catch(Exception e)
      {
         QJavalinAccessLogger.logEndFail(e);
         QJavalinImplementation.handleException(context, e);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void getReferencedRecordToEnsureAccess(Context context) throws QException
   {
      /////////////////////////////////////////////////////////////////////////////////
      // make sure user can get the record they're trying to do a related action for //
      /////////////////////////////////////////////////////////////////////////////////
      String         tableName = context.pathParam("table");
      QTableMetaData table     = QJavalinImplementation.qInstance.getTable(tableName);
      GetInput       getInput  = new GetInput();
      getInput.setTableName(tableName);
      QJavalinImplementation.setupSession(context, getInput);
      PermissionsHelper.checkTablePermissionThrowing(getInput, TablePermissionSubType.READ);

      String primaryKey = context.pathParam("primaryKey");
      getInput.setPrimaryKey(primaryKey);

      GetAction getAction = new GetAction();
      GetOutput getOutput = getAction.execute(getInput);

      ///////////////////////////////////////////////////////
      // throw a not found error if the record isn't found //
      ///////////////////////////////////////////////////////
      QRecord record = getOutput.getRecord();
      if(record == null)
      {
         throw (new QNotFoundException("Could not find " + table.getLabel() + " with "
            + table.getFields().get(table.getPrimaryKeyField()).getLabel() + " of " + primaryKey));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void storeRecordAssociatedScript(Context context)
   {
      context.contentType(ContentType.APPLICATION_JSON);

      try
      {
         StoreAssociatedScriptInput input = new StoreAssociatedScriptInput();
         QJavalinImplementation.setupSession(context, input);

         String fieldName  = context.pathParam("fieldName");
         String table      = context.pathParam("table");
         String primaryKey = context.pathParam("primaryKey");

         input.setCode(context.formParam("contents"));
         input.setCommitMessage(context.formParam("commitMessage"));
         input.setFieldName(fieldName);
         input.setTableName(table);
         input.setRecordPrimaryKey(primaryKey);
         QJavalinAccessLogger.logStart("storeRecordAssociatedScript", logPair("table", table), logPair("fieldName", fieldName), logPair("primaryKey", primaryKey));

         PermissionsHelper.checkTablePermissionThrowing(input, TablePermissionSubType.EDIT); // todo ... is this enough??

         StoreAssociatedScriptOutput output = new StoreAssociatedScriptOutput();

         StoreAssociatedScriptAction storeAssociatedScriptAction = new StoreAssociatedScriptAction();
         storeAssociatedScriptAction.run(input, output);

         QJavalinAccessLogger.logEndSuccess();
         context.result(JsonUtils.toJson(output));
      }
      catch(Exception e)
      {
         QJavalinAccessLogger.logEndFail(e);
         QJavalinImplementation.handleException(context, e);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void testAssociatedScript(Context context)
   {
      context.contentType(ContentType.APPLICATION_JSON);

      try
      {
         TestScriptInput input = new TestScriptInput();
         QJavalinImplementation.setupSession(context, input);

         getReferencedRecordToEnsureAccess(context);

         // todo delete? input.setRecordPrimaryKey(context.pathParam("primaryKey"));
         Map<String, Serializable> inputValues = new HashMap<>();
         input.setInputValues(inputValues);

         String         tableName = context.pathParam("table");
         String         fieldName = context.pathParam("fieldName");
         QTableMetaData table     = QJavalinImplementation.qInstance.getTable(tableName);
         QJavalinAccessLogger.logStart("testAssociatedScript", logPair("table", tableName), logPair("fieldName", fieldName));

         Optional<AssociatedScript> optionalAssociatedScript = table.getAssociatedScripts().stream().filter(as -> as.getFieldName().equals(fieldName)).findFirst();
         if(optionalAssociatedScript.isEmpty())
         {
            throw new IllegalArgumentException("No associated script was found for field " + fieldName + " on table " + tableName);
         }
         AssociatedScript associatedScript = optionalAssociatedScript.get();

         QCodeReference scriptTesterCodeRef = associatedScript.getScriptTester();
         if(scriptTesterCodeRef == null)
         {
            throw (new IllegalArgumentException("This scriptType cannot be tested, as it does not define a scriptTester codeReference."));
         }

         for(Map.Entry<String, List<String>> entry : context.formParamMap().entrySet())
         {
            String key   = entry.getKey();
            String value = entry.getValue().get(0);

            switch(key)
            {
               case "code" -> input.setCodeReference(new QCodeReference().withInlineCode(value).withCodeType(QCodeType.JAVA_SCRIPT));
               case "apiName" -> input.setApiName(value);
               case "apiVersion" -> input.setApiVersion(value);
               default -> inputValues.put(key, value);
            }
         }

         TestScriptActionInterface scriptTester = QCodeLoader.getAdHoc(TestScriptActionInterface.class, scriptTesterCodeRef);
         TestScriptOutput          output       = new TestScriptOutput();

         scriptTester.execute(input, output);

         QJavalinAccessLogger.logEndSuccess();
         context.result(JsonUtils.toJson(output));
      }
      catch(Exception e)
      {
         QJavalinAccessLogger.logEndFail(e);
         QJavalinImplementation.handleException(context, e);
      }
   }
}
