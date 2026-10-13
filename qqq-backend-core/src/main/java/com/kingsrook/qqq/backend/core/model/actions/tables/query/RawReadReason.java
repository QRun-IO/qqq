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

package com.kingsrook.qqq.backend.core.model.actions.tables.query;


import com.kingsrook.qqq.backend.core.actions.permissions.CustomPermissionChecker;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** Why (and under what authority) a caller is asking for a "raw read" - see
 ** {@link QueryInput#withRawRead(RawReadReason)}.
 **
 ** A raw read returns hidden fields, unmasked password fields, and heavy fields
 ** (and skips possible-value translation and display values), so it is gated:
 ** the reason carries either the name of a permission that the current session
 ** must hold, or a {@link CustomPermissionChecker} that must not throw.  There is
 ** deliberately no "no check" option.
 *******************************************************************************/
public class RawReadReason
{
   private final String                  reason;
   private final String                  permissionName;
   private final CustomPermissionChecker customPermissionChecker;



   /*******************************************************************************
    **
    *******************************************************************************/
   private RawReadReason(String reason, String permissionName, CustomPermissionChecker customPermissionChecker)
   {
      if(!StringUtils.hasContent(reason))
      {
         throw (new IllegalArgumentException("A reason is required for a raw read"));
      }

      this.reason = reason;
      this.permissionName = permissionName;
      this.customPermissionChecker = customPermissionChecker;
   }



   /*******************************************************************************
    ** A raw read allowed to sessions holding the named permission (e.g., "backups.run").
    *******************************************************************************/
   public static RawReadReason requiringPermission(String reason, String permissionName)
   {
      if(!StringUtils.hasContent(permissionName))
      {
         throw (new IllegalArgumentException("A permission name is required for a raw read"));
      }
      return (new RawReadReason(reason, permissionName, null));
   }



   /*******************************************************************************
    ** A raw read allowed when the supplied checker does not throw.  The checker is
    ** called with the query input and the queried table's metadata.
    *******************************************************************************/
   public static RawReadReason checkedBy(String reason, CustomPermissionChecker customPermissionChecker)
   {
      if(customPermissionChecker == null)
      {
         throw (new IllegalArgumentException("A permission checker is required for a raw read"));
      }
      return (new RawReadReason(reason, null, customPermissionChecker));
   }



   /*******************************************************************************
    ** Check that the current session may do this raw read of the given table.
    **
    ** @throws QPermissionDeniedException if it may not.
    *******************************************************************************/
   public void checkPermission(QueryInput queryInput, QTableMetaData table) throws QPermissionDeniedException
   {
      if(customPermissionChecker != null)
      {
         customPermissionChecker.checkPermissionsThrowing(queryInput, table);
         return;
      }

      if(QContext.getQSession() == null || !QContext.getQSession().hasPermission(permissionName))
      {
         throw (new QPermissionDeniedException("Permission denied."));
      }
   }



   /*******************************************************************************
    ** Getter for reason
    *******************************************************************************/
   public String getReason()
   {
      return (reason);
   }



   /*******************************************************************************
    ** Getter for permissionName
    *******************************************************************************/
   public String getPermissionName()
   {
      return (permissionName);
   }



   /*******************************************************************************
    ** Getter for customPermissionChecker
    *******************************************************************************/
   public CustomPermissionChecker getCustomPermissionChecker()
   {
      return (customPermissionChecker);
   }

}
