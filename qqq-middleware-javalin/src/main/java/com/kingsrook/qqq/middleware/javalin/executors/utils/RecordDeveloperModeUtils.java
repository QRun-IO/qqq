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

package com.kingsrook.qqq.middleware.javalin.executors.utils;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.actions.scripts.StoreAssociatedScriptInput;
import com.kingsrook.qqq.backend.core.model.actions.scripts.StoreAssociatedScriptOutput;
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
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.AssociatedScript;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.scripts.Script;
import com.kingsrook.qqq.backend.core.model.scripts.ScriptType;
import com.kingsrook.qqq.backend.core.processes.utils.GeneralProcessUtils;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.lambdas.UnsafeConsumer;


/*******************************************************************************
 ** Record "developer mode" logic - a record's associated scripts with their
 ** revisions and test fields, a script revision's logs, and storing a new
 ** revision - shared by the legacy script routes (QJavalinScriptsHandler) and
 ** the v1 developer-mode specs, so that both apply the same permission checks
 ** and return the same data.
 **
 ** Methods that read more than one table take a sessionSetup, which (when not
 ** null) is run on each action input before it is used:  the legacy routes
 ** establish their session per input; v1 has one session per request.
 *******************************************************************************/
public class RecordDeveloperModeUtils
{
   private static final QLogger LOG = QLogger.getLogger(RecordDeveloperModeUtils.class);

   public static final int SCRIPT_LOG_LIMIT = 100;



   /*******************************************************************************
    ** Get the record that developer mode is for:  requires READ permission on its
    ** table, generates display values and translates possible values, and throws
    ** not-found if there is no such record.
    **
    ** @param getInput a new GetInput (which the caller may already have used to
    ** set up its session)
    *******************************************************************************/
   public static QRecord getRecord(GetInput getInput, String tableName, String primaryKey) throws QException
   {
      getInput.setTableName(tableName);
      getInput.setShouldGenerateDisplayValues(true);
      getInput.setShouldTranslatePossibleValues(true);

      PermissionsHelper.checkTablePermissionThrowing(getInput, TablePermissionSubType.READ);

      // todo - validate that the primary key is of the proper type (e.g,. not a string for an id field)
      //  and throw a 400-series error (tell the user bad-request), rather than, we're doing a 500 (server error)

      getInput.setPrimaryKey(primaryKey);
      GetOutput getOutput = new GetAction().execute(getInput);

      ///////////////////////////////////////////////////////
      // throw a not found error if the record isn't found //
      ///////////////////////////////////////////////////////
      QRecord record = getOutput.getRecord();
      if(record == null)
      {
         throw (recordNotFound(tableName, primaryKey));
      }

      return (record);
   }



   /*******************************************************************************
    ** Make sure the user can get the record that they're trying to do a related
    ** (script) action for:  requires READ permission on its table, and throws
    ** not-found if there is no such record.
    *******************************************************************************/
   public static void checkRecordIsReadable(String tableName, String primaryKey, UnsafeConsumer<AbstractActionInput, QException> sessionSetup) throws QException
   {
      GetInput getInput = new GetInput();
      getInput.setTableName(tableName);
      runSessionSetup(sessionSetup, getInput);
      PermissionsHelper.checkTablePermissionThrowing(getInput, TablePermissionSubType.READ);

      getInput.setPrimaryKey(primaryKey);
      GetOutput getOutput = new GetAction().execute(getInput);

      ///////////////////////////////////////////////////////
      // throw a not found error if the record isn't found //
      ///////////////////////////////////////////////////////
      if(getOutput.getRecord() == null)
      {
         throw (recordNotFound(tableName, primaryKey));
      }
   }



   /*******************************************************************************
    ** Load the details of each of a table's associated scripts, for a record of
    ** that table:  the script type, the script the record references (if any)
    ** with its revisions (newest first), and the script tester's input and output
    ** fields (if the associated script has a tester).  Empty if the instance does
    ** not have the script tables.
    *******************************************************************************/
   public static List<AssociatedScriptDetails> getAssociatedScripts(QTableMetaData table, QRecord record, UnsafeConsumer<AbstractActionInput, QException> sessionSetup) throws QException
   {
      List<AssociatedScriptDetails> associatedScripts = new ArrayList<>();

      QTableMetaData scriptTypeTable     = QContext.getQInstance().getTable(ScriptType.TABLE_NAME);
      QTableMetaData scriptRevisionTable = QContext.getQInstance().getTable(ScriptType.TABLE_NAME);
      QTableMetaData scriptTable         = QContext.getQInstance().getTable(Script.TABLE_NAME);
      if(scriptTypeTable == null || scriptTable == null || scriptRevisionTable == null)
      {
         LOG.info("One or more script tables was not found in the instance.");
         return (associatedScripts);
      }

      Map<Serializable, QRecord> scriptTypeMap = GeneralProcessUtils.loadTableToMap(ScriptType.TABLE_NAME, "id");

      ///////////////////////////////////////////////////////
      // process each associated script type for the table //
      ///////////////////////////////////////////////////////
      QInstanceEnricher qInstanceEnricher = new QInstanceEnricher(QContext.getQInstance());
      for(AssociatedScript associatedScript : CollectionUtils.nonNullList(table.getAssociatedScripts()))
      {
         AssociatedScriptDetails details = new AssociatedScriptDetails();
         associatedScripts.add(details);
         details.setAssociatedScript(associatedScript);
         details.setScriptType(scriptTypeMap.get(associatedScript.getScriptTypeId()));

         /////////////////////////////////////////////////////////////////////
         // load the associated script and current revision from the record //
         /////////////////////////////////////////////////////////////////////
         String       fieldName = associatedScript.getFieldName();
         Serializable scriptId  = record.getValue(fieldName);
         if(scriptId != null)
         {
            GetInput getScriptInput = new GetInput();
            runSessionSetup(sessionSetup, getScriptInput);
            getScriptInput.setTableName("script");
            getScriptInput.setPrimaryKey(scriptId);
            GetOutput getScriptOutput = new GetAction().execute(getScriptInput);
            if(getScriptOutput.getRecord() != null)
            {
               details.setScript(getScriptOutput.getRecord());

               QueryInput queryInput = new QueryInput();
               runSessionSetup(sessionSetup, queryInput);
               queryInput.setTableName("scriptRevision");
               queryInput.setFilter(new QQueryFilter()
                  .withCriteria(new QFilterCriteria("scriptId", QCriteriaOperator.EQUALS, List.of(getScriptOutput.getRecord().getValue("id"))))
                  .withOrderBy(new QFilterOrderBy("id", false))
               );
               QueryOutput queryOutput = new QueryAction().execute(queryInput);
               details.setScriptRevisions(new ArrayList<>(queryOutput.getRecords()));
            }
         }

         ///////////////////////////////////////////////////////////
         // load testing info about the script type, if available //
         ///////////////////////////////////////////////////////////
         QCodeReference scriptTesterCodeRef = associatedScript.getScriptTester();
         if(scriptTesterCodeRef != null)
         {
            TestScriptActionInterface scriptTester = QCodeLoader.getAdHoc(TestScriptActionInterface.class, scriptTesterCodeRef);
            details.setTestInputFields(enrichFields(qInstanceEnricher, scriptTester.getTestInputFields()));
            details.setTestOutputFields(enrichFields(qInstanceEnricher, scriptTester.getTestOutputFields()));
         }
      }

      return (associatedScripts);
   }



   /*******************************************************************************
    ** Get the (newest, up to 100) logs of a script revision, each with its log
    ** lines as a list of records in its scriptLogLine value.
    **
    ** @param queryInput a new QueryInput (which the caller may already have used
    ** to set up its session)
    *******************************************************************************/
   public static List<QRecord> getScriptLogRecords(QueryInput queryInput, String scriptRevisionId) throws QException
   {
      queryInput.setTableName("scriptLog");
      queryInput.setFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria("scriptRevisionId", QCriteriaOperator.EQUALS, List.of(scriptRevisionId)))
         .withOrderBy(new QFilterOrderBy("id", false))
         .withLimit(SCRIPT_LOG_LIMIT));
      QueryOutput queryOutput = new QueryAction().execute(queryInput);

      if(CollectionUtils.nullSafeHasContents(queryOutput.getRecords()))
      {
         GeneralProcessUtils.addForeignRecordsListToRecordList(queryOutput.getRecords(), "id", "scriptLogLine", "scriptLogId");
      }

      return (new ArrayList<>(queryOutput.getRecords()));
   }



   /*******************************************************************************
    ** Store a new revision of a record's associated script:  requires EDIT
    ** permission on the record's table (checked before anything is stored).
    *******************************************************************************/
   public static StoreAssociatedScriptOutput storeAssociatedScript(StoreAssociatedScriptInput input) throws QException
   {
      PermissionsHelper.checkTablePermissionThrowing(input, TablePermissionSubType.EDIT); // todo ... is this enough??

      StoreAssociatedScriptOutput output = new StoreAssociatedScriptOutput();
      new StoreAssociatedScriptAction().run(input, output);
      return (output);
   }



   /*******************************************************************************
    ** The not-found error for a record, as the legacy routes word it.
    *******************************************************************************/
   private static QNotFoundException recordNotFound(String tableName, String primaryKey)
   {
      QTableMetaData table = QContext.getQInstance().getTable(tableName);
      return (new QNotFoundException("Could not find " + table.getLabel() + " with "
         + table.getFields().get(table.getPrimaryKeyField()).getLabel() + " of " + primaryKey));
   }



   /*******************************************************************************
    ** Enrich fields (e.g., to give them labels), as a new list (empty for none).
    *******************************************************************************/
   private static ArrayList<QFieldMetaData> enrichFields(QInstanceEnricher qInstanceEnricher, List<QFieldMetaData> fields)
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
    ** Run the (optional) session setup on an action input.
    *******************************************************************************/
   private static void runSessionSetup(UnsafeConsumer<AbstractActionInput, QException> sessionSetup, AbstractActionInput input) throws QException
   {
      if(sessionSetup != null)
      {
         sessionSetup.run(input);
      }
   }



   /*******************************************************************************
    ** Everything developer mode shows about one of a table's associated scripts,
    ** for one record.  script and scriptRevisions are null when the record does
    ** not reference an existing script; the test fields are null when the
    ** associated script has no tester.
    *******************************************************************************/
   public static class AssociatedScriptDetails
   {
      private AssociatedScript     associatedScript;
      private QRecord              scriptType;
      private QRecord              script;
      private List<QRecord>        scriptRevisions;
      private List<QFieldMetaData> testInputFields;
      private List<QFieldMetaData> testOutputFields;



      /*******************************************************************************
       ** Getter for associatedScript
       *******************************************************************************/
      public AssociatedScript getAssociatedScript()
      {
         return (this.associatedScript);
      }



      /*******************************************************************************
       ** Setter for associatedScript
       *******************************************************************************/
      public void setAssociatedScript(AssociatedScript associatedScript)
      {
         this.associatedScript = associatedScript;
      }



      /*******************************************************************************
       ** Getter for scriptType
       *******************************************************************************/
      public QRecord getScriptType()
      {
         return (this.scriptType);
      }



      /*******************************************************************************
       ** Setter for scriptType
       *******************************************************************************/
      public void setScriptType(QRecord scriptType)
      {
         this.scriptType = scriptType;
      }



      /*******************************************************************************
       ** Getter for script
       *******************************************************************************/
      public QRecord getScript()
      {
         return (this.script);
      }



      /*******************************************************************************
       ** Setter for script
       *******************************************************************************/
      public void setScript(QRecord script)
      {
         this.script = script;
      }



      /*******************************************************************************
       ** Getter for scriptRevisions
       *******************************************************************************/
      public List<QRecord> getScriptRevisions()
      {
         return (this.scriptRevisions);
      }



      /*******************************************************************************
       ** Setter for scriptRevisions
       *******************************************************************************/
      public void setScriptRevisions(List<QRecord> scriptRevisions)
      {
         this.scriptRevisions = scriptRevisions;
      }



      /*******************************************************************************
       ** Getter for testInputFields
       *******************************************************************************/
      public List<QFieldMetaData> getTestInputFields()
      {
         return (this.testInputFields);
      }



      /*******************************************************************************
       ** Setter for testInputFields
       *******************************************************************************/
      public void setTestInputFields(List<QFieldMetaData> testInputFields)
      {
         this.testInputFields = testInputFields;
      }



      /*******************************************************************************
       ** Getter for testOutputFields
       *******************************************************************************/
      public List<QFieldMetaData> getTestOutputFields()
      {
         return (this.testOutputFields);
      }



      /*******************************************************************************
       ** Setter for testOutputFields
       *******************************************************************************/
      public void setTestOutputFields(List<QFieldMetaData> testOutputFields)
      {
         this.testOutputFields = testOutputFields;
      }
   }

}
