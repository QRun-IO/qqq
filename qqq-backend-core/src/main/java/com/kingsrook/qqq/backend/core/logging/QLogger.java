/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.logging;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QMetaDataVariableInterpreter;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.ExceptionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Wrapper for
 **
 *******************************************************************************/
public class QLogger
{
   private static Map<String, QLogger> loggerMap = Collections.synchronizedMap(new HashMap<>());

   private static boolean logSessionIdEnabled = true;

   //////////////////////////////////////////////////////////////////////
   // note - read in LogUtils, where log pairs are made into a string. //
   //////////////////////////////////////////////////////////////////////
   static String processTagLogPairJson = null;

   private Logger logger;

   static
   {
      //////////////////////////////////////////////////////////////////////////////////////////////
      // read the property to see if sessionIds in log messages is enabled, just once, statically //
      //////////////////////////////////////////////////////////////////////////////////////////////
      try
      {
         String propertyName  = "qqq.logger.logSessionId.disabled";
         String propertyValue = System.getProperty(propertyName, "");
         if(propertyValue.equals("true"))
         {
            logSessionIdEnabled = false;
         }
      }
      catch(Exception e)
      {
         e.printStackTrace();
      }

      ////////////////////////////////////////////////////////////////////////////////////////////
      // read the property (or env var) to see if there's a "processTag" to put on all messages //
      ////////////////////////////////////////////////////////////////////////////////////////////
      try
      {
         String processTag = System.getProperty("qqq.logger.processTag");
         if(processTag == null)
         {
            processTag = new QMetaDataVariableInterpreter().interpret("${env.QQQ_LOGGER_PROCESS_TAG}");
         }

         if(StringUtils.hasContent(processTag))
         {
            processTagLogPairJson = "\"processTag\":\"" + processTag + "\"";
         }
      }
      catch(Exception e)
      {
         e.printStackTrace();
      }
   }

   /*******************************************************************************
    **
    *******************************************************************************/
   public QLogger(Logger logger)
   {
      this.logger = logger;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QLogger getLogger(Class<?> c)
   {
      return (loggerMap.computeIfAbsent(c.getName(), x -> new QLogger(LogManager.getLogger(c))));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QCollectingLogger activateCollectingLoggerForClass(Class<?> c)
   {
      Logger            loggerFromLogManager = LogManager.getLogger(c);
      QCollectingLogger collectingLogger     = new QCollectingLogger(loggerFromLogManager);

      QLogger qLogger = getLogger(c);
      qLogger.setLogger(collectingLogger);

      return collectingLogger;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void deactivateCollectingLoggerForClass(Class<?> c)
   {
      Logger  loggerFromLogManager = LogManager.getLogger(c);
      QLogger qLogger              = getLogger(c);
      qLogger.setLogger(loggerFromLogManager);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public <T extends Throwable> T warnAndThrow(T t, LogPair... logPairs) throws T
   {
      warn(t.getMessage(), t, logPairs);
      throw (t);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public <T extends Throwable> T errorAndThrow(T t, LogPair... logPairs) throws T
   {
      error(t.getMessage(), t, logPairs);
      throw (t);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void log(Level level, String message)
   {
      logger.log(level, () -> makeJsonString(message));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void log(Level level, String message, Throwable t)
   {
      logger.log(level, () -> makeJsonString(message, t));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void log(Level level, String message, Throwable t, LogPair... logPairs)
   {
      logger.log(level, () -> makeJsonString(message, t, logPairs));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void log(Level level, Throwable t)
   {
      logger.log(level, () -> makeJsonString(null, t));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void trace(String message)
   {
      logger.trace(() -> makeJsonString(message));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void trace(String message, LogPair... logPairs)
   {
      logger.trace(() -> makeJsonString(message, null, logPairs));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void trace(String message, Object... values)
   {
      logger.trace(makeJsonString(message), values);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void trace(String message, Throwable t)
   {
      logger.trace(() -> makeJsonString(message, t));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void trace(String message, Throwable t, LogPair... logPairs)
   {
      logger.trace(() -> makeJsonString(message, t, logPairs));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void trace(Throwable t)
   {
      logger.trace(() -> makeJsonString(null, t));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void debug(String message)
   {
      logger.debug(() -> makeJsonString(message));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void debug(LogPair... logPairs)
   {
      logger.warn(() -> makeJsonString(null, null, logPairs));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void debug(String message, LogPair... logPairs)
   {
      logger.debug(() -> makeJsonString(message, null, logPairs));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void debug(String message, Object... values)
   {
      logger.debug(makeJsonString(message), values);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void debug(String message, Throwable t)
   {
      logger.debug(() -> makeJsonString(message, t));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void debug(String message, Throwable t, LogPair... logPairs)
   {
      logger.debug(() -> makeJsonString(message, t, logPairs));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void debug(Throwable t)
   {
      logger.debug(() -> makeJsonString(null, t));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void info(String message)
   {
      logger.info(() -> makeJsonString(message));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void info(LogPair... logPairs)
   {
      logger.info(() -> makeJsonString(null, null, logPairs));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void info(List<LogPair> logPairList)
   {
      logger.info(() -> makeJsonString(null, null, logPairList));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void info(String message, LogPair... logPairs)
   {
      logger.info(() -> makeJsonString(message, null, logPairs));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void info(String message, Object... values)
   {
      logger.info(makeJsonString(message), values);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void info(String message, Throwable t)
   {
      logger.info(() -> makeJsonString(message, t));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void info(String message, Throwable t, LogPair... logPairs)
   {
      logger.info(() -> makeJsonString(message, t, logPairs));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void info(Throwable t)
   {
      logger.info(() -> makeJsonString(null, t));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void warn(String message)
   {
      logger.warn(() -> makeJsonString(message));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void warn(LogPair... logPairs)
   {
      logger.warn(() -> makeJsonString(null, null, logPairs));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void warn(String message, LogPair... logPairs)
   {
      logger.warn(() -> makeJsonString(message, null, logPairs));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void warn(String message, Object... values)
   {
      logger.warn(makeJsonString(message), values);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void warn(String message, Throwable t)
   {
      logger.log(determineIfShouldDowngrade(t, Level.WARN), () -> makeJsonString(message, t));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void warn(String message, Throwable t, LogPair... logPairs)
   {
      logger.log(determineIfShouldDowngrade(t, Level.WARN), () -> makeJsonString(message, t, logPairs));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void warn(Throwable t)
   {
      logger.log(determineIfShouldDowngrade(t, Level.WARN), () -> makeJsonString(null, t));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void error(String message)
   {
      logger.error(() -> makeJsonString(message));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void error(LogPair... logPairs)
   {
      logger.warn(() -> makeJsonString(null, null, logPairs));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void error(String message, LogPair... logPairs)
   {
      logger.error(() -> makeJsonString(message, null, logPairs));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void error(String message, Object... values)
   {
      logger.error(makeJsonString(message), values);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void error(String message, Throwable t)
   {
      logger.log(determineIfShouldDowngrade(t, Level.ERROR), () -> makeJsonString(message, t));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void error(String message, Throwable t, LogPair... logPairs)
   {
      logger.log(determineIfShouldDowngrade(t, Level.ERROR), () -> makeJsonString(message, t, logPairs));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void error(Throwable t)
   {
      logger.log(determineIfShouldDowngrade(t, Level.ERROR), () -> makeJsonString(null, t));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String makeJsonString(String message)
   {
      return (makeJsonString(message, null));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String makeJsonString(String message, Throwable t)
   {
      return (makeJsonString(message, t, (List<LogPair>) null));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String makeJsonString(String message, Throwable t, LogPair[] logPairs)
   {
      List<LogPair> logPairList = new ArrayList<>();
      if(logPairs != null)
      {
         logPairList.addAll(Arrays.stream(logPairs).toList());
      }

      return (makeJsonString(message, t, logPairList));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   protected String makeJsonString(String message, Throwable t, List<LogPair> logPairList)
   {
      if(logPairList == null)
      {
         logPairList = new ArrayList<>();
      }

      if(StringUtils.hasContent(message))
      {
         logPairList.add(0, logPair("message", message));
      }

      addSessionLogPair(logPairList);

      if(t != null)
      {
         logPairList.add(logPair("stackTrace", LogUtils.filterStackTrace(org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(t))));
      }

      return (LogUtils.jsonLog(logPairList));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void addSessionLogPair(List<LogPair> logPairList)
   {
      if(logSessionIdEnabled)
      {
         QSession session = QContext.getQSession();
         LogPair  sessionLogPair;

         if(session == null)
         {
            ///////////////////////////////////////////////////////////////////////////////////////////////////
            // note - being careful here to make the same json structure whether session is known or unknown //
            // (e.g., not a string in one case and an object in another case) - to help loggly.              //
            ///////////////////////////////////////////////////////////////////////////////////////////////////
            sessionLogPair = logPair("session", logPair("id", "unknown"));
         }
         else
         {
            String user = "unknown";
            if(session.getUser() != null)
            {
               user = session.getUser().getIdReference();
            }

            LogPair variantsLogPair = getVariantsLogPair(session);

            sessionLogPair = logPair("session", logPair("id", session.getUuid()), logPair("user", user), variantsLogPair);
         }

         try
         {
            logPairList.add(sessionLogPair);
         }
         catch(Exception e)
         {
            //////////////////////////////////////
            // deal with not-modifiable list... //
            //////////////////////////////////////
            logPairList = new ArrayList<>(logPairList);
            logPairList.add(sessionLogPair);
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static LogPair getVariantsLogPair(QSession session)
   {
      LogPair variantsLogPair = null;
      try
      {
         if(session.getBackendVariants() != null)
         {
            LogPair[] variants = new LogPair[session.getBackendVariants().size()];

            int i = 0;
            for(Map.Entry<String, Serializable> entry : session.getBackendVariants().entrySet())
            {
               variants[i] = new LogPair(entry.getKey(), entry.getValue());
            }

            variantsLogPair = new LogPair("variants", variants);
         }
      }
      catch(Exception e)
      {
         ////////////////
         // leave null //
         ////////////////
      }
      return variantsLogPair;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   protected Level determineIfShouldDowngrade(Throwable t, Level level)
   {
      //////////////////////////////////////////////////////////////////////////////////////
      // look for QExceptions in the chain, if none found, return the log level passed in //
      //////////////////////////////////////////////////////////////////////////////////////
      List<QException> exceptionList = ExceptionUtils.getClassListFromRootChain(t, QException.class);
      if(CollectionUtils.nullSafeIsEmpty(exceptionList))
      {
         return (level);
      }

      ////////////////////////////////////////////////////////////////////
      // check if any QException in this chain to see if it has already //
      // logged this level, if so, downgrade to INFO                    //
      ////////////////////////////////////////////////////////////////////
      for(QException qException : exceptionList)
      {
         if(qException.hasLoggedLevel(level))
         {
            log(Level.DEBUG, "Downgrading log message from " + level.toString() + " to " + Level.INFO, t);
            return (Level.INFO);
         }
      }

      ///////////////////////////////////////////////////////////////////////////////////////////////////
      // if it has not logged at this level, set that it has in QException, and return passed in level //
      ///////////////////////////////////////////////////////////////////////////////////////////////////
      exceptionList.get(0).setHasLoggedLevel(level);
      return (level);
   }



   /*******************************************************************************
    ** Setter for logger
    **
    *******************************************************************************/
   private void setLogger(Logger logger)
   {
      this.logger = logger;
   }
}
