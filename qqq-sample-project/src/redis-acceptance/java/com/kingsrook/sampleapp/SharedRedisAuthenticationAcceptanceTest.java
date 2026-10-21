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

package com.kingsrook.sampleapp;


import java.time.Duration;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.OAuth2AuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.model.UserSession;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Optional Redis integration using the canonical authentication HTTP fixture.
 *******************************************************************************/
class SharedRedisAuthenticationAcceptanceTest extends SampleAuthenticationAcceptanceTest
{
   /*******************************************************************************
    ** A real Redis outage must not acknowledge logout before cached sessions revoke.
    *******************************************************************************/
   @Test
   void testSharedRedisLogoutOutageAndRetry() throws Exception
   {
      configureOAuthAuthentication();
      ((OAuth2AuthenticationMetaData) instance.getAuthentication()).setSessionStoreEnabled(true);
      try(var redis = new org.testcontainers.containers.GenericContainer<>(
         org.testcontainers.utility.DockerImageName.parse("redis:7.4-alpine")).withExposedPorts(6379))
      {
         redis.start();
         new com.kingsrook.qbits.redis.RedisQBitProducer().withQBitConfig(
            new com.kingsrook.qbits.redis.RedisQBitConfig().withConnection("sessions",
               new com.kingsrook.qbits.redis.RedisConnectionConfig().withHost(redis.getHost())
                  .withPort(redis.getMappedPort(6379)).withTls(false)
                  .withConnectionTimeoutMillis(300).withSocketTimeoutMillis(300)))
            .produce(instance);
         new com.kingsrook.qbits.sessionstore.QSessionStoreQBitProducer().withConfig(
            new com.kingsrook.qbits.sessionstore.QSessionStoreQBitConfig()
               .withProviderType(com.kingsrook.qbits.sessionstore.QSessionStoreProviderType.SHARED_REDIS)
               .withSharedRedis(new com.kingsrook.qbits.sessionstore.SharedRedisSessionStoreSettings(
                  "sessions", "test", "oauth-http", "authentication", 65536))
               .withDefaultTtl(Duration.ofMinutes(5)))
            .produce(instance, "session-store");
         var redisRuntime = new com.kingsrook.qbits.redis.RedisRuntimeService();
         var sessionRuntime = new com.kingsrook.qbits.sessionstore.SharedRedisSessionStoreRuntimeService();
         boolean paused = false;
         try
         {
            start();
            redisRuntime.start(instance);
            sessionRuntime.start(instance);
            var store = com.kingsrook.qbits.sessionstore.QSessionStoreQBitContext.getProvider(instance);
            String uuid = sessionCookie(request("POST", "/manageSession", grant(provider.code(username), SampleAuthenticationProvider.CALLBACK, SampleAuthenticationProvider.VERIFIER), null));
            String other = sessionCookie(request("POST", "/manageSession", grant(provider.code("redis-other-user"), SampleAuthenticationProvider.CALLBACK, SampleAuthenticationProvider.VERIFIER), null));
            assertTrue(store.load(uuid).isPresent());
            assertTrue(store.load(other).isPresent());
            assertEquals(200, request("GET", "/metaData/table/person", null, uuid).statusCode());
            String token = provider.logoutToken(username, false);
            redis.getDockerClient().pauseContainerCmd(redis.getContainerId()).exec();
            paused = true;
            long before = System.nanoTime();
            assertEquals(500, logoutEvent(token).statusCode());
            assertTrue(Duration.ofNanos(System.nanoTime() - before).compareTo(Duration.ofSeconds(5)) < 0);
            QContext.init(instance, new QSystemUserSession());
            assertNotNull(new GetAction().executeForRecord(new GetInput(UserSession.TABLE_NAME).withUniqueKey(Map.of("uuid", uuid))));
            assertEquals(200, request("GET", "/metaData/table/person", null, uuid).statusCode());
            assertEquals(com.kingsrook.qqq.backend.core.health.HealthStatus.DOWN,
               new com.kingsrook.qbits.redis.RedisHealthCheck().run(
                  new com.kingsrook.qqq.backend.core.health.HealthCheckInput().withQInstance(instance)).getStatus());
            redis.getDockerClient().unpauseContainerCmd(redis.getContainerId()).exec();
            paused = false;
            ///////////////////////////////////////////////////////////////////////
            // A timed-out DEL may execute after Redis resumes. Retry must work   //
            // whether the first command deleted the cache entry or not.          //
            ///////////////////////////////////////////////////////////////////////
            success(logoutEvent(token));
            assertTrue(store.load(uuid).isEmpty());
            assertTrue(store.load(other).isPresent());
            assertEquals(401, request("GET", "/metaData/table/person", null, uuid).statusCode());
            assertEquals(200, request("GET", "/metaData/table/person", null, other).statusCode());
            assertEquals(400, logoutEvent(token).statusCode());
            assertEquals(com.kingsrook.qqq.backend.core.health.HealthStatus.UP,
               new com.kingsrook.qbits.redis.RedisHealthCheck().run(
                  new com.kingsrook.qqq.backend.core.health.HealthCheckInput().withQInstance(instance)).getStatus());
         }
         finally
         {
            if(paused)
            {
               redis.getDockerClient().unpauseContainerCmd(redis.getContainerId()).exec();
            }
            if(server != null)
            {
               server.stop();
               server = null;
            }
            sessionRuntime.stop();
            sessionRuntime.afterApplicationStop();
            redisRuntime.stop();
            redisRuntime.afterApplicationStop();
         }
      }
   }
}
