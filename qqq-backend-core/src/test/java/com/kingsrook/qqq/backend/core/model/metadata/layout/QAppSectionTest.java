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

package com.kingsrook.qqq.backend.core.model.metadata.layout;


import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Tests for inferred navigation behavior and independent section clones.
 *******************************************************************************/
class QAppSectionTest
{
   /*******************************************************************************
    ** Legacy sections infer roles; explicit values take precedence.
    *******************************************************************************/
   @Test
   void testEffectiveRoleAndCollapsedState()
   {
      QAppSection section = new QAppSection();
      assertEquals(QAppSectionRole.PRIMARY, section.getEffectiveRole());
      assertFalse(section.getEffectiveCollapsedByDefault());
      section.withName("SeTtInGs");
      assertEquals(QAppSectionRole.SETTINGS, section.getEffectiveRole());
      assertTrue(section.getEffectiveCollapsedByDefault());
      section.withName("other").withLabel("Settings");
      assertEquals(QAppSectionRole.SETTINGS, section.getEffectiveRole());
      section.withLabel(null).withName("HiStOrY");
      assertEquals(QAppSectionRole.HISTORY, section.getEffectiveRole());
      assertFalse(section.getEffectiveCollapsedByDefault());
      section.withName(null).withApp("child");
      assertEquals(QAppSectionRole.GROUP, section.getEffectiveRole());
      section.withTable("table");
      assertEquals(QAppSectionRole.PRIMARY, section.getEffectiveRole());
      section.setTables(null);
      section.withProcess("process");
      assertEquals(QAppSectionRole.PRIMARY, section.getEffectiveRole());
      section.setProcesses(null);
      section.withReport("report");
      assertEquals(QAppSectionRole.PRIMARY, section.getEffectiveRole());
      section.withRole(QAppSectionRole.SETTINGS).withCollapsedByDefault(false);
      assertEquals(QAppSectionRole.SETTINGS, section.getEffectiveRole());
      assertFalse(section.getEffectiveCollapsedByDefault());
      section.withRole(QAppSectionRole.PRIMARY).withCollapsedByDefault(true);
      assertTrue(section.getEffectiveCollapsedByDefault());
   }



   /*******************************************************************************
    ** Each list is copied, including lists supplied as immutable values.
    *******************************************************************************/
   @Test
   void testCloneIndependence()
   {
      QAppSection original = new QAppSection().withApp("app").withTables(List.of("table"))
         .withProcess("process").withReport("report").withRole(QAppSectionRole.HISTORY).withCollapsedByDefault(true);
      QAppSection clone = original.clone();
      clone.getApps().clear();
      clone.getTables().clear();
      clone.getProcesses().clear();
      clone.getReports().clear();
      assertEquals(List.of("app"), original.getApps());
      assertEquals(List.of("table"), original.getTables());
      assertEquals(List.of("process"), original.getProcesses());
      assertEquals(List.of("report"), original.getReports());
      assertEquals(QAppSectionRole.HISTORY, clone.getRole());
      assertTrue(clone.getCollapsedByDefault());
      QAppSection empty = new QAppSection().clone();
      assertNull(empty.getApps());
      assertNull(empty.getTables());
      assertNull(empty.getProcesses());
      assertNull(empty.getReports());
   }
}
