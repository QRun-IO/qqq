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

package com.kingsrook.qqq.backend.core.model.metadata.processes;


/*******************************************************************************
 ** Types of UI Components that can be specified in frontend process steps.
 *******************************************************************************/
public enum QComponentType
{
   HELP_TEXT,
   BULK_EDIT_FORM,
   BULK_LOAD_FILE_MAPPING_FORM,
   BULK_LOAD_VALUE_MAPPING_FORM,
   BULK_LOAD_PROFILE_FORM,
   VALIDATION_REVIEW_SCREEN,
   EDIT_FORM,
   VIEW_FORM,
   DOWNLOAD_FORM,
   RECORD_LIST,
   PROCESS_SUMMARY_RESULTS,
   GOOGLE_DRIVE_SELECT_FOLDER,
   WIDGET,
   HTML;
   ///////////////////////////////////////////////////////////////////////////
   // keep these values in sync with QComponentType.ts in qqq-frontend-core //
   ///////////////////////////////////////////////////////////////////////////
}
