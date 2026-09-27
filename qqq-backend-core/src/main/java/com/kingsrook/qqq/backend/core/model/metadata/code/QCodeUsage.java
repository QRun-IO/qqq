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

package com.kingsrook.qqq.backend.core.model.metadata.code;


/*******************************************************************************
 ** Possible usages for Q-Code entities
 **
 *******************************************************************************/
public enum QCodeUsage
{
   BACKEND_STEP, // a backend-step in a process
   CUSTOMIZER, // a function to customize part of a QQQ table's behavior
   POSSIBLE_VALUE_PROVIDER, // code that drives a custom possibleValueSource
   RECORD_AUTOMATION_HANDLER, // code that executes record automations
   REPORT_STATIC_DATA_SUPPLIER, // code that supplies static data to a report
   SCRIPT_TESTER // class that is used to test scripts.
}
