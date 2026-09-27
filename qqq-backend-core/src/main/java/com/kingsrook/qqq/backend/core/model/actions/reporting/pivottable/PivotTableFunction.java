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

package com.kingsrook.qqq.backend.core.model.actions.reporting.pivottable;


/*******************************************************************************
 ** Functions that can be applied to Values in a pivot table.
 *******************************************************************************/
public enum PivotTableFunction
{
   AVERAGE("Average"),
   COUNT("Count Values (COUNTA)"),
   COUNT_NUMS("Count Numbers (COUNT)"),
   MAX("Max"),
   MIN("Min"),
   PRODUCT("Product"),
   STD_DEV("StdDev"),
   STD_DEVP("StdDevp"),
   SUM("Sum"),
   VAR("Var"),
   VARP("Varp");


   private final String label;



   /*******************************************************************************
    **
    *******************************************************************************/
   PivotTableFunction(String label)
   {
      this.label = label;
   }



   /*******************************************************************************
    ** Getter for label
    **
    *******************************************************************************/
   public String getLabel()
   {
      return label;
   }
}
