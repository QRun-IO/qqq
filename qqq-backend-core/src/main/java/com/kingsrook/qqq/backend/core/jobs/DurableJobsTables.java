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


package com.kingsrook.qqq.backend.core.jobs;


/*******************************************************************************
 ** Names shared by the durable-jobs kernel: tables, security keys, permissions
 ** and defaults.
 *******************************************************************************/
public final class DurableJobsTables
{
   public static final String TABLE_JOB       = "jobs_job";
   public static final String TABLE_CHUNK     = "jobs_chunk";
   public static final String TABLE_ROW_ERROR = "jobs_row_error";
   public static final String TABLE_EVENT     = "jobs_event";
   public static final String TABLE_WORKER    = "jobs_worker";
   public static final String TABLE_UPLOAD    = "jobs_upload";

   public static final String SECURITY_KEY_USER            = "jobs_user";
   public static final String SECURITY_KEY_USER_ALL_ACCESS = "jobs_user_all_access";

   public static final String PERMISSION_ADMIN = "jobs_admin";

   public static final String CHANNEL_ASSISTANT = "assistant";

   public static final int DEFAULT_PRIORITY     = 5;
   public static final int DEFAULT_MAX_ATTEMPTS = 3;



   /*******************************************************************************
    ** not instantiable
    *******************************************************************************/
   private DurableJobsTables()
   {
   }

}
