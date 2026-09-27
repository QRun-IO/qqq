/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.module.rdbms.model.metadata;


/*******************************************************************************
 ** Settings for a connection pool (if your backend is configured to use one).
 ** Originally based on the most common settings for C3P0 - see
 ** https://www.mchange.com/projects/c3p0/#configuration
 **
 ** If you want more - you'll be looking at defining your own subclass of
 ** C3P0PooledConnectionProvider and possibly this class.
 **
 ** If using a pool other than C3P0 - some of these may apply others may not.
 *******************************************************************************/
public class ConnectionPoolSettings
{
   private Integer initialPoolSize;
   private Integer minPoolSize;
   private Integer maxPoolSize;
   private Integer acquireIncrement;
   private Integer maxConnectionAgeSeconds;
   private Integer maxIdleTimeSeconds;
   private Integer maxIdleTimeExcessConnectionsSeconds;
   private Integer checkoutTimeoutSeconds;
   private Boolean testConnectionOnCheckout;



   /*******************************************************************************
    ** Getter for initialPoolSize
    *******************************************************************************/
   public Integer getInitialPoolSize()
   {
      return (this.initialPoolSize);
   }



   /*******************************************************************************
    ** Setter for initialPoolSize
    *******************************************************************************/
   public void setInitialPoolSize(Integer initialPoolSize)
   {
      this.initialPoolSize = initialPoolSize;
   }



   /*******************************************************************************
    ** Fluent setter for initialPoolSize
    *******************************************************************************/
   public ConnectionPoolSettings withInitialPoolSize(Integer initialPoolSize)
   {
      this.initialPoolSize = initialPoolSize;
      return (this);
   }



   /*******************************************************************************
    ** Getter for minPoolSize
    *******************************************************************************/
   public Integer getMinPoolSize()
   {
      return (this.minPoolSize);
   }



   /*******************************************************************************
    ** Setter for minPoolSize
    *******************************************************************************/
   public void setMinPoolSize(Integer minPoolSize)
   {
      this.minPoolSize = minPoolSize;
   }



   /*******************************************************************************
    ** Fluent setter for minPoolSize
    *******************************************************************************/
   public ConnectionPoolSettings withMinPoolSize(Integer minPoolSize)
   {
      this.minPoolSize = minPoolSize;
      return (this);
   }



   /*******************************************************************************
    ** Getter for maxPoolSize
    *******************************************************************************/
   public Integer getMaxPoolSize()
   {
      return (this.maxPoolSize);
   }



   /*******************************************************************************
    ** Setter for maxPoolSize
    *******************************************************************************/
   public void setMaxPoolSize(Integer maxPoolSize)
   {
      this.maxPoolSize = maxPoolSize;
   }



   /*******************************************************************************
    ** Fluent setter for maxPoolSize
    *******************************************************************************/
   public ConnectionPoolSettings withMaxPoolSize(Integer maxPoolSize)
   {
      this.maxPoolSize = maxPoolSize;
      return (this);
   }



   /*******************************************************************************
    ** Getter for acquireIncrement
    *******************************************************************************/
   public Integer getAcquireIncrement()
   {
      return (this.acquireIncrement);
   }



   /*******************************************************************************
    ** Setter for acquireIncrement
    *******************************************************************************/
   public void setAcquireIncrement(Integer acquireIncrement)
   {
      this.acquireIncrement = acquireIncrement;
   }



   /*******************************************************************************
    ** Fluent setter for acquireIncrement
    *******************************************************************************/
   public ConnectionPoolSettings withAcquireIncrement(Integer acquireIncrement)
   {
      this.acquireIncrement = acquireIncrement;
      return (this);
   }



   /*******************************************************************************
    ** Getter for maxConnectionAgeSeconds
    *******************************************************************************/
   public Integer getMaxConnectionAgeSeconds()
   {
      return (this.maxConnectionAgeSeconds);
   }



   /*******************************************************************************
    ** Setter for maxConnectionAgeSeconds
    *******************************************************************************/
   public void setMaxConnectionAgeSeconds(Integer maxConnectionAgeSeconds)
   {
      this.maxConnectionAgeSeconds = maxConnectionAgeSeconds;
   }



   /*******************************************************************************
    ** Fluent setter for maxConnectionAgeSeconds
    *******************************************************************************/
   public ConnectionPoolSettings withMaxConnectionAgeSeconds(Integer maxConnectionAgeSeconds)
   {
      this.maxConnectionAgeSeconds = maxConnectionAgeSeconds;
      return (this);
   }



   /*******************************************************************************
    ** Getter for maxIdleTimeSeconds
    *******************************************************************************/
   public Integer getMaxIdleTimeSeconds()
   {
      return (this.maxIdleTimeSeconds);
   }



   /*******************************************************************************
    ** Setter for maxIdleTimeSeconds
    *******************************************************************************/
   public void setMaxIdleTimeSeconds(Integer maxIdleTimeSeconds)
   {
      this.maxIdleTimeSeconds = maxIdleTimeSeconds;
   }



   /*******************************************************************************
    ** Fluent setter for maxIdleTimeSeconds
    *******************************************************************************/
   public ConnectionPoolSettings withMaxIdleTimeSeconds(Integer maxIdleTimeSeconds)
   {
      this.maxIdleTimeSeconds = maxIdleTimeSeconds;
      return (this);
   }



   /*******************************************************************************
    ** Getter for maxIdleTimeExcessConnectionsSeconds
    *******************************************************************************/
   public Integer getMaxIdleTimeExcessConnectionsSeconds()
   {
      return (this.maxIdleTimeExcessConnectionsSeconds);
   }



   /*******************************************************************************
    ** Setter for maxIdleTimeExcessConnectionsSeconds
    *******************************************************************************/
   public void setMaxIdleTimeExcessConnectionsSeconds(Integer maxIdleTimeExcessConnectionsSeconds)
   {
      this.maxIdleTimeExcessConnectionsSeconds = maxIdleTimeExcessConnectionsSeconds;
   }



   /*******************************************************************************
    ** Fluent setter for maxIdleTimeExcessConnectionsSeconds
    *******************************************************************************/
   public ConnectionPoolSettings withMaxIdleTimeExcessConnectionsSeconds(Integer maxIdleTimeExcessConnectionsSeconds)
   {
      this.maxIdleTimeExcessConnectionsSeconds = maxIdleTimeExcessConnectionsSeconds;
      return (this);
   }



   /*******************************************************************************
    ** Getter for testConnectionOnCheckout
    *******************************************************************************/
   public Boolean getTestConnectionOnCheckout()
   {
      return (this.testConnectionOnCheckout);
   }



   /*******************************************************************************
    ** Setter for testConnectionOnCheckout
    *******************************************************************************/
   public void setTestConnectionOnCheckout(Boolean testConnectionOnCheckout)
   {
      this.testConnectionOnCheckout = testConnectionOnCheckout;
   }



   /*******************************************************************************
    ** Fluent setter for testConnectionOnCheckout
    *******************************************************************************/
   public ConnectionPoolSettings withTestConnectionOnCheckout(Boolean testConnectionOnCheckout)
   {
      this.testConnectionOnCheckout = testConnectionOnCheckout;
      return (this);
   }



   /*******************************************************************************
    ** Getter for checkoutTimeoutSeconds
    *******************************************************************************/
   public Integer getCheckoutTimeoutSeconds()
   {
      return (this.checkoutTimeoutSeconds);
   }



   /*******************************************************************************
    ** Setter for checkoutTimeoutSeconds
    *******************************************************************************/
   public void setCheckoutTimeoutSeconds(Integer checkoutTimeoutSeconds)
   {
      this.checkoutTimeoutSeconds = checkoutTimeoutSeconds;
   }



   /*******************************************************************************
    ** Fluent setter for checkoutTimeoutSeconds
    *******************************************************************************/
   public ConnectionPoolSettings withCheckoutTimeoutSeconds(Integer checkoutTimeoutSeconds)
   {
      this.checkoutTimeoutSeconds = checkoutTimeoutSeconds;
      return (this);
   }

}
