/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.ai;


/*******************************************************************************
 ** Data sensitivity, ordered from least to most sensitive.
 ** Null means not classified and is less sensitive than every declared value.
 *******************************************************************************/
public enum DataClassification
{
   /*******************************************************************************
    ** May be shown to anyone.
    *******************************************************************************/
   PUBLIC,

   /*******************************************************************************
    ** Business-confidential information.
    *******************************************************************************/
   CONFIDENTIAL,

   /*******************************************************************************
    ** Personally identifiable information.
    *******************************************************************************/
   PII,

   /*******************************************************************************
    ** Protected health information under HIPAA.
    *******************************************************************************/
   PHI;



   /*******************************************************************************
    ** Whether this classification is at least as sensitive as other.
    ** Every classification is at least as sensitive as null.
    *******************************************************************************/
   public boolean isAtLeast(DataClassification other)
   {
      return (other == null || ordinal() >= other.ordinal());
   }



   /*******************************************************************************
    ** The more sensitive classification; null loses to any declared value.
    ** Returns null when both values are null.
    *******************************************************************************/
   public static DataClassification max(DataClassification a, DataClassification b)
   {
      return (a == null ? b : a.isAtLeast(b) ? a : b);
   }
}
