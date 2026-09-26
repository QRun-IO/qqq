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
import java.util.Objects;
import java.util.stream.Collectors;
import com.kingsrook.qqq.backend.core.utils.lambdas.UnsafeSupplier;


/*******************************************************************************
 **
 *******************************************************************************/
public class LogPair
{
   private String key;
   private Object value;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public LogPair(String key, Object value)
   {
      this.key = key;
      this.value = value;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String toString()
   {
      String valueString = getValueString(value);

      return "\"" + Objects.requireNonNullElse(key, "null").replace('"', '.') + "\":" + valueString;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String getValueString(Object value)
   {
      String valueString;
      if(value == null)
      {
         valueString = "null";
      }
      else if(value instanceof LogPair subLogPair)
      {
         valueString = '{' + subLogPair.toString() + '}';
      }
      else if(value instanceof LogPair[] subLogPairs)
      {
         String subLogPairsString = Arrays.stream(subLogPairs).filter(Objects::nonNull).map(LogPair::toString).collect(Collectors.joining(","));
         valueString = '{' + subLogPairsString + '}';
      }
      else if(value instanceof UnsafeSupplier<?, ?> us)
      {
         try
         {
            Object o = us.get();
            return getValueString(o);
         }
         catch(Exception e)
         {
            valueString = "LogValueError";
         }
      }
      else if(value instanceof Number n)
      {
         valueString = String.valueOf(n);
      }
      else
      {
         valueString = '"' + String.valueOf(value).replace("\"", "\\\"") + '"';
      }
      return valueString;
   }



   /*******************************************************************************
    ** Getter for key
    *******************************************************************************/
   public String getKey()
   {
      return (this.key);
   }



   /*******************************************************************************
    ** Setter for key
    *******************************************************************************/
   public void setKey(String key)
   {
      this.key = key;
   }



   /*******************************************************************************
    ** Fluent setter for key
    *******************************************************************************/
   public LogPair withKey(String key)
   {
      this.key = key;
      return (this);
   }



   /*******************************************************************************
    ** Getter for value
    *******************************************************************************/
   public Object getValue()
   {
      return (this.value);
   }



   /*******************************************************************************
    ** Setter for value
    *******************************************************************************/
   public void setValue(Object value)
   {
      this.value = value;
   }



   /*******************************************************************************
    ** Fluent setter for value
    *******************************************************************************/
   public LogPair withValue(Object value)
   {
      this.value = value;
      return (this);
   }

}
