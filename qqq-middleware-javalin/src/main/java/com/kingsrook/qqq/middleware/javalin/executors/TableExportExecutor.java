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


import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Locale;
import com.kingsrook.qqq.backend.core.actions.async.AsyncJobManager;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.actions.reporting.ExportAction;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ExportInput;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportDestination;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportFormat;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.middleware.javalin.executors.io.TableExportInput;
import com.kingsrook.qqq.middleware.javalin.executors.io.TableExportOutputInterface;


/*******************************************************************************
 ** Executor for the table export endpoint. Manages the piped stream setup
 ** and asynchronous execution of the export action.
 *******************************************************************************/
public class TableExportExecutor extends AbstractMiddlewareExecutor<TableExportInput, TableExportOutputInterface>
{
   private static final QLogger LOG = QLogger.getLogger(TableExportExecutor.class);



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void execute(TableExportInput input, TableExportOutputInterface output) throws QException
   {
      try
      {
         ////////////////////////////////////////
         // resolve the report format to use   //
         ////////////////////////////////////////
         ReportFormat reportFormat = ReportFormat.fromString(input.getFormat());

         ////////////////////////////////////////////////////////////////////
         // generate filename if not provided: tableName + "." + extension //
         ////////////////////////////////////////////////////////////////////
         String filename = input.getFilename();
         if(!StringUtils.hasContent(filename))
         {
            filename = input.getTableName() + "." + reportFormat.toString().toLowerCase(Locale.ROOT);
         }

         /////////////////////////////////////////////
         // set up the report action's input object //
         /////////////////////////////////////////////
         ExportInput exportInput = new ExportInput();
         exportInput.setTableName(input.getTableName());
         exportInput.setInputSource(QInputSource.USER);

         PermissionsHelper.checkTablePermissionThrowing(exportInput, TablePermissionSubType.READ);

         exportInput.setQueryFilter(input.getFilter());
         exportInput.setFieldNames(input.getFieldNames());
         exportInput.setLimit(input.getLimit());

         if(input.getIncludeHeaderRow() != null)
         {
            exportInput.setIncludeHeaderRow(input.getIncludeHeaderRow());
         }

         exportInput.withReportDestination(new ReportDestination()
            .withReportFormat(reportFormat)
            .withFilename(filename));

         ///////////////////////////////////////////////////////////////////////////////////////////////////////
         // set up the I/O pipe streams.                                                                      //
         // Critically, we must NOT open the outputStream in a try-with-resources. The thread that writes to  //
         // the stream must close it when it's done writing.                                                  //
         ///////////////////////////////////////////////////////////////////////////////////////////////////////
         PipedOutputStream pipedOutputStream = new PipedOutputStream();
         PipedInputStream  pipedInputStream  = new PipedInputStream();
         pipedOutputStream.connect(pipedInputStream);

         exportInput.getReportDestination().setReportOutputStream(pipedOutputStream);

         /////////////////////////////////////////////////////
         // run pre-execute synchronously for validation    //
         /////////////////////////////////////////////////////
         ExportAction exportAction = new ExportAction();
         exportAction.preExecute(exportInput);

         /////////////////////////////////////////////////////////////////////////////////////////////////////
         // start the async job.                                                                            //
         // Critically, this must happen before the pipedInputStream is passed to the javalin result method //
         /////////////////////////////////////////////////////////////////////////////////////////////////////
         CapturedContext capturedContext = QContext.capture();
         new AsyncJobManager().startJob("Javalin>ExportAction", (o) ->
         {
            try
            {
               QContext.init(capturedContext);
               exportAction.execute(exportInput);
               return (true);
            }
            catch(Exception e)
            {
               LOG.warn("Exception in export async job", e);
               pipedOutputStream.close();
               return (false);
            }
         });

         ///////////////////////////////////////////////////
         // set the output fields for the spec to consume //
         ///////////////////////////////////////////////////
         output.setReportFormat(reportFormat);
         output.setFilename(filename);
         output.setInputStream(pipedInputStream);
      }
      catch(QException qe)
      {
         throw (qe);
      }
      catch(Exception e)
      {
         throw (new QException("Error executing table export", e));
      }
   }

}
