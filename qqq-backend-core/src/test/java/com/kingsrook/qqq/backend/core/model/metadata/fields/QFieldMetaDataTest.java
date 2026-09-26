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

package com.kingsrook.qqq.backend.core.model.metadata.fields;


import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for QFieldMetaData 
 *******************************************************************************/
class QFieldMetaDataTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testFieldBehaviors()
   {
      /////////////////////////////////////////
      // create field - assert default state //
      /////////////////////////////////////////
      QFieldMetaData field = new QFieldMetaData("createDate", QFieldType.DATE_TIME);
      assertTrue(CollectionUtils.nullSafeIsEmpty(field.getBehaviors()));
      assertNull(field.getBehaviorOnlyIfSet(DynamicDefaultValueBehavior.class));
      assertEquals(DynamicDefaultValueBehavior.NONE, field.getBehaviorOrDefault(new QInstance(), DynamicDefaultValueBehavior.class));

      //////////////////////////////////////
      // add NONE behavior - assert state //
      //////////////////////////////////////
      field.withBehavior(DynamicDefaultValueBehavior.NONE);
      assertEquals(1, field.getBehaviors().size());
      assertEquals(DynamicDefaultValueBehavior.NONE, field.getBehaviorOnlyIfSet(DynamicDefaultValueBehavior.class));
      assertEquals(DynamicDefaultValueBehavior.NONE, field.getBehaviorOrDefault(new QInstance(), DynamicDefaultValueBehavior.class));

      /////////////////////////////////////////////////////////
      // replace behavior - assert it got rid of the old one //
      /////////////////////////////////////////////////////////
      field.withBehavior(DynamicDefaultValueBehavior.CREATE_DATE);
      assertEquals(1, field.getBehaviors().size());
      assertEquals(DynamicDefaultValueBehavior.CREATE_DATE, field.getBehaviorOnlyIfSet(DynamicDefaultValueBehavior.class));
      assertEquals(DynamicDefaultValueBehavior.CREATE_DATE, field.getBehaviorOrDefault(new QInstance(), DynamicDefaultValueBehavior.class));
   }

}