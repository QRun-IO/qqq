/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.metadata.qbits;


import java.util.List;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QRuntimeException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerHelper;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerInterface;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerMultiOutput;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerOutput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** 2nd generation interface for top-level meta-data production classes that make
 ** a qbit (evolution over QBitProducer).
 **
 *******************************************************************************/
public interface QBitMetaDataProducer<C extends QBitConfig> extends MetaDataProducerInterface<MetaDataProducerMultiOutput>
{
   QLogger LOG = QLogger.getLogger(QBitMetaDataProducer.class);

   /***************************************************************************
    **
    ***************************************************************************/
   C getQBitConfig();


   /***************************************************************************
    **
    ***************************************************************************/
   QBitMetaData getQBitMetaData();


   /***************************************************************************
    **
    ***************************************************************************/
   default String getNamespace()
   {
      return (null);
   }

   /***************************************************************************
    **
    ***************************************************************************/
   default void postProduceActions(MetaDataProducerMultiOutput metaDataProducerMultiOutput, QInstance qinstance) throws QException
   {
      /////////////////////
      // noop by default //
      /////////////////////
   }

   /***************************************************************************
    **
    ***************************************************************************/
   default String getPackageNameForFindingMetaDataProducers()
   {
      Class<?> clazz = getClass();

      ////////////////////////////////////////////////////////////////
      // Walk up the hierarchy until we find the direct implementer //
      ////////////////////////////////////////////////////////////////
      while(clazz != null)
      {
         Class<?>[] interfaces = clazz.getInterfaces();
         for(Class<?> interfaze : interfaces)
         {
            if(interfaze == QBitMetaDataProducer.class)
            {
               return clazz.getPackageName();
            }
         }
         clazz = clazz.getSuperclass();
      }

      throw (new QRuntimeException("Unable to find packageName for QBitMetaDataProducer.  You may need to implement getPackageName yourself..."));
   }


   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   default MetaDataProducerMultiOutput produce(QInstance qInstance) throws QException
   {
      MetaDataProducerMultiOutput rs = new MetaDataProducerMultiOutput();

      QBitMetaData qBitMetaData = getQBitMetaData();
      C            qBitConfig   = getQBitConfig();

      qInstance.addQBit(qBitMetaData);

      QBitProductionContext.pushQBitConfig(qBitConfig);
      QBitProductionContext.pushMetaDataProducerMultiOutput(rs);

      try
      {
         qBitConfig.validate(qInstance);

         List<MetaDataProducerInterface<?>> producers = MetaDataProducerHelper.findProducers(qInstance, getPackageNameForFindingMetaDataProducers());
         MetaDataProducerHelper.sortMetaDataProducers(producers);
         for(MetaDataProducerInterface<?> producer : producers)
         {
            if(producer.getClass().equals(this.getClass()))
            {
               /////////////////////////////////////////////
               // avoid recursive processing of ourselves //
               /////////////////////////////////////////////
               continue;
            }

            ////////////////////////////////////////////////////////////////////////////
            // todo is this deprecated in favor of QBitProductionContext's stack... ? //
            ////////////////////////////////////////////////////////////////////////////
            if(producer instanceof QBitComponentMetaDataProducerInterface<?, ?>)
            {
               QBitComponentMetaDataProducerInterface<?, C> qBitComponentMetaDataProducer = (QBitComponentMetaDataProducerInterface<?, C>) producer;
               qBitComponentMetaDataProducer.setQBitConfig(qBitConfig);
            }

            if(!producer.isEnabled())
            {
               LOG.debug("Not using producer which is not enabled", logPair("producer", producer.getClass().getSimpleName()));
               continue;
            }

            MetaDataProducerOutput subProducerOutput = producer.produce(qInstance);

            /////////////////////////////////////////////////
            // apply some things from the config to tables //
            /////////////////////////////////////////////////
            if(subProducerOutput instanceof QTableMetaData table)
            {
               if(qBitConfig.getTableMetaDataCustomizer() != null)
               {
                  subProducerOutput = qBitConfig.getTableMetaDataCustomizer().customizeMetaData(qInstance, table);
               }

               if(!StringUtils.hasContent(table.getBackendName()) && StringUtils.hasContent(qBitConfig.getDefaultBackendNameForTables()))
               {
                  table.setBackendName(qBitConfig.getDefaultBackendNameForTables());
               }
            }

            ////////////////////////////////////////////////////////////
            // set source qbit, if subProducerOutput is aware of such //
            ////////////////////////////////////////////////////////////
            if(subProducerOutput instanceof SourceQBitAware sourceQBitAware)
            {
               sourceQBitAware.setSourceQBitName(qBitMetaData.getName());
            }

            rs.add(subProducerOutput);
         }

         postProduceActions(rs, qInstance);

         return (rs);
      }
      finally
      {
         QBitProductionContext.popQBitConfig();
         QBitProductionContext.popMetaDataProducerMultiOutput();
      }
   }

}
