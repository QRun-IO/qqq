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


import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import com.kingsrook.qqq.backend.core.actions.processes.QProcessCallback;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import picocli.CommandLine;


/*******************************************************************************
 ** Define how a PicoCLI process gets data back to a QProcess.
 *******************************************************************************/
public class PicoCliProcessCallback implements QProcessCallback
{
   private final CommandLine commandLine;



   /*******************************************************************************
    ** Constructor that takes the picocli CommandLine object
    *******************************************************************************/
   public PicoCliProcessCallback(CommandLine commandLine)
   {
      this.commandLine = commandLine;
   }



   /*******************************************************************************
    ** Get the filter query for this callback.
    *******************************************************************************/
   @Override
   public QQueryFilter getQueryFilter()
   {
      return null;
   }



   /*******************************************************************************
    ** Get the field values for this callback.
    *******************************************************************************/
   @Override
   public Map<String, Serializable> getFieldValues(List<QFieldMetaData> fields)
   {
      Map<String, Serializable> rs      = new HashMap<>();
      final Scanner             scanner = new Scanner(System.in);

      ///////////////////////////////////
      // todo - only if "interactive?" //
      ///////////////////////////////////
      for(QFieldMetaData field : fields)
      {
         commandLine.getOut().println("Please supply a value for the field: [" + field.getLabel() + "]:");
         rs.put(field.getName(), scanner.nextLine());
      }

      return (rs);
   }

}
