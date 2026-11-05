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


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Set;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.module.rdbms.RDBMSBackendModule;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSTableBackendDetails;


/*******************************************************************************
 ** Qualify atomic native writes against the actual supplied connection. This
 ** proves transactional participation, not crash durability; durable Jobs adds
 ** synchronous-storage and schema qualification before accepting an enlistment.
 *******************************************************************************/
final class RDBMSAtomicWrites
{
   /*******************************************************************************
    ** Static connection qualification only.
    *******************************************************************************/
   private RDBMSAtomicWrites() {}



   /*******************************************************************************
    ** Require the configured native database and transactional table. No pooled
    ** connection settings are changed and no independent connection is opened.
    *******************************************************************************/
   static String require(Connection connection, QTableMetaData table) throws QException
   {
      try
      {
         QTableMetaData canonical = table == null ? null : QContext.getQInstance().getTable(table.getName());
         if(canonical == null || !Objects.equals(canonical.getBackendName(), table.getBackendName())
            || !Objects.equals(physicalTable(canonical), physicalTable(table))
            || !(QContext.getQInstance().getBackendForTable(table.getName()) instanceof RDBMSBackendMetaData backend)
            || !isNativeBackend(backend) || connection.isClosed() || connection.isReadOnly() || connection.getAutoCommit())
         {
            throw (new QException("Required record writes need a live native writable transaction"));
         }
         String actual = connection.getMetaData().getURL();
         String expected = ConnectionManager.getJdbcUrl(backend);
         String vendor = backend.getVendor();
         if("h2".equals(vendor))
         {
            if(!"H2".equals(connection.getMetaData().getDatabaseProductName()) || expected == null || !expected.split(";", 2)[0].equals(actual))
            {
               throw (new QException("Required record transaction has the wrong H2 database identity"));
            }
            try(PreparedStatement statement = connection.prepareStatement("SELECT TABLE_TYPE, STORAGE_TYPE FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA=? AND TABLE_NAME=?"))
            {
               statement.setQueryTimeout(5);
               statement.setString(1, connection.getSchema());
               statement.setString(2, physicalTable(table));
               try(ResultSet rows = statement.executeQuery())
               {
                  if(!rows.next() || !"BASE TABLE".equals(rows.getString(1)) || !Set.of("CACHED", "MEMORY").contains(rows.getString(2)) || rows.next())
                  {
                     throw (new QException("Required record writes need a native H2 base table, not a view, link, or temporary table"));
                  }
               }
            }
         }
         else if("mysql".equals(vendor))
         {
            if(!"MySQL".equals(connection.getMetaData().getDatabaseProductName()) || connection.getMetaData().getDatabaseMajorVersion() != 8
               || connection.getMetaData().getDatabaseMinorVersion() != 4 || expected == null || actual == null
               || !expected.split("\\?", 2)[0].equals(actual.split("\\?", 2)[0]) || !Objects.equals(backend.getDatabaseName(), connection.getCatalog()))
            {
               throw (new QException("Required record transaction has the wrong MySQL8.4 database identity"));
            }
            try(PreparedStatement statement = connection.prepareStatement("SELECT ENGINE FROM information_schema.TABLES WHERE TABLE_SCHEMA=? AND TABLE_NAME=?"))
            {
               statement.setQueryTimeout(5);
               statement.setString(1, connection.getCatalog());
               statement.setString(2, physicalTable(table));
               try(ResultSet rows = statement.executeQuery())
               {
                  if(!rows.next() || !"InnoDB".equals(rows.getString(1)) || rows.next())
                  {
                     throw (new QException("Required record writes need an InnoDB table"));
                  }
               }
            }
            try(PreparedStatement statement = connection.prepareStatement("SELECT @@SESSION.transaction_read_only"))
            {
               statement.setQueryTimeout(5);
               try(ResultSet rows = statement.executeQuery())
               {
                  if(!rows.next() || rows.getBoolean(1))
                  {
                     throw (new QException("Required record writes need a writable MySQL session"));
                  }
               }
            }
         }
         else if(isNativePostgres(backend))
         {
            requirePostgres(connection, backend, table, expected, actual);
         }
         else
         {
            throw (new QException("Required record writes are only qualified for native H2, MySQL8.4 and PostgreSQL17"));
         }
         return (backend.getName());
      }
      catch(QException error)
      {
         throw (error);
      }
      catch(SQLException error)
      {
         throw (new QException("Could not qualify required native transaction", error));
      }
   }



   /*******************************************************************************
    ** The native QQQ postgres module: vendor AND backend type are both "postgres".
    ** A "postgres" vendor label on the generic rdbms backend type is not native
    ** PostgreSQL and is never admitted.
    *******************************************************************************/
   static boolean isNativePostgres(RDBMSBackendMetaData backend)
   {
      return ("postgres".equals(backend.getVendor()) && "postgres".equals(backend.getBackendType()));
   }



   /*******************************************************************************
    ** A backend whose transaction class this module owns: the generic rdbms type
    ** (H2/MySQL) or the native postgres type, never a mismatched pair.
    *******************************************************************************/
   private static boolean isNativeBackend(RDBMSBackendMetaData backend)
   {
      return (isNativePostgres(backend) || (RDBMSBackendModule.NAME.equals(backend.getBackendType()) && !"postgres".equals(backend.getVendor())));
   }



   /*******************************************************************************
    ** PostgreSQL 17 on this exact connection: the same server URL, database and
    ** schema as the configured backend, a writable primary, a least-privilege
    ** (DML-only) role, and a table qualified as below.
    *******************************************************************************/
   static void requirePostgres(Connection connection, RDBMSBackendMetaData backend, QTableMetaData table, String expected, String actual) throws SQLException, QException
   {
      if(!"PostgreSQL".equals(connection.getMetaData().getDatabaseProductName()) || connection.getMetaData().getDatabaseMajorVersion() != 17
         || expected == null || actual == null || !expected.startsWith("jdbc:postgresql://")
         || !expected.split("\\?", 2)[0].equals(actual.split("\\?", 2)[0]) || backend.getDatabaseName() == null
         || !backend.getDatabaseName().equals(connection.getCatalog()))
      {
         throw (new QException("Required record transaction has the wrong PostgreSQL17 database identity"));
      }
      try(PreparedStatement statement = connection.prepareStatement("SELECT current_database(), current_schema(), session_user = current_user, current_setting('transaction_read_only'),"
         + " pg_is_in_recovery(), r.rolsuper OR r.rolcreaterole OR r.rolcreatedb OR r.rolbypassrls OR r.rolreplication FROM pg_roles r WHERE r.rolname = current_user"))
      {
         statement.setQueryTimeout(5);
         try(ResultSet rows = statement.executeQuery())
         {
            if(!rows.next() || !backend.getDatabaseName().equals(rows.getString(1)) || rows.getString(2) == null || !rows.getString(2).equals(connection.getSchema())
               || !rows.getBoolean(3))
            {
               throw (new QException("Required record transaction has the wrong PostgreSQL17 database identity"));
            }
            if(!"off".equals(rows.getString(4)) || rows.getBoolean(5))
            {
               throw (new QException("Required record writes need a writable PostgreSQL primary session"));
            }
            if(rows.getBoolean(6) || rows.next())
            {
               throw (new QException("Required record writes need a data-manipulation-only PostgreSQL role"));
            }
         }
      }
      requireNoForbiddenRoleAuthority(connection, physicalTable(table));
      requirePostgresTable(connection, physicalTable(table));
   }



   /*******************************************************************************
    ** Server-level roles whose membership (with inheritable, SET or ADMIN authority)
    ** would give a write-all, server-file, server-program, signalling, maintenance or
    ** database-ownership power. pg_read_all_stats / pg_monitor / pg_read_all_settings
    ** and pg_read_all_data are deliberately NOT listed: they carry no write power.
    *******************************************************************************/
   private static final String[] FORBIDDEN_PREDEFINED_ROLES = { "pg_database_owner", "pg_write_all_data", "pg_read_server_files", "pg_write_server_files",
      "pg_execute_server_program", "pg_signal_backend", "pg_maintain", "pg_checkpoint", "pg_create_subscription" };

   /*******************************************************************************
    ** Real authority over a role (PostgreSQL 17 pg_has_role semantics), for BOTH the
    ** session login and the current effective role: USAGE = the role's privileges are
    ** inherited (INHERIT), SET = can SET ROLE to it, MEMBER WITH ADMIN OPTION = may
    ** grant/revoke it (regrant). Raw MEMBER alone (INHERIT FALSE, SET FALSE, no ADMIN)
    ** confers none of these and is not authority. Special attributes (SUPERUSER,
    ** CREATEROLE, ...) are never inherited, but SET reaches them, so SET counts.
    *******************************************************************************/
   private static final String AUTHORITY_OVER = "pg_has_role(current_user, @ROLE@, 'USAGE') OR pg_has_role(current_user, @ROLE@, 'SET') OR pg_has_role(current_user, @ROLE@, 'MEMBER WITH ADMIN OPTION')"
      + " OR pg_has_role(session_user, @ROLE@, 'USAGE') OR pg_has_role(session_user, @ROLE@, 'SET') OR pg_has_role(session_user, @ROLE@, 'MEMBER WITH ADMIN OPTION')";



   /*******************************************************************************
    ** The login must have NO authority (inherit, SET ROLE or ADMIN/regrant, direct or
    ** through any membership chain) over a role that could mutate beyond DML: a role
    ** with an elevated attribute, one of the server-level write/file/program roles,
    ** the owner of this database, of this schema, or of the table being qualified.
    ** Genuine narrow DML membership and read-only monitoring membership stay allowed.
    *******************************************************************************/
   static void requireNoForbiddenRoleAuthority(Connection connection, String tableName) throws SQLException, QException
   {
      String sql = "SELECT r.rolname FROM pg_roles r WHERE (r.rolsuper OR r.rolcreaterole OR r.rolcreatedb OR r.rolbypassrls OR r.rolreplication"
         + " OR r.rolname = ANY(?)"
         + " OR r.oid = (SELECT datdba FROM pg_database WHERE datname = current_database())"
         + " OR r.oid = (SELECT nspowner FROM pg_namespace WHERE nspname = current_schema())"
         + " OR r.oid = (SELECT c.relowner FROM pg_class c WHERE c.oid = to_regclass(quote_ident(?))))"
         + " AND (" + AUTHORITY_OVER.replace("@ROLE@", "r.oid") + ") LIMIT 1";
      try(PreparedStatement statement = connection.prepareStatement(sql))
      {
         statement.setQueryTimeout(5);
         statement.setArray(1, connection.createArrayOf("text", FORBIDDEN_PREDEFINED_ROLES));
         statement.setString(2, tableName);
         try(ResultSet rows = statement.executeQuery())
         {
            if(rows.next())
            {
               throw (new QException("Required record writes need a data-manipulation-only PostgreSQL role without owner, privileged or server-level role authority"));
            }
         }
      }
   }



   /*******************************************************************************
    ** Resolve one name the way the next unqualified statement will (so a temporary
    ** table can never shadow a qualified one) and require a permanent, ordinary,
    ** non-partition table in the current schema that the session role does not own.
    *******************************************************************************/
   static void requirePostgresTable(Connection connection, String name) throws SQLException, QException
   {
      try(PreparedStatement statement = connection.prepareStatement("SELECT n.nspname = current_schema(), c.relkind, c.relpersistence, c.relispartition,"
         + " (" + AUTHORITY_OVER.replace("@ROLE@", "c.relowner") + ") FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace"
         + " WHERE c.oid = to_regclass(quote_ident(?))"))
      {
         statement.setQueryTimeout(5);
         statement.setString(1, name);
         try(ResultSet rows = statement.executeQuery())
         {
            if(!rows.next() || !rows.getBoolean(1) || !"r".equals(rows.getString(2)) || !"p".equals(rows.getString(3)) || rows.getBoolean(4))
            {
               throw (new QException("Required record writes need a permanent ordinary PostgreSQL table, not a view, foreign, partitioned, temporary or unlogged table"));
            }
            if(rows.getBoolean(5) || rows.next())
            {
               throw (new QException("Required record writes need a PostgreSQL table that the application role neither owns nor can control through role membership"));
            }
         }
      }
   }



   /*******************************************************************************
    ** Resolve native physical table names without parsing caller SQL.
    *******************************************************************************/
   static String physicalTable(QTableMetaData table) throws QException
   {
      if(table.getBackendDetails() == null)
      {
         return (table.getName());
      }
      if(!(table.getBackendDetails() instanceof RDBMSTableBackendDetails details))
      {
         throw (new QException("Required record writes need native RDBMS table details"));
      }
      return (Objects.requireNonNullElse(details.getTableName(), table.getName()));
   }
}
