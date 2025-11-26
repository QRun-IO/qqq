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

package com.kingsrook.qqq.middleware.health.model.metadata;


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QSupplementalInstanceMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.middleware.health.HealthIndicator;


/*******************************************************************************
 ** MetaData for health check configuration.
 **
 ** This class implements QSupplementalInstanceMetaData to integrate with
 ** QInstance without requiring changes to the core QInstance class.
 *******************************************************************************/
public class HealthCheckMetaData implements QSupplementalInstanceMetaData
{
   public static final String METADATA_KEY = "healthCheck";

   private Boolean               enabled      = true;
   private String                endpointPath = "/health";
   private List<HealthIndicator> indicators   = new ArrayList<>();
   private QCodeReference        authenticator;
   private String                requiredSecurityKeyType;
   private Integer               timeoutMs    = 5000;



   /*******************************************************************************
    ** Get the unique key for this supplemental metadata.
    ** Required by QSupplementalInstanceMetaData interface.
    *******************************************************************************/
   @Override
   public String getName()
   {
      return (METADATA_KEY);
   }



   /*******************************************************************************
    ** Add this metadata to the QInstance.
    ** Required by QSupplementalInstanceMetaData interface.
    *******************************************************************************/
   @Override
   public void addSelfToInstance(QInstance qInstance)
   {
      qInstance.withSupplementalMetaData(this);
   }



   /*******************************************************************************
    ** Fluent setter for enabled
    *******************************************************************************/
   public HealthCheckMetaData withEnabled(Boolean enabled)
   {
      this.enabled = enabled;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for endpointPath
    *******************************************************************************/
   public HealthCheckMetaData withEndpointPath(String endpointPath)
   {
      this.endpointPath = endpointPath;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for indicators
    *******************************************************************************/
   public HealthCheckMetaData withIndicators(List<HealthIndicator> indicators)
   {
      this.indicators = indicators;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for authenticator
    *******************************************************************************/
   public HealthCheckMetaData withAuthenticator(QCodeReference authenticator)
   {
      this.authenticator = authenticator;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for requiredSecurityKeyType
    *******************************************************************************/
   public HealthCheckMetaData withRequiredSecurityKeyType(String requiredSecurityKeyType)
   {
      this.requiredSecurityKeyType = requiredSecurityKeyType;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for timeoutMs
    *******************************************************************************/
   public HealthCheckMetaData withTimeoutMs(Integer timeoutMs)
   {
      this.timeoutMs = timeoutMs;
      return (this);
   }



   /*******************************************************************************
    ** Getter for enabled
    *******************************************************************************/
   public Boolean getEnabled()
   {
      return (this.enabled);
   }



   /*******************************************************************************
    ** Setter for enabled
    *******************************************************************************/
   public void setEnabled(Boolean enabled)
   {
      this.enabled = enabled;
   }



   /*******************************************************************************
    ** Getter for endpointPath
    *******************************************************************************/
   public String getEndpointPath()
   {
      return (this.endpointPath);
   }



   /*******************************************************************************
    ** Setter for endpointPath
    *******************************************************************************/
   public void setEndpointPath(String endpointPath)
   {
      this.endpointPath = endpointPath;
   }



   /*******************************************************************************
    ** Getter for indicators
    *******************************************************************************/
   public List<HealthIndicator> getIndicators()
   {
      return (this.indicators);
   }



   /*******************************************************************************
    ** Setter for indicators
    *******************************************************************************/
   public void setIndicators(List<HealthIndicator> indicators)
   {
      this.indicators = indicators;
   }



   /*******************************************************************************
    ** Getter for authenticator
    *******************************************************************************/
   public QCodeReference getAuthenticator()
   {
      return (this.authenticator);
   }



   /*******************************************************************************
    ** Setter for authenticator
    *******************************************************************************/
   public void setAuthenticator(QCodeReference authenticator)
   {
      this.authenticator = authenticator;
   }



   /*******************************************************************************
    ** Getter for requiredSecurityKeyType
    *******************************************************************************/
   public String getRequiredSecurityKeyType()
   {
      return (this.requiredSecurityKeyType);
   }



   /*******************************************************************************
    ** Setter for requiredSecurityKeyType
    *******************************************************************************/
   public void setRequiredSecurityKeyType(String requiredSecurityKeyType)
   {
      this.requiredSecurityKeyType = requiredSecurityKeyType;
   }



   /*******************************************************************************
    ** Getter for timeoutMs
    *******************************************************************************/
   public Integer getTimeoutMs()
   {
      return (this.timeoutMs);
   }



   /*******************************************************************************
    ** Setter for timeoutMs
    *******************************************************************************/
   public void setTimeoutMs(Integer timeoutMs)
   {
      this.timeoutMs = timeoutMs;
   }
}
