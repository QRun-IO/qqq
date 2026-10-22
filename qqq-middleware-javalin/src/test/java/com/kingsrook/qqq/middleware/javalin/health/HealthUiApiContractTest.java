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

package com.kingsrook.qqq.middleware.javalin.health;


import java.nio.file.Path;
import com.kingsrook.qqq.middleware.javalin.tools.UiApiContractAssertions;
import org.junit.jupiter.api.Test;


/*******************************************************************************
 ** The extension fragment remains independent of the committed core document.
 *******************************************************************************/
class HealthUiApiContractTest
{
   /*******************************************************************************
    ** Verify the committed versioned health schemas and paths.
    *******************************************************************************/
   @Test
   void extensionMatchesCommitted() throws Exception
   {
      UiApiContractAssertions.assertExtensionMatchesCommitted(HealthJavalinMetaDataProducer.defineExtension(), Path.of("src/main/resources/openapi/qqq-v1/health.yaml"));
   }
}
