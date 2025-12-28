/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.metadata.authentication;


/*******************************************************************************
 ** Context information for resolving the appropriate authentication provider
 ** for a request.
 **
 ** <p>This context carries information about the request that can be used
 ** to determine which scoped authentication provider should be used. The
 ** resolver checks scopes in order of specificity (API/RouteProvider →
 ** InstanceDefault).</p>
 *******************************************************************************/
public class AuthResolutionContext
{
   private String apiName;
   private Object apiMetaData; // Should be ApiInstanceMetaData from qqq-middleware-api module
   private String routeProviderName;
   private Object routeMetaData; // Should be JavalinRouteProviderMetaData from qqq-middleware-javalin module
   private String requestPath;


   /*******************************************************************************
    ** Default constructor
    *******************************************************************************/
   public AuthResolutionContext()
   {
   }


   /*******************************************************************************
    ** Getter for apiName
    **
    ** @return The API name (if this is an API request)
    *******************************************************************************/
   public String getApiName()
   {
      return apiName;
   }


   /*******************************************************************************
    ** Setter for apiName
    **
    ** @param apiName The API name
    *******************************************************************************/
   public void setApiName(String apiName)
   {
      this.apiName = apiName;
   }


   /*******************************************************************************
    ** Fluent setter for apiName
    **
    ** @param apiName The API name
    ** @return This context for method chaining
    *******************************************************************************/
   public AuthResolutionContext withApiName(String apiName)
   {
      this.apiName = apiName;
      return this;
   }


   /*******************************************************************************
    ** Getter for apiMetaData
    **
    ** <p>Note: Returns Object to avoid module dependencies. Cast to
    ** {@code ApiInstanceMetaData} when used in code that has access to
    ** the qqq-middleware-api module.</p>
    **
    ** @return The API metadata (if this is an API request, should be ApiInstanceMetaData)
    *******************************************************************************/
   public Object getApiMetaData()
   {
      return apiMetaData;
   }


   /*******************************************************************************
    ** Setter for apiMetaData
    **
    ** @param apiMetaData The API metadata (should be ApiInstanceMetaData)
    *******************************************************************************/
   public void setApiMetaData(Object apiMetaData)
   {
      this.apiMetaData = apiMetaData;
   }


   /*******************************************************************************
    ** Fluent setter for apiMetaData
    **
    ** <p>Note: Uses reflection to extract name if available.</p>
    **
    ** @param apiMetaData The API metadata (should be ApiInstanceMetaData)
    ** @return This context for method chaining
    *******************************************************************************/
   public AuthResolutionContext withApiMetaData(Object apiMetaData)
   {
      this.apiMetaData = apiMetaData;
      if(apiMetaData != null)
      {
         try
         {
            Object name = apiMetaData.getClass().getMethod("getName").invoke(apiMetaData);
            if(name != null)
            {
               this.apiName = name.toString();
            }
         }
         catch(Exception e)
         {
            // Ignore - name will remain null or previous value
         }
      }
      return this;
   }


   /*******************************************************************************
    ** Getter for routeProviderName
    **
    ** @return The route provider name (if this is a route provider request)
    *******************************************************************************/
   public String getRouteProviderName()
   {
      return routeProviderName;
   }


   /*******************************************************************************
    ** Setter for routeProviderName
    **
    ** @param routeProviderName The route provider name
    *******************************************************************************/
   public void setRouteProviderName(String routeProviderName)
   {
      this.routeProviderName = routeProviderName;
   }


   /*******************************************************************************
    ** Fluent setter for routeProviderName
    **
    ** @param routeProviderName The route provider name
    ** @return This context for method chaining
    *******************************************************************************/
   public AuthResolutionContext withRouteProviderName(String routeProviderName)
   {
      this.routeProviderName = routeProviderName;
      return this;
   }


   /*******************************************************************************
    ** Getter for routeMetaData
    **
    ** <p>Note: Returns Object to avoid module dependencies. Cast to
    ** {@code JavalinRouteProviderMetaData} when used in code that has access to
    ** the qqq-middleware-javalin module.</p>
    **
    ** @return The route provider metadata (if this is a route provider request, should be JavalinRouteProviderMetaData)
    *******************************************************************************/
   public Object getRouteMetaData()
   {
      return routeMetaData;
   }


   /*******************************************************************************
    ** Setter for routeMetaData
    **
    ** @param routeMetaData The route provider metadata (should be JavalinRouteProviderMetaData)
    *******************************************************************************/
   public void setRouteMetaData(Object routeMetaData)
   {
      this.routeMetaData = routeMetaData;
   }


   /*******************************************************************************
    ** Fluent setter for routeMetaData
    **
    ** <p>Note: Uses reflection to extract name if available.</p>
    **
    ** @param routeMetaData The route provider metadata (should be JavalinRouteProviderMetaData)
    ** @return This context for method chaining
    *******************************************************************************/
   public AuthResolutionContext withRouteMetaData(Object routeMetaData)
   {
      this.routeMetaData = routeMetaData;
      if(routeMetaData != null)
      {
         try
         {
            Object name = routeMetaData.getClass().getMethod("getName").invoke(routeMetaData);
            if(name != null)
            {
               this.routeProviderName = name.toString();
            }
         }
         catch(Exception e)
         {
            // Ignore - name will remain null or previous value
         }
      }
      return this;
   }


   /*******************************************************************************
    ** Getter for requestPath
    **
    ** @return The request path (for diagnostics/logging)
    *******************************************************************************/
   public String getRequestPath()
   {
      return requestPath;
   }


   /*******************************************************************************
    ** Setter for requestPath
    **
    ** @param requestPath The request path
    *******************************************************************************/
   public void setRequestPath(String requestPath)
   {
      this.requestPath = requestPath;
   }


   /*******************************************************************************
    ** Fluent setter for requestPath
    **
    ** @param requestPath The request path
    ** @return This context for method chaining
    *******************************************************************************/
   public AuthResolutionContext withRequestPath(String requestPath)
   {
      this.requestPath = requestPath;
      return this;
   }
}

