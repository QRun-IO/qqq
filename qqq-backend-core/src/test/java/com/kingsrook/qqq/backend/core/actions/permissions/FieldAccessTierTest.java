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

package com.kingsrook.qqq.backend.core.actions.permissions;


import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessTier;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Secured field access contract tests.
 *******************************************************************************/
class FieldAccessTierTest
{



   /*******************************************************************************
    ** Verify the secured-data contract.
    *******************************************************************************/
   @ParameterizedTest
   @EnumSource(FieldAccessTier.class)
   void capabilities(FieldAccessTier tier)
   {
      assertEquals(tier == FieldAccessTier.FULL, tier.canReadValue());
      assertEquals(tier == FieldAccessTier.FULL, tier.canWrite());
      assertEquals(tier == FieldAccessTier.FULL, tier.canSort());
      assertEquals(tier == FieldAccessTier.FULL, tier.canQuickSearch());
      assertEquals(tier == FieldAccessTier.FULL, tier.canGroupOrAggregate());
      assertEquals(tier != FieldAccessTier.HIDDEN, tier.isInMetaData());
      for(QCriteriaOperator operator : QCriteriaOperator.values())
      {
         assertEquals(tier == FieldAccessTier.FULL || (tier == FieldAccessTier.REVEALABLE && (operator == QCriteriaOperator.EQUALS || operator == QCriteriaOperator.IN)), tier.canFilter(operator));
      }
      assertEquals(tier, tier.mostRestrictive(null));
      assertEquals(FieldAccessTier.FULL, tier.leastRestrictive(null));
      for(FieldAccessTier other : FieldAccessTier.values())
      {
         assertEquals(FieldAccessTier.values()[Math.max(tier.ordinal(), other.ordinal())], tier.mostRestrictive(other));
         assertEquals(FieldAccessTier.values()[Math.min(tier.ordinal(), other.ordinal())], tier.leastRestrictive(other));
      }
   }
}
