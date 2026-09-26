/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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


import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ObjectMessage;
import org.apache.logging.log4j.message.SimpleMessageFactory;
import org.apache.logging.log4j.simple.SimpleLogger;
import org.apache.logging.log4j.util.PropertiesUtil;


/*******************************************************************************
 ** QQQ log4j implementation, used within a QLogger, to "collect" log messages
 ** in an internal list - the idea being - for tests, to assert that logs happened.
 *******************************************************************************/
public class QCollectingLogger extends SimpleLogger
{
   private List<CollectedLogMessage> collectedMessages = new ArrayList<>();

   ///////////////////////////////////////////////////////////////////////////////////////////////////////////
   // just in case one of these gets activated, and left on, put a limit on how many messages we'll collect //
   ///////////////////////////////////////////////////////////////////////////////////////////////////////////
   private int capacity = 100;

   private Logger logger;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public QCollectingLogger(Logger logger)
   {
      super(logger.getName(), logger.getLevel(), false, false, true, false, "", new SimpleMessageFactory(), new PropertiesUtil(new Properties()), System.out);
      this.logger = logger;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void logMessage(String fqcn, Level level, Marker marker, Message message, Throwable throwable)
   {
      ////////////////////////////////////////////
      // add this log message to our collection //
      ////////////////////////////////////////////
      collectedMessages.add(new CollectedLogMessage()
         .withLevel(level)
         .withMessage(message.getFormattedMessage())
         .withException(throwable));

      ////////////////////////////////////////////////////////////////////////////////////////
      // if we've gone over our capacity, remove the 1st entry until we're back at capacity //
      ////////////////////////////////////////////////////////////////////////////////////////
      while(collectedMessages.size() > capacity)
      {
         collectedMessages.remove(0);
      }

      //////////////////////////////////////////////////////////////////////
      // update the message that we log to indicate that we collected it. //
      // if it looks like JSON, insert as a name:value pair; else text.   //
      //////////////////////////////////////////////////////////////////////
      String formattedMessage = message.getFormattedMessage();
      String updatedMessage;
      if(formattedMessage.startsWith("{"))
      {
         updatedMessage = """
            {"collected":true,""" + formattedMessage.substring(1);
      }
      else
      {
         updatedMessage = "[Collected] " + formattedMessage;
      }
      ObjectMessage myMessage = new ObjectMessage(updatedMessage);

      ///////////////////////////////////////////////////////////////////////////////////////
      // log the message with the original log4j logger, with our slightly updated message //
      ///////////////////////////////////////////////////////////////////////////////////////
      logger.logMessage(level, marker, fqcn, null, myMessage, throwable);
   }



   /*******************************************************************************
    ** Setter for logger
    **
    *******************************************************************************/
   public void setLogger(Logger logger)
   {
      this.logger = logger;
   }



   /*******************************************************************************
    ** Getter for collectedMessages
    **
    *******************************************************************************/
   public List<CollectedLogMessage> getCollectedMessages()
   {
      return collectedMessages;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void clear()
   {
      this.collectedMessages.clear();
   }



   /*******************************************************************************
    ** Setter for capacity
    **
    *******************************************************************************/
   public void setCapacity(int capacity)
   {
      this.capacity = capacity;
   }

}
