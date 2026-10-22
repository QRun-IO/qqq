/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.metadata.authentication;


import java.util.Objects;


/*******************************************************************************
 ** Represents a scope for authentication provider registration.
 **
 ** <p>Scopes allow different authentication providers to be registered for
 ** different parts of a QQQ application (instance default, specific APIs,
 ** or route providers). Scopes use value-based equality, comparing the
 ** underlying metadata objects.</p>
 **
 ** <p>Factory methods are provided to create scopes:
 ** <ul>
 **   <li>{@link #instanceDefault()} - Instance-wide default authentication</li>
 **   <li>{@link #api(ApiInstanceMetaData)} - API-specific authentication</li>
 **   <li>{@link #routeProvider(JavalinRouteProviderMetaData)} - Route provider-specific authentication</li>
 ** </ul>
 ** </p>
 *******************************************************************************/
public class AuthScope
{
   /*******************************************************************************
    ** Private constructor - use factory methods to create instances
    *******************************************************************************/
   protected AuthScope()
   {
   }


   /*******************************************************************************
    ** Factory method to create an instance default scope.
    **
    ** @return AuthScope representing the instance-wide default authentication
    *******************************************************************************/
   public static InstanceDefault instanceDefault()
   {
      return new InstanceDefault();
   }


   /*******************************************************************************
    ** Factory method to create an API-specific scope.
    **
    ** <p>Note: The apiMetaData parameter is stored as Object to avoid module
    ** dependencies. It should be an instance of {@code ApiInstanceMetaData}
    ** from the qqq-middleware-api module.</p>
    **
    ** @param apiMetaData The API metadata this scope is associated with (must not be null, should be ApiInstanceMetaData)
    ** @return AuthScope representing API-specific authentication
    *******************************************************************************/
   public static Api api(Object apiMetaData)
   {
      return new Api(apiMetaData);
   }


   /*******************************************************************************
    ** Factory method to create a route provider-specific scope.
    **
    ** <p>Note: The routeMetaData parameter is stored as Object to avoid module
    ** dependencies. It should be an instance of {@code JavalinRouteProviderMetaData}
    ** from the qqq-middleware-javalin module.</p>
    **
    ** @param routeMetaData The route provider metadata this scope is associated with (must not be null, should be JavalinRouteProviderMetaData)
    ** @return AuthScope representing route provider-specific authentication
    *******************************************************************************/
   public static RouteProvider routeProvider(Object routeMetaData)
   {
      return new RouteProvider(routeMetaData);
   }


   /*******************************************************************************
    ** Instance default authentication scope - applies to all requests unless
    ** overridden by a more specific scope.
    *******************************************************************************/
   public static class InstanceDefault extends AuthScope
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      private InstanceDefault()
      {
         super();
      }


      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public boolean equals(Object obj)
      {
         return obj instanceof InstanceDefault;
      }


      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public int hashCode()
      {
         return InstanceDefault.class.hashCode();
      }


      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public String toString()
      {
         return "AuthScope.InstanceDefault";
      }
   }


   /*******************************************************************************
    ** API-specific authentication scope - applies to requests for a specific API.
    *******************************************************************************/
   public static class Api extends AuthScope
   {
      private final Object apiMetaData;


      /*******************************************************************************
       **
       *******************************************************************************/
      private Api(Object apiMetaData)
      {
         super();
         this.apiMetaData = Objects.requireNonNull(apiMetaData, "apiMetaData must not be null");
      }


      /*******************************************************************************
       ** Get the API metadata associated with this scope.
       **
       ** <p>Note: Returns Object to avoid module dependencies. Cast to
       ** {@code ApiInstanceMetaData} when used in code that has access to
       ** the qqq-middleware-api module.</p>
       **
       ** @return The API metadata (never null, should be ApiInstanceMetaData)
       *******************************************************************************/
      public Object getApiMetaData()
      {
         return apiMetaData;
      }


      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public boolean equals(Object obj)
      {
         if(this == obj)
         {
            return true;
         }
         if(obj == null || getClass() != obj.getClass())
         {
            return false;
         }
         Api api = (Api) obj;
         return Objects.equals(apiMetaData, api.apiMetaData);
      }


      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public int hashCode()
      {
         return Objects.hash(apiMetaData);
      }


      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public String toString()
      {
         // Use reflection to get name if available, otherwise use class name
         String apiName = "unknown";
         try
         {
            Object name = apiMetaData.getClass().getMethod("getName").invoke(apiMetaData);
            if(name != null)
            {
               apiName = name.toString();
            }
         }
         catch(Exception e)
         {
            // Ignore - use default
         }
         return "AuthScope.Api{apiName=" + apiName + "}";
      }
   }


   /*******************************************************************************
    ** Route provider-specific authentication scope - applies to requests handled
    ** by a specific route provider (e.g., an SPA).
    *******************************************************************************/
   public static class RouteProvider extends AuthScope
   {
      private final Object routeMetaData;


      /*******************************************************************************
       **
       *******************************************************************************/
      private RouteProvider(Object routeMetaData)
      {
         super();
         this.routeMetaData = Objects.requireNonNull(routeMetaData, "routeMetaData must not be null");
      }


      /*******************************************************************************
       ** Get the route provider metadata associated with this scope.
       **
       ** <p>Note: Returns Object to avoid module dependencies. Cast to
       ** {@code JavalinRouteProviderMetaData} when used in code that has access to
       ** the qqq-middleware-javalin module.</p>
       **
       ** @return The route provider metadata (never null, should be JavalinRouteProviderMetaData)
       *******************************************************************************/
      public Object getRouteMetaData()
      {
         return routeMetaData;
      }


      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public boolean equals(Object obj)
      {
         if(this == obj)
         {
            return true;
         }
         if(obj == null || getClass() != obj.getClass())
         {
            return false;
         }
         RouteProvider that = (RouteProvider) obj;
         return Objects.equals(routeMetaData, that.routeMetaData);
      }


      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public int hashCode()
      {
         return Objects.hash(routeMetaData);
      }


      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public String toString()
      {
         // Use reflection to get name if available, otherwise use class name
         String name = "unknown";
         try
         {
            Object nameObj = routeMetaData.getClass().getMethod("getName").invoke(routeMetaData);
            if(nameObj != null)
            {
               name = nameObj.toString();
            }
         }
         catch(Exception e)
         {
            // Ignore - use default
         }
         return "AuthScope.RouteProvider{name=" + name + "}";
      }
   }
}

