/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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
import com.kingsrook.qqq.backend.core.model.metadata.tables.Capability;
import com.kingsrook.qqq.backend.core.utils.ExceptionUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import com.kingsrook.qqq.middleware.javalin.JoinedTablePermissions;
import com.kingsrook.qqq.middleware.javalin.TableCapabilities;
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
         TableCapabilities.checkCapabilityThrowing(input.getTableName(), Capability.TABLE_COUNT);

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
