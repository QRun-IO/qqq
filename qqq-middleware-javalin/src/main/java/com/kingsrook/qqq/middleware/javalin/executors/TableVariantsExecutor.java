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


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import com.kingsrook.qqq.middleware.javalin.executors.io.TableVariantsInput;
import com.kingsrook.qqq.middleware.javalin.executors.io.TableVariantsOutputInterface;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.TableVariant;


/*******************************************************************************
 ** List the variants a table's backend can be scoped to, as the legacy
 ** GET /data/{table}/variants route does - and, beyond it, only for a user who
 ** may read the table (an unknown table is refused the same way) and the
 ** backend's variant options table.
 *******************************************************************************/
public class TableVariantsExecutor extends AbstractMiddlewareExecutor<TableVariantsInput, TableVariantsOutputInterface>
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void execute(TableVariantsInput input, TableVariantsOutputInterface output) throws QException
   {
      QueryInput tableInput = new QueryInput(input.getTableName());
      tableInput.setInputSource(QInputSource.USER);
      PermissionsHelper.checkTablePermissionThrowing(tableInput, TablePermissionSubType.READ);

      List<TableVariant> variants      = new ArrayList<>();
      QTableMetaData     tableMetaData = QContext.getQInstance().getTable(input.getTableName());
      QBackendMetaData   backend       = QContext.getQInstance().getBackend(tableMetaData.getBackendName());
      if(backend != null && backend.getUsesVariants())
      {
         QTableMetaData variantsTable = QContext.getQInstance().getTable(backend.getBackendVariantsConfig().getOptionsTableName());

         QueryInput queryInput = new QueryInput(variantsTable.getName());
         queryInput.setInputSource(QInputSource.USER);
         PermissionsHelper.checkTablePermissionThrowing(queryInput, TablePermissionSubType.READ);
         queryInput.setFilter(backend.getBackendVariantsConfig().getOptionsFilter());
         queryInput.setShouldGenerateDisplayValues(true);

         for(QRecord record : new QueryAction().execute(queryInput).getRecords())
         {
            variants.add(new TableVariant()
               .withId(ValueUtils.getValueAsString(record.getValue(variantsTable.getPrimaryKeyField())))
               .withType(backend.getBackendVariantsConfig().getVariantTypeKey())
               .withName(record.getRecordLabel()));
         }
      }

      output.setVariants(variants);
   }

}
