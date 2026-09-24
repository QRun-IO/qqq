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

package com.kingsrook.sampleapp;


import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.AbstractQQQApplication;
import com.kingsrook.qqq.middleware.javalin.QApplicationJavalinServer;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import static com.kingsrook.sampleapp.metadata.SampleMetaDataProvider.primeTestDatabase;


/*******************************************************************************
 ** Runs the sample application with a freshly populated sample database.
 *******************************************************************************/
public class SampleJavalinServer extends QApplicationJavalinServer
{
   /*******************************************************************************
    **
    *******************************************************************************/
   public SampleJavalinServer()
   {
      this(new SampleMetaDataProvider());
   }



   /*******************************************************************************
    ** Sample modes share the same owned database and startup lifecycle.
    *******************************************************************************/
   public SampleJavalinServer(AbstractQQQApplication application)
   {
      super(application);
      setPort(Integer.getInteger("qqq.sample.port", 8000));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void main(String[] args) throws QException
   {
      new SampleJavalinServer().start();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void start() throws QException
   {
      try
      {
         primeTestDatabase("prime-test-database.sql");
         if(Boolean.getBoolean("qqq.sample.sharing"))
         {
            primeTestDatabase("prime-sharing-database.sql");
         }
      }
      catch(Exception e)
      {
         throw new QException("Failed to initialize the sample database.", e);
      }

      super.start();
   }
}
