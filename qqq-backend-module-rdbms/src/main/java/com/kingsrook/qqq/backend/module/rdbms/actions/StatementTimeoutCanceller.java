/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.module.rdbms.actions;


import java.sql.Statement;
import com.kingsrook.qqq.backend.core.exceptions.QRuntimeException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Helper to cancel statements that timeout.
 *******************************************************************************/
public class StatementTimeoutCanceller implements Runnable
{
   private static final QLogger LOG = QLogger.getLogger(StatementTimeoutCanceller.class);

   private final Statement statement;
   private final String    sql;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public StatementTimeoutCanceller(Statement statement, CharSequence sql)
   {
      this.statement = statement;
      this.sql = sql.toString();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void run()
   {
      try
      {
         statement.cancel();
         LOG.info("Cancelled timed out statement", logPair("sql", sql));
      }
      catch(Exception e)
      {
         LOG.warn("Error trying to cancel statement after timeout", e, logPair("sql", sql));
      }

      throw (new QRuntimeException("Statement timed out and was cancelled."));
   }
}
