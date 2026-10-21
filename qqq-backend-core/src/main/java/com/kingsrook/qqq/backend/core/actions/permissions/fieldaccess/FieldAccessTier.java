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

package com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess;


import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;


/*******************************************************************************
 ** Effective field access, ordered from least to most restrictive.
 *******************************************************************************/
public enum FieldAccessTier
{
   FULL, REVEALABLE, MASKED, HIDDEN;



   /*******************************************************************************
    ** canReadValue
    *******************************************************************************/
   public boolean canReadValue()
   {
      return (this == FULL);
   }



   /*******************************************************************************
    ** canSort
    *******************************************************************************/
   public boolean canSort()
   {
      return (this == FULL);
   }



   /*******************************************************************************
    ** canGroupOrAggregate
    *******************************************************************************/
   public boolean canGroupOrAggregate()
   {
      return (this == FULL);
   }



   /*******************************************************************************
    ** canQuickSearch
    *******************************************************************************/
   public boolean canQuickSearch()
   {
      return (this == FULL);
   }



   /*******************************************************************************
    ** canWrite
    *******************************************************************************/
   public boolean canWrite()
   {
      return (this == FULL);
   }



   /*******************************************************************************
    ** Whether the operator is available for this tier.
    *******************************************************************************/
   public boolean canFilter(QCriteriaOperator operator)
   {
      return (this == FULL || (this == REVEALABLE && (operator == QCriteriaOperator.EQUALS || operator == QCriteriaOperator.IN)));
   }



   /*******************************************************************************
    ** Whether the field may be described to the caller.
    *******************************************************************************/
   public boolean isInMetaData()
   {
      return (this != HIDDEN);
   }



   /*******************************************************************************
    ** Combine tiers; null means FULL.
    *******************************************************************************/
   public FieldAccessTier mostRestrictive(FieldAccessTier other)
   {
      FieldAccessTier candidate = other == null ? FULL : other;
      return (ordinal() > candidate.ordinal() ? this : candidate);
   }



   /*******************************************************************************
    ** Combine tiers; null means FULL.
    *******************************************************************************/
   public FieldAccessTier leastRestrictive(FieldAccessTier other)
   {
      FieldAccessTier candidate = other == null ? FULL : other;
      return (ordinal() < candidate.ordinal() ? this : candidate);
   }
}
