/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.instances;


import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.amazonaws.services.secretsmanager.model.CreateSecretRequest;
import com.amazonaws.services.secretsmanager.model.Filter;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;
import com.amazonaws.services.secretsmanager.model.ListSecretsRequest;
import com.amazonaws.services.secretsmanager.model.ListSecretsResult;
import com.amazonaws.services.secretsmanager.model.PutSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.ResourceExistsException;
import com.amazonaws.services.secretsmanager.model.SecretListEntry;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;


/*******************************************************************************
 ** Utility class for working with AWS Secrets Manager.
 **
 ** Relies on environment variables:
 ** SECRETS_MANAGER_ACCESS_KEY
 ** SECRETS_MANAGER_SECRET_KEY
 ** SECRETS_MANAGER_REGION
 **
 *******************************************************************************/
public class SecretsManagerUtils
{
   private static final Logger LOG = LogManager.getLogger(SecretsManagerUtils.class);

   private static QMetaDataVariableInterpreter qMetaDataVariableInterpreter;
   private static AWSSecretsManager            _client = null;



   /*******************************************************************************
    ** IF secret manager ENV vars are set,
    ** THEN lookup all secrets starting with the given prefix,
    ** and write them to a .env file (backing up any pre-existing .env files first).
    *******************************************************************************/
   public static void writeEnvFromSecretsWithNamePrefix(String prefix) throws IOException
   {
      writeEnvFromSecretsWithNamePrefix(prefix, true);
   }



   /*******************************************************************************
    ** IF secret manager ENV vars are set,
    ** THEN lookup all secrets starting with the given prefix,
    ** and write them to a .env file (backing up any pre-existing .env files first).
    *******************************************************************************/
   public static void writeEnvFromSecretsWithNamePrefix(String prefix, boolean quoteValues) throws IOException
   {
      Optional<AWSSecretsManager> optionalSecretsManagerClient = getSecretsManagerClient();
      if(optionalSecretsManagerClient.isPresent())
      {
         AWSSecretsManager client = optionalSecretsManagerClient.get();

         ListSecretsRequest listSecretsRequest = new ListSecretsRequest().withFilters(new Filter().withKey("name").withValues(prefix));
         listSecretsRequest.withMaxResults(100);
         ListSecretsResult listSecretsResult = client.listSecrets(listSecretsRequest);

         StringBuilder fullEnv = new StringBuilder();
         while(true)
         {
            for(SecretListEntry secretListEntry : listSecretsResult.getSecretList())
            {
               String           nameWithoutPrefix = secretListEntry.getName().replace(prefix, "");
               Optional<String> secretValue       = getSecret(prefix, nameWithoutPrefix);
               if(secretValue.isPresent())
               {
                  String envLine = quoteValues
                     ? nameWithoutPrefix + "=\"" + secretValue.get() + "\""
                     : nameWithoutPrefix + "=" + secretValue.get();
                  fullEnv.append(envLine).append('\n');
               }
            }

            if(listSecretsResult.getNextToken() != null)
            {
               LOG.trace("Calling for next token...");
               listSecretsRequest.setNextToken(listSecretsResult.getNextToken());
               listSecretsResult = client.listSecrets(listSecretsRequest);
            }
            else
            {
               break;
            }
         }

         File dotEnv = new File(".env");
         if(dotEnv.exists())
         {
            dotEnv.renameTo(new File(".env.backup-" + System.currentTimeMillis()));
         }

         FileUtils.writeStringToFile(dotEnv, fullEnv.toString(), StandardCharsets.UTF_8);
      }
      else
      {
         LOG.info("Not writing .env from secrets manager");
      }
   }



   /*******************************************************************************
    ** Get a single secret value.
    **
    ** The lookup in secrets manager is done by (path + name).  Then, in the value
    ** that comes back, if it looks like JSON, we look for a value inside it under
    ** the key of just "name".  Else, if we didn't get JSON back, then we just return
    ** the full text value of the secret.
    *******************************************************************************/
   public static Optional<String> getSecret(String path, String name)
   {
      Optional<AWSSecretsManager> optionalSecretsManagerClient = getSecretsManagerClient();
      if(optionalSecretsManagerClient.isPresent())
      {
         try
         {
            AWSSecretsManager     client                = optionalSecretsManagerClient.get();
            String                secretId              = path + name;
            GetSecretValueRequest getSecretValueRequest = new GetSecretValueRequest().withSecretId(secretId);

            GetSecretValueResult getSecretValueResult = client.getSecretValue(getSecretValueRequest);

            try
            {
               JSONObject secretJSON = new JSONObject(getSecretValueResult.getSecretString());

               ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
               // know we know it's a json object - so - commit to either returning the value under this name, else warning and returning empty //
               ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
               if(secretJSON.has(name))
               {
                  return (Optional.of(secretJSON.getString(name)));
               }
               else
               {
                  // Security: Don't log secretId as it reveals secret structure
                  LOG.warn("SecretsManager secret was a JSON object, but it did not contain the expected key [{}] - returning empty.", name);
                  return (Optional.empty());
               }
            }
            catch(JSONException je)
            {
               //////////////////////////////////////////////////////////////////////////////////////////////////
               // if the secret value couldn't be parsed as json, then assume it to be text and just return it //
               //////////////////////////////////////////////////////////////////////////////////////////////////
               return (Optional.of(getSecretValueResult.getSecretString()));
            }
         }
         catch(Exception e)
         {
            LOG.debug("Error getting secret from secretManager: ", e);
         }
      }

      return (Optional.empty());
   }



   /*******************************************************************************
    ** Tries to do a Create - if that fails, then does a Put (update).
    **
    ** Path is expected to end in a /, but I suppose it isn't strictly required.
    *******************************************************************************/
   public static void writeSecret(String path, String name, String value)
   {
      JSONObject secretJson = new JSONObject();
      secretJson.put(name, value);

      Optional<AWSSecretsManager> optionalSecretsManagerClient = getSecretsManagerClient();
      if(optionalSecretsManagerClient.isPresent())
      {
         AWSSecretsManager client = optionalSecretsManagerClient.get();

         try
         {
            CreateSecretRequest createSecretRequest = new CreateSecretRequest();
            createSecretRequest.setName(path + name);
            createSecretRequest.setSecretString(secretJson.toString());
            client.createSecret(createSecretRequest);
         }
         catch(ResourceExistsException e)
         {
            PutSecretValueRequest putSecretValueRequest = new PutSecretValueRequest();
            putSecretValueRequest.setSecretId(path + name);
            putSecretValueRequest.setSecretString(secretJson.toString());
            client.putSecretValue(putSecretValueRequest);
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static Optional<AWSSecretsManager> getSecretsManagerClient()
   {
      if(_client == null)
      {
         QMetaDataVariableInterpreter interpreter = getQMetaDataVariableInterpreter();

         String accessKey = interpreter.interpret("${env.SECRETS_MANAGER_ACCESS_KEY}");
         String secretKey = interpreter.interpret("${env.SECRETS_MANAGER_SECRET_KEY}");
         String region    = interpreter.interpret("${env.SECRETS_MANAGER_REGION}");

         if(StringUtils.hasContent(accessKey) && StringUtils.hasContent(secretKey) && StringUtils.hasContent(region))
         {
            try
            {
               BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
               _client = AWSSecretsManagerClientBuilder.standard()
                  .withCredentials(new AWSStaticCredentialsProvider(credentials))
                  .withRegion(region)
                  .build();
            }
            catch(Exception e)
            {
               LOG.error("Error opening Secrets Manager client", e);
            }
         }
         else
         {
            LOG.warn("One or more SECRETS_MANAGER env var was not set.  Unable to open Secrets Manager client.");
         }
      }

      return (Optional.ofNullable(_client));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QMetaDataVariableInterpreter getQMetaDataVariableInterpreter()
   {
      return Objects.requireNonNullElseGet(qMetaDataVariableInterpreter, QMetaDataVariableInterpreter::new);
   }



   /**
    * Sets the {@link QMetaDataVariableInterpreter} instance to be used by this utility.
    * <p>
    * This method is primarily intended for tests or one-off usage to configure
    * a variable interpreter with an override environment.
    *
    * @param qMetaDataVariableInterpreter the interpreter instance to use; may be {@code null}
    *                                     to reset to the default behavior.
    */
   static void setQMetaDataVariableInterpreter(QMetaDataVariableInterpreter qMetaDataVariableInterpreter)
   {
      SecretsManagerUtils.qMetaDataVariableInterpreter = qMetaDataVariableInterpreter;
   }

}
