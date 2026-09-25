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

package com.kingsrook.qqq.middleware.javalin.executors.io;


/*******************************************************************************
 **
 *******************************************************************************/
public class ManageSessionInput extends AbstractMiddlewareInput
{
   private String accessToken;
   private String basicAuthString;



   /*******************************************************************************
    ** Getter for accessToken
    *******************************************************************************/
   public String getAccessToken()
   {
      return (this.accessToken);
   }



   /*******************************************************************************
    ** Setter for accessToken
    *******************************************************************************/
   public void setAccessToken(String accessToken)
   {
      this.accessToken = accessToken;
   }



   /*******************************************************************************
    ** Fluent setter for accessToken
    *******************************************************************************/
   public ManageSessionInput withAccessToken(String accessToken)
   {
      this.accessToken = accessToken;
      return (this);
   }



   /*******************************************************************************
    ** Getter for basicAuthString - the base64 user-id:password of an
    ** `Authorization: Basic` header (TABLE_BASED authentication)
    *******************************************************************************/
   public String getBasicAuthString()
   {
      return (this.basicAuthString);
   }



   /*******************************************************************************
    ** Setter for basicAuthString
    *******************************************************************************/
   public void setBasicAuthString(String basicAuthString)
   {
      this.basicAuthString = basicAuthString;
   }



   /*******************************************************************************
    ** Fluent setter for basicAuthString
    *******************************************************************************/
   public ManageSessionInput withBasicAuthString(String basicAuthString)
   {
      this.basicAuthString = basicAuthString;
      return (this);
   }

}
