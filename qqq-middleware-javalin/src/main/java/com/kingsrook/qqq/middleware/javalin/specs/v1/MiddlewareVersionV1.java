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

package com.kingsrook.qqq.middleware.javalin.specs.v1;


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractMiddlewareVersion;


/*******************************************************************************
 **
 *******************************************************************************/
public class MiddlewareVersionV1 extends AbstractMiddlewareVersion
{
   private static List<AbstractEndpointSpec<?, ?, ?>> list = new ArrayList<>();

   static
   {
      list.add(new AuthenticationMetaDataSpecV1());
      list.add(new ManageSessionSpecV1());
      list.add(new LogoutSpecV1());

      list.add(new MetaDataSpecV1());

      list.add(new TableMetaDataSpecV1());
      list.add(new TableQuerySpecV1());
      list.add(new TableCountSpecV1());

      list.add(new ProcessMetaDataSpecV1());
      list.add(new ProcessInitSpecV1());
      list.add(new ProcessStepSpecV1());
      list.add(new ProcessStatusSpecV1());
   }

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public String getVersion()
   {
      return "v1";
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public List<AbstractEndpointSpec<?, ?, ?>> getEndpointSpecs()
   {
      return (list);
   }

}
