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
import com.kingsrook.qqq.backend.core.actions.scripts.TestScriptActionInterface;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.scripts.StoreAssociatedScriptInput;
import com.kingsrook.qqq.backend.core.model.actions.scripts.StoreAssociatedScriptOutput;
import com.kingsrook.qqq.backend.core.model.actions.scripts.TestScriptInput;
import com.kingsrook.qqq.backend.core.model.actions.scripts.TestScriptOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.AssociatedScript;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.executors.utils.RecordDeveloperModeUtils;
import com.kingsrook.qqq.middleware.javalin.executors.utils.RecordDeveloperModeUtils.AssociatedScriptDetails;
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

         QRecord record = RecordDeveloperModeUtils.getRecord(getInput, tableName, primaryKey);

         Map<String, Serializable> rs = new HashMap<>();
         rs.put("record", record);

         ArrayList<HashMap<String, Serializable>> associatedScripts = new ArrayList<>();
         rs.put("associatedScripts", associatedScripts);

         for(AssociatedScriptDetails details : RecordDeveloperModeUtils.getAssociatedScripts(table, record, input -> QJavalinImplementation.setupSession(context, input)))
         {
            HashMap<String, Serializable> thisScriptData = new HashMap<>();
            associatedScripts.add(thisScriptData);
            thisScriptData.put("associatedScript", details.getAssociatedScript());
            thisScriptData.put("scriptType", details.getScriptType());

            if(details.getScript() != null)
            {
               thisScriptData.put("script", details.getScript());
               thisScriptData.put("scriptRevisions", new ArrayList<>(details.getScriptRevisions()));
            }

            if(details.getTestInputFields() != null)
            {
               thisScriptData.put("testInputFields", new ArrayList<>(details.getTestInputFields()));
               thisScriptData.put("testOutputFields", new ArrayList<>(details.getTestOutputFields()));
            }
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
   public static void getAssociatedScriptLogs(Context context)
   {
      try
      {
         String scriptRevisionId = context.pathParam("scriptRevisionId");
         QJavalinAccessLogger.logStart("getAssociatedScriptLogs", logPair("scriptRevisionId", scriptRevisionId));

         QueryInput queryInput = new QueryInput();
         QJavalinImplementation.setupSession(context, queryInput);

         getReferencedRecordToEnsureAccess(context);

         Map<String, Serializable> rs = new HashMap<>();
         rs.put("scriptLogRecords", new ArrayList<>(RecordDeveloperModeUtils.getScriptLogRecords(queryInput, scriptRevisionId)));

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
      RecordDeveloperModeUtils.checkRecordIsReadable(context.pathParam("table"), context.pathParam("primaryKey"), input -> QJavalinImplementation.setupSession(context, input));
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

         StoreAssociatedScriptOutput output = RecordDeveloperModeUtils.storeAssociatedScript(input);

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
