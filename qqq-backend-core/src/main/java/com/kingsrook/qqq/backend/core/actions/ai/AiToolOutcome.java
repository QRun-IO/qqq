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

package com.kingsrook.qqq.backend.core.actions.ai;


/*******************************************************************************
 ** The outcome of running an AI tool call through the {@link AiToolKernel}.
 **
 ** OK - the tool ran and its full result is in the model content.
 ** DENIED - the session may not do this (or the object does not exist, or is
 **    not exposed to the assistant - these are deliberately indistinguishable,
 **    so a caller cannot probe which objects exist).
 ** NOT_FOUND - the table is readable, but the requested record is not (it does
 **    not exist, or record security hides it - again indistinguishable).
 ** INVALID_INPUT - the tool name or its input was not valid.
 ** TRUNCATED - the tool ran, but the server cap on rows reduced its result.
 ** ERROR - an unexpected error (details are logged, never sent to the model).
 *******************************************************************************/
public enum AiToolOutcome
{
   OK,
   DENIED,
   NOT_FOUND,
   INVALID_INPUT,
   TRUNCATED,
   ERROR
}
