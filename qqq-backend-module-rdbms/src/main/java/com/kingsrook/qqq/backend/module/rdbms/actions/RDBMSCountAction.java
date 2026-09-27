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

package com.kingsrook.qqq.backend.module.rdbms.actions;


import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import com.kingsrook.qqq.backend.core.actions.interfaces.CountInterface;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.ActionTimeoutHelper;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.JoinsContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import org.apache.commons.lang3.BooleanUtils;


/*******************************************************************************
 **
 *******************************************************************************/
public class RDBMSCountAction extends AbstractRDBMSAction implements CountInterface
{
   private static final QLogger LOG = QLogger.getLogger(RDBMSCountAction.class);

   private ActionTimeoutHelper actionTimeoutHelper;



   /*******************************************************************************
    **
    *******************************************************************************/
   public CountOutput execute(CountInput countInput) throws QException
   {
      try
      {
         QTableMetaData table = countInput.getTable();
         setBackendMetaData(countInput.getBackend());

         QQueryFilter                          filter                   = clonedOrNewFilter(countInput.getFilter());
         JoinsContext                          joinsContext             = new JoinsContext(QContext.getQInstance(), countInput, filter);
         JoinsContext.FieldAndTableNameOrAlias fieldAndTableNameOrAlias = joinsContext.getFieldAndTableNameOrAlias(table.getPrimaryKeyField());

         boolean requiresDistinct = doesSelectClauseRequireDistinct(table);
         String  primaryKeyColumn = escapeIdentifier(fieldAndTableNameOrAlias.tableNameOrAlias()) + "." + escapeIdentifier(getColumnName(fieldAndTableNameOrAlias.field()));
         String  clausePrefix     = (requiresDistinct) ? "SELECT COUNT(DISTINCT (" + primaryKeyColumn + "))" : "SELECT COUNT(*)";

         if(BooleanUtils.isTrue(countInput.getIncludeDistinctCount()))
         {
            clausePrefix = "SELECT COUNT(DISTINCT (" + primaryKeyColumn + ")) AS distinct_count, COUNT(*)";
         }

         List<Serializable> params = new ArrayList<>();
         String sql = clausePrefix + " AS record_count "
            + " FROM " + makeFromClause(QContext.getQInstance(), table.getName(), joinsContext, params)
            + " WHERE " + makeWhereClause(joinsContext, filter, params);
         // todo sql customization - can edit sql and/or param list

         setSqlAndJoinsInQueryStat(sql, joinsContext);

         CountOutput rs = new CountOutput();
         long mark = System.currentTimeMillis();

         Connection connection;
         boolean    needToCloseConnection = false;
         if(countInput.getTransaction() != null && countInput.getTransaction() instanceof RDBMSTransaction rdbmsTransaction)
         {
            connection = rdbmsTransaction.getConnection();
         }
         else
         {
            connection = getConnection(countInput);
            needToCloseConnection = true;
         }

         try(PreparedStatement countStatement = connection.prepareStatement(sql))
         {
            statement = countStatement;

            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // set up & start an actionTimeoutHelper (note, internally it'll deal with the time being null or negative as meaning not to timeout) //
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            actionTimeoutHelper = new ActionTimeoutHelper(countInput.getTimeoutSeconds(), TimeUnit.SECONDS, new StatementTimeoutCanceller(statement, sql));
            actionTimeoutHelper.start();

            ///////////////////////////////////////////////////////////////////////////////////////////////////
            // to avoid counting time spent acquiring a connection, re-set the queryStat startTimestamp here //
            ///////////////////////////////////////////////////////////////////////////////////////////////////
            if(queryStat != null)
            {
               queryStat.setStartTimestamp(Instant.now());
            }

            getActionStrategy().executeStatement(statement, sql, ((ResultSet resultSet) ->
            {
               /////////////////////////////////////////////////////////////////////////
               // once we've started getting results, go ahead and cancel the timeout //
               /////////////////////////////////////////////////////////////////////////
               actionTimeoutHelper.cancel();

               if(resultSet.next())
               {
                  rs.setCount(resultSet.getInt("record_count"));

                  if(BooleanUtils.isTrue(countInput.getIncludeDistinctCount()))
                  {
                     rs.setDistinctCount(resultSet.getInt("distinct_count"));
                  }
               }

               setQueryStatFirstResultTime();

            }), params);
         }
         finally
         {
            logSQL(sql, params, mark);

            if(needToCloseConnection)
            {
               connection.close();
            }
         }

         return rs;
      }
      catch(Exception e)
      {
         if(actionTimeoutHelper != null && actionTimeoutHelper.getDidTimeout())
         {
            setQueryStatFirstResultTime();
            throw (new QUserFacingException("Count timed out."));
         }

         if(isCancelled)
         {
            throw (new QUserFacingException("Count was cancelled."));
         }

         LOG.warn("Error executing count", e);
         throw new QException("Error executing count", e);
      }
      finally
      {
         if(actionTimeoutHelper != null)
         {
            /////////////////////////////////////////
            // make sure the timeout got cancelled //
            /////////////////////////////////////////
            actionTimeoutHelper.cancel();
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void cancelAction()
   {
      doCancelQuery();
   }

}
