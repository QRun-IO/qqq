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
import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.filter.LevelRangeFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Unit test for com.kingsrook.qqq.backend.core.logging.QLogger
 **
 *******************************************************************************/
@Disabled // disabled because could never get the custom appender class to receive logEvents that have their levels set (always null)
class QLoggerTest extends BaseTest
{
   private static final QLogger      LOG          = QLogger.getLogger(QLoggerTest.class);
   private static final ListAppender listAppender = ListAppender.createAppender();



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void beforeAll() throws Exception
   {
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testLogAndThrowMethods() throws QException
   {
      try
      {
         LOG.info("Some info");
         LOG.warnAndThrow(new QException("Something failed"), new LogPair("something", 1));
      }
      catch(Exception e)
      {
         //////////////
         // ok, done //
         //////////////
      }

      assertThatThrownBy(() ->
         {
            try
            {
               methodThatThrows();
            }
            catch(Exception e)
            {
               throw LOG.errorAndThrow(new QException("I caught, now i errorAndThrow", e), new LogPair("iLove", "logPairs"));
            }
         }
      ).isInstanceOf(QException.class).hasMessageContaining("I caught").rootCause().hasMessageContaining("See, I throw");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void methodThatThrows() throws QException
   {
      throw (new QException("See, I throw"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testDowngradingWarnings() throws Exception
   {
      org.apache.logging.log4j.core.Logger logger = (org.apache.logging.log4j.core.Logger) LogManager.getLogger(QLoggerTest.class);
      logger.addAppender(listAppender);
      listAppender.start();

      try
      {
         try
         {
            try
            {
               try
               {
                  throw (new QException("Some deepest exception..."));
               }
               catch(Exception e)
               {
                  String warning = "Less deep warning";
                  LOG.warn(warning, e);
                  throw (new QException(warning, e));
               }
            }
            catch(Exception e2)
            {
               String warning = "Middle warning";
               LOG.warn(warning, e2);
               throw (new QException(warning, e2));
            }
         }
         catch(Exception e2)
         {
            String warning = "Last warning";
            LOG.warn(warning, e2);
            throw (new QException(warning, e2));
         }
      }
      catch(Exception e3)
      {
         /////////////////////////
         // check results below //
         /////////////////////////
      }

      assertThat(listAppender.getEventList()).isNotNull();
      assertThat(listAppender.getEventList().size()).isEqualTo(5);
      int counter = 0;
      for(LogEvent logEvent : listAppender.getEventList())
      {
         if(counter == 0)
         {
            assertThat(logEvent.getLevel()).isEqualTo(Level.WARN);
         }
         else
         {
            assertThat(logEvent.getLevel()).isEqualTo(Level.INFO);
         }
         counter++;
      }
   }



   /*******************************************************************************
    ** appender to add to logger to keep a list of log events
    *******************************************************************************/
   @Plugin(name = "ListAppender", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE)
   public static class ListAppender extends AbstractAppender
   {
      private List<LogEvent> eventList = new ArrayList<>();



      /*******************************************************************************
       **
       *******************************************************************************/
      protected ListAppender(final String name, final Filter filter, final Layout<? extends Serializable> layout, final boolean ignoreExceptions, final Property[] properties)
      {
         super(name, filter, layout, ignoreExceptions, properties);
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @PluginFactory
      public static ListAppender createAppender()
      {
         LevelRangeFilter levelRangeFilter = LevelRangeFilter.createFilter(Level.TRACE, Level.ERROR, Filter.Result.ACCEPT, Filter.Result.ACCEPT);
         // return (new ListAppender("ListApppender", levelRangeFilter, null, true, null));
         return (new ListAppender("ListApppender", null, null, true, null));
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void append(LogEvent event)
      {
         eventList.add(event);
      }



      /*******************************************************************************
       ** Getter for eventList
       *******************************************************************************/
      public List<LogEvent> getEventList()
      {
         return (this.eventList);
      }
   }

}
