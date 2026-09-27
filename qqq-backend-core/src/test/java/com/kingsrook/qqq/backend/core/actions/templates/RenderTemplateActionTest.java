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

package com.kingsrook.qqq.backend.core.actions.templates;


import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.templates.RenderTemplateInput;
import com.kingsrook.qqq.backend.core.model.actions.templates.RenderTemplateOutput;
import com.kingsrook.qqq.backend.core.model.templates.TemplateType;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for RenderTemplateAction
 *******************************************************************************/
public class RenderTemplateActionTest extends BaseTest
{
   private int doThrowCallCount = 0;



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test() throws QException
   {
      RenderTemplateInput renderTemplateInput = new RenderTemplateInput();
      renderTemplateInput.setCode("""
         Hello, $name""");
      renderTemplateInput.setContext(Map.of("name", "Darin"));
      renderTemplateInput.setTemplateType(TemplateType.VELOCITY);
      RenderTemplateOutput output = new RenderTemplateAction().execute(renderTemplateInput);
      assertEquals("Hello, Darin", output.getResult());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testConvenientWrapper() throws QException
   {
      String template = "Hello, $name";
      assertEquals("Hello, Darin", RenderTemplateAction.renderVelocity(Map.of("name", "Darin"), template));
      assertEquals("Hello, Tim", RenderTemplateAction.renderVelocity(Map.of("name", "Tim"), template));
      assertEquals("Hello, $name", RenderTemplateAction.renderVelocity(Map.of(), template));

      template = "Hello, $!name";
      assertEquals("Hello, ", RenderTemplateAction.renderVelocity(Map.of(), template));
      assertEquals("Hello, ", RenderTemplateAction.renderVelocity(null, template));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testMissingType()
   {
      assertThatThrownBy(() -> RenderTemplateAction.render(null, Map.of("name", "Darin"), "Hello, $name"))
         .isInstanceOf(QException.class)
         .hasMessageContaining("Unsupported Template Type");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testExceptionInVelocity() throws QException
   {
      RenderTemplateInput renderTemplateInput = new RenderTemplateInput();
      renderTemplateInput.setCode("""
         This should throw: $this.doThrow().
         This should throw silently: $!this.doThrow().
         """);
      renderTemplateInput.setContext(Map.of("this", this));
      renderTemplateInput.setTemplateType(TemplateType.VELOCITY);
      RenderTemplateOutput output = new RenderTemplateAction().execute(renderTemplateInput);
      assertThat(output.getResult())
         .contains("throw: $this.doThrow().")
         .contains("throw silently: .");

      ///////////////////////////////////////////////////////
      // make sure our method got called twice as expected //
      ///////////////////////////////////////////////////////
      assertEquals(2, doThrowCallCount);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public String doThrow() throws Exception
   {
      doThrowCallCount++;
      throw (new Exception("You asked to throw..."));
   }

}
