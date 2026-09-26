/*
 * Copyright 2026 QRun-IO.
 * SPDX-License-Identifier: Apache-2.0
 */

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
