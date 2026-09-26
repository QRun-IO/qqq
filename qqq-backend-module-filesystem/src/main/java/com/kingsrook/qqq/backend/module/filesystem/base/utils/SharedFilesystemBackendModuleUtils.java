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

package com.kingsrook.qqq.backend.module.filesystem.base.utils;


import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.module.filesystem.base.model.metadata.AbstractFilesystemTableBackendDetails;


/*******************************************************************************
 ** utility methods shared by s3 & local-filesystem utils classes
 *******************************************************************************/
public class SharedFilesystemBackendModuleUtils
{

   /*******************************************************************************
    **
    *******************************************************************************/
   public static boolean doesFilePathMatchFilter(String filePath, QQueryFilter filter, AbstractFilesystemTableBackendDetails tableDetails) throws QException
   {
      if(filter == null || !filter.hasAnyCriteria())
      {
         return (true);
      }

      if(CollectionUtils.nullSafeHasContents(filter.getSubFilters()))
      {
         ///////////////////////////////
         // todo - well, we could ... //
         ///////////////////////////////
         throw (new QException("Filters with sub-filters are not supported for querying filesystems at this time."));
      }

      Path path = Path.of(URI.create("file:///" + filePath));

      ////////////////////////////////////////////////////////////////////////////////////////////////////
      // foreach criteria, build a pathmatcher (or many, for an in-list), and check if the file matches //
      ////////////////////////////////////////////////////////////////////////////////////////////////////
      for(QFilterCriteria criteria : filter.getCriteria())
      {
         boolean matches = doesFilePathMatchOneCriteria(criteria, tableDetails, path);

         if(!matches && QQueryFilter.BooleanOperator.AND.equals(filter.getBooleanOperator()))
         {
            ////////////////////////////////////////////////////////////////////////////////
            // if it's not a match, and it's an AND filter, then the whole thing is false //
            ////////////////////////////////////////////////////////////////////////////////
            return (false);
         }

         if(matches && QQueryFilter.BooleanOperator.OR.equals(filter.getBooleanOperator()))
         {
            ////////////////////////////////////////////////////////////
            // if it's an OR filter, and we've a match, return a true //
            ////////////////////////////////////////////////////////////
            return (true);
         }
      }

      //////////////////////////////////////////////////////////////////////
      // if we didn't return above, return now                            //
      // for an OR - if we didn't find something true, then return false. //
      // else, an AND - if we didn't find a false, we can return true.    //
      //////////////////////////////////////////////////////////////////////
      if(QQueryFilter.BooleanOperator.OR.equals(filter.getBooleanOperator()))
      {
         return (false);
      }

      return (true);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static boolean doesFilePathMatchOneCriteria(QFilterCriteria criteria, AbstractFilesystemTableBackendDetails tableBackendDetails, Path path) throws QException
   {
      if(tableBackendDetails.getFileNameFieldName().equals(criteria.getFieldName()))
      {
         if(QCriteriaOperator.EQUALS.equals(criteria.getOperator()) && CollectionUtils.nonNullList(criteria.getValues()).size() == 1)
         {
            return (FileSystems.getDefault().getPathMatcher("glob:**/" + criteria.getValues().get(0)).matches(path));
         }
         else if(QCriteriaOperator.IN.equals(criteria.getOperator()) && !CollectionUtils.nonNullList(criteria.getValues()).isEmpty())
         {
            boolean anyMatch = false;
            for(int i = 0; i < criteria.getValues().size(); i++)
            {
               if(FileSystems.getDefault().getPathMatcher("glob:**/" + criteria.getValues().get(i)).matches(path))
               {
                  anyMatch = true;
                  break;
               }
            }

            return (anyMatch);
         }
         else
         {
            throw (new QException("Unable to query filename field using operator: " + criteria.getOperator()));
         }
      }
      else
      {
         ///////////////////////////////////////////////////////////////////////////////////////////////
         // this happens in base class now, like, for query action, so, we think okay to just ignore. //
         ///////////////////////////////////////////////////////////////////////////////////////////////
         // throw (new QException("Unable to query filesystem table by field: " + criteria.getFieldName()));
         return (true);
      }
   }

}
