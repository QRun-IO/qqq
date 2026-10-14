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

package com.kingsrook.qqq.middleware.javalin.tools;


import java.nio.file.Files;
import java.nio.file.Path;
import com.kingsrook.qqq.middleware.javalin.specs.extensions.MiddlewareSpecExtension;
import com.kingsrook.qqq.middleware.javalin.specs.v1.MiddlewareVersionV1;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Shared extension fragment assertion, with explicit opt-in regeneration.
 *******************************************************************************/
public final class UiApiContractAssertions
{
   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private UiApiContractAssertions()
   {
   }



   /*******************************************************************************
    ** Compare generated OpenAPI with the committed fragment.
    *******************************************************************************/
   public static void assertExtensionMatchesCommitted(MiddlewareSpecExtension extension, Path path) throws Exception
   {
      String generated = UiApiContract.generateExtensionYaml(new MiddlewareVersionV1(), extension);
      if(Boolean.getBoolean("qqq.uiApi.regenerate"))
      {
         Files.createDirectories(path.getParent());
         Files.writeString(path, generated);
      }
      assertThat(UiApiContract.check(generated, Files.readString(path), null, 1, "Regenerate with -Dqqq.uiApi.regenerate=true")).isEmpty();
   }
}
