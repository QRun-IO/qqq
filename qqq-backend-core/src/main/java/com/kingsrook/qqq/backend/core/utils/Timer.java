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

package com.kingsrook.qqq.backend.core.utils;


import java.time.Duration;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import org.apache.logging.log4j.Level;


/*******************************************************************************
 ** Object to help track performance/runtime of codes.
 *******************************************************************************/
public class Timer
{
   private static final QLogger LOG = QLogger.getLogger(Timer.class);

   private String name;
   private long   start;
   private long   last;
   private Level  level = Level.DEBUG;



   /*******************************************************************************
    **
    *******************************************************************************/
   public Timer(String name)
   {
      this.name = name;
      start = System.currentTimeMillis();
      last = start;
   }



   /*******************************************************************************
    ** Setter for level
    **
    *******************************************************************************/
   public void setLevel(Level level)
   {
      this.level = level;
   }



   /*******************************************************************************
    ** Fluent setter for level
    **
    *******************************************************************************/
   public Timer withLevel(Level level)
   {
      this.level = level;
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void mark(String message)
   {
      mark(message, false);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void mark(String message, boolean prettyPrint)
   {
      long now = System.currentTimeMillis();

      if(!prettyPrint)
      {
         LOG.log(level, String.format("%s: Last [%5d] Total [%5d] %s", name, (now - last), (now - start), message));
      }
      else
      {

         Duration lastDuration  = Duration.ofMillis(now - last);
         Duration totalDuration = Duration.ofMillis(now - start);

         LOG.log(level, String.format(
            "%s: Last [%d hours, %d minutes, %d seconds, %d milliseconds] Total [%d hours, %d minutes, %d seconds, %d milliseconds] %s",
            name, lastDuration.toHours(), lastDuration.toMinutesPart(), lastDuration.toSecondsPart(), lastDuration.toMillisPart(),
            totalDuration.toHours(), totalDuration.toMinutesPart(), totalDuration.toSecondsPart(), totalDuration.toMillisPart(),
            message));
      }

      last = now;
   }
}
