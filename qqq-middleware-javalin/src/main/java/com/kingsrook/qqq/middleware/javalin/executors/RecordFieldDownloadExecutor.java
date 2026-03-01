/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.middleware.javalin.executors;


import java.io.Serializable;
import java.util.Map;
import java.util.Optional;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.actions.processes.QProcessCallbackFactory;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QNotFoundException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetOutput;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.AdornmentType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.FieldAdornment;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import com.kingsrook.qqq.middleware.javalin.executors.io.RecordFieldDownloadInput;
import com.kingsrook.qqq.middleware.javalin.executors.io.RecordFieldDownloadOutputInterface;
import com.kingsrook.qqq.middleware.javalin.misc.DownloadFileSupplementalAction;


/*******************************************************************************
 ** Executor for the record field download endpoint. Retrieves a record's blob
 ** field and provides the binary content for download.
 *******************************************************************************/
public class RecordFieldDownloadExecutor extends AbstractMiddlewareExecutor<RecordFieldDownloadInput, RecordFieldDownloadOutputInterface>
{
   private static final QLogger LOG = QLogger.getLogger(RecordFieldDownloadExecutor.class);



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void execute(RecordFieldDownloadInput input, RecordFieldDownloadOutputInterface output) throws QException
   {
      String tableName  = input.getTableName();
      String primaryKey = input.getPrimaryKey();
      String fieldName  = input.getFieldName();
      String filename   = input.getFilename();

      QTableMetaData table = QContext.getQInstance().getTable(tableName);
      if(table == null)
      {
         throw (new QNotFoundException("Could not find table named " + tableName));
      }

      ////////////////////////////////////////////
      // validate field name - 404 if not found //
      ////////////////////////////////////////////
      QFieldMetaData fieldMetaData;
      try
      {
         fieldMetaData = table.getField(fieldName);
      }
      catch(Exception e)
      {
         throw (new QNotFoundException("Could not find field named " + fieldName + " on table " + tableName));
      }

      GetInput getInput = new GetInput();
      getInput.setTableName(tableName);
      getInput.setShouldFetchHeavyFields(true);

      PermissionsHelper.checkTablePermissionThrowing(getInput, TablePermissionSubType.READ);

      getInput.setPrimaryKey(primaryKey);

      GetAction getAction = new GetAction();
      GetOutput getOutput = getAction.execute(getInput);

      ///////////////////////////////////////////////////////
      // throw a not found error if the record isn't found //
      ///////////////////////////////////////////////////////
      if(getOutput.getRecord() == null)
      {
         throw (new QNotFoundException("Could not find " + table.getLabel() + " with "
            + table.getFields().get(table.getPrimaryKeyField()).getLabel() + " of " + primaryKey));
      }

      //////////////////////////////////////////////////////////////
      // determine content type from field adornment if available //
      //////////////////////////////////////////////////////////////
      String                           mimeType              = null;
      Optional<FieldAdornment>         fileDownloadAdornment = fieldMetaData.getAdornments().stream().filter(a -> a.getType().equals(AdornmentType.FILE_DOWNLOAD)).findFirst();
      Map<String, Serializable>        adornmentValues       = null;
      if(fileDownloadAdornment.isPresent())
      {
         adornmentValues = fileDownloadAdornment.get().getValues();
         mimeType = ValueUtils.getValueAsString(adornmentValues.get(AdornmentType.FileDownloadValues.DEFAULT_MIME_TYPE));
      }

      if(mimeType != null)
      {
         output.setContentType(mimeType);
      }

      output.setFilename(filename);

      //////////////////////////////////////////////////////////////////////////////////////////////
      // if the adornment has a supplemental process name in it, or a supplemental code reference //
      // then execute that custom code, e.g., to log that the file was downloaded.                //
      //////////////////////////////////////////////////////////////////////////////////////////////
      if(fileDownloadAdornment.isPresent())
      {
         String processName = ValueUtils.getValueAsString(adornmentValues.get(AdornmentType.FileDownloadValues.SUPPLEMENTAL_PROCESS_NAME));
         if(StringUtils.hasContent(processName))
         {
            RunProcessInput runProcessInput = new RunProcessInput();
            runProcessInput.setProcessName(processName);
            runProcessInput.setCallback(QProcessCallbackFactory.forRecord(getOutput.getRecord()));
            runProcessInput.setFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.SKIP);
            runProcessInput.addValue("tableName", tableName);
            runProcessInput.addValue("primaryKey", primaryKey);
            runProcessInput.addValue("fieldName", fieldName);
            runProcessInput.addValue("filename", filename);
            new RunProcessAction().execute(runProcessInput);
         }
         else if(adornmentValues.containsKey(AdornmentType.FileDownloadValues.SUPPLEMENTAL_CODE_REFERENCE))
         {
            QCodeReference codeReference = (QCodeReference) adornmentValues.get(AdornmentType.FileDownloadValues.SUPPLEMENTAL_CODE_REFERENCE);

            DownloadFileSupplementalAction action = QCodeLoader.getAdHoc(DownloadFileSupplementalAction.class, codeReference);

            DownloadFileSupplementalAction.DownloadFileSupplementalActionInput supplementalInput = new DownloadFileSupplementalAction.DownloadFileSupplementalActionInput()
               .withTableName(tableName)
               .withFieldName(fieldName)
               .withPrimaryKey(primaryKey)
               .withFileName(filename);

            DownloadFileSupplementalAction.DownloadFileSupplementalActionOutput supplementalOutput = new DownloadFileSupplementalAction.DownloadFileSupplementalActionOutput();
            action.run(supplementalInput, supplementalOutput);
         }
      }

      /////////////////////////////////////////////////////////
      // if the field is a BLOB - send the bytes to the user //
      /////////////////////////////////////////////////////////
      if(QFieldType.BLOB.equals(fieldMetaData.getType()))
      {
         byte[] bytes = getOutput.getRecord().getValueByteArray(fieldName);
         output.setBytes(bytes);
      }
      else
      {
         //////////////////////////////////////////////////////////////////
         // else - assume a string is a URL - and issue a redirect to it //
         //////////////////////////////////////////////////////////////////
         String value = getOutput.getRecord().getValueString(fieldName);
         if(value != null)
         {
            output.setRedirectUrl(value);
         }
      }
   }

}
