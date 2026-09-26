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

package com.kingsrook.qqq.backend.core.actions;


import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionOutput;


/*******************************************************************************
 ** Base class for QQQ Actions (both framework and application defined) that
 ** have a signature like a Function - taking an Input object as a parameter,
 ** and returning an Output object.
 *******************************************************************************/
public abstract class AbstractQActionFunction<I extends AbstractActionInput, O extends AbstractActionOutput>
{

   /*******************************************************************************
    **
    *******************************************************************************/
   public abstract O execute(I input) throws QException;



   /*******************************************************************************
    **
    *******************************************************************************/
   public Future<O> executeAsync(I input)
   {
      CapturedContext      capturedContext   = QContext.capture();
      CompletableFuture<O> completableFuture = new CompletableFuture<>();
      ActionHelper.getExecutorService().submit(() ->
      {
         try
         {
            QContext.init(capturedContext);
            O output = execute(input);
            completableFuture.complete(output);
         }
         catch(QException e)
         {
            completableFuture.completeExceptionally(e);
         }
         finally
         {
            QContext.clear();
         }
      });
      return (completableFuture);
   }

}
