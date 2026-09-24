/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.instances.loaders;


import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.kingsrook.qqq.backend.core.instances.loaders.implementations.GenericMetaDataLoader;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QMetaDataObject;
import com.kingsrook.qqq.backend.core.utils.ClassPathUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import com.kingsrook.qqq.backend.core.utils.memoization.AnyKey;
import com.kingsrook.qqq.backend.core.utils.memoization.Memoization;


/*******************************************************************************
 ** Generic implementation of AbstractMetaDataLoader, who "detects" the class
 ** of meta data object to be created, then defers to an appropriate subclass
 ** to do the work.
 *******************************************************************************/
public class ClassDetectingMetaDataLoader extends AbstractMetaDataLoader<QMetaDataObject>
{
   private static final Memoization<AnyKey, List<Class<?>>> memoizedMetaDataObjectClasses = new Memoization<>();


   /***************************************************************************
    *
    ***************************************************************************/
   public AbstractMetaDataLoader<?> getLoaderForFile(InputStream inputStream, String fileName) throws QMetaDataLoaderException
   {
      Map<String, Object> map = fileToMap(inputStream, fileName);
      return (getLoaderForMap(map));
   }



   /***************************************************************************
    *
    ***************************************************************************/
   public AbstractMetaDataLoader<?> getLoaderForMap(Map<String, Object> map) throws QMetaDataLoaderException
   {
      if(map.containsKey("class"))
      {
         String classProperty = ValueUtils.getValueAsString(map.get("class"));
         try
         {
            if(MetaDataLoaderRegistry.hasLoaderForSimpleName(classProperty))
            {
               Class<? extends AbstractMetaDataLoader<?>> loaderClass = MetaDataLoaderRegistry.getLoaderForSimpleName(classProperty);
               return (loaderClass.getConstructor().newInstance());
            }
            else
            {
               Optional<List<Class<?>>> metaDataClasses = memoizedMetaDataObjectClasses.getResult(AnyKey.getInstance(), k -> ClassPathUtils.getClassesContainingNameAndOfType("MetaData", QMetaDataObject.class));
               if(metaDataClasses.isEmpty())
               {
                  throw (new QMetaDataLoaderException("Could not get list of metaDataObjects from class loader"));
               }

               for(Class<?> c : metaDataClasses.get())
               {
                  if(c.getSimpleName().equals(classProperty) && QMetaDataObject.class.isAssignableFrom(c))
                  {
                     @SuppressWarnings("unchecked")
                     Class<? extends QMetaDataObject> metaDataClass = (Class<? extends QMetaDataObject>) c;
                     return new GenericMetaDataLoader<>(metaDataClass);
                  }
               }
            }
            throw new QMetaDataLoaderException("Unexpected class [" + classProperty + "] (not a QMetaDataObject; doesn't have a registered MetaDataLoader) specified in " + getFileName());
         }
         catch(QMetaDataLoaderException qmdle)
         {
            throw (qmdle);
         }
         catch(Exception e)
         {
            throw new QMetaDataLoaderException("Error handling class [" + classProperty + "] specified in " + getFileName(), e);
         }
      }
      else
      {
         throw new QMetaDataLoaderException("Cannot detect meta-data type, because [class] attribute was not specified in file: " + getFileName());
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public QMetaDataObject mapToMetaDataObject(QInstance qInstance, Map<String, Object> map, LoadingContext context) throws QMetaDataLoaderException
   {
      AbstractMetaDataLoader<?> loaderForMap = getLoaderForMap(map);
      QMetaDataObject metadata = loaderForMap.mapToMetaDataObject(qInstance, map, context);
      getProblems().addAll(loaderForMap.getProblems());
      return metadata;
   }
}
