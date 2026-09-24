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


import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.middleware.picocli.QPicoCliImplementation;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;


/*******************************************************************************
 **
 *******************************************************************************/
public class SampleCli
{
   /*******************************************************************************
    **
    *******************************************************************************/
   public static void main(String[] args)
   {
      int exitCode = new SampleCli().run(args);
      System.exit(exitCode);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   int run(String[] args)
   {
      try
      {
         SampleMetaDataProvider.primeTestDatabase("prime-test-database.sql");
         QInstance qInstance = new SampleMetaDataProvider().defineQInstance();
         return (run(qInstance, args));
      }
      catch(Exception e)
      {
         e.printStackTrace();
         return (-1);
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   int run(QInstance qInstance, String[] args)
   {
      try
      {
         QPicoCliImplementation qPicoCliImplementation = new QPicoCliImplementation(qInstance);

         return (qPicoCliImplementation.runCli("my-sample-cli", args));
      }
      catch(Exception e)
      {
         e.printStackTrace();
         return (-1);
      }
   }

}
