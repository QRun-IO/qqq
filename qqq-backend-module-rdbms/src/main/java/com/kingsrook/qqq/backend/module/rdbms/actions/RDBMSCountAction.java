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

package com.kingsrook.qqq.backend.module.rdbms.actions;


import java.io.Serializable;
import java.sql.Connection;
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
         JoinsContext                          joinsContext             = new JoinsContext(QContext.getQInstance(), countInput.getTableName(), countInput.getQueryJoins(), filter);
         JoinsContext.FieldAndTableNameOrAlias fieldAndTableNameOrAlias = joinsContext.getFieldAndTableNameOrAlias(table.getPrimaryKeyField());

         boolean requiresDistinct = doesSelectClauseRequireDistinct(table);
         String  primaryKeyColumn = escapeIdentifier(fieldAndTableNameOrAlias.tableNameOrAlias()) + "." + escapeIdentifier(fieldAndTableNameOrAlias.field().getName());
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

         try
         {
            statement = connection.prepareStatement(sql);

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
