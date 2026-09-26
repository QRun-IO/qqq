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


import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import com.kingsrook.qqq.backend.core.instances.QMetaDataVariableInterpreter;
import com.kingsrook.qqq.backend.core.utils.lambdas.UnsafeSupplier;


/*******************************************************************************
 **
 *******************************************************************************/
public class LogUtils
{
   ///////////////////////////////////////////////////////////////////////////////////////////////
   // This string will be used in regex, inside ()'s, so you can supply pipe-delimited packages //
   // as in, com.kingsrook|com.yourdomain|org.some.other.package                                //
   ///////////////////////////////////////////////////////////////////////////////////////////////
   private static String packagesToKeep = ".";

   static
   {
      packagesToKeep = new QMetaDataVariableInterpreter().getStringFromPropertyOrEnvironment("qqq.logger.packagesToKeep", "QQQ_LOGGER_PACKAGES_TO_KEEP", ".");
   }


   /*******************************************************************************
    **
    *******************************************************************************/
   public static String jsonLog(List<LogPair> logPairs)
   {
      List<LogPair> filteredList = logPairs.stream().filter(Objects::nonNull).toList();
      if(filteredList.isEmpty())
      {
         if(QLogger.processTagLogPairJson != null)
         {
            return ("{" + QLogger.processTagLogPairJson + "}");
         }
         else
         {
            return ("{}");
         }
      }

      return ('{' + filteredList.stream().map(LogPair::toString).collect(Collectors.joining(","))
         + (QLogger.processTagLogPairJson != null ? (',' + QLogger.processTagLogPairJson) : "") + '}');
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static String jsonLog(LogPair... logPairs)
   {
      if(logPairs == null || logPairs.length == 0)
      {
         return ("{}");
      }

      return (jsonLog(Arrays.asList(logPairs)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static LogPair logPair(String key, Object value)
   {
      return (new LogPair(key, value));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static LogPair logPair(String key, UnsafeSupplier<Object, Exception> valueSupplier)
   {
      try
      {
         return (new LogPair(key, valueSupplier.get()));
      }
      catch(Exception e)
      {
         return (new LogPair(key, "exceptionLoggingValue: " + e.getMessage()));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static LogPair logPair(String key, LogPair... values)
   {
      return (new LogPair(key, values));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   static String filterStackTrace(String stackTrace)
   {
      try
      {
         StringBuilder rs    = new StringBuilder();
         String[]      lines = stackTrace.split("\n");

         int    indexWithinSubStack = 0;
         int    skipsInThisPackage  = 0;
         String packageBeingSkipped = null;

         for(String line : lines)
         {
            boolean keepLine = true;

            if(line.matches("^\\s+at .*"))
            {
               keepLine = false;
               indexWithinSubStack++;

               /////////////////////////////////////////////////////////////////////////////
               // avoid NPE on packages to keep (and keep all packages)                   //
               // also, avoid the regex call if it's the default of "." (e.g., match all) //
               // otherwise, check if the line matches "at (packagesToKeep).*"            //
               /////////////////////////////////////////////////////////////////////////////
               if(packagesToKeep == null || ".".equals(packagesToKeep) || line.matches("^\\s+at (" + packagesToKeep + ").*"))
               {
                  keepLine = true;
               }
               if(indexWithinSubStack == 1)
               {
                  keepLine = true;
               }
            }
            else
            {
               indexWithinSubStack = 0;

               if(skipsInThisPackage > 0)
               {
                  rs.append("\t... ").append(skipsInThisPackage).append(" in ").append(packageBeingSkipped).append("\n");
                  skipsInThisPackage = 0;
               }
            }

            if(keepLine)
            {
               rs.append(line).append("\n");
            }
            else
            {
               String thisPackage = line.replaceFirst("\\s+at ", "").replaceFirst("(\\w+\\.\\w+).*", "$1");
               if(Objects.equals(thisPackage, packageBeingSkipped))
               {
                  skipsInThisPackage++;
               }
               else
               {
                  if(skipsInThisPackage > 0)
                  {
                     rs.append("\t... ").append(skipsInThisPackage).append(" in ").append(packageBeingSkipped).append("\n");
                  }
                  skipsInThisPackage = 1;
               }
               packageBeingSkipped = thisPackage;
            }
         }

         if(rs.length() > 0)
         {
            rs.deleteCharAt(rs.length() - 1);
         }

         return (rs.toString());
      }
      catch(Exception e)
      {
         e.printStackTrace();

         ///////////////////////////////////////////////
         // upon any exception, just return the input //
         ///////////////////////////////////////////////
         return (stackTrace);
      }
   }
}
