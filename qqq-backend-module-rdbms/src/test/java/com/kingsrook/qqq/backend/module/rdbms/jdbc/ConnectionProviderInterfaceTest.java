/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.module.rdbms.jdbc;


import java.sql.Connection;
import java.sql.SQLException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.module.rdbms.BaseTest;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import org.junit.jupiter.api.Test;


/*******************************************************************************
 ** Unit test for ConnectionProviderInterface 
 *******************************************************************************/
class ConnectionProviderInterfaceTest extends BaseTest
{


   /***************************************************************************
    * jacoco doesn't like that our interface isn't covered, so... cover it
    ***************************************************************************/
   @Test
   void dumpDebug() throws SQLException
   {
      new ConnectionProviderInterface()
      {
         @Override
         public void init(RDBMSBackendMetaData backend) throws QException
         {

         }



         @Override
         public Connection getConnection() throws SQLException
         {
            return null;
         }
      }.dumpDebug();
   }
}