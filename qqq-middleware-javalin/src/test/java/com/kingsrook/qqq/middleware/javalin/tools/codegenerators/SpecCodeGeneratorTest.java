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

package com.kingsrook.qqq.middleware.javalin.tools.codegenerators;


import java.io.File;
import java.io.IOException;
import java.util.UUID;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for SpecCodeGenerator 
 *******************************************************************************/
class SpecCodeGeneratorTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test() throws IOException
   {
      String rootPath = "/tmp/" + UUID.randomUUID() + "/";
      File   dir      = new File(rootPath + "/qqq-middleware-javalin/src/main/java/com/kingsrook/qqq/middleware/javalin/specs/v1/responses");
      assertTrue(dir.mkdirs());
      new SpecCodeGenerator().writeAllFiles(rootPath, "v1", "SomeTest");

      File anExpectedFile = new File(dir.getAbsolutePath() + "/SomeTestResponseV1.java");
      assertTrue(anExpectedFile.exists());

      FileUtils.deleteDirectory(new File(rootPath));
   }

}