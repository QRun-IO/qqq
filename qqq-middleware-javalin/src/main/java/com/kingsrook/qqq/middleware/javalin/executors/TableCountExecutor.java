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
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.QueryHint;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountOutput;
import com.kingsrook.qqq.backend.core.utils.ExceptionUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import com.kingsrook.qqq.middleware.javalin.JoinedTablePermissions;
import com.kingsrook.qqq.middleware.javalin.executors.io.TableCountInput;
import com.kingsrook.qqq.middleware.javalin.executors.io.TableCountOutputInterface;
import org.apache.commons.lang3.BooleanUtils;


/*******************************************************************************
 **
 *******************************************************************************/
public class TableCountExecutor extends AbstractMiddlewareExecutor<TableCountInput, TableCountOutputInterface>
{
   private static final QLogger LOG = QLogger.getLogger(TableCountExecutor.class);

   protected static final Integer DEFAULT_QUERY_TIMEOUT_SECONDS = 60;



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void execute(TableCountInput input, TableCountOutputInterface output) throws QException
   {
      try
      {
         ExecutorSessionUtils.setTableVariantInSession(input.getTableVariant());

         CountInput countInput = new CountInput();
         countInput.setTableName(input.getTableName());
         countInput.setInputSource(QInputSource.USER);

         PermissionsHelper.checkTablePermissionThrowing(countInput, TablePermissionSubType.READ);

         countInput.setFilter(input.getFilter());
         countInput.setQueryJoins(input.getJoins());
         countInput.setIncludeDistinctCount(input.getIncludeDistinct());
         countInput.setTimeoutSeconds(DEFAULT_QUERY_TIMEOUT_SECONDS); // todo param
         countInput.withQueryHint(QueryHint.MAY_USE_READ_ONLY_BACKEND);

         JoinedTablePermissions.checkReadPermissions(countInput, countInput.getQueryJoins(), countInput.getFilter());

         if(countInput.getFilter() != null)
         {
            // todo - where should values come from?
            countInput.getFilter().interpretValues(Collections.emptyMap());
         }

         CountOutput countOutput = new CountAction().execute(countInput);
         output.setCount(ValueUtils.getValueAsLong(countOutput.getCount()));

         if(BooleanUtils.isTrue(input.getIncludeDistinct()))
         {
            output.setDistinctCount(ValueUtils.getValueAsLong(countOutput.getDistinctCount()));
         }
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
         throw (new QException("Unexpected error occurred while executing count query: " + e.getMessage(), e));
      }
   }

}
