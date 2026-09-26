/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.instances.loaders;


import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QMetaDataObject;
import com.kingsrook.qqq.backend.core.model.metadata.TopLevelMetaDataInterface;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 ** Loads application metadata, rejecting invalid input instead of partial success.
 *******************************************************************************/
public class MetaDataLoaderHelper
{
   /*******************************************************************************
    ** Read a YAML/JSON stream. The caller owns and closes the stream.
    *******************************************************************************/
   public static QMetaDataObject readMetaDataFile(QInstance instance, InputStream input, String fileName) throws QException
   {
      if(input == null)
      {
         throw new QException("Metadata input not found: " + fileName);
      }
      try
      {
         ClassDetectingMetaDataLoader loader = new ClassDetectingMetaDataLoader();
         QMetaDataObject metadata = loader.fileToMetaDataObject(instance, input, fileName);
         if(CollectionUtils.nullSafeHasContents(loader.getProblems()))
         {
            throw new QException("Invalid metadata in " + fileName + ": " + loader.getProblems());
         }
         return metadata;
      }
      catch(QException e)
      {
         throw e;
      }
      catch(Exception e)
      {
         throw new QException("Error reading metadata file: " + fileName, e);
      }
   }



   /*******************************************************************************
    ** Load top-level metadata in filename order, recursively. Every file must be
    ** valid YAML/JSON metadata; an unreadable directory or invalid file fails.
    *******************************************************************************/
   public static void processAllMetaDataFilesInDirectory(QInstance instance, String path) throws QException
   {
      File[] files = new File(path).listFiles();
      if(files == null)
      {
         throw new QException("Cannot read metadata directory: " + path);
      }
      Arrays.sort(files, Comparator.comparing(File::getName));
      for(File file : files)
      {
         if(file.isDirectory())
         {
            processAllMetaDataFilesInDirectory(instance, file.getPath());
         }
         else
         {
            try(InputStream input = new FileInputStream(file))
            {
               QMetaDataObject metadata = readMetaDataFile(instance, input, file.getPath());
               if(!(metadata instanceof TopLevelMetaDataInterface topLevel))
               {
                  throw new QException("Metadata file must define a top-level object: " + file.getPath());
               }
               topLevel.addSelfToInstance(instance);
            }
            catch(QException e)
            {
               throw e;
            }
            catch(Exception e)
            {
               throw new QException("Error loading metadata file: " + file.getPath(), e);
            }
         }
      }
   }
}
