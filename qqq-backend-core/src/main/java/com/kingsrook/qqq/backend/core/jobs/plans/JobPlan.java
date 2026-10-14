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


package com.kingsrook.qqq.backend.core.jobs.plans;


import java.util.List;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.jobs.model.ChunkOutcome;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** How a job's work is split into chunks and how one chunk runs. Plans are
 ** generic: they run the process's own steps, never a rewrite of the process.
 *******************************************************************************/
public interface JobPlan
{

   /*******************************************************************************
    ** Plan the chunks for a newly launched job (runs as the launching user).
    ** Return chunk records with seq and rangeJson (jobId is set by the caller).
    *******************************************************************************/
   List<QRecord> planChunks(QRecord job) throws QException;


   /*******************************************************************************
    ** Run one chunk, as the job's user. Call context.checkpoint and
    ** context.check at safe points.
    *******************************************************************************/
   ChunkOutcome runChunk(ChunkContext context) throws Exception;

}
