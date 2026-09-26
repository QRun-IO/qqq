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

package com.kingsrook.qqq.backend.core.actions.dashboard;


import java.net.UnknownHostException;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.QuickSightChartRenderer;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QuickSightChartMetaData;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Unit test for QuickSightChartRenderer
 *******************************************************************************/
class QuickSightChartRendererTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testWrongMetaDataClass() throws QException
   {
      RenderWidgetInput input = getInput().withWidgetMetaData(new QWidgetMetaData());
      assertThatThrownBy(() -> new QuickSightChartRenderer().render(input))
         .hasRootCauseInstanceOf(ClassCastException.class);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNoCredentials() throws QException
   {
      RenderWidgetInput input = getInput().withWidgetMetaData(new QuickSightChartMetaData());
      assertThatThrownBy(() -> new QuickSightChartRenderer().render(input))
         .hasRootCauseInstanceOf(NullPointerException.class);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testBadCredentials() throws QException
   {
      RenderWidgetInput input = getInput().withWidgetMetaData(
         new QuickSightChartMetaData()
            .withName("test")
            .withAccessKey("FAIL")
            .withSecretKey("FAIL")
            .withRegion("FAIL")
            .withAccountId("FAIL")
      );
      assertThatThrownBy(() -> new QuickSightChartRenderer().render(input))
         .hasRootCauseInstanceOf(UnknownHostException.class);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private RenderWidgetInput getInput()
   {
      return (new RenderWidgetInput());

   }

}
