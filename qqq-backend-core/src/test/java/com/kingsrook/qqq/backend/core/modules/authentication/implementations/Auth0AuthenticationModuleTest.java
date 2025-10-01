/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.kingsrook.qqq.backend.core.modules.authentication.implementations;


import java.io.Serializable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.auth0.exception.Auth0Exception;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QAuthenticationException;
import com.kingsrook.qqq.backend.core.instances.QMetaDataVariableInterpreter;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.Auth0AuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleCustomizerInterface;
import com.kingsrook.qqq.backend.core.state.InMemoryStateProvider;
import com.kingsrook.qqq.backend.core.state.SimpleStateKey;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static com.kingsrook.qqq.backend.core.modules.authentication.implementations.Auth0AuthenticationModule.ACCESS_TOKEN_KEY;
import static com.kingsrook.qqq.backend.core.modules.authentication.implementations.Auth0AuthenticationModule.BASIC_AUTH_KEY;
import static com.kingsrook.qqq.backend.core.modules.authentication.implementations.Auth0AuthenticationModule.COULD_NOT_DECODE_ERROR;
import static com.kingsrook.qqq.backend.core.modules.authentication.implementations.Auth0AuthenticationModule.EXPIRED_TOKEN_ERROR;
import static com.kingsrook.qqq.backend.core.modules.authentication.implementations.Auth0AuthenticationModule.INVALID_TOKEN_ERROR;
import static com.kingsrook.qqq.backend.core.modules.authentication.implementations.Auth0AuthenticationModule.TOKEN_NOT_PROVIDED_ERROR;
import static com.kingsrook.qqq.backend.core.modules.authentication.implementations.Auth0AuthenticationModule.maskForLog;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


/*******************************************************************************
 ** Unit test for the FullyAnonymousAuthenticationModule
 *******************************************************************************/
public class Auth0AuthenticationModuleTest extends BaseTest
{
   private static final String INVALID_TOKEN     = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6IllrY2FkWTA0Q3RFVUFxQUdLNTk3ayJ9.eyJnaXZlbl9uYW1lIjoiVGltIiwiZmFtaWx5X25hbWUiOiJDaGFtYmVybGFpbiIsIm5pY2tuYW1lIjoidGltLmNoYW1iZXJsYWluIiwibmFtZSI6IlRpbSBDaGFtYmVybGFpbiIsInBpY3R1cmUiOiJodHRwczovL2xoMy5nb29nbGV1c2VyY29udGVudC5jb20vYS0vQUZkWnVjcXVSaUFvTzk1RG9URklnbUtseVA1akVBVnZmWXFnS0lHTkVubzE9czk2LWMiLCJsb2NhbGUiOiJlbiIsInVwZGF0ZWRfYXQiOiIyMDIyLTA3LTE5VDE2OjI0OjQ1LjgyMloiLCJlbWFpbCI6InRpbS5jaGFtYmVybGFpbkBraW5nc3Jvb2suY29tIiwiZW1haWxfdmVyaWZpZWQiOnRydWUsImlzcyI6Imh0dHBzOi8va2luZ3Nyb29rLnVzLmF1dGgwLmNvbS8iLCJzdWIiOiJnb29nbGUtb2F1dGgyfDEwODk2NDEyNjE3MjY1NzAzNDg2NyIsImF1ZCI6InNwQ1NtczAzcHpVZGRYN1BocHN4ZDlUd2FLMDlZZmNxIiwiaWF0IjoxNjU4MjQ3OTAyLCJleHAiOjE2NTgyODM5MDIsIm5vbmNlIjoiZUhOdFMxbEtUR2N5ZG5KS1VVY3RkRTFVT0ZKNmJFNUxVVkEwZEdsRGVXOXZkVkl4UW41eVRrUlJlZz09In0.hib7JR8NDU2kx8Fj1bnzo3IUuabE6Hb-Z7HHZAJPQuF_Zdg3L1KDypn6SY7HAd_dsz2N8RkXfvQto-Y2g2ukuz7FxzNFgcVL99cyEO3YqmyCa6JTOTCrxdeaIE8QZpCEKvC28oeJBv0wO1Dwc--OVJMsK2vSzyxj1WNok64YYjWKLL4c0dFf-nj0KWFr1IU-tMiyWLDDiJw2Sa8M4YxXZYqdlkgNmrBPExgcm9l9SiT2l3Ts3Sgc_IyMVyMrnV8XX50EWdsm6vuCOSUcqf0XhjDQ7urZveoVwVLnYq3GcLhVBcy1Hr9RL8zPdPynOzsbX6uCww2Esrv6iwWrgQ5zBA-thismakesinvalid";
   private static final String EXPIRED_TOKEN     = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6IllrY2FkWTA0Q3RFVUFxQUdLNTk3ayJ9.eyJnaXZlbl9uYW1lIjoiVGltIiwiZmFtaWx5X25hbWUiOiJDaGFtYmVybGFpbiIsIm5pY2tuYW1lIjoidGltLmNoYW1iZXJsYWluIiwibmFtZSI6IlRpbSBDaGFtYmVybGFpbiIsInBpY3R1cmUiOiJodHRwczovL2xoMy5nb29nbGV1c2VyY29udGVudC5jb20vYS0vQUZkWnVjcXVSaUFvTzk1RG9URklnbUtseVA1akVBVnZmWXFnS0lHTkVubzE9czk2LWMiLCJsb2NhbGUiOiJlbiIsInVwZGF0ZWRfYXQiOiIyMDIyLTA3LTE4VDIxOjM4OjE1LjM4NloiLCJlbWFpbCI6InRpbS5jaGFtYmVybGFpbkBraW5nc3Jvb2suY29tIiwiZW1haWxfdmVyaWZpZWQiOnRydWUsImlzcyI6Imh0dHBzOi8va2luZ3Nyb29rLnVzLmF1dGgwLmNvbS8iLCJzdWIiOiJnb29nbGUtb2F1dGgyfDEwODk2NDEyNjE3MjY1NzAzNDg2NyIsImF1ZCI6InNwQ1NtczAzcHpVZGRYN1BocHN4ZDlUd2FLMDlZZmNxIiwiaWF0IjoxNjU4MTgwNDc3LCJleHAiOjE2NTgyMTY0NzcsIm5vbmNlIjoiVkZkQlYzWmplR2hvY1cwMk9WZEtabHBLU0c1K1ZXbElhMEV3VkZaeFpVdEJVMDErZUZaT1RtMTNiZz09In0.fU7EwUgNrupOPz_PX_aQKON2xG1-LWD85xVo1Bn41WNEek-iMyJoch8l6NUihi7Bou14BoOfeWIG_sMqsLHqI2Pk7el7l1kigsjURx0wpiXadBt8piMxdIlxdToZEMuZCBzg7eJvXh4sM8tlV5cm0gPa6FT9Ih3VGJajNlXi5BcYS_JRpIvFvHn8-Bxj4KiAlZ5XPPkopjnDgP8kFfc4cMn_nxDkqWYlhj-5TaGW2xCLC9Qr_9UNxX0fm-CkKjYs3Z5ezbiXNkc-bxrCYvxeBeDPf8-T3EqrxCRVqCZSJ85BHdOc_E7UZC_g8bNj0umoplGwlCbzO4XIuOO-KlIaOg";
   private static final String UNDECODABLE_TOKEN = "UNDECODABLE";

   private static final QLogger LOG = QLogger.getLogger(Auth0AuthenticationModuleTest.class);



   /*******************************************************************************
    ** Test a token where last-checked is set to a time that would not require it to be
    ** re-checked, so it'll show as valid no matter what the token is.
    **
    *******************************************************************************/
   @Test
   public void testLastTimeCheckedJustUnderThreshold()
   {
      Instant underThreshold = Instant.now().minus(Auth0AuthenticationModule.ID_TOKEN_VALIDATION_INTERVAL_SECONDS - 60, ChronoUnit.SECONDS);
      assertTrue(testLastTimeChecked(underThreshold, INVALID_TOKEN), "A session checked under threshold should be valid");
   }



   /*******************************************************************************
    ** Test a token where last-checked is set to a time that would require it to be
    ** re-checked, so it'll show as invalid.
    **
    *******************************************************************************/
   @Test
   public void testLastTimeCheckedJustOverThreshold()
   {
      Instant overThreshold = Instant.now().minus(Auth0AuthenticationModule.ID_TOKEN_VALIDATION_INTERVAL_SECONDS + 60, ChronoUnit.SECONDS);
      assertFalse(testLastTimeChecked(overThreshold, INVALID_TOKEN), "A session checked over threshold should be re-validated, and in this case, not be valid.");
   }



   /*******************************************************************************
    ** Test a token where last-checked is past the threshold, so it'll get re-checked,
    ** and will fail.
    **
    *******************************************************************************/
   @Test
   public void testLastTimeCheckedOverThresholdAndUndecodable()
   {
      Instant overThreshold = Instant.now().minus(Auth0AuthenticationModule.ID_TOKEN_VALIDATION_INTERVAL_SECONDS + 60, ChronoUnit.SECONDS);
      assertFalse(testLastTimeChecked(overThreshold, UNDECODABLE_TOKEN), "A session checked over threshold should be re-validated, and in this case, not be valid.");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private boolean testLastTimeChecked(Instant lastTimeChecked, String token)
   {
      /////////////////////////////////////////////////////////////
      // put the input last-time-checked into the state provider //
      /////////////////////////////////////////////////////////////
      SimpleStateKey<String> key = new SimpleStateKey<>(token);
      InMemoryStateProvider.getInstance().put(key, lastTimeChecked);

      //////////////////////
      // build up session //
      //////////////////////
      QSession session = new QSession();
      session.setIdReference(token);

      Auth0AuthenticationModule auth0AuthenticationModule = new Auth0AuthenticationModule();
      return (auth0AuthenticationModule.isSessionValid(getQInstance(false), session));
   }



   /*******************************************************************************
    ** Test failure case, token is invalid
    **
    *******************************************************************************/
   @Test
   public void testInvalidToken()
   {
      Map<String, String> context = new HashMap<>();
      context.put(ACCESS_TOKEN_KEY, INVALID_TOKEN);

      try
      {
         Auth0AuthenticationModule auth0AuthenticationModule = new Auth0AuthenticationModule();
         auth0AuthenticationModule.createSession(getQInstance(true), context);
         fail("Should never get here");
      }
      catch(MissingEnvVarsException meve)
      {
         LOG.warn("Unable to execute testInvalidToken, due to missing env vars: " + meve.getMessage());
      }
      catch(QAuthenticationException qae)
      {
         assertThat(qae.getMessage()).contains(INVALID_TOKEN_ERROR);
      }
   }



   /*******************************************************************************
    ** Test failure case, token can't be decoded
    **
    *******************************************************************************/
   @Test
   public void testUndecodableToken()
   {
      Map<String, String> context = new HashMap<>();
      context.put(ACCESS_TOKEN_KEY, UNDECODABLE_TOKEN);

      try
      {
         Auth0AuthenticationModule auth0AuthenticationModule = new Auth0AuthenticationModule();
         auth0AuthenticationModule.createSession(getQInstance(false), context);
         fail("Should never get here");
      }
      catch(QAuthenticationException qae)
      {
         assertThat(qae.getMessage()).contains(COULD_NOT_DECODE_ERROR);
      }
   }



   /*******************************************************************************
    ** Test failure case, token is expired
    **
    *******************************************************************************/
   @Test
   public void testProperlyFormattedButExpiredToken()
   {
      Map<String, String> context = new HashMap<>();
      context.put(ACCESS_TOKEN_KEY, EXPIRED_TOKEN);

      try
      {
         Auth0AuthenticationModule auth0AuthenticationModule = new Auth0AuthenticationModule();
         auth0AuthenticationModule.createSession(getQInstance(true), context);
         fail("Should never get here");
      }
      catch(MissingEnvVarsException meve)
      {
         LOG.warn("Unable to execute testProperlyFormattedButExpiredToken, due to missing env vars: " + meve.getMessage());
      }
      catch(QAuthenticationException qae)
      {
         assertThat(qae.getMessage()).contains(EXPIRED_TOKEN_ERROR);
      }
   }



   /*******************************************************************************
    ** Test failure case, empty context
    **
    *******************************************************************************/
   @Test
   public void testEmptyContext()
   {
      try
      {
         Auth0AuthenticationModule auth0AuthenticationModule = new Auth0AuthenticationModule();
         auth0AuthenticationModule.createSession(getQInstance(false), new HashMap<>());
         fail("Should never get here");
      }
      catch(QAuthenticationException qae)
      {
         assertThat(qae.getMessage()).contains(TOKEN_NOT_PROVIDED_ERROR);
      }
   }



   /*******************************************************************************
    ** Test failure case, null token
    **
    *******************************************************************************/
   @Test
   public void testNullToken()
   {
      Map<String, String> context = new HashMap<>();
      context.put(ACCESS_TOKEN_KEY, null);

      try
      {
         Auth0AuthenticationModule auth0AuthenticationModule = new Auth0AuthenticationModule();
         auth0AuthenticationModule.createSession(getQInstance(false), context);
         fail("Should never get here");
      }
      catch(QAuthenticationException qae)
      {
         assertThat(qae.getMessage()).contains(TOKEN_NOT_PROVIDED_ERROR);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testBasicAuthSuccess() throws QAuthenticationException, Auth0Exception
   {
      Map<String, String> context = new HashMap<>();
      context.put(BASIC_AUTH_KEY, encodeBasicAuth("darin.kelkhoff@gmail.com", "6-EQ!XzBJ!F*LRVDK6VZY__92!"));

      try
      {
         QInstance qInstance = getQInstance(true);

         Auth0AuthenticationModule auth0Spy = spy(Auth0AuthenticationModule.class);
         auth0Spy.createSession(qInstance, context);
         auth0Spy.createSession(qInstance, context);
         auth0Spy.createSession(qInstance, context);
         verify(auth0Spy, times(1)).getAccessTokenForUsernameAndPasswordFromAuth0(any(), any(), any());
      }
      catch(MissingEnvVarsException meve)
      {
         LOG.warn("Unable to execute testBasicAuthSuccess, due to missing env vars: " + meve.getMessage());
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSetPermissionsInSessionFromJwtPayload()
   {
      QSession qSession = new QSession();
      JSONObject payload = new JSONObject("""
         {
           "com.kingsrook.qqq.client_metadata": {
             "securityKeyValues:clientIdAllAccess": "true"
           },
           "iss": "https://kingsrook.us.auth0.com/",
           "sub": "LRuqVS2awusyOyqTzMH6oPC00XXKJj@clients",
           "aud": "https://www.kingsrook.com",
           "iat": 1673379451,
           "exp": 1675971451,
           "azp": "LRuqVS2awOyqTFwzMH6oPC00XXKJj",
           "gty": "client-credentials",
           "permissions": [
             "client.read",
             "client.insert"
          ]
         }
         """);
      Auth0AuthenticationModule.setPermissionsInSessionFromJwtPayload(payload, qSession);
      assertTrue(qSession.hasPermission("client.read"));
      assertTrue(qSession.hasPermission("client.insert"));
      assertEquals(2, qSession.getPermissions().size());

      ///////////////////////////////
      // test w/ empty permissions //
      ///////////////////////////////
      qSession = new QSession();
      payload = new JSONObject("""
         {
           "iss": "https://kingsrook.us.auth0.com/",
           "azp": "LRuqVS2awOyqTFwzMH6oPC00XXKJj",
           "gty": "client-credentials",
           "permissions": []
         }
         """);
      Auth0AuthenticationModule.setPermissionsInSessionFromJwtPayload(payload, qSession);
      assertTrue(qSession.getPermissions().isEmpty());

      /////////////////////////////////
      // test w/ missing permissions //
      /////////////////////////////////
      qSession = new QSession();
      payload = new JSONObject("""
         {
           "iss": "https://kingsrook.us.auth0.com/",
           "azp": "LRuqVS2awOyqTFwzMH6oPC00XXKJj",
           "gty": "client-credentials"
         }
         """);
      Auth0AuthenticationModule.setPermissionsInSessionFromJwtPayload(payload, qSession);
      assertTrue(qSession.getPermissions().isEmpty());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSetSecurityKeysInSessionFromJwtPayload()
   {
      QInstance qInstance = getQInstance(false);
      QSession  qSession  = new QSession();
      JSONObject payload = new JSONObject("""
         {
           "com.kingsrook.qqq.client_metadata": {
             "securityKeyValues:storeAllAccess": "true"
           },
           "iss": "https://kingsrook.us.auth0.com/",
           "sub": "LRuqVS2awusyOyqTzMH6oPC00XXKJj@clients",
           "iat": 1673379451
         }
         """);
      new Auth0AuthenticationModule().setSecurityKeysInSessionFromJwtPayload(qInstance, payload, qSession);
      assertEquals(List.of("true"), qSession.getSecurityKeyValues("storeAllAccess"));

      /////////////////////////////////////////////
      // app_metadata instead of client_metadata //
      /////////////////////////////////////////////
      qSession = new QSession();
      payload = new JSONObject("""
         {
           "com.kingsrook.qqq.app_metadata": {
             "securityKeyValues": {
                "store": 2
             }
           },
           "iss": "https://kingsrook.us.auth0.com/",
           "sub": "LRuqVS2awusyOyqTzMH6oPC00XXKJj@clients",
           "iat": 1673379451
         }
         """);
      new Auth0AuthenticationModule().setSecurityKeysInSessionFromJwtPayload(qInstance, payload, qSession);
      assertEquals(List.of("2"), qSession.getSecurityKeyValues("store"));

      //////////////////////////
      // list of values       //
      // and, more than 1 key //
      //////////////////////////
      qSession = new QSession();
      payload = new JSONObject("""
         {
           "com.kingsrook.qqq.app_metadata": {
             "securityKeyValues": {
                "store": [3, 4, 5],
                "internalOrExternal": "internal"
             }
           },
           "iss": "https://kingsrook.us.auth0.com/",
           "sub": "LRuqVS2awusyOyqTzMH6oPC00XXKJj@clients",
           "iat": 1673379451
         }
         """);
      new Auth0AuthenticationModule().setSecurityKeysInSessionFromJwtPayload(qInstance, payload, qSession);
      assertEquals(List.of("3", "4", "5"), qSession.getSecurityKeyValues("store"));
      assertEquals(List.of("internal"), qSession.getSecurityKeyValues("internalOrExternal"));

      ///////////////////////////////////////////
      // missing meta data -> no security keys //
      ///////////////////////////////////////////
      qSession = new QSession();
      payload = new JSONObject("""
         {
           "iss": "https://kingsrook.us.auth0.com/",
           "sub": "LRuqVS2awusyOyqTzMH6oPC00XXKJj@clients",
           "iat": 1673379451
         }
         """);
      new Auth0AuthenticationModule().setSecurityKeysInSessionFromJwtPayload(qInstance, payload, qSession);
      assertTrue(CollectionUtils.nullSafeIsEmpty(qSession.getSecurityKeyValues()));

      /////////////////////////////////////////////////////
      // unrecognized security key -> no keys in session //
      /////////////////////////////////////////////////////
      qSession = new QSession();
      payload = new JSONObject("""
         {
           "com.kingsrook.qqq.app_metadata": {
             "securityKeyValues": {
                "notAKey": 47
             }
           },
           "iss": "https://kingsrook.us.auth0.com/",
           "sub": "LRuqVS2awusyOyqTzMH6oPC00XXKJj@clients",
           "iat": 1673379451
         }
         """);
      new Auth0AuthenticationModule().setSecurityKeysInSessionFromJwtPayload(qInstance, payload, qSession);
      assertTrue(CollectionUtils.nullSafeIsEmpty(qSession.getSecurityKeyValues()));
   }



   /*******************************************************************************
    ** utility method to prime a qInstance for auth0 tests
    **
    *******************************************************************************/
   private QInstance getQInstance(boolean throwIfEnvMissing)
   {
      String auth0BaseUrl      = new QMetaDataVariableInterpreter().interpret("${env.AUTH0_BASE_URL}");
      String auth0ClientId     = new QMetaDataVariableInterpreter().interpret("${env.AUTH0_CLIENT_ID}");
      String auth0ClientSecret = new QMetaDataVariableInterpreter().interpret("${env.AUTH0_CLIENT_SECRET}");
      String auth0Audience     = new QMetaDataVariableInterpreter().interpret("${env.AUTH0_AUDIENCE}");

      if(throwIfEnvMissing && (!StringUtils.hasContent(auth0BaseUrl) || !StringUtils.hasContent(auth0ClientId) || !StringUtils.hasContent(auth0ClientSecret) || !StringUtils.hasContent(auth0Audience)))
      {
         throw new MissingEnvVarsException("You must set the following environment variables to run this test: AUTH0_BASE_URL, AUTH0_CLIENT_ID, AUTH0_CLIENT_SECRET, AUTH0_AUDIENCE");
      }

      QAuthenticationMetaData authenticationMetaData = new Auth0AuthenticationMetaData()
         .withBaseUrl(auth0BaseUrl)
         .withClientId(auth0ClientId)
         .withClientSecret(auth0ClientSecret)
         .withAudience(auth0Audience)
         .withName("auth0");

      QInstance qInstance = QContext.getQInstance();
      qInstance.setAuthentication(authenticationMetaData);
      return (qInstance);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String encodeBasicAuth(String username, String password)
   {
      Base64.Encoder encoder        = Base64.getEncoder();
      String         originalString = username + ":" + password;
      return (encoder.encodeToString(originalString.getBytes()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testMask()
   {
      assertNull(maskForLog(null));
      assertEquals("******", maskForLog("1"));
      assertEquals("******", maskForLog("12"));
      assertEquals("******", maskForLog("123"));
      assertEquals("******", maskForLog("1234"));
      assertEquals("******", maskForLog("12345"));
      assertEquals("******", maskForLog("12345"));
      assertEquals("******", maskForLog("123456"));
      assertEquals("******", maskForLog("1234567"));
      assertEquals("123456******", maskForLog("12345678"));
      assertEquals("123456******", maskForLog("123456789"));
      assertEquals("123456******", maskForLog("1234567890"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testCustomizer()
   {
      QInstance qInstance = QContext.getQInstance();
      qInstance.setAuthentication(new Auth0AuthenticationMetaData()
         .withCustomizer(new QCodeReference(Customizer.class)));

      {
         /////////////////////////////////////////////////////////////////
         // baseline case - value in json becomes value in security key //
         /////////////////////////////////////////////////////////////////
         QSession qSession = new QSession();
         JSONObject payload = new JSONObject("""
            {
              "com.kingsrook.qqq.app_metadata": {
                "securityKeyValues": {
                   "store": "1"
                }
              }
            }
            """);
         new Auth0AuthenticationModule().setSecurityKeysInSessionFromJwtPayload(qInstance, payload, qSession);
         assertEquals(List.of("1"), qSession.getSecurityKeyValues("store"));
      }

      {
         QSession qSession = new QSession();
         JSONObject payload = new JSONObject("""
            {
              "com.kingsrook.qqq.app_metadata": {
                "securityKeyValues": {
                   "store": "oddDigits"
                }
              }
            }
            """);
         new Auth0AuthenticationModule().setSecurityKeysInSessionFromJwtPayload(qInstance, payload, qSession);
         assertEquals(List.of("1", "3", "5", "7", "9"), qSession.getSecurityKeyValues("store"));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class Customizer implements QAuthenticationModuleCustomizerInterface
   {
      @Override
      public void addSecurityKeyValueToSession(QSession session, String keyName, Serializable value)
      {
         if("oddDigits".equals(value))
         {
            for(String oddValue : List.of("1", "3", "5", "7", "9"))
            {
               QAuthenticationModuleCustomizerInterface.super.addSecurityKeyValueToSession(session, keyName, oddValue);
            }
         }
         else
         {
            QAuthenticationModuleCustomizerInterface.super.addSecurityKeyValueToSession(session, keyName, value);
         }
      }
   }



   /***************************************************************************
    *
    ***************************************************************************/
   public static class MissingEnvVarsException extends RuntimeException
   {
      /***************************************************************************
       *
       ***************************************************************************/
      public MissingEnvVarsException(String message)
      {
         super(message);
      }
   }
}
