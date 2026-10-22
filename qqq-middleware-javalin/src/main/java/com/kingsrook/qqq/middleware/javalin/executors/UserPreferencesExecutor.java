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

package com.kingsrook.qqq.middleware.javalin.executors;


import java.util.List;
import com.kingsrook.qqq.backend.core.actions.preferences.UserPreferencesAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.middleware.javalin.executors.io.UserPreferencesMiddlewareInput;
import com.kingsrook.qqq.middleware.javalin.executors.io.UserPreferencesOutputInterface;


/*******************************************************************************
 ** Typed executor for the preferences API.
 *******************************************************************************/
public class UserPreferencesExecutor extends AbstractMiddlewareExecutor<UserPreferencesMiddlewareInput, UserPreferencesOutputInterface>
{
   /*******************************************************************************
    ** Invoke the core action with the request's authenticated QContext.
    *******************************************************************************/
   @Override
   public void execute(UserPreferencesMiddlewareInput input, UserPreferencesOutputInterface output) throws QException
   {
      UserPreferencesAction action = new UserPreferencesAction();
      if(input.getOperation() == UserPreferencesMiddlewareInput.Operation.ACTION)
      {
         output.setActionResult(action.runAction(input.getSectionName(), input.getActionName(), input.getActionRequest().getBody()));
         return;
      }
      output.setPreferenceResults(switch(input.getOperation())
      {
         case LIST -> action.listSections();
         case GET -> List.of(action.getSection(input.getSectionName()));
         case ACTION -> throw new IllegalStateException("Action already handled");
         case PATCH -> List.of(action.patchSection(input.getSectionName(), input.getRequest().getBody()));
      });
   }
}
