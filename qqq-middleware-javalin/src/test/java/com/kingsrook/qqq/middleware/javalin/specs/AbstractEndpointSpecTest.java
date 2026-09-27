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

package com.kingsrook.qqq.middleware.javalin.specs;


import java.util.List;
import com.kingsrook.qqq.middleware.javalin.specs.v1.AuthenticationMetaDataSpecV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.TagsV1;
import com.kingsrook.qqq.openapi.model.Method;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 ** Tests the shared OpenAPI method definition used by endpoint specs.
 *******************************************************************************/
class AbstractEndpointSpecTest
{
   /*******************************************************************************
    ** An endpoint may omit its tag and still define a complete operation.
    *******************************************************************************/
   @Test
   void testDefineCompleteOperation_withoutTag_preservesMethod()
   {
      AuthenticationMetaDataSpecV1 spec = new AuthenticationMetaDataSpecV1()
      {
         @Override
         public BasicOperation defineBasicOperation()
         {
            return super.defineBasicOperation().withTag(null);
         }



         @Override
         protected void customizeMethod(Method method)
         {
            method.setOperationId("authenticationWithoutTag");
         }
      };

      CompleteOperation operation = spec.defineCompleteOperation();
      assertNull(operation.getTag());
      assertNull(operation.getMethod().getTags());
      assertEquals(spec.defineBasicOperation().getShortSummary(), operation.getMethod().getSummary());
      assertEquals(spec.defineBasicOperation().getLongDescription(), operation.getMethod().getDescription());
      assertEquals("authenticationWithoutTag", operation.getMethod().getOperationId());
      assertNotNull(operation.getMethod().getResponses().get(200));
   }



   /*******************************************************************************
    ** Tagged endpoints retain their tag in the OpenAPI method.
    *******************************************************************************/
   @Test
   void testDefineMethod_withTag_preservesTag()
   {
      Method method = new AuthenticationMetaDataSpecV1().defineMethod();
      assertEquals(List.of(TagsV1.AUTHENTICATION.getText()), method.getTags());
   }
}
