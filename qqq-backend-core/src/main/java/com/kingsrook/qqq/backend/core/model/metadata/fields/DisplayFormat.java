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

package com.kingsrook.qqq.backend.core.model.metadata.fields;


/*******************************************************************************
 **
 *******************************************************************************/
public interface DisplayFormat
{
   String DEFAULT = "%s";
   String STRING  = "%s";
   String COMMAS  = "%,d";

   String DECIMAL1_COMMAS = "%,.1f";
   String DECIMAL2_COMMAS = "%,.2f";
   String DECIMAL3_COMMAS = "%,.3f";

   String DECIMAL1 = "%.1f";
   String DECIMAL2 = "%.2f";
   String DECIMAL3 = "%.3f";

   String CURRENCY = "$%,.2f";

   String PERCENT        = "%.0f%%";
   String PERCENT_POINT1 = "%.1f%%";
   String PERCENT_POINT2 = "%.2f%%";


   /*******************************************************************************
    **
    *******************************************************************************/
   static String getExcelFormat(String javaDisplayFormat)
   {
      if(javaDisplayFormat == null)
      {
         return (null);
      }

      return switch(javaDisplayFormat)
      {
         case DisplayFormat.DEFAULT -> null;
         case DisplayFormat.COMMAS -> "#,##0";
         case DisplayFormat.DECIMAL1 -> "0.0";
         case DisplayFormat.DECIMAL2 -> "0.00";
         case DisplayFormat.DECIMAL3 -> "0.000";
         case DisplayFormat.DECIMAL1_COMMAS -> "#,##0.0";
         case DisplayFormat.DECIMAL2_COMMAS -> "#,##0.00";
         case DisplayFormat.DECIMAL3_COMMAS -> "#,##0.000";
         case DisplayFormat.CURRENCY -> "$#,##0.00";
         case DisplayFormat.PERCENT -> "0%";
         case DisplayFormat.PERCENT_POINT1 -> "0.0%";
         case DisplayFormat.PERCENT_POINT2 -> "0.00%";
         default -> null;
      };

   }
}
