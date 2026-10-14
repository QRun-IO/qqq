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

package com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.assist;


/*******************************************************************************
 ** The AI provider route of the caller, from the X-QQQ-AI-Route header the
 ** assistant's invoker sets (never the model). Only HIPAA may receive values
 ** of classified or unmapped columns; anything else, including no header, is
 ** STANDARD (fail closed). Design spec #1171 section 3.3.
 *******************************************************************************/
public enum BulkLoadAiRoute
{
   HIPAA,
   STANDARD;

   public static final String HEADER_NAME = "X-QQQ-AI-Route";



   /*******************************************************************************
    ** HIPAA only for "hipaa" (any case); otherwise STANDARD.
    *******************************************************************************/
   public static BulkLoadAiRoute fromHeader(String headerValue)
   {
      return (headerValue != null && "hipaa".equalsIgnoreCase(headerValue.strip()) ? HIPAA : STANDARD);
   }
}
