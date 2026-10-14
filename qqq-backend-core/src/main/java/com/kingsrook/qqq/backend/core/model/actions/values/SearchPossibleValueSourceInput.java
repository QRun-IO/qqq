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

package com.kingsrook.qqq.backend.core.model.actions.values;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.InputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;


/*******************************************************************************
 ** Input for the Search possible value source action
 *******************************************************************************/
public class SearchPossibleValueSourceInput extends AbstractActionInput implements Cloneable
{
   private InputSource inputSource = QInputSource.SYSTEM;

   private String                    possibleValueSourceName;
   private QQueryFilter              defaultQueryFilter;
   private String                    searchTerm;
   private List<Serializable>        idList;
   private List<String>              labelList;
   private Map<String, String>       pathParamMap;
   private Map<String, List<String>> queryParamMap;

   private Map<String, Serializable> otherValues;

   private Integer skip  = 0;
   private Integer limit = 250;

   //////////////////////////////////////////////////////////////////////////////////
   // record picker requests (the source must declare a picker): searchTerm uses   //
   // CONTAINS over the picker's search fields, pickerFilter adds criteria and an  //
   // ordering limited to the picker's filter and sort fields, afterId pages by    //
   // keyset when ordering by the primary key alone, and each option comes back   //
   // with its picker display values plus a hasMore flag (never a count).         //
   //////////////////////////////////////////////////////////////////////////////////
   private boolean      pickerRequest = false;
   private QQueryFilter pickerFilter;
   private Serializable afterId;




   /*******************************************************************************
    **
    *******************************************************************************/
   public SearchPossibleValueSourceInput()
   {
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public SearchPossibleValueSourceInput clone()
   {
      try
      {
         SearchPossibleValueSourceInput clone = (SearchPossibleValueSourceInput) super.clone();
         if(defaultQueryFilter != null)
         {
            clone.setDefaultQueryFilter(defaultQueryFilter.clone());
         }
         if(pickerFilter != null)
         {
            clone.setPickerFilter(pickerFilter.clone());
         }
         if(idList != null)
         {
            clone.setIdList(new ArrayList<>(idList));
         }
         return clone;
      }
      catch(CloneNotSupportedException e)
      {
         throw new AssertionError();
      }
   }



   /*******************************************************************************
    ** Getter for possibleValueSourceName
    **
    *******************************************************************************/
   public String getPossibleValueSourceName()
   {
      return possibleValueSourceName;
   }



   /*******************************************************************************
    ** Setter for possibleValueSourceName
    **
    *******************************************************************************/
   public void setPossibleValueSourceName(String possibleValueSourceName)
   {
      this.possibleValueSourceName = possibleValueSourceName;
   }



   /*******************************************************************************
    ** Fluent setter for possibleValueSourceName
    **
    *******************************************************************************/
   public SearchPossibleValueSourceInput withPossibleValueSourceName(String possibleValueSourceName)
   {
      this.possibleValueSourceName = possibleValueSourceName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for defaultQueryFilter
    **
    *******************************************************************************/
   public QQueryFilter getDefaultQueryFilter()
   {
      return defaultQueryFilter;
   }



   /*******************************************************************************
    ** Setter for defaultQueryFilter
    **
    *******************************************************************************/
   public void setDefaultQueryFilter(QQueryFilter defaultQueryFilter)
   {
      this.defaultQueryFilter = defaultQueryFilter;
   }



   /*******************************************************************************
    ** Fluent setter for defaultQueryFilter
    **
    *******************************************************************************/
   public SearchPossibleValueSourceInput withDefaultQueryFilter(QQueryFilter defaultQueryFilter)
   {
      this.defaultQueryFilter = defaultQueryFilter;
      return (this);
   }



   /*******************************************************************************
    ** Getter for searchTerm
    **
    *******************************************************************************/
   public String getSearchTerm()
   {
      return searchTerm;
   }



   /*******************************************************************************
    ** Setter for searchTerm
    **
    *******************************************************************************/
   public void setSearchTerm(String searchTerm)
   {
      this.searchTerm = searchTerm;
   }



   /*******************************************************************************
    ** Fluent setter for searchTerm
    **
    *******************************************************************************/
   public SearchPossibleValueSourceInput withSearchTerm(String searchTerm)
   {
      this.searchTerm = searchTerm;
      return (this);
   }



   /*******************************************************************************
    ** Getter for idList
    **
    *******************************************************************************/
   public List<Serializable> getIdList()
   {
      return idList;
   }



   /*******************************************************************************
    ** Setter for idList
    **
    *******************************************************************************/
   public void setIdList(List<Serializable> idList)
   {
      this.idList = idList;
   }



   /*******************************************************************************
    ** Fluent setter for idList
    **
    *******************************************************************************/
   public SearchPossibleValueSourceInput withIdList(List<Serializable> idList)
   {
      this.idList = idList;
      return (this);
   }



   /*******************************************************************************
    ** Getter for skip
    **
    *******************************************************************************/
   public Integer getSkip()
   {
      return skip;
   }



   /*******************************************************************************
    ** Setter for skip
    **
    *******************************************************************************/
   public void setSkip(Integer skip)
   {
      this.skip = skip;
   }



   /*******************************************************************************
    ** Fluent setter for skip
    **
    *******************************************************************************/
   public SearchPossibleValueSourceInput withSkip(Integer skip)
   {
      this.skip = skip;
      return (this);
   }



   /*******************************************************************************
    ** Getter for limit
    **
    *******************************************************************************/
   public Integer getLimit()
   {
      return limit;
   }



   /*******************************************************************************
    ** Setter for limit
    **
    *******************************************************************************/
   public void setLimit(Integer limit)
   {
      this.limit = limit;
   }



   /*******************************************************************************
    ** Fluent setter for limit
    **
    *******************************************************************************/
   public SearchPossibleValueSourceInput withLimit(Integer limit)
   {
      this.limit = limit;
      return (this);
   }



   /*******************************************************************************
    ** Getter for labelList
    *******************************************************************************/
   public List<String> getLabelList()
   {
      return (this.labelList);
   }



   /*******************************************************************************
    ** Setter for labelList
    *******************************************************************************/
   public void setLabelList(List<String> labelList)
   {
      this.labelList = labelList;
   }



   /*******************************************************************************
    ** Fluent setter for labelList
    *******************************************************************************/
   public SearchPossibleValueSourceInput withLabelList(List<String> labelList)
   {
      this.labelList = labelList;
      return (this);
   }



   /*******************************************************************************
    ** Getter for pathParamMap
    *******************************************************************************/
   public Map<String, String> getPathParamMap()
   {
      return (this.pathParamMap);
   }



   /*******************************************************************************
    ** Setter for pathParamMap
    *******************************************************************************/
   public void setPathParamMap(Map<String, String> pathParamMap)
   {
      this.pathParamMap = pathParamMap;
   }



   /*******************************************************************************
    ** Fluent setter for pathParamMap
    *******************************************************************************/
   public SearchPossibleValueSourceInput withPathParamMap(Map<String, String> pathParamMap)
   {
      this.pathParamMap = pathParamMap;
      return (this);
   }



   /*******************************************************************************
    ** Getter for queryParamMap
    *******************************************************************************/
   public Map<String, List<String>> getQueryParamMap()
   {
      return (this.queryParamMap);
   }



   /*******************************************************************************
    ** Setter for queryParamMap
    *******************************************************************************/
   public void setQueryParamMap(Map<String, List<String>> queryParamMap)
   {
      this.queryParamMap = queryParamMap;
   }



   /*******************************************************************************
    ** Fluent setter for queryParamMap
    *******************************************************************************/
   public SearchPossibleValueSourceInput withQueryParamMap(Map<String, List<String>> queryParamMap)
   {
      this.queryParamMap = queryParamMap;
      return (this);
   }



   /*******************************************************************************
    ** Getter for otherValues
    *******************************************************************************/
   public Map<String, Serializable> getOtherValues()
   {
      return (this.otherValues);
   }



   /*******************************************************************************
    ** Setter for otherValues
    *******************************************************************************/
   public void setOtherValues(Map<String, Serializable> otherValues)
   {
      this.otherValues = otherValues;
   }



   /*******************************************************************************
    ** Fluent setter for otherValues
    *******************************************************************************/
   public SearchPossibleValueSourceInput withOtherValues(Map<String, Serializable> otherValues)
   {
      this.otherValues = otherValues;
      return (this);
   }



   /*******************************************************************************
    ** Getter for pickerRequest
    *******************************************************************************/
   public boolean getPickerRequest()
   {
      return (pickerRequest);
   }



   /*******************************************************************************
    ** Setter for pickerRequest
    *******************************************************************************/
   public void setPickerRequest(boolean pickerRequest)
   {
      this.pickerRequest = pickerRequest;
   }



   /*******************************************************************************
    ** Fluent setter for pickerRequest
    *******************************************************************************/
   public SearchPossibleValueSourceInput withPickerRequest(boolean pickerRequest)
   {
      this.pickerRequest = pickerRequest;
      return (this);
   }



   /*******************************************************************************
    ** Getter for pickerFilter
    *******************************************************************************/
   public QQueryFilter getPickerFilter()
   {
      return (pickerFilter);
   }



   /*******************************************************************************
    ** Setter for pickerFilter
    *******************************************************************************/
   public void setPickerFilter(QQueryFilter pickerFilter)
   {
      this.pickerFilter = pickerFilter;
   }



   /*******************************************************************************
    ** Fluent setter for pickerFilter
    *******************************************************************************/
   public SearchPossibleValueSourceInput withPickerFilter(QQueryFilter pickerFilter)
   {
      this.pickerFilter = pickerFilter;
      return (this);
   }



   /*******************************************************************************
    ** Getter for afterId
    *******************************************************************************/
   public Serializable getAfterId()
   {
      return (afterId);
   }



   /*******************************************************************************
    ** Setter for afterId
    *******************************************************************************/
   public void setAfterId(Serializable afterId)
   {
      this.afterId = afterId;
   }



   /*******************************************************************************
    ** Fluent setter for afterId
    *******************************************************************************/
   public SearchPossibleValueSourceInput withAfterId(Serializable afterId)
   {
      this.afterId = afterId;
      return (this);
   }



   /*******************************************************************************
    ** Source of the possible-value request.
    *******************************************************************************/
   public InputSource getInputSource()
   {
      return (inputSource);
   }



   /*******************************************************************************
    ** Set the source without changing table permission enforcement.
    *******************************************************************************/
   public void setInputSource(InputSource inputSource)
   {
      this.inputSource = inputSource;
   }
}
