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


package com.kingsrook.qqq.backend.core.jobs.model;


/*******************************************************************************
 ** What a chunk run produced.
 **
 ** @param rowsIn      rows the chunk read
 ** @param rowsOk      rows it applied
 ** @param rowsFailed  rows that failed (recorded in jobs_row_error)
 ** @param summaryJson merged ProcessSummaryLines for the chunk, as JSON (may be null)
 ** @param outputsJson outputs left in storage (reports, exports), as JSON (may be null)
 *******************************************************************************/
public record ChunkOutcome(int rowsIn, int rowsOk, int rowsFailed, String summaryJson, String outputsJson)
{
}
