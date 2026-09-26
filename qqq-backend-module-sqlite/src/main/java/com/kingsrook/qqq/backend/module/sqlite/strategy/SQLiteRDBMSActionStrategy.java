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

package com.kingsrook.qqq.backend.module.sqlite.strategy;


import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.QueryManager;
import com.kingsrook.qqq.backend.module.rdbms.strategy.BaseRDBMSActionStrategy;


/*******************************************************************************
 ** SQLite specialization of the default RDBMS/JDBC action strategy
 *******************************************************************************/
public class SQLiteRDBMSActionStrategy extends BaseRDBMSActionStrategy
{

   /*******************************************************************************
    ** SQLite requires DEFAULT VALUES when no columns are supplied.
    *******************************************************************************/
   @Override
   public String getInsertDefaultValuesClause()
   {
      return "DEFAULT VALUES";
   }



   /***************************************************************************
    ** deal with sqlite not having temporal types... so temporal values
    ** i guess are stored as strings, as that's how they come back to us - so
    ** the JDBC methods fail trying to getDate or whatever from them - but
    ** getting the values as strings, they parse nicely, so do that.
    ***************************************************************************/
   @Override
   public Serializable getFieldValueFromResultSet(QFieldType type, ResultSet resultSet, int i) throws SQLException
   {
      return switch(type)
      {
         case DATE ->
         {
            try
            {
               yield parseString(s -> LocalDate.parse(s), resultSet, i);
            }
            catch(Exception e)
            {
               /////////////////////////////////////////////////////////////////////////////////
               // handle the case of, the value we got back is actually a date-time -- so --  //
               // let's parse it as such, and then map into a LocalDate in the session zoneId //
               /////////////////////////////////////////////////////////////////////////////////
               Instant instant = (Instant) parseString(s -> Instant.parse(s), resultSet, i);
               if(instant == null)
               {
                  yield null;
               }
               ZoneId zoneId = ValueUtils.getSessionOrInstanceZoneId();
               yield instant.atZone(zoneId).toLocalDate();
            }
         }
         case TIME -> parseString(s -> LocalTime.parse(s), resultSet, i);
         case DATE_TIME -> parseString(s -> Instant.parse(s), resultSet, i);
         default -> super.getFieldValueFromResultSet(type, resultSet, i);
      };
   }



   /***************************************************************************
    ** helper method for getFieldValueFromResultSet
    ***************************************************************************/
   private Serializable parseString(Function<String, Serializable> parser, ResultSet resultSet, int i) throws SQLException
   {
      String valueString = QueryManager.getString(resultSet, i);
      if(valueString == null)
      {
         return (null);
      }
      else
      {
         return parser.apply(valueString);
      }
   }



   /***************************************************************************
    * bind temporal types as strings (see above comment re: sqlite temporal types)
    ***************************************************************************/
   @Override
   protected int bindParamObject(PreparedStatement statement, int index, Object value) throws SQLException
   {
      if(value instanceof Instant || value instanceof LocalTime || value instanceof LocalDate)
      {
         bindParam(statement, index, value.toString());
         return 1;
      }
      else
      {
         return super.bindParamObject(statement, index, value);
      }
   }



   /***************************************************************************
    ** per discussion (and rejected PR mentioned) on https://github.com/prrvchr/sqlite-jdbc
    ** sqlite jdbc by default will only return the latest generated serial.  but we can get
    ** them all by appending this "RETURNING id" to the query, and then calling execute()
    ** (instead of executeUpdate()) and getResultSet (instead of getGeneratedKeys())
    ***************************************************************************/
   @Override
   public List<Serializable> executeInsertForGeneratedIds(Connection connection, String sql, List<Object> params, QFieldMetaData primaryKeyField) throws SQLException
   {
      sql = sql + " RETURNING " + getColumnName(primaryKeyField);

      try(PreparedStatement statement = connection.prepareStatement(sql))
      {
         bindParams(params.toArray(), statement);
         incrementStatistic(STAT_QUERIES_RAN);
         statement.execute();

         ResultSet          generatedKeys = statement.getResultSet();
         List<Serializable> rs            = new ArrayList<>();
         while(generatedKeys.next())
         {
            rs.add(getFieldValueFromResultSet(primaryKeyField.getType(), generatedKeys, 1));
         }
         return (rs);
      }
   }

}
