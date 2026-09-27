/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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
