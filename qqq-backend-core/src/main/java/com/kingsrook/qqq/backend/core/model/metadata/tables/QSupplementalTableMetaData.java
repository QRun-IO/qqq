/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.metadata.tables;


import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Base-class for table-level meta-data defined by some supplemental module, etc,
 ** outside of qqq core
 *******************************************************************************/
public abstract class QSupplementalTableMetaData implements Cloneable
{
   private static final QLogger LOG = QLogger.getLogger(QSupplementalTableMetaData.class);

   private static Set<Class<?>> warnedAboutMissingFinishClones = new HashSet<>();



   /***************************************************************************
    * Return the supplemental table meta data object assigned to the input table
    * under the given type.  If no object is assigned, returns null.
    *
    * @param table where to look for the supplemental table meta data
    * @param type identifier for the supplemental meta data - as returned by getType
    * @return QSupplementalTableMetaData subclass assigned to the table - may be null.
    ***************************************************************************/
   @SuppressWarnings("unchecked")
   protected static <S extends QSupplementalTableMetaData> S of(QTableMetaData table, String type)
   {
      return ((S) table.getSupplementalMetaData(type));
   }



   /***************************************************************************
    * Return the supplemental table meta data object assigned to the input table
    * under the given type - but also - if no object is assigned, create a new
    * one by running the input supplier, and then assign that one to the table
    * (that's the "withNew" part of the method name
    *
    * @param table where to look for the supplemental table meta data
    * @param type identifier for the supplemental meta data - as returned by getType
    * @param supplier source of new objects if one isn't already on the table.
    *                 typically a constructor for the QSupplementalTableMetaData subtype.
    * @return QSupplementalTableMetaData subclass assigned to the table - shouldn't
    * ever be null (though if the supplier returns null, then it could be).
    ***************************************************************************/
   protected static <S extends QSupplementalTableMetaData> S ofOrWithNew(QTableMetaData table, String type, Supplier<S> supplier)
   {
      S s = of(table, type);
      if(s == null)
      {
         s = supplier.get();
         table.withSupplementalMetaData(s);
      }
      return (s);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public boolean includeInPartialFrontendMetaData()
   {
      return (false);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public boolean includeInFullFrontendMetaData()
   {
      return (false);
   }



   /*******************************************************************************
    ** Getter for type
    *******************************************************************************/
   public abstract String getType();



   /*******************************************************************************
    **
    *******************************************************************************/
   public void enrich(QInstance qInstance, QTableMetaData table)
   {
      ////////////////////////
      // noop in base class //
      ////////////////////////
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void validate(QInstance qInstance, QTableMetaData tableMetaData, QInstanceValidator qInstanceValidator)
   {
      ////////////////////////
      // noop in base class //
      ////////////////////////
   }



   /***************************************************************************
    * adding cloneable to this type hierarchy - subclasses need to implement
    * finishClone to copy their specific state.
    ***************************************************************************/
   @Override
   public final QSupplementalTableMetaData clone()
   {
      try
      {
         QSupplementalTableMetaData clone = (QSupplementalTableMetaData) super.clone();
         finishClone(clone);
         return clone;
      }
      catch(CloneNotSupportedException e)
      {
         throw new AssertionError();
      }
   }



   /***************************************************************************
    * finish the cloning operation started in the base class. copy all state
    * from the subclass into the input clone (which can be safely casted to
    * the subclass's type, as it was obtained by super.clone())
    *
    * Rather than making this public and breaking all existing implementations
    * that don't have it - we're making it protected, with a one-time warning
    * if it isn't implemented in a subclass.
    ***************************************************************************/
   protected QSupplementalTableMetaData finishClone(QSupplementalTableMetaData abstractClone)
   {
      if(!warnedAboutMissingFinishClones.contains(abstractClone.getClass()))
      {
         LOG.warn("Missing finishClone method in a subclass of QSupplementalTableMetaData.", logPair("className", abstractClone.getClass().getName()));
         warnedAboutMissingFinishClones.add(abstractClone.getClass());
      }

      return (abstractClone);
   }

}
