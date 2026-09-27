/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.metadata.tables;


import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for SectionFactory 
 *******************************************************************************/
class SectionFactoryTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test()
   {
      QFieldSection t1section = SectionFactory.defaultT1("id", "name");
      assertEquals(SectionFactory.getDefaultT1name(), t1section.getName());
      assertEquals(SectionFactory.getDefaultT1iconName(), t1section.getIcon().getName());
      assertEquals(Tier.T1, t1section.getTier());
      assertEquals(List.of("id", "name"), t1section.getFieldNames());

      QFieldSection t2section = SectionFactory.defaultT2("size", "age");
      assertEquals(SectionFactory.getDefaultT2name(), t2section.getName());
      assertEquals(SectionFactory.getDefaultT2iconName(), t2section.getIcon().getName());
      assertEquals(Tier.T2, t2section.getTier());
      assertEquals(List.of("size", "age"), t2section.getFieldNames());

      QFieldSection t3section = SectionFactory.defaultT3("createDate", "modifyDate");
      assertEquals(SectionFactory.getDefaultT3name(), t3section.getName());
      assertEquals(SectionFactory.getDefaultT3iconName(), t3section.getIcon().getName());
      assertEquals(Tier.T3, t3section.getTier());
      assertEquals(List.of("createDate", "modifyDate"), t3section.getFieldNames());
   }

}