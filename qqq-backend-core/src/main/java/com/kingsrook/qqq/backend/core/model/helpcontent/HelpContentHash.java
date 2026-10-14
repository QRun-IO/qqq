/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.helpcontent;


import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** Stable three-way merge baseline for raw help text.
 *******************************************************************************/
public final class HelpContentHash
{
   /*******************************************************************************
    ** Hash format, summary and content, treating null as empty.
    *******************************************************************************/
   public static String of(String format, String summary, String content)
   {
      String text = Objects.requireNonNullElse(format, "") + '\u0000' + Objects.requireNonNullElse(summary, "") + '\u0000' + Objects.requireNonNullElse(content, "");
      try
      {
         return (HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8))));
      }
      catch(NoSuchAlgorithmException e)
      {
         throw new IllegalStateException("SHA-256 is required by Java", e);
      }
   }



   /*******************************************************************************
    ** Hash the persisted text fields.
    *******************************************************************************/
   public static String of(QRecord record)
   {
      return (of(record.getValueString("format"), record.getValueString("summary"), record.getValueString("content")));
   }
}
