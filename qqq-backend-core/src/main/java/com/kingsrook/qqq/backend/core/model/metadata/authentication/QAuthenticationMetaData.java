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

package com.kingsrook.qqq.backend.core.model.metadata.authentication;


import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonFilter;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.TopLevelMetaDataInterface;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;


/*******************************************************************************
 ** Meta-data to provide details of an authentication provider (e.g., google, saml,
 ** etc) within a qqq instance
 **
 *******************************************************************************/
public class QAuthenticationMetaData implements TopLevelMetaDataInterface
{
   private String              name;
   private QAuthenticationType type;

   @JsonFilter("secretsFilter")
   private Map<String, String> values;

   private QCodeReference customizer;

   private Boolean sessionStoreEnabled = false;



   /*******************************************************************************
    **
    *******************************************************************************/
   public String getValue(String key)
   {
      if(values == null)
      {
         return null;
      }
      return values.get(key);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void setValue(String key, String value)
   {
      if(values == null)
      {
         values = new HashMap<>();
      }
      values.put(key, value);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QAuthenticationMetaData withValue(String key, String value)
   {
      if(values == null)
      {
         values = new HashMap<>();
      }
      values.put(key, value);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public String getName()
   {
      return name;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void setName(String name)
   {
      this.name = name;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QAuthenticationMetaData withName(String name)
   {
      this.name = name;
      return (this);
   }



   /*******************************************************************************
    ** Getter for type
    **
    *******************************************************************************/
   public QAuthenticationType getType()
   {
      return type;
   }



   /*******************************************************************************
    ** Setter for type
    **
    *******************************************************************************/
   public void setType(QAuthenticationType type)
   {
      this.type = type;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QAuthenticationMetaData withType(QAuthenticationType type)
   {
      this.type = type;
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public Map<String, String> getValues()
   {
      return values;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void setValues(Map<String, String> values)
   {
      this.values = values;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QAuthenticationMetaData withValues(Map<String, String> values)
   {
      this.values = values;
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void addSelfToInstance(QInstance qInstance)
   {
      qInstance.setAuthentication(this);
   }



   /*******************************************************************************
    ** Getter for customizer
    *******************************************************************************/
   public QCodeReference getCustomizer()
   {
      return (this.customizer);
   }



   /*******************************************************************************
    ** Setter for customizer
    *******************************************************************************/
   public void setCustomizer(QCodeReference customizer)
   {
      this.customizer = customizer;
   }



   /*******************************************************************************
    ** Fluent setter for customizer
    *******************************************************************************/
   public QAuthenticationMetaData withCustomizer(QCodeReference customizer)
   {
      this.customizer = customizer;
      return (this);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public void validate(QInstance qInstance, QInstanceValidator qInstanceValidator)
   {
      //////////////////
      // noop at base //
      //////////////////
   }



   /*******************************************************************************
    ** Getter for sessionStoreEnabled
    *******************************************************************************/
   public Boolean getSessionStoreEnabled()
   {
      return (this.sessionStoreEnabled);
   }



   /*******************************************************************************
    ** Setter for sessionStoreEnabled
    *******************************************************************************/
   public void setSessionStoreEnabled(Boolean sessionStoreEnabled)
   {
      this.sessionStoreEnabled = sessionStoreEnabled;
   }



   /*******************************************************************************
    ** Fluent setter for sessionStoreEnabled
    **
    ** @param sessionStoreEnabled whether to use the QSessionStore QBit for caching sessions
    *******************************************************************************/
   public QAuthenticationMetaData withSessionStoreEnabled(Boolean sessionStoreEnabled)
   {
      this.sessionStoreEnabled = sessionStoreEnabled;
      return (this);
   }

}
