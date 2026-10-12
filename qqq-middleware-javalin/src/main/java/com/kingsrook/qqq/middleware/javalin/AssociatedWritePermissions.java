/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.middleware.javalin;


import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;


/*******************************************************************************
 ** Association write permission checks for the HTTP write routes.  The logic
 ** now lives in core (com.kingsrook.qqq.backend.core.actions.permissions.
 ** AssociatedWritePermissions), where the core actions also use it when
 ** permissions are enforced (QRun-IO/qqq#1061); this class delegates to it.
 *******************************************************************************/
public final class AssociatedWritePermissions
{

   /*******************************************************************************
    ** static utility class.
    *******************************************************************************/
   private AssociatedWritePermissions()
   {
   }



   /*******************************************************************************
    ** The caller has already authorized INSERT on the parent table.
    *******************************************************************************/
   public static void check(InsertInput input) throws QException
   {
      com.kingsrook.qqq.backend.core.actions.permissions.AssociatedWritePermissions.check(input);
   }



   /*******************************************************************************
    ** The caller has already authorized EDIT on the parent table.
    *******************************************************************************/
   public static void check(UpdateInput input) throws QException
   {
      com.kingsrook.qqq.backend.core.actions.permissions.AssociatedWritePermissions.check(input);
   }



   /*******************************************************************************
    ** The caller has already authorized DELETE on the parent table.
    *******************************************************************************/
   public static void check(DeleteInput input) throws QException
   {
      com.kingsrook.qqq.backend.core.actions.permissions.AssociatedWritePermissions.check(input);
   }
}
