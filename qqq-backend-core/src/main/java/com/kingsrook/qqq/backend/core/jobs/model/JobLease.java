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


import java.time.Instant;


/*******************************************************************************
 ** A worker's lease on one chunk. Every chunk write (progress, checkpoint,
 ** completion) is fenced by the token: a writer whose lease was reclaimed fails.
 **
 ** @param chunkId     the leased chunk
 ** @param jobId       its job
 ** @param seq         the chunk's sequence in the job
 ** @param attempt     the attempt number this lease runs (1 = first)
 ** @param podId       the pod holding the lease
 ** @param token       the fencing token
 ** @param expiresAt   when the lease ends without a heartbeat (database clock)
 *******************************************************************************/
public record JobLease(Integer chunkId, Integer jobId, Integer seq, Integer attempt, String podId, String token, Instant expiresAt)
{
}
