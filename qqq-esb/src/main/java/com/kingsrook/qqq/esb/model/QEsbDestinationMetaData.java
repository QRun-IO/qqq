/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.esb.model;


import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.TopLevelMetaDataInterface;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 * Meta-data for an ESB destination - a queue or topic on a provider.
 *
 * name is how QQQ meta-data (publications, triggers) refers to the destination;
 * destinationName is its name on the broker, which defaults to name.  Triggers
 * build their default subscription and dead-letter names from the broker-side
 * name (getEffectiveDestinationName).
 *
 * A top-level meta-data object: adding it to an instance puts it in that
 * instance's EsbInstanceMetaData.
 *******************************************************************************/
public class QEsbDestinationMetaData implements TopLevelMetaDataInterface
{
   private static final long serialVersionUID = 1L;

   private String             name;
   private EsbDestinationType type;
   private String             providerName;
   private String             destinationName;



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void addSelfToInstance(QInstance qInstance)
   {
      EsbInstanceMetaData.of(qInstance).withDestination(this);
   }



   /*******************************************************************************
    ** The broker-side name of this destination: destinationName if set, else name.
    *******************************************************************************/
   public String getEffectiveDestinationName()
   {
      return (StringUtils.hasContent(destinationName) ? destinationName : name);
   }



   /*******************************************************************************
    ** Getter for name
    *******************************************************************************/
   @Override
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
   public QEsbDestinationMetaData withName(String name)
   {
      this.name = name;
      return (this);
   }



   /*******************************************************************************
    ** Getter for type
    *******************************************************************************/
   public EsbDestinationType getType()
   {
      return (this.type);
   }



   /*******************************************************************************
    ** Setter for type
    *******************************************************************************/
   public void setType(EsbDestinationType type)
   {
      this.type = type;
   }



   /*******************************************************************************
    ** Fluent setter for type
    *******************************************************************************/
   public QEsbDestinationMetaData withType(EsbDestinationType type)
   {
      this.type = type;
      return (this);
   }



   /*******************************************************************************
    ** Getter for providerName
    *******************************************************************************/
   public String getProviderName()
   {
      return (this.providerName);
   }



   /*******************************************************************************
    ** Setter for providerName
    *******************************************************************************/
   public void setProviderName(String providerName)
   {
      this.providerName = providerName;
   }



   /*******************************************************************************
    ** Fluent setter for providerName
    *******************************************************************************/
   public QEsbDestinationMetaData withProviderName(String providerName)
   {
      this.providerName = providerName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for destinationName (the broker-side name, if different from name).
    ** See also getEffectiveDestinationName.
    *******************************************************************************/
   public String getDestinationName()
   {
      return (this.destinationName);
   }



   /*******************************************************************************
    ** Setter for destinationName
    *******************************************************************************/
   public void setDestinationName(String destinationName)
   {
      this.destinationName = destinationName;
   }



   /*******************************************************************************
    ** Fluent setter for destinationName
    *******************************************************************************/
   public QEsbDestinationMetaData withDestinationName(String destinationName)
   {
      this.destinationName = destinationName;
      return (this);
   }

}
