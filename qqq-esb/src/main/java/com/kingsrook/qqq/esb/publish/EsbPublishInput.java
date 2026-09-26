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

package com.kingsrook.qqq.esb.publish;


import java.io.Serializable;
import java.util.Map;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;


/*******************************************************************************
 * Input for EsbPublishAction: the destination (its QQQ name) to publish one
 * event to, the event's type (required, e.g., com.example.order.shipped), and
 * its data (optional; none means empty).
 *
 * sourcePath (optional) is what follows the instance name in the event's
 * source - e.g., process/syncOrder gives qqq://instanceName/process/syncOrder.
 * Without it, the source is the application itself (qqq://instanceName/).
 *******************************************************************************/
public class EsbPublishInput extends AbstractActionInput
{
   private String                    destinationName;
   private String                    type;
   private String                    sourcePath;
   private Map<String, Serializable> data;



   /*******************************************************************************
    ** Getter for destinationName
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
   public EsbPublishInput withDestinationName(String destinationName)
   {
      this.destinationName = destinationName;
      return (this);
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
   public EsbPublishInput withType(String type)
   {
      this.type = type;
      return (this);
   }



   /*******************************************************************************
    ** Getter for sourcePath
    *******************************************************************************/
   public String getSourcePath()
   {
      return (this.sourcePath);
   }



   /*******************************************************************************
    ** Setter for sourcePath
    *******************************************************************************/
   public void setSourcePath(String sourcePath)
   {
      this.sourcePath = sourcePath;
   }



   /*******************************************************************************
    ** Fluent setter for sourcePath
    *******************************************************************************/
   public EsbPublishInput withSourcePath(String sourcePath)
   {
      this.sourcePath = sourcePath;
      return (this);
   }



   /*******************************************************************************
    ** Getter for data
    *******************************************************************************/
   public Map<String, Serializable> getData()
   {
      return (this.data);
   }



   /*******************************************************************************
    ** Setter for data
    *******************************************************************************/
   public void setData(Map<String, Serializable> data)
   {
      this.data = data;
   }



   /*******************************************************************************
    ** Fluent setter for data
    *******************************************************************************/
   public EsbPublishInput withData(Map<String, Serializable> data)
   {
      this.data = data;
      return (this);
   }

}
