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

package com.kingsrook.qqq.backend.core.health;


import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Secrets and identifiers must be removed without destroying useful diagnosis.
 *******************************************************************************/
class HealthRedactorTest extends BaseTest
{


   /*******************************************************************************
    ** Verify health behavior against the public contract.
    *******************************************************************************/
   @Test
   void stripsUrlAuthenticationQueryAndFragment()
   {
      assertThat(HealthRedactor.redact("failed https://user:pass@host/x?token=abc#secret"))
         .isEqualTo("failed https://host/x");
      assertThat(HealthRedactor.redact("http://user:pass@host http://host/x#id https://host/x"))
         .isEqualTo("http://host http://host/x https://host/x");
   }



   /*******************************************************************************
    ** Verify health behavior against the public contract.
    *******************************************************************************/
   @ParameterizedTest
   @ValueSource(strings = { "Bearer abc.def-123", "Basic dXNlcjpwYXNz", "key=abc123", "secret='abc def'", "password=hello", "token=xyz", "api_key: abc", "AKIA1234567890ABCDEF", "ASIA1234567890ABCDEF", "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjMifQ.signature", "alice@example.com", "+1 (314) 555-1212", "123456789", "0123456789abcdef0123456789abcdef", "abcdefghijklmnopqrstuvwxyzABCDEF", "S3NT1NEL-secret-value" })
   void masksSensitivePatterns(String value)
   {
      assertThat(HealthRedactor.redact("failure: " + value)).doesNotContain(value).contains("[redacted]");
   }



   /*******************************************************************************
    ** Verify health behavior against the public contract.
    *******************************************************************************/
   @Test
   void masksMultilinePemBeforeTakingTheExceptionFirstLine()
   {
      String pem = "-----BEGIN PRIVATE KEY-----\nabc123\n-----END PRIVATE KEY-----";
      assertThat(HealthRedactor.redact(pem)).isEqualTo("[redacted]");
      assertThat(HealthRedactor.redactThrowable(new IllegalStateException("Bearer abc123\nsecond line")))
         .isEqualTo("IllegalStateException: [redacted]");
      assertThat(HealthRedactor.redactThrowable(new IllegalStateException())).isEqualTo("IllegalStateException: ");
      assertThat(HealthRedactor.redact("HTTP 503 after 42ms")).isEqualTo("HTTP 503 after 42ms");
      assertThat(HealthRedactor.redact(null)).isNull();
   }



   /*******************************************************************************
    ** Verify health behavior against the public contract.
    *******************************************************************************/
   @Test
   void detailsAllowOnlySafeScalarsAndBoundedValidJson()
   {
      HealthDetails details = new HealthDetails().withValue("duration.ms", 12).withValue("healthy", true)
         .withValue("at", Instant.EPOCH).withValue("message", "alice@example.com")
         .withValue("apiKey", "must not be stored").withValue("password", new Object());
      assertThat(details.getValues()).containsEntry("apiKey", "[redacted]").containsEntry("password", "[redacted]")
         .containsEntry("message", "[redacted]");
      assertThatThrownBy(() -> details.withValue("invalid-key", "x")).isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> details.withValue(null, "x")).isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> details.withValue("nested", Map.of("x", 1))).isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> details.withValue("nullValue", null)).isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> details.withValue("nan", Double.NaN)).isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> details.getValues().put("bad", "mutation")).isInstanceOf(UnsupportedOperationException.class);
      details.withValue("longValue", "test ".repeat(150));
      assertThat(details.getValues().get("longValue").toString()).hasSize(500);
      for(int i = 0; i < 25; i++)
      {
         details.withValue("value" + i, "ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â§ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢".repeat(500));
      }
      assertThat(details.toJson().getBytes(StandardCharsets.UTF_8).length).isLessThanOrEqualTo(4096);
      assertThat(new JSONObject(details.toJson()).getInt("duration.ms")).isEqualTo(12);
   }



   /*******************************************************************************
    ** Verify health behavior against the public contract.
    *******************************************************************************/
   @Test
   void aggregateMatchesLegacyPrecedenceAndEnumsArePossibleValues()
   {
      assertThat(HealthStatus.aggregate()).isEqualTo(HealthStatus.UNKNOWN);
      assertThat(HealthStatus.aggregate((HealthStatus[]) null)).isEqualTo(HealthStatus.UNKNOWN);
      assertThat(HealthStatus.aggregate(HealthStatus.UP, null)).isEqualTo(HealthStatus.UNKNOWN);
      assertThat(HealthStatus.aggregate(HealthStatus.UNKNOWN, HealthStatus.UP, HealthStatus.DEGRADED)).isEqualTo(HealthStatus.DEGRADED);
      assertThat(HealthStatus.aggregate(HealthStatus.DEGRADED, HealthStatus.DOWN)).isEqualTo(HealthStatus.DOWN);
      assertThat(HealthStatus.aggregate(HealthStatus.UP)).isEqualTo(HealthStatus.UP);
      assertThat(HealthResultStatus.RUNNING.getPossibleValueId()).isEqualTo("RUNNING");
      assertThat(HealthCheckKind.READINESS.getPossibleValueLabel()).isEqualTo("READINESS");
      assertThat(HealthCategory.CORE.getPossibleValueId()).isEqualTo("CORE");
      assertThat(HealthTriggeredBy.STARTUP.getPossibleValueLabel()).isEqualTo("STARTUP");
   }
}
