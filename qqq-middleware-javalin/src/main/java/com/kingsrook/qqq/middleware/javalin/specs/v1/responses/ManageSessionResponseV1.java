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


import java.io.Serializable;
import java.util.Map;
import com.kingsrook.qqq.middleware.javalin.executors.io.ManageSessionOutputInterface;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIHasAdditionalProperties;


/*******************************************************************************
 **
 *******************************************************************************/
public class ManageSessionResponseV1 implements ManageSessionOutputInterface, ToSchema
{
   @OpenAPIDescription("Unique identifier of the session. Required to be returned on subsequent requests in the sessionUUID Cookie, to prove authentication. Not included when the cookie is HttpOnly or the session was resumed from that cookie.")
   private String uuid;

   @OpenAPIDescription("Optional object with application-defined values.")
   @OpenAPIHasAdditionalProperties()
   private Map<String, Serializable> values;



   /*******************************************************************************
    ** Getter for uuid
    *******************************************************************************/
   public String getUuid()
   {
      return (this.uuid);
   }



   /*******************************************************************************
    ** Setter for uuid
    *******************************************************************************/
   public void setUuid(String uuid)
   {
      this.uuid = uuid;
   }



   /*******************************************************************************
    ** Fluent setter for uuid
    *******************************************************************************/
   public ManageSessionResponseV1 withUuid(String uuid)
   {
      this.uuid = uuid;
      return (this);
   }



   /*******************************************************************************
    ** Getter for values
    *******************************************************************************/
   public Map<String, Serializable> getValues()
   {
      return (this.values);
   }



   /*******************************************************************************
    ** Setter for values
    *******************************************************************************/
   public void setValues(Map<String, Serializable> values)
   {
      this.values = values;
   }



   /*******************************************************************************
    ** Fluent setter for values
    *******************************************************************************/
   public ManageSessionResponseV1 withValues(Map<String, Serializable> values)
   {
      this.values = values;
      return (this);
   }

}
