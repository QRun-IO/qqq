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

package com.kingsrook.qqq.backend.core.actions.dashboard.widgets;


import java.util.HashMap;
import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QNotFoundException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.CronUISetupData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.CronUIWidgetData;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;


/*******************************************************************************
 ** Source reads require user permission; expression-only and trusted calls do not.
 *******************************************************************************/
class CronUIWidgetRendererTest extends BaseTest
{
   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSourceReadPermissionAndTrustedCaller() throws QException
   {
      QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON_MEMORY).setPermissionRules(
         QPermissionRules.defaultInstance().withLevel(PermissionLevel.READ_WRITE_PERMISSIONS));
      new InsertAction().executeForRecord(new InsertInput(TestUtils.TABLE_NAME_PERSON_MEMORY)
         .withRecord(new QRecord().withValue("id", 1).withValue("firstName", "0 0 9 * * ?")));
      QContext.setQSession(new QSession());
      RenderWidgetInput input = input(Map.of("id", "1"));
      input.setInputSource(QInputSource.USER);
      assertThrows(QPermissionDeniedException.class, () -> new CronUIWidgetRenderer().render(input));
      QContext.getQSession().withPermission(TestUtils.TABLE_NAME_PERSON_MEMORY + ".read");
      String description = ((CronUIWidgetData) new CronUIWidgetRenderer().render(input).getWidgetData()).getCronDescription();
      assertNotNull(description);
      QContext.setQSession(new QSession());
      input.setInputSource(QInputSource.SYSTEM);
      assertEquals(description, ((CronUIWidgetData) new CronUIWidgetRenderer().render(input).getWidgetData()).getCronDescription());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testExpressionOnlyEmptyAndInvalid() throws QException
   {
      RenderWidgetInput input = input(Map.of("cronExpression", "0 0 9 * * ?"));
      input.setInputSource(QInputSource.USER);
      CronUIWidgetData data = (CronUIWidgetData) new CronUIWidgetRenderer().render(input).getWidgetData();
      assertNotNull(data.getCronDescription());
      assertNull(data.getError());
      input.setQueryParams(new HashMap<>());
      data = (CronUIWidgetData) new CronUIWidgetRenderer().render(input).getWidgetData();
      assertNull(data.getCronDescription());
      input.setQueryParams(new HashMap<>(Map.of("cronExpression", "invalid")));
      data = (CronUIWidgetData) new CronUIWidgetRenderer().render(input).getWidgetData();
      assertNotNull(data.getError());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testMissingRecord()
   {
      assertThrows(QNotFoundException.class, () -> new CronUIWidgetRenderer().render(input(Map.of("id", "999"))));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private RenderWidgetInput input(Map<String, String> query)
   {
      QWidgetMetaData widget = CronUIWidgetRenderer.buildWidgetMetaData("ownedCron", "Owned cron",
         new CronUISetupData(TestUtils.TABLE_NAME_PERSON_MEMORY, "firstName", null));
      RenderWidgetInput input = new RenderWidgetInput().withWidgetMetaData(widget);
      input.setQueryParams(new HashMap<>(query));
      return input;
   }
}
