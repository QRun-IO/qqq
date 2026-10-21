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

package com.kingsrook.qqq.backend.core.actions.ai.tools;


import java.util.Collection;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.ai.AiToolDefinition;
import com.kingsrook.qqq.backend.core.actions.ai.AiToolKind;
import com.kingsrook.qqq.backend.core.exceptions.QException;


/*******************************************************************************
 ** Base class for the AI tools the kernel runs.  A tool declares its wire name,
 ** description, kind and strict input schema; and executes in the requesting
 ** user's QContext, returning the data for the model (which the kernel
 ** serializes and wraps in the data envelope), while filling the per-call
 ** result (summary, record refs, render payload...) through the context.
 **
 ** A tool signals DENIED / NOT_FOUND / INVALID_INPUT by throwing an
 ** {@link AiToolException}.
 *******************************************************************************/
public abstract class AbstractAiTool
{

   /*******************************************************************************
    ** The tool's wire name (snake_case).
    *******************************************************************************/
   public abstract String getName();

   /*******************************************************************************
    ** The tool's description, for the model.
    *******************************************************************************/
   public abstract String getDescription();

   /*******************************************************************************
    ** The tool's kind.
    *******************************************************************************/
   public abstract AiToolKind getKind();

   /*******************************************************************************
    ** The tool's strict input schema (an object schema).
    *******************************************************************************/
   public abstract Map<String, Object> getInputSchema();

   /*******************************************************************************
    ** Run the tool - returning the data to send to the model.
    *******************************************************************************/
   public abstract Map<String, Object> execute(AiToolContext context, AiToolInput input) throws QException;



   /*******************************************************************************
    ** The tool's definition.
    *******************************************************************************/
   public AiToolDefinition getDefinition()
   {
      return (new AiToolDefinition()
         .withName(getName())
         .withDescription(getDescription())
         .withKind(getKind())
         .withStrict(true)
         .withInputSchema(getInputSchema()));
   }



   /*******************************************************************************
    ** The names of the tool's input properties (all others are rejected).
    *******************************************************************************/
   @SuppressWarnings("unchecked")
   public Collection<String> getInputPropertyNames()
   {
      return (((Map<String, Object>) getInputSchema().get("properties")).keySet());
   }
}
