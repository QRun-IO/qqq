/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.permissions;


/*******************************************************************************
 **
 *******************************************************************************/
enum PrivatePermissionSubType implements PermissionSubType
{
   HAS_ACCESS("hasAccess"), // for processes, reports, etc - basically, not tables.
   READ("read"), // for a table in read/write mode - or - for read (query, get, count) on a table in full-mode
   WRITE("write");

   private final String permissionSuffix;



   /*******************************************************************************
    **
    *******************************************************************************/
   PrivatePermissionSubType(String permissionSuffix)
   {
      this.permissionSuffix = permissionSuffix;
   }



   /*******************************************************************************
    ** Getter for permissionSuffix
    *******************************************************************************/
   public String getPermissionSuffix()
   {
      return (this.permissionSuffix);
   }

}
