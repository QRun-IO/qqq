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

package com.kingsrook.qqq.backend.core.actions.scripts.logging;


import java.io.Serializable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.scripts.ExecuteCodeInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/*******************************************************************************
 ** Implementation of a code execution logger that builds a scriptLog and 0 or more
 ** scriptLogLine records - but doesn't insert them.  e.g., useful for testing
 ** (both in junit, and for users in-app).
 *******************************************************************************/
public class BuildScriptLogAndScriptLogLineExecutionLogger implements QCodeExecutionLoggerInterface, ScriptExecutionLoggerInterface
{
   private static final QLogger LOG = QLogger.getLogger(BuildScriptLogAndScriptLogLineExecutionLogger.class);

   private   QRecord          scriptLog;
   private   List<QRecord>    scriptLogLines = new ArrayList<>();
   protected ExecuteCodeInput executeCodeInput;

   private Serializable scriptId;
   private Serializable scriptRevisionId;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public BuildScriptLogAndScriptLogLineExecutionLogger()
   {
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public BuildScriptLogAndScriptLogLineExecutionLogger(Serializable scriptId, Serializable scriptRevisionId)
   {
      this.scriptId = scriptId;
      this.scriptRevisionId = scriptRevisionId;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QRecord buildHeaderRecord(ExecuteCodeInput executeCodeInput)
   {
      return (new QRecord()
         .withValue("scriptId", scriptId)
         .withValue("scriptRevisionId", scriptRevisionId)
         .withValue("startTimestamp", Instant.now())
         .withValue("input", truncate(executeCodeInput.getInput())));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   protected QRecord buildDetailLogRecord(String logLine)
   {
      return (new QRecord()
         .withValue("scriptLogId", scriptLog == null ? null : scriptLog.getValue("id"))
         .withValue("timestamp", Instant.now())
         .withValue("text", truncate(logLine)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String truncate(Object o)
   {
      return StringUtils.safeTruncate(ValueUtils.getValueAsString(o), 1000, "...");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   protected void updateHeaderAtEnd(Serializable output, Exception exception)
   {
      Instant startTimestamp = (Instant) scriptLog.getValue("startTimestamp");
      Instant endTimestamp   = Instant.now();
      scriptLog.setValue("endTimestamp", endTimestamp);
      scriptLog.setValue("runTimeMillis", startTimestamp.until(endTimestamp, ChronoUnit.MILLIS));

      if(exception != null)
      {
         scriptLog.setValue("hadError", true);
         scriptLog.setValue("error", exception.getMessage());
      }
      else
      {
         scriptLog.setValue("hadError", false);
         scriptLog.setValue("output", truncate(output));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void acceptExecutionStart(ExecuteCodeInput executeCodeInput)
   {
      try
      {
         this.executeCodeInput = executeCodeInput;
         this.scriptLog = buildHeaderRecord(executeCodeInput);

         if(scriptLogLines != null)
         {
            for(QRecord scriptLogLine : scriptLogLines)
            {
               scriptLogLine.setValue("scriptLogId", scriptLog.getValue("id"));
            }
         }
      }
      catch(Exception e)
      {
         LOG.warn("Error starting storage of script log", e);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void acceptLogLine(String logLine)
   {
      scriptLogLines.add(buildDetailLogRecord(logLine));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void acceptException(Exception exception)
   {
      updateHeaderAtEnd(null, exception);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void acceptExecutionEnd(Serializable output)
   {
      updateHeaderAtEnd(output, null);
   }



   /*******************************************************************************
    ** Getter for scriptLog
    **
    *******************************************************************************/
   public QRecord getScriptLog()
   {
      return scriptLog;
   }



   /*******************************************************************************
    ** Getter for scriptLogLines
    **
    *******************************************************************************/
   public List<QRecord> getScriptLogLines()
   {
      return scriptLogLines;
   }



   /*******************************************************************************
    ** Setter for scriptLog
    **
    *******************************************************************************/
   protected void setScriptLog(QRecord scriptLog)
   {
      this.scriptLog = scriptLog;
   }



   /*******************************************************************************
    ** Setter for scriptLogLines
    **
    *******************************************************************************/
   protected void setScriptLogLines(List<QRecord> scriptLogLines)
   {
      this.scriptLogLines = scriptLogLines;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void setScriptId(Integer scriptId)
   {
      this.scriptId = scriptId;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void setScriptRevisionId(Integer scriptRevisionId)
   {
      this.scriptRevisionId = scriptRevisionId;
   }
}
