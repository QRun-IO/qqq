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

package com.kingsrook.qqq.middleware.picocli;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import picocli.CommandLine;


/*******************************************************************************
 ** Helper class for QPicCliImplementation to build the Command
 **
 *******************************************************************************/
public class QCommandBuilder
{
   private final QInstance qInstance;



   /*******************************************************************************
    ** Constructor.
    **
    *******************************************************************************/
   public QCommandBuilder(QInstance qInstance)
   {
      this.qInstance = qInstance;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   CommandLine.Model.CommandSpec buildCommandSpec(String topCommandName)
   {
      //////////////////////////////////
      // define the top-level command //
      //////////////////////////////////
      CommandLine.Model.CommandSpec topCommandSpec = CommandLine.Model.CommandSpec.create();
      topCommandSpec.name(topCommandName);
      topCommandSpec.version(topCommandName + " v1.0"); // todo... uh?
      topCommandSpec.mixinStandardHelpOptions(true); // usageHelp and versionHelp options
      topCommandSpec.addOption(CommandLine.Model.OptionSpec.builder("-m", "--meta-data")
         .type(boolean.class)
         .description("Output the meta-data for this CLI")
         .build());

      /////////////////////////////////////
      // add each table as a sub-command //
      /////////////////////////////////////
      qInstance.getTables().keySet().stream().sorted().forEach(tableName ->
      {
         QTableMetaData table = qInstance.getTable(tableName);

         CommandLine.Model.CommandSpec tableCommand = CommandLine.Model.CommandSpec.create();
         topCommandSpec.addSubcommand(table.getName(), tableCommand);

         ///////////////////////////////////////////////////
         // add table-specific sub-commands for the table //
         ///////////////////////////////////////////////////
         tableCommand.addSubcommand("meta-data", defineMetaDataCommand(table));
         tableCommand.addSubcommand("count", defineCountCommand(table));
         tableCommand.addSubcommand("get", defineGetCommand(table));
         tableCommand.addSubcommand("query", defineQueryCommand(table));
         tableCommand.addSubcommand("insert", defineInsertCommand(table));
         tableCommand.addSubcommand("update", defineUpdateCommand(table));
         tableCommand.addSubcommand("delete", defineDeleteCommand(table));
         tableCommand.addSubcommand("export", defineExportCommand(table));

         List<QProcessMetaData> processes = qInstance.getProcessesForTable(tableName);
         if(CollectionUtils.nullSafeHasContents(processes))
         {
            tableCommand.addSubcommand("process", defineProcessesCommand(processes));
         }
      });

      ///////////////////////////////////////////////////////////////////////////
      // add all orphan processes (e.g., ones without tables) to the top-level //
      ///////////////////////////////////////////////////////////////////////////
      List<QProcessMetaData> orphanProcesses = new ArrayList<>();
      for(QProcessMetaData process : qInstance.getProcesses().values())
      {
         if(!StringUtils.hasContent(process.getTableName()))
         {
            orphanProcesses.add(process);
         }
      }

      if(!orphanProcesses.isEmpty())
      {
         topCommandSpec.addSubcommand("processes", defineProcessesCommand(orphanProcesses));
      }

      return topCommandSpec;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private CommandLine.Model.CommandSpec defineMetaDataCommand(QTableMetaData table)
   {
      return CommandLine.Model.CommandSpec.create();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private CommandLine.Model.CommandSpec defineQueryCommand(QTableMetaData table)
   {
      CommandLine.Model.CommandSpec queryCommand = CommandLine.Model.CommandSpec.create();
      queryCommand.addOption(CommandLine.Model.OptionSpec.builder("-l", "--limit")
         .type(int.class)
         .build());
      queryCommand.addOption(CommandLine.Model.OptionSpec.builder("-s", "--skip")
         .type(int.class)
         .build());
      queryCommand.addOption(CommandLine.Model.OptionSpec.builder("-c", "--criteria")
         .type(String[].class)
         .build());

      // todo - add the fields as explicit params?

      return queryCommand;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private CommandLine.Model.CommandSpec defineExportCommand(QTableMetaData table)
   {
      CommandLine.Model.CommandSpec exportCommand = CommandLine.Model.CommandSpec.create();
      exportCommand.addOption(CommandLine.Model.OptionSpec.builder("-f", "--filename")
         .type(String.class)
         .description("File name (including path) to write to.  File extension will be used to determine the report format.  Supported formats are:  csv, xlsx.")
         .required(true)
         .build());
      exportCommand.addOption(CommandLine.Model.OptionSpec.builder("-e", "--fieldNames")
         .type(String.class)
         .description("Comma-separated list of field names (e.g., from table meta-data) to include in the export.  If not given, then all fields in the table are included.")
         .build());
      exportCommand.addOption(CommandLine.Model.OptionSpec.builder("-l", "--limit")
         .type(int.class)
         .description("Optional limit on the max number of records to include in the export.")
         .build());
      addCriteriaOption(exportCommand);

      // todo - add the fields as explicit params?

      return exportCommand;
   }



   /*******************************************************************************
    ** add the standard '--criteria' option
    *******************************************************************************/
   private void addCriteriaOption(CommandLine.Model.CommandSpec commandSpec)
   {
      commandSpec.addOption(CommandLine.Model.OptionSpec.builder("-c", "--criteria")
         .type(String[].class)
         .description("""
            Query filter criteria.  May be given multiple times.
            Use format:  "$fieldName $operator $value".
            e.g., "id EQUALS 42\"""")
         .build());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void addPrimaryKeyOrKeysOption(CommandLine.Model.CommandSpec updateCommand, String verbForDescription)
   {
      updateCommand.addOption(CommandLine.Model.OptionSpec.builder("--primaryKey")
         // type(getClassForField(primaryKeyField))
         .type(String.class) // todo - mmm, better as picocli's "compound" thing, w/ the actual pkey's type?
         .description("""
            Primary Key(s) for the records to %s.
            May provide multiple values, separated by commas""".formatted(verbForDescription))
         .build());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private CommandLine.Model.CommandSpec defineGetCommand(QTableMetaData table)
   {
      CommandLine.Model.CommandSpec getCommand = CommandLine.Model.CommandSpec.create();
      getCommand.addPositional(CommandLine.Model.PositionalParamSpec.builder()
         .index("0")
         // .type(String.class) // todo - mmm, better as picocli's "compound" thing, w/ the actual pkey's type?
         .description("Primary key value from the table")
         .build());

      return getCommand;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private CommandLine.Model.CommandSpec defineCountCommand(QTableMetaData table)
   {
      CommandLine.Model.CommandSpec countCommand = CommandLine.Model.CommandSpec.create();
      addCriteriaOption(countCommand);

      // todo - add the fields as explicit params?

      return countCommand;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private CommandLine.Model.CommandSpec defineUpdateCommand(QTableMetaData table)
   {
      CommandLine.Model.CommandSpec updateCommand = CommandLine.Model.CommandSpec.create();

      /*
      todo - future may accept files, similar to (bulk) insert
      updateCommand.addOption(CommandLine.Model.OptionSpec.builder("--jsonBody")
         .type(String.class)
         .build());

      updateCommand.addOption(CommandLine.Model.OptionSpec.builder("--jsonFile")
         .type(String.class)
         .build());

      updateCommand.addOption(CommandLine.Model.OptionSpec.builder("--csvFile")
         .type(String.class)
         .build());

      updateCommand.addOption(CommandLine.Model.OptionSpec.builder("--mapping")
         .type(String.class)
         .build());
      */

      QFieldMetaData primaryKeyField = null;
      if(table.getPrimaryKeyField() != null)
      {
         primaryKeyField = table.getField(table.getPrimaryKeyField());
         addPrimaryKeyOrKeysOption(updateCommand, "update");
      }

      for(QFieldMetaData field : table.getFields().values())
      {
         if(!field.equals(primaryKeyField))
         {
            updateCommand.addOption(CommandLine.Model.OptionSpec.builder("--field-" + field.getName())
               .type(getClassForField(field))
               .description("""
                  Value to set for the field %s""".formatted(field.getName()))
               .build());
         }
      }

      addCriteriaOption(updateCommand);

      return updateCommand;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private CommandLine.Model.CommandSpec defineInsertCommand(QTableMetaData table)
   {
      CommandLine.Model.CommandSpec insertCommand = CommandLine.Model.CommandSpec.create();

      insertCommand.addOption(CommandLine.Model.OptionSpec.builder("--jsonBody")
         .type(String.class)
         .build());

      insertCommand.addOption(CommandLine.Model.OptionSpec.builder("--jsonFile")
         .type(String.class)
         .build());

      insertCommand.addOption(CommandLine.Model.OptionSpec.builder("--csvFile")
         .type(String.class)
         .build());

      insertCommand.addOption(CommandLine.Model.OptionSpec.builder("--mapping")
         .type(String.class)
         .build());

      for(QFieldMetaData field : table.getFields().values())
      {
         insertCommand.addOption(CommandLine.Model.OptionSpec.builder("--field-" + field.getName())
            .type(getClassForField(field))
            .build());
      }
      return insertCommand;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private CommandLine.Model.CommandSpec defineDeleteCommand(QTableMetaData table)
   {
      CommandLine.Model.CommandSpec deleteCommand = CommandLine.Model.CommandSpec.create();

      deleteCommand.addOption(CommandLine.Model.OptionSpec.builder("--primaryKey")
         .type(String.class) // todo - mmm, better as picocli's "compound" thing, w/ the actual pkey's type?
         .build());

      addCriteriaOption(deleteCommand);

      return deleteCommand;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private CommandLine.Model.CommandSpec defineProcessesCommand(List<QProcessMetaData> processes)
   {
      CommandLine.Model.CommandSpec processesCommand = CommandLine.Model.CommandSpec.create();

      for(QProcessMetaData process : processes)
      {
         ///////////////////////////////////////////
         // add the sub-command to run the proces //
         ///////////////////////////////////////////
         CommandLine.Model.CommandSpec processCommand = CommandLine.Model.CommandSpec.create();
         processesCommand.addSubcommand(process.getName(), processCommand);

         //////////////////////////////////////////////////////////////////////////////////
         // add all (distinct, by name) input fields to the command as --field-* options //
         //////////////////////////////////////////////////////////////////////////////////
         Map<String, QFieldMetaData> inputFieldMap = new LinkedHashMap<>();
         for(QFieldMetaData inputField : process.getInputFields())
         {
            inputFieldMap.put(inputField.getName(), inputField);
         }

         for(QFieldMetaData field : inputFieldMap.values())
         {
            processCommand.addOption(CommandLine.Model.OptionSpec.builder("--field-" + field.getName())
               .type(Objects.requireNonNullElse(getClassForField(field), String.class))
               .build());
         }

      }

      return (processesCommand);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @SuppressWarnings("checkstyle:Indentation")
   private Class<?> getClassForField(QFieldMetaData field)
   {
      if(field.getType() == null)
      {
         ///////////////////////////////////////////////////
         // shouldn't happen, but just in case, avoid NPE //
         ///////////////////////////////////////////////////
         return (null);
      }

      // @formatter:off // IJ can't do new-style switch correctly yet...
      return switch(field.getType())
      {
         case STRING, TEXT, HTML, PASSWORD -> String.class;
         case INTEGER -> Integer.class;
         case LONG -> Long.class;
         case DECIMAL -> BigDecimal.class;
         case DATE -> LocalDate.class;
         case TIME -> LocalTime.class;
         case BOOLEAN -> Boolean.class;
         case DATE_TIME -> LocalDateTime.class;
         case BLOB -> byte[].class;
      };
      // @formatter:on
   }

}
