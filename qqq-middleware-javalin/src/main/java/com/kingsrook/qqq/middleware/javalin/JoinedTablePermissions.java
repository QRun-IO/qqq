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


import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerAction;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.actions.AbstractTableActionInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 ** Authorize tables read through client-supplied joins, filters and ordering.
 *******************************************************************************/
public class JoinedTablePermissions
{
   /*******************************************************************************
    ** Call after the base table's READ check. Resolve a copy because JoinsContext
    ** expands joins and security filters. Framework-only security joins enforce
    ** the base table's access policy and do not grant the client joined reads.
    *******************************************************************************/
   public static void checkReadPermissions(AbstractTableActionInput input, List<QueryJoin> queryJoins, QQueryFilter filter) throws QException
   {
      PermissionsHelper.checkJoinedTableReadPermissions(input, queryJoins, filter);
   }



   /*******************************************************************************
    ** Get can request the complete association tree. Follow the same personalized
    ** metadata as the root Get and subsequent child queries, while
    ** keeping permission evaluation attached to the original HTTP action.
    *******************************************************************************/
   public static void checkAssociationReadPermissions(GetInput input) throws QException
   {
      if(!input.getIncludeAssociations())
      {
         return;
      }

      QTableMetaData rootTable = TableMetaDataPersonalizerAction.execute(input);
      ArrayDeque<String> pendingTables = new ArrayDeque<>();
      for(Association association : CollectionUtils.nonNullList(rootTable.getAssociations()))
      {
         pendingTables.add(association.getAssociatedTableName());
      }

      Set<String> visitedTables = new HashSet<>();
      while(!pendingTables.isEmpty())
      {
         String tableName = pendingTables.removeFirst();
         if(!visitedTables.add(tableName))
         {
            continue;
         }

         if(!PermissionsHelper.hasTablePermission(input, tableName, TablePermissionSubType.READ))
         {
            throw new QPermissionDeniedException("Permission denied.");
         }

         QTableMetaData table = TableMetaDataPersonalizerAction.execute(new QueryInput(tableName).withInputSource(input.getInputSource()));
         for(Association association : CollectionUtils.nonNullList(table.getAssociations()))
         {
            pendingTables.add(association.getAssociatedTableName());
         }
      }
   }
}
