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

/* Copyright 2026 QRun-IO. */

package com.kingsrook.qbits.reference;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Adapts the data-QBit template's tokenized Liquibase changelog to this consumer. */
public final class ReferenceDataLiquibaseGenerator
{
   private static final String TEMPLATE_PATH = "/db/reference-category-changelog.xml";

   private ReferenceDataLiquibaseGenerator()
   {
   }

   /** Produce the changelog for a validated QBit prefix and table name. */
   public static String generate(String prefix, String tableName)
   {
      try(InputStream stream = ReferenceDataLiquibaseGenerator.class.getResourceAsStream(TEMPLATE_PATH))
      {
         if(stream == null)
         {
            throw new IllegalStateException("Reference-data changelog template is missing");
         }
         return new String(stream.readAllBytes(), StandardCharsets.UTF_8)
            .replace("${prefix}", prefix).replace("${table}", tableName);
      }
      catch(IOException e)
      {
         throw new IllegalStateException("Could not load reference-data changelog template", e);
      }
   }
}
