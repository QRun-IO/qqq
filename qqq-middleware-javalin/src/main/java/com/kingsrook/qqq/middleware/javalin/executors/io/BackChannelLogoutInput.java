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

package com.kingsrook.qqq.middleware.javalin.executors.io;


/*******************************************************************************
 ** Input for the OIDC back-channel logout endpoint.
 **
 ** Per OIDC Back-Channel Logout spec, the IdP sends a POST request with
 ** Content-Type: application/x-www-form-urlencoded containing a logout_token.
 *******************************************************************************/
public class BackChannelLogoutInput extends AbstractMiddlewareInput
{
   private String logoutToken;



   /*******************************************************************************
    ** Getter for logoutToken
    *******************************************************************************/
   public String getLogoutToken()
   {
      return (this.logoutToken);
   }



   /*******************************************************************************
    ** Setter for logoutToken
    *******************************************************************************/
   public void setLogoutToken(String logoutToken)
   {
      this.logoutToken = logoutToken;
   }



   /*******************************************************************************
    ** Fluent setter for logoutToken
    *******************************************************************************/
   public BackChannelLogoutInput withLogoutToken(String logoutToken)
   {
      this.logoutToken = logoutToken;
      return (this);
   }

}
