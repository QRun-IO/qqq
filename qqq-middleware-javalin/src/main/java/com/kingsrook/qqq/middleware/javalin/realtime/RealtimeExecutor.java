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

package com.kingsrook.qqq.middleware.javalin.realtime;


import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.middleware.javalin.executors.AbstractMiddlewareExecutor;
import com.kingsrook.qqq.middleware.javalin.executors.io.AbstractMiddlewareInput;
import com.kingsrook.qqq.middleware.javalin.executors.io.AbstractMiddlewareOutputInterface;


/*******************************************************************************
 ** Transport-neutral subscription executor; stream attachment is done in handleOutput.
 *******************************************************************************/
public class RealtimeExecutor extends AbstractMiddlewareExecutor<RealtimeExecutor.Input, RealtimeExecutor.Output>
{
   /*******************************************************************************
    ** Pass the stream hub to the output or apply a subscription request.
    *******************************************************************************/
   @Override
   public void execute(Input input, Output output) throws QException
   {
      output.hub = input.hub;
      if(input.connectionId != null)
      {
         output.subscriptions = input.hub.subscriptions(QContext.getQSession(), input.connectionId, input.add, input.remove);
      }
   }



   /*******************************************************************************
    ** Internal execution input; wire schemas are separate components.
    *******************************************************************************/
   public static class Input extends AbstractMiddlewareInput
   {
      public RealtimeHub hub;
      public String connectionId;
      public List<String> add = List.of();
      public List<String> remove = List.of();
   }



   /*******************************************************************************
    ** Internal execution output; never serialized as a wire payload.
    *******************************************************************************/
   public static class Output implements AbstractMiddlewareOutputInterface
   {
      public RealtimeHub hub;
      public Map<String, List<String>> subscriptions;
   }
}

