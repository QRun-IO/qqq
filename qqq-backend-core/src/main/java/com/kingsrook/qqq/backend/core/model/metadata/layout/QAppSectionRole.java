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

package com.kingsrook.qqq.backend.core.model.metadata.layout;


/*******************************************************************************
 ** Navigation role of an app section.
 *******************************************************************************/
public enum QAppSectionRole
{
   /*******************************************************************************
    ** Records people create or work on; clients show its items without a heading.
    *******************************************************************************/
   PRIMARY,

   /*******************************************************************************
    ** Records the system writes (runs, deliveries, sent messages); shown under a heading.
    *******************************************************************************/
   HISTORY,

   /*******************************************************************************
    ** Lookups and configuration; shown under a heading, collapsed by default.
    *******************************************************************************/
   SETTINGS,

   /*******************************************************************************
    ** A heading that groups child apps; never navigable (no page, no URL, no breadcrumb level).
    *******************************************************************************/
   GROUP
}
