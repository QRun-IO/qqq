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


import java.util.Collections;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.QueryHint;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.utils.ExceptionUtils;
import com.kingsrook.qqq.middleware.javalin.JoinedTablePermissions;
import com.kingsrook.qqq.middleware.javalin.QJavalinMetaData;
import com.kingsrook.qqq.middleware.javalin.QJavalinUtils;
import com.kingsrook.qqq.middleware.javalin.executors.io.TableQueryInput;
import com.kingsrook.qqq.middleware.javalin.executors.io.TableQueryOutputInterface;


/*******************************************************************************
 **
 *******************************************************************************/
public class TableQueryExecutor extends AbstractMiddlewareExecutor<TableQueryInput, TableQueryOutputInterface>
{
   private static final QLogger LOG = QLogger.getLogger(TableQueryExecutor.class);

   protected static final Integer DEFAULT_QUERY_TIMEOUT_SECONDS = 60;



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void execute(TableQueryInput input, TableQueryOutputInterface output) throws QException
   {
      try
      {
         ExecutorSessionUtils.setTableVariantInSession(input.getTableVariant());

         QueryInput queryInput = new QueryInput();
         queryInput.setTableName(input.getTableName());
         queryInput.setInputSource(QInputSource.USER);

         PermissionsHelper.checkTablePermissionThrowing(queryInput, TablePermissionSubType.READ);

         queryInput.setFilter(input.getFilter());
         queryInput.setQueryJoins(input.getJoins());
         queryInput.setShouldGenerateDisplayValues(true);
         queryInput.setShouldTranslatePossibleValues(true);
         queryInput.setTimeoutSeconds(DEFAULT_QUERY_TIMEOUT_SECONDS); // todo param
         queryInput.withQueryHint(QueryHint.MAY_USE_READ_ONLY_BACKEND);

         JoinedTablePermissions.checkReadPermissions(queryInput, queryInput.getQueryJoins(), queryInput.getFilter());

         if(queryInput.getFilter() != null)
         {
            // todo
            queryInput.getFilter().interpretValues(Collections.emptyMap());
         }

         if(queryInput.getFilter() == null || queryInput.getFilter().getLimit() == null)
         {
            QJavalinUtils.handleQueryNullLimit(QJavalinMetaData.of(QContext.getQInstance()), queryInput, null);
         }

         QueryOutput queryOutput = new QueryAction().execute(queryInput);

         // todo not sure QValueFormatter.setBlobValuesToDownloadUrls(QContext.getQInstance().getTable(input.getTableName()), queryOutput.getRecords());

         output.setRecords(queryOutput.getRecords());
      }
      catch(QException e)
      {
         QUserFacingException userFacingException = ExceptionUtils.findClassInRootChain(e, QUserFacingException.class);
         if(userFacingException != null)
         {
            throw userFacingException;
         }

         throw (e);
      }
      catch(Exception e)
      {
         throw (new QException("Unexpected error occurred while executing query: " + e.getMessage(), e));
      }
   }

}
