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


package com.kingsrook.qqq.backend.core.jobs.progress;


import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** Attempt-local monotonic progress and a sixty-second sliding rate window.
 *******************************************************************************/
public class JobProgressMath
{
   private int attempt = -1;
   private Double highWater;
   private final Deque<Sample> samples = new ArrayDeque<>();

   /*******************************************************************************
    ** One rate sample.
    *******************************************************************************/
   private record Sample(Instant at, double percent)
   {
   }



   /*******************************************************************************
    ** Derive percent and ETA. A new attempt discards the previous high-water mark.
    *******************************************************************************/
   public void update(JobProgressSnapshot snapshot, QRecord job, int currentAttempt, Instant now)
   {
      if(attempt != currentAttempt)
      {
         attempt = currentAttempt;
         highWater = null;
         samples.clear();
      }
      Double percent = fraction(snapshot.getBytesDone(), snapshot.getBytesTotal());
      if(percent == null)
      {
         percent = fraction(snapshot.getDone(), snapshot.getTotal());
      }
      List<JobPhase> phases = snapshot.getPhases();
      if(phases != null && !phases.isEmpty())
      {
         double totalWeight = phases.stream().mapToDouble(p -> p.getWeight() == null ? 0 : Math.max(0, p.getWeight())).sum();
         if(totalWeight > 0)
         {
            double sum = 0;
            boolean known = true;
            for(JobPhase phase : phases)
            {
               double weight = phase.getWeight() == null ? 0 : Math.max(0, phase.getWeight());
               if(phase.getStatus() == JobPhase.Status.DONE || phase.getStatus() == JobPhase.Status.SKIPPED)
               {
                  sum += weight;
               }
               else if(phase.getStatus() == JobPhase.Status.RUNNING)
               {
                  if(percent == null && weight > 0)
                  {
                     known = false;
                  }
                  sum += weight * (percent == null ? 0 : percent);
               }
            }
            percent = known ? sum / totalWeight : null;
         }
      }
      if(percent == null && job != null)
      {
         percent = committedFraction(job);
      }
      if(percent != null)
      {
         percent = Math.max(0, Math.min(100, percent * 100));
         highWater = highWater == null ? percent : Math.max(highWater, percent);
      }
      snapshot.setPercent(highWater);
      snapshot.setEtaSeconds(null);
      snapshot.setUpdatedAt(now);
      while(!samples.isEmpty() && samples.peekFirst().at().isBefore(now.minusSeconds(60)))
      {
         samples.removeFirst();
      }
      if(highWater != null)
      {
         Sample first = samples.peekFirst();
         if(first != null)
         {
            double seconds = Duration.between(first.at(), now).toMillis() / 1000.0;
            double delta = highWater - first.percent();
            if(seconds > 0 && delta > 0)
            {
               snapshot.setEtaSeconds((long) Math.ceil((100 - highWater) * seconds / delta));
            }
         }
         if(samples.isEmpty() || Duration.between(samples.peekLast().at(), now).toMillis() >= 1000)
         {
            samples.addLast(new Sample(now, highWater));
         }
      }
   }



   /*******************************************************************************
    ** Committed rows first, then chunks; null means indeterminate.
    *******************************************************************************/
   public static Double committedFraction(QRecord job)
   {
      Double rows = fraction(job.getValueLong("rowsDone"), job.getValueLong("rowsTotal"));
      return (rows != null ? rows : fraction(job.getValueLong("chunksDone"), job.getValueLong("chunksTotal")));
   }



   /*******************************************************************************
    ** A bounded fraction, or unknown for missing/zero totals.
    *******************************************************************************/
   private static Double fraction(Long done, Long total)
   {
      return (done == null || total == null || total <= 0 ? null : Math.max(0, Math.min(1, done.doubleValue() / total)));
   }
}
