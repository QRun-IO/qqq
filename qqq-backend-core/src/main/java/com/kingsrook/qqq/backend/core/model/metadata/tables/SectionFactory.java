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

package com.kingsrook.qqq.backend.core.model.metadata.tables;


import java.util.List;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;


/*******************************************************************************
 ** Factory class for creating "standard" qfield sections.  e.g., if you want
 ** the same t1, t2, and t3 section on all your tables, use this class to
 ** produce them.
 **
 ** You can change the default name & iconNames for those sections, but note,
 ** this is a static/utility style class, so those settings are static fields.
 **
 ** The method customT2 is provided as not much of a shortcut over "doing it yourself",
 ** but to allow all sections for a table to be produced through calls to this factory,
 ** so they look more similar.
 *******************************************************************************/
public class SectionFactory
{
   private static String defaultT1name     = "identity";
   private static String defaultT1iconName = "badge";
   private static String defaultT2name     = "data";
   private static String defaultT2iconName = "text_snippet";
   private static String defaultT3name     = "dates";
   private static String defaultT3iconName = "calendar_month";


   /*******************************************************************************
    ** private constructor, to enforce static usage, e.g., to make clear the fields
    ** are static fields.
    **
    *******************************************************************************/
   private SectionFactory()
   {
   }


   /***************************************************************************
    **
    ***************************************************************************/
   public static QFieldSection defaultT1(String... fieldNames)
   {
      return new QFieldSection(defaultT1name, new QIcon().withName(defaultT1iconName), Tier.T1, List.of(fieldNames));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static QFieldSection defaultT2(String... fieldNames)
   {
      return new QFieldSection(defaultT2name, new QIcon().withName(defaultT2iconName), Tier.T2, List.of(fieldNames));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static QFieldSection customT2(String name, QIcon icon, String... fieldNames)
   {
      return new QFieldSection(name, icon, Tier.T2, List.of(fieldNames));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static QFieldSection defaultT3(String... fieldNames)
   {
      return new QFieldSection(defaultT3name, new QIcon().withName(defaultT3iconName), Tier.T3, List.of(fieldNames));
   }



   /*******************************************************************************
    ** Getter for defaultT1name
    *******************************************************************************/
   public static String getDefaultT1name()
   {
      return (SectionFactory.defaultT1name);
   }



   /*******************************************************************************
    ** Setter for defaultT1name
    *******************************************************************************/
   public static void setDefaultT1name(String defaultT1name)
   {
      SectionFactory.defaultT1name = defaultT1name;
   }



   /*******************************************************************************
    ** Getter for defaultT1iconName
    *******************************************************************************/
   public static String getDefaultT1iconName()
   {
      return (SectionFactory.defaultT1iconName);
   }



   /*******************************************************************************
    ** Setter for defaultT1iconName
    *******************************************************************************/
   public static void setDefaultT1iconName(String defaultT1iconName)
   {
      SectionFactory.defaultT1iconName = defaultT1iconName;
   }



   /*******************************************************************************
    ** Getter for defaultT2name
    *******************************************************************************/
   public static String getDefaultT2name()
   {
      return (SectionFactory.defaultT2name);
   }



   /*******************************************************************************
    ** Setter for defaultT2name
    *******************************************************************************/
   public static void setDefaultT2name(String defaultT2name)
   {
      SectionFactory.defaultT2name = defaultT2name;
   }



   /*******************************************************************************
    ** Getter for defaultT2iconName
    *******************************************************************************/
   public static String getDefaultT2iconName()
   {
      return (SectionFactory.defaultT2iconName);
   }



   /*******************************************************************************
    ** Setter for defaultT2iconName
    *******************************************************************************/
   public static void setDefaultT2iconName(String defaultT2iconName)
   {
      SectionFactory.defaultT2iconName = defaultT2iconName;
   }



   /*******************************************************************************
    ** Getter for defaultT3name
    *******************************************************************************/
   public static String getDefaultT3name()
   {
      return (SectionFactory.defaultT3name);
   }



   /*******************************************************************************
    ** Setter for defaultT3name
    *******************************************************************************/
   public static void setDefaultT3name(String defaultT3name)
   {
      SectionFactory.defaultT3name = defaultT3name;
   }



   /*******************************************************************************
    ** Getter for defaultT3iconName
    *******************************************************************************/
   public static String getDefaultT3iconName()
   {
      return (SectionFactory.defaultT3iconName);
   }



   /*******************************************************************************
    ** Setter for defaultT3iconName
    *******************************************************************************/
   public static void setDefaultT3iconName(String defaultT3iconName)
   {
      SectionFactory.defaultT3iconName = defaultT3iconName;
   }



}
