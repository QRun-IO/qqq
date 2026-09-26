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
