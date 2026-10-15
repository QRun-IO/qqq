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


package com.kingsrook.qqq.backend.core.jobs;


import com.kingsrook.qqq.backend.core.jobs.progress.DefaultJobLogRedactor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** No credentials or common personal identifiers reach job logs.
 *******************************************************************************/
class JobLogRedactorTest
{
   /*******************************************************************************
    ** Every supported credential key is redacted.
    *******************************************************************************/
   @ParameterizedTest
   @ValueSource(strings = {"password", "passwd", "secret", "token", "api_key", "api-key", "apikey", "authorization", "credential", "private_key", "private-key"})
   void keys(String key)
   {
      assertThat(new DefaultJobLogRedactor().redact(key + "=sensitive-value")).doesNotContain("sensitive-value").contains("[redacted]");
      assertThat(new DefaultJobLogRedactor().redact("{\"" + key + "\":\"sensitive value\"}")).doesNotContain("sensitive value");
   }



   /*******************************************************************************
    ** Tokens, identifiers and URL credentials are removed.
    *******************************************************************************/
   @ParameterizedTest
   @ValueSource(strings = {"Bearer sensitive-value", "Authorization: Bearer sensitive-value", "AKIA1234567890ABCDEF", "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0In0.signature", "https://user:password@example.org/path", "0123456789abcdef0123456789abcdef", "aBcDeFgHiJkLmNoPqRsTuVwXyZ0123456789aBcDeFgHiJkLmNoPqR==", "someone@example.org", "123-45-6789", "(618) 555-1234", "+1-618-555-1234"})
   void patterns(String value)
   {
      String result = new DefaultJobLogRedactor().redact(value);
      assertThat(result).contains("[redacted]").doesNotContain("sensitive-value");
   }



   /*******************************************************************************
    ** Useful operational text survives; control characters and excessive length do not.
    *******************************************************************************/
   @Test
   void safeTextAndBounds()
   {
      DefaultJobLogRedactor redactor = new DefaultJobLogRedactor();
      String text = "Wrote kof22_blog_post: 1,204 rows, 2.1 MB in 450 ms";
      assertThat(redactor.redact(text)).isEqualTo(text);
      assertThat(redactor.redact("hi\nthere\t!")).isEqualTo("hithere!");
      assertThat(redactor.redact("words ".repeat(300))).hasSize(1000);
      assertThat(redactor.redact(null)).isNull();
      assertThat(redactor.redact("(618)555-1234")).isEqualTo("[redacted]");

      //////////////////////////////////////////////////////////////////////
      // storage keys, paths and long table names are not secrets         //
      //////////////////////////////////////////////////////////////////////
      String key = "Wrote runs/2026-10-14T12-00-00Z/tables/kof22_blog_post/part-00001-of-00004";
      assertThat(redactor.redact(key)).isEqualTo(key);
      String table = "kof22_blog_post_attachment_media_file_references_by_owner";
      assertThat(redactor.redact(table)).isEqualTo(table);
      assertThat(redactor.redact("key aBcDeFgHiJkLmNoPqRsTuVwXyZ0123456789aBcD/eFgH")).contains("[redacted]");
   }
}
