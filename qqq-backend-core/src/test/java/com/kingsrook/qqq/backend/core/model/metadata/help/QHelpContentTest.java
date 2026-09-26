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

package com.kingsrook.qqq.backend.core.model.metadata.help;


import com.kingsrook.qqq.backend.core.BaseTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 ** Unit test for QHelpContent 
 *******************************************************************************/
class QHelpContentTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetContentAsHtml()
   {
      assertNull(new QHelpContent().withFormat(null).withContent(null).getContentAsHtml());
      assertNull(new QHelpContent().withFormat(HelpFormat.MARKDOWN).withContent(null).getContentAsHtml());
      assertNull(new QHelpContent().withFormat(HelpFormat.HTML).withContent(null).getContentAsHtml());
      assertNull(new QHelpContent().withFormat(HelpFormat.TEXT).withContent(null).getContentAsHtml());

      assertEquals("<p><em>hi</em></p>\n", new QHelpContent().withFormat(HelpFormat.MARKDOWN).withContent("*hi*").getContentAsHtml());
      assertEquals("<i>hi</i>", new QHelpContent().withFormat(HelpFormat.HTML).withContent("<i>hi</i>").getContentAsHtml());
      assertEquals("hi", new QHelpContent().withFormat(HelpFormat.TEXT).withContent("hi").getContentAsHtml());
      assertEquals("*hi*", new QHelpContent().withFormat(HelpFormat.TEXT).withContent("*hi*").getContentAsHtml());
      assertEquals("*hi*", new QHelpContent().withFormat(null).withContent("*hi*").getContentAsHtml());
   }

}