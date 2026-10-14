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

package com.kingsrook.qqq.backend.core.utils.webhooks;


import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;


/*******************************************************************************
 ** Unit test for WebhookSecret, WebhookSigner and WebhookVerifier.
 *******************************************************************************/
class WebhookSigningTest extends BaseTest
{
   private static final Instant NOW  = Instant.ofEpochSecond(1_760_000_000L);
   private static final byte[]  BODY = "{\"type\":\"cms_blog_post.updated\",\"id\":42}".getBytes(StandardCharsets.UTF_8);



   /*******************************************************************************
    ** The Standard Webhooks reference vector (standard-webhooks/standard-webhooks,
    ** libraries/go/webhook_test.go, TestWebhookSign).
    *******************************************************************************/
   @Test
   void testStandardWebhooksReferenceVector()
   {
      WebhookSecret secret = WebhookSecret.parse("whsec_MfKQ9r8GKYqrTwjUPD8ILPZIo2LaLaSw");
      String signature = WebhookSigner.signatureHeader("msg_p5jXN8AQM9LWM0D4loKWxJek", 1614265330L,
         "{\"test\": 2432232314}".getBytes(StandardCharsets.UTF_8), List.of(secret));
      assertThat(signature).isEqualTo("v1,g0hM9SsE+OTPJTGt/tmIKtSyZlE3uFJELVlNIOLJ1OE=");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testRoundTrip() throws Exception
   {
      WebhookSecret       secret  = WebhookSecret.generate();
      Map<String, String> headers = WebhookSigner.headers("evt_123", NOW, BODY, List.of(secret));

      assertThat(headers).containsOnlyKeys(WebhookSigner.HEADER_ID, WebhookSigner.HEADER_TIMESTAMP, WebhookSigner.HEADER_SIGNATURE);
      assertThat(headers.get(WebhookSigner.HEADER_ID)).isEqualTo("evt_123");
      assertThat(headers.get(WebhookSigner.HEADER_TIMESTAMP)).isEqualTo("1760000000");
      assertThat(headers.get(WebhookSigner.HEADER_SIGNATURE)).startsWith("v1,");

      verifier(List.of(secret)).verify(headers, BODY);
   }



   /*******************************************************************************
    ** Header names are matched case-insensitively.
    *******************************************************************************/
   @Test
   void testHeaderNamesAreCaseInsensitive() throws Exception
   {
      WebhookSecret       secret  = WebhookSecret.generate();
      Map<String, String> headers = new HashMap<>();
      WebhookSigner.headers("evt_1", NOW, BODY, List.of(secret)).forEach((k, v) -> headers.put(k.toUpperCase(), v));
      verifier(List.of(secret)).verify(headers, BODY);
   }



   /*******************************************************************************
    ** During a rotation both secrets sign; a receiver with either one accepts.
    *******************************************************************************/
   @Test
   void testRotationSignsWithBothSecrets() throws Exception
   {
      WebhookSecret       oldSecret = WebhookSecret.generate();
      WebhookSecret       newSecret = WebhookSecret.generate();
      Map<String, String> headers   = WebhookSigner.headers("evt_rotate", NOW, BODY, List.of(newSecret, oldSecret));

      String[] parts = headers.get(WebhookSigner.HEADER_SIGNATURE).split(" ");
      assertThat(parts).hasSize(2).allMatch(part -> part.startsWith("v1,"));

      verifier(List.of(oldSecret)).verify(headers, BODY);
      verifier(List.of(newSecret)).verify(headers, BODY);

      assertThatThrownBy(() -> verifier(List.of(WebhookSecret.generate())).verify(headers, BODY))
         .isInstanceOf(WebhookVerificationException.class)
         .extracting(e -> ((WebhookVerificationException) e).getReason())
         .isEqualTo(WebhookVerificationException.Reason.NO_MATCHING_SIGNATURE);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTimestampToleranceRejectsOldAndFuture()
   {
      WebhookSecret secret = WebhookSecret.generate();
      for(Instant sent : List.of(NOW.minus(Duration.ofMinutes(6)), NOW.plus(Duration.ofMinutes(6))))
      {
         Map<String, String> headers = WebhookSigner.headers("evt_t", sent, BODY, List.of(secret));
         assertThatThrownBy(() -> verifier(List.of(secret)).verify(headers, BODY))
            .isInstanceOf(WebhookVerificationException.class)
            .extracting(e -> ((WebhookVerificationException) e).getReason())
            .isEqualTo(WebhookVerificationException.Reason.TIMESTAMP_OUT_OF_TOLERANCE);
      }

      Map<String, String> justInside = WebhookSigner.headers("evt_t", NOW.minus(Duration.ofMinutes(4)), BODY, List.of(secret));
      assertDoesNotThrow(() -> verifier(List.of(secret)).verify(justInside, BODY));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTamperingIsRejected()
   {
      WebhookSecret       secret  = WebhookSecret.generate();
      Map<String, String> headers = WebhookSigner.headers("evt_x", NOW, BODY, List.of(secret));

      byte[] tamperedBody = "{\"type\":\"cms_blog_post.updated\",\"id\":43}".getBytes(StandardCharsets.UTF_8);
      assertThatThrownBy(() -> verifier(List.of(secret)).verify(headers, tamperedBody)).isInstanceOf(WebhookVerificationException.class);

      Map<String, String> otherId = new HashMap<>(headers);
      otherId.put(WebhookSigner.HEADER_ID, "evt_y");
      assertThatThrownBy(() -> verifier(List.of(secret)).verify(otherId, BODY)).isInstanceOf(WebhookVerificationException.class);

      Map<String, String> otherTime = new HashMap<>(headers);
      otherTime.put(WebhookSigner.HEADER_TIMESTAMP, Long.toString(NOW.getEpochSecond() + 1));
      assertThatThrownBy(() -> verifier(List.of(secret)).verify(otherTime, BODY)).isInstanceOf(WebhookVerificationException.class);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testMissingAndBadHeaders()
   {
      WebhookSecret       secret  = WebhookSecret.generate();
      Map<String, String> headers = WebhookSigner.headers("evt_m", NOW, BODY, List.of(secret));

      for(String name : List.of(WebhookSigner.HEADER_ID, WebhookSigner.HEADER_TIMESTAMP, WebhookSigner.HEADER_SIGNATURE))
      {
         Map<String, String> missing = new HashMap<>(headers);
         missing.remove(name);
         assertThatThrownBy(() -> verifier(List.of(secret)).verify(missing, BODY))
            .extracting(e -> ((WebhookVerificationException) e).getReason())
            .isEqualTo(WebhookVerificationException.Reason.MISSING_HEADERS);
      }
      assertThatThrownBy(() -> verifier(List.of(secret)).verify(null, BODY))
         .extracting(e -> ((WebhookVerificationException) e).getReason())
         .isEqualTo(WebhookVerificationException.Reason.MISSING_HEADERS);

      Map<String, String> badTime = new HashMap<>(headers);
      badTime.put(WebhookSigner.HEADER_TIMESTAMP, "yesterday");
      assertThatThrownBy(() -> verifier(List.of(secret)).verify(badTime, BODY))
         .extracting(e -> ((WebhookVerificationException) e).getReason())
         .isEqualTo(WebhookVerificationException.Reason.BAD_TIMESTAMP);
   }



   /*******************************************************************************
    ** Unknown versions are skipped, and a valid v1 among them still passes.
    *******************************************************************************/
   @Test
   void testUnknownVersionsAreIgnored() throws Exception
   {
      WebhookSecret       secret  = WebhookSecret.generate();
      Map<String, String> headers = new HashMap<>(WebhookSigner.headers("evt_v", NOW, BODY, List.of(secret)));
      String              v1      = headers.get(WebhookSigner.HEADER_SIGNATURE);

      headers.put(WebhookSigner.HEADER_SIGNATURE, "v2,AAAA v1a,BBBB garbage " + v1);
      verifier(List.of(secret)).verify(headers, BODY);

      headers.put(WebhookSigner.HEADER_SIGNATURE, v1.replace("v1,", "v2,"));
      assertThatThrownBy(() -> verifier(List.of(secret)).verify(headers, BODY))
         .extracting(e -> ((WebhookVerificationException) e).getReason())
         .isEqualTo(WebhookVerificationException.Reason.NO_MATCHING_SIGNATURE);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSecretParseGenerateAndToString()
   {
      WebhookSecret generated = WebhookSecret.generate();
      String        text      = generated.reveal();
      assertThat(text).startsWith(WebhookSecret.PREFIX);
      assertThat(generated.keyBytes()).hasSize(WebhookSecret.GENERATED_KEY_BYTES);
      assertThat(WebhookSecret.parse(text)).isEqualTo(generated);
      assertThat(WebhookSecret.parse("  " + text + "\n")).isEqualTo(generated);
      assertThat(WebhookSecret.generate()).isNotEqualTo(generated);

      assertThat(generated.toString()).doesNotContain(text.substring(WebhookSecret.PREFIX.length())).contains(generated.fingerprint());
      assertThat(generated.fingerprint()).hasSize(12).matches("[0-9a-f]+");
      assertThat(generated.hashCode()).isEqualTo(WebhookSecret.parse(text).hashCode());

      WebhookSecret raw = WebhookSecret.parse("a-raw-shared-secret-of-enough-length");
      assertThat(raw.keyBytes()).isEqualTo("a-raw-shared-secret-of-enough-length".getBytes(StandardCharsets.UTF_8));

      byte[] copy = generated.keyBytes();
      copy[0]++;
      assertThat(generated.keyBytes()[0]).isNotEqualTo(copy[0]);

      assertThatThrownBy(() -> WebhookSecret.parse(null)).isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> WebhookSecret.parse(" ")).isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> WebhookSecret.parse("short")).isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> WebhookSecret.parse("whsec_!!!not-base64!!!")).isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> WebhookSecret.parse("whsec_c2hvcnQ=")).isInstanceOf(IllegalArgumentException.class);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSignerArgumentChecks()
   {
      WebhookSecret secret = WebhookSecret.generate();
      assertThatThrownBy(() -> WebhookSigner.signatureHeader("evt", 1L, BODY, List.of())).isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> WebhookSigner.signatureHeader("evt", 1L, BODY, null)).isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> WebhookSigner.sign("a.b", 1L, BODY, secret)).isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> WebhookSigner.sign("a b", 1L, BODY, secret)).isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> WebhookSigner.sign("", 1L, BODY, secret)).isInstanceOf(IllegalArgumentException.class);
      assertThat(WebhookSigner.sign("evt", 1L, null, secret)).isEqualTo(WebhookSigner.sign("evt", 1L, new byte[0], secret));
      assertThatThrownBy(() -> new WebhookVerifier(List.of())).isInstanceOf(IllegalArgumentException.class);
      assertDoesNotThrow(() -> new WebhookVerifier(List.of(secret)));
   }



   /*******************************************************************************
    ** A verifier at NOW with the default tolerance.
    *******************************************************************************/
   private static WebhookVerifier verifier(List<WebhookSecret> secrets)
   {
      return (new WebhookVerifier(secrets, WebhookVerifier.DEFAULT_TOLERANCE, Clock.fixed(NOW, ZoneOffset.UTC)));
   }
}
