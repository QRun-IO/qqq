/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.middleware.javalin.specs.v1.responses;


import com.kingsrook.qqq.backend.core.model.metadata.authentication.Auth0AuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.OAuth2AuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.middleware.javalin.executors.io.AuthenticationMetaDataOutputInterface;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIOneOf;


/*******************************************************************************
 **
 *******************************************************************************/
public class AuthenticationMetaDataResponseV1 implements AuthenticationMetaDataOutputInterface, ToSchema
{
   @OpenAPIDescription("""
      Specifier for the type of authentication module being used.
      
      Frontends should use this value to determine how to prompt the user for authentication credentials.
      In addition, depending on this value, additional properties will be included in this object, as
      may be needed to complete the authorization workflow with the provider (e.g., a baseUrl, clientId,
      and audience for an OAuth type workflow).""")
   private String type;

   @OpenAPIDescription("""
      Unique name for the authentication metaData object within the QInstance.
      """)
   private String name;

   @OpenAPIDescription("""
      Additional values, as determined by the type of authentication provider.
      """)
   @OpenAPIOneOf()
   private Values values;



   /***************************************************************************
    **
    ***************************************************************************/
   public sealed interface Values permits EmptyValues, Auth0Values, OAuth2Values
   {

   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("No additional values are used for some authentication providers.")
   public static final class EmptyValues implements Values
   {

   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Additional values used by the Auth0 type authentication provider.")
   public static final class Auth0Values implements Values
   {
      @OpenAPIDescription("ClientId for auth0")
      private String clientId;

      @OpenAPIDescription("BaseUrl for auth0")
      private String baseUrl;

      @OpenAPIDescription("Audience for auth0")
      private String audience;



      /*******************************************************************************
       ** Getter for clientId
       **
       *******************************************************************************/
      public String getClientId()
      {
         return clientId;
      }



      /*******************************************************************************
       ** Setter for clientId
       **
       *******************************************************************************/
      public void setClientId(String clientId)
      {
         this.clientId = clientId;
      }



      /*******************************************************************************
       ** Fluent setter for clientId
       **
       *******************************************************************************/
      public Auth0Values withClientId(String clientId)
      {
         this.clientId = clientId;
         return (this);
      }



      /*******************************************************************************
       ** Getter for baseUrl
       **
       *******************************************************************************/
      public String getBaseUrl()
      {
         return baseUrl;
      }



      /*******************************************************************************
       ** Setter for baseUrl
       **
       *******************************************************************************/
      public void setBaseUrl(String baseUrl)
      {
         this.baseUrl = baseUrl;
      }



      /*******************************************************************************
       ** Fluent setter for baseUrl
       **
       *******************************************************************************/
      public Auth0Values withBaseUrl(String baseUrl)
      {
         this.baseUrl = baseUrl;
         return (this);
      }



      /*******************************************************************************
       ** Getter for audience
       **
       *******************************************************************************/
      public String getAudience()
      {
         return audience;
      }



      /*******************************************************************************
       ** Setter for audience
       **
       *******************************************************************************/
      public void setAudience(String audience)
      {
         this.audience = audience;
      }



      /*******************************************************************************
       ** Fluent setter for audience
       **
       *******************************************************************************/
      public Auth0Values withAudience(String audience)
      {
         this.audience = audience;
         return (this);
      }

   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Additional values used by the OAuth2 type authentication provider.")
   public static final class OAuth2Values implements Values
   {
      @OpenAPIDescription("ClientId for OAuth2 provider")
      private String clientId;

      @OpenAPIDescription("BaseUrl for OAuth2 provider")
      private String baseUrl;

      @OpenAPIDescription("OAuth2 scopes to request (space-separated)")
      private String scopes;



      /*******************************************************************************
       ** Getter for clientId
       *******************************************************************************/
      public String getClientId()
      {
         return clientId;
      }



      /*******************************************************************************
       ** Setter for clientId
       *******************************************************************************/
      public void setClientId(String clientId)
      {
         this.clientId = clientId;
      }



      /*******************************************************************************
       ** Fluent setter for clientId
       *******************************************************************************/
      public OAuth2Values withClientId(String clientId)
      {
         this.clientId = clientId;
         return (this);
      }



      /*******************************************************************************
       ** Getter for baseUrl
       *******************************************************************************/
      public String getBaseUrl()
      {
         return baseUrl;
      }



      /*******************************************************************************
       ** Setter for baseUrl
       *******************************************************************************/
      public void setBaseUrl(String baseUrl)
      {
         this.baseUrl = baseUrl;
      }



      /*******************************************************************************
       ** Fluent setter for baseUrl
       *******************************************************************************/
      public OAuth2Values withBaseUrl(String baseUrl)
      {
         this.baseUrl = baseUrl;
         return (this);
      }



      /*******************************************************************************
       ** Getter for scopes
       *******************************************************************************/
      public String getScopes()
      {
         return scopes;
      }



      /*******************************************************************************
       ** Setter for scopes
       *******************************************************************************/
      public void setScopes(String scopes)
      {
         this.scopes = scopes;
      }



      /*******************************************************************************
       ** Fluent setter for scopes
       *******************************************************************************/
      public OAuth2Values withScopes(String scopes)
      {
         this.scopes = scopes;
         return (this);
      }

   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void setAuthenticationMetaData(QAuthenticationMetaData qAuthenticationMetaData)
   {
      setType(qAuthenticationMetaData.getType().name());
      setName(qAuthenticationMetaData.getName());

      if(qAuthenticationMetaData instanceof OAuth2AuthenticationMetaData oauth2MetaData)
      {
         OAuth2Values oauth2Values = new OAuth2Values();
         values = oauth2Values;
         oauth2Values.setClientId(oauth2MetaData.getClientId());
         oauth2Values.setBaseUrl(oauth2MetaData.getBaseUrl());
         oauth2Values.setScopes(oauth2MetaData.getScopes());
      }
      else if(qAuthenticationMetaData instanceof Auth0AuthenticationMetaData auth0MetaData)
      {
         Auth0Values auth0Values = new Auth0Values();
         values = auth0Values;
         auth0Values.setClientId(auth0MetaData.getClientId());
         auth0Values.setBaseUrl(auth0MetaData.getBaseUrl());
         auth0Values.setAudience(auth0MetaData.getAudience());
      }

      /*
      JSONObject jsonObject = new JSONObject(JsonUtils.toJson(qAuthenticationMetaData));
      for(String key : jsonObject.keySet())
      {
         if("name".equals(key) || "type".equals(key))
         {
            continue;
         }

         if(values == null)
         {
            values = new LinkedHashMap<>();
         }

         Object value = jsonObject.get(key);
         if(value instanceof  Serializable s)
         {
            values.put(key, s);
         }
      }
      */
   }



   /*******************************************************************************
    ** Getter for type
    *******************************************************************************/
   public String getType()
   {
      return (this.type);
   }



   /*******************************************************************************
    ** Setter for type
    *******************************************************************************/
   public void setType(String type)
   {
      this.type = type;
   }



   /*******************************************************************************
    ** Fluent setter for type
    *******************************************************************************/
   public AuthenticationMetaDataResponseV1 withType(String type)
   {
      this.type = type;
      return (this);
   }



   /*******************************************************************************
    ** Getter for name
    *******************************************************************************/
   public String getName()
   {
      return (this.name);
   }



   /*******************************************************************************
    ** Setter for name
    *******************************************************************************/
   public void setName(String name)
   {
      this.name = name;
   }



   /*******************************************************************************
    ** Fluent setter for name
    *******************************************************************************/
   public AuthenticationMetaDataResponseV1 withName(String name)
   {
      this.name = name;
      return (this);
   }



   /*******************************************************************************
    ** Getter for values
    *******************************************************************************/
   public Values getValues()
   {
      return (this.values);
   }



   /*******************************************************************************
    ** Setter for values
    *******************************************************************************/
   public void setValues(Values values)
   {
      this.values = values;
   }



   /*******************************************************************************
    ** Fluent setter for values
    *******************************************************************************/
   public AuthenticationMetaDataResponseV1 withValues(Values values)
   {
      this.values = values;
      return (this);
   }

}
