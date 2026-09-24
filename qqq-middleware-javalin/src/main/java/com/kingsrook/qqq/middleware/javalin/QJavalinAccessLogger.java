/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.kingsrook.qqq.middleware.javalin;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.kingsrook.qqq.backend.core.logging.LogPair;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLineInterface;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.StreamedETLWithFrontendProcess;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Access-Logger used for QJavalin handlers.
 **
 ** Can be fully disabled through the JavalinMetaData object, or via system property:
 ** qqq.javalin.loggerDisabled
 *
 ** Alternatively, individual log types and even actions can be enabled or disabled
 ** via the logFilter in JavalinMetaData.
 **
 ** Note - when working in this class - be overly aggressive with wrapping
 ** everything in try-catch, and not allowing exceptions to bubble.  There isn't
 ** much more of a disappointment then when logging code breaks user actions...
 *******************************************************************************/
public class QJavalinAccessLogger
{
   private static final QLogger LOG = QLogger.getLogger(QJavalinAccessLogger.class);

   public static final String DISABLED_PROPERTY = "qqq.javalin.loggerDisabled";

   private static ThreadLocal<Long>      requestStartTime  = new ThreadLocal<>();
   private static ThreadLocal<String>    requestActionName = new ThreadLocal<>();
   private static ThreadLocal<LogPair[]> requestLogPairs   = new ThreadLocal<>();



   /*******************************************************************************
    ** Types of log entries - useful for a filter (from javalin meta data).
    *******************************************************************************/
   public enum LogType
   {
      START,
      END_SUCCESS,
      END_SUCCESS_SLOW,
      END_FAIL,
      PROCESS_SUMMARY
   }



   /*******************************************************************************
    ** input to filter method (from javalin meta data), to decide if entry should be logged.
    *******************************************************************************/
   public record LogEntry(LogType logType, String actionName)
   {

   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void logStart(String actionName, LogPair... logPairs)
   {
      try
      {
         if(isLoggerDisabled())
         {
            /////////////////////////////////////////////////////
            // if we're not to log starts or ends, just return //
            /////////////////////////////////////////////////////
            return;
         }

         setThreadLocals(actionName, logPairs);

         List<LogPair> pairList = new ArrayList<>(Arrays.asList(logPairs));
         pairList.add(0, logPair("action", actionName));
         pairList.add(0, logPair("access", "start"));

         if(shouldLog(new LogEntry(LogType.START, actionName)))
         {
            LOG.info(pairList);
         }
      }
      catch(Exception e)
      {
         LOG.warn("Error in javalin access logger", e);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static boolean shouldLog(LogEntry logEntry)
   {
      if(isLoggerDisabled())
      {
         return (false);
      }

      if(QJavalinImplementation.javalinMetaData != null && QJavalinImplementation.javalinMetaData.getLogFilter() != null)
      {
         return (QJavalinImplementation.javalinMetaData.getLogFilter().apply(logEntry));
      }

      return (true);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static boolean isLoggerDisabled()
   {
      /////////////////////////////////////////////////////////////////////////////////////
      // if there's no javalin metadata, then just read the disabled property ourselves, //
      // and be disabled iff the property value is "true"                                //
      /////////////////////////////////////////////////////////////////////////////////////
      if(QJavalinImplementation.javalinMetaData == null)
      {
         return System.getProperty(QJavalinAccessLogger.DISABLED_PROPERTY, "false").equals("true");
      }

      ///////////////////////////////////////////////////////////////////////////////////////////////////
      // else, there is javalinMetaData, then let IT say if we're disabled.                            //
      // note that will use the same system property by default, but it may be overridden via app code //
      ///////////////////////////////////////////////////////////////////////////////////////////////////
      return QJavalinImplementation.javalinMetaData.getLoggerDisabled();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   static void logStartSilent(String actionName)
   {
      setThreadLocals(actionName, null);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void setThreadLocals(String actionName, LogPair[] logPairs)
   {
      try
      {
         if(!isLoggerDisabled())
         {
            requestActionName.set(actionName);
            requestStartTime.set(System.currentTimeMillis());
            requestLogPairs.set(logPairs);
         }
      }
      catch(Exception e)
      {
         LOG.warn("Error in javalin access logger", e);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void logEndSuccessIfSlow(long slowThreshold, LogPair... logPairs)
   {
      try
      {
         if(isLoggerDisabled())
         {
            return;
         }

         Long startTime = requestStartTime.get();
         Long millis    = null;
         if(startTime != null)
         {
            long endTime = System.currentTimeMillis();
            millis = endTime - startTime;
         }

         if(millis != null && millis > slowThreshold)
         {
            logEnd(LogType.END_SUCCESS_SLOW, "success", new ArrayList<>(Arrays.asList(logPairs)));
         }
      }
      catch(Exception e)
      {
         LOG.warn("Error in javalin access logger", e);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void logEndSuccess(LogPair... logPairs)
   {
      try
      {
         if(isLoggerDisabled())
         {
            return;
         }

         List<LogPair> pairList = new ArrayList<>(Arrays.asList(logPairs));
         logEnd(LogType.END_SUCCESS, "success", pairList);
      }
      catch(Exception e)
      {
         LOG.warn("Error in javalin access logger", e);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void logEndFail(Throwable t, LogPair... logPairs)
   {
      try
      {
         if(isLoggerDisabled())
         {
            return;
         }

         List<LogPair> pairList = new ArrayList<>(Arrays.asList(logPairs));
         // pairList.add(0, logPair("exceptionMessage", t.getMessage()));
         // pairList.add(0, logPair("exceptionType", t.getClass().getSimpleName()));
         logEnd(LogType.END_FAIL, "failure", pairList);
      }
      catch(Exception e)
      {
         LOG.warn("Error in javalin access logger", e);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void logEnd(LogType logType, String accessType, List<LogPair> pairList)
   {
      try
      {
         if(isLoggerDisabled())
         {
            return;
         }

         LogPair[] logPairsFromStart = requestLogPairs.get();
         if(logPairsFromStart != null)
         {
            for(int i = logPairsFromStart.length - 1; i >= 0; i--)
            {
               pairList.add(0, logPairsFromStart[i]);
            }
         }

         String actionName = requestActionName.get();
         requestActionName.remove();
         if(StringUtils.hasContent(actionName))
         {
            pairList.add(0, logPair("action", actionName));
         }

         pairList.add(0, logPair("access", accessType));

         Long startTime = requestStartTime.get();
         requestStartTime.remove();
         Long millis;
         if(startTime != null)
         {
            long endTime = System.currentTimeMillis();
            millis = endTime - startTime;
            pairList.add(logPair("millis", millis));
         }
         else
         {
            //////////////////////////////////////////////////////////////////////
            // done so var can be effectively null, and be used in lambda below //
            //////////////////////////////////////////////////////////////////////
            millis = null;
         }

         ////////////////////////////////////////////////////////////////////////////////////////
         // filter out any LogPairIfSlow objects we have, for in case the request was not slow //
         ////////////////////////////////////////////////////////////////////////////////////////
         pairList.removeIf(logPair ->
         {
            if(logPair instanceof LogPairIfSlow logPairIfSlow)
            {
               if(millis != null && millis < logPairIfSlow.getSlowThreshold())
               {
                  return (true);
               }
            }
            return (false);
         });

         if(shouldLog(new LogEntry(logType, actionName)))
         {
            LOG.info(pairList);
         }
      }
      catch(Exception e)
      {
         LOG.warn("Error in javalin access logger", e);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static LogPairIfSlow logPairIfSlow(String key, Object value, long slowThreshold)
   {
      return (new LogPairIfSlow(key, value, slowThreshold));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void logProcessSummary(String processName, String processUUID, RunProcessOutput runProcessOutput)
   {
      try
      {
         if(isLoggerDisabled())
         {
            return;
         }

         List<LogPair> logPairs = new ArrayList<>();
         logPairs.add(logPair("processName", processName));
         logPairs.add(logPair("processUUID", processUUID));

         if(runProcessOutput != null)
         {
            runProcessOutput.getProcessState().getNextStepName().ifPresent(nextStep -> logPairs.add(logPair("nextStep", nextStep)));

            try
            {
               if(runProcessOutput.getValues().containsKey(StreamedETLWithFrontendProcess.FIELD_PROCESS_SUMMARY))
               {
                  @SuppressWarnings("unchecked")
                  List<ProcessSummaryLineInterface> processSummaryLines = (List<ProcessSummaryLineInterface>) runProcessOutput.getValues().get(StreamedETLWithFrontendProcess.FIELD_PROCESS_SUMMARY);
                  processSummaryListToLogPairs(logPairs, processSummaryLines);
               }
               else if(runProcessOutput.getValues().containsKey(StreamedETLWithFrontendProcess.FIELD_VALIDATION_SUMMARY))
               {
                  @SuppressWarnings("unchecked")
                  List<ProcessSummaryLineInterface> processSummaryLines = (List<ProcessSummaryLineInterface>) runProcessOutput.getValues().get(StreamedETLWithFrontendProcess.FIELD_VALIDATION_SUMMARY);
                  processSummaryListToLogPairs(logPairs, processSummaryLines);
               }
            }
            catch(Exception e)
            {
               logPairs.add(logPair("errorLoggingSummary", e.getMessage()));
            }
         }

         logEnd(LogType.PROCESS_SUMMARY, "processSummary", logPairs);
      }
      catch(Exception e)
      {
         LOG.warn("Error in javalin access logger", e);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void processSummaryListToLogPairs(List<LogPair> logPairs, List<ProcessSummaryLineInterface> processSummaryLines)
   {
      int i = 0;
      for(ProcessSummaryLineInterface processSummaryLine : CollectionUtils.nonNullList(processSummaryLines))
      {
         LogPair logPair = processSummaryLine.toLogPair();
         logPair.setKey(logPair.getKey() + i++);
         logPairs.add(logPair);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class LogPairIfSlow extends LogPair
   {
      private final long slowThreshold;



      /*******************************************************************************
       ** Constructor
       **
       *******************************************************************************/
      public LogPairIfSlow(String key, Object value, long slowThreshold)
      {
         super(key, value);
         this.slowThreshold = slowThreshold;
      }



      /*******************************************************************************
       ** Getter for slowThreshold
       **
       *******************************************************************************/
      public long getSlowThreshold()
      {
         return slowThreshold;
      }
   }
}
