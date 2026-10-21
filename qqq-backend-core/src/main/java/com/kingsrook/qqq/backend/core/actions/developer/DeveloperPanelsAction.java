/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.developer;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.developer.DeveloperPanel;
import com.kingsrook.qqq.backend.core.model.developer.DeveloperPanelContext;
import com.kingsrook.qqq.backend.core.model.developer.DeveloperPanelProviderInterface;
import com.kingsrook.qqq.backend.core.model.developer.DeveloperPanelsMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Build the developer panels for a table's record: the built-in panels
 ** (record security), then every provider registered in the instance's
 ** {@link DeveloperPanelsMetaData}, each asked whether it is available first.
 **
 ** A provider that fails (to load, to say whether it is available, or to build)
 ** yields a panel with an error message, so one broken module does not hide the
 ** others.  Panels come back sorted by order, then title.
 *******************************************************************************/
public class DeveloperPanelsAction
{
   private static final QLogger LOG = QLogger.getLogger(DeveloperPanelsAction.class);

   private static final Integer DEFAULT_ORDER = 500;



   /*******************************************************************************
    ** Build the panels for the context's table and record, for the current session.
    *******************************************************************************/
   public List<DeveloperPanel> execute(DeveloperPanelContext context)
   {
      List<DeveloperPanel> panels = new ArrayList<>();
      Set<String>          usedIds = new HashSet<>();

      for(DeveloperPanelProviderInterface provider : getProviders(QContext.getQInstance()))
      {
         if(provider == null || !usedIds.add(Objects.requireNonNullElse(provider.getId(), provider.getClass().getName())))
         {
            continue;
         }

         DeveloperPanel panel = buildPanel(provider, context);
         if(panel != null)
         {
            panels.add(panel);
         }
      }

      panels.sort(Comparator.comparing((DeveloperPanel panel) -> Objects.requireNonNullElse(panel.getOrder(), DEFAULT_ORDER))
         .thenComparing(panel -> Objects.requireNonNullElse(panel.getTitle(), "")));
      return (panels);
   }



   /*******************************************************************************
    ** The built-in providers, then the instance's registered ones.  A registered
    ** provider that cannot be loaded is logged and skipped.
    *******************************************************************************/
   static List<DeveloperPanelProviderInterface> getProviders(QInstance qInstance)
   {
      List<DeveloperPanelProviderInterface> providers = new ArrayList<>();
      providers.add(new RecordSecurityDeveloperPanelProvider());

      DeveloperPanelsMetaData registry = qInstance == null ? null : DeveloperPanelsMetaData.of(qInstance);
      if(registry != null)
      {
         for(QCodeReference codeReference : registry.getProviders())
         {
            try
            {
               providers.add(QCodeLoader.getAdHoc(DeveloperPanelProviderInterface.class, codeReference));
            }
            catch(Exception e)
            {
               LOG.warn("Could not load a developer panel provider", e, logPair("provider", codeReference.getName()));
            }
         }
      }

      return (providers);
   }



   /*******************************************************************************
    ** Ask one provider for its panel: null when it is not available; a panel
    ** with an error message when it fails.
    *******************************************************************************/
   private DeveloperPanel buildPanel(DeveloperPanelProviderInterface provider, DeveloperPanelContext context)
   {
      try
      {
         if(!provider.isAvailable(context))
         {
            return (null);
         }

         DeveloperPanel panel = provider.build(context);
         if(panel == null)
         {
            return (null);
         }

         return (withProviderDefaults(panel, provider));
      }
      catch(Exception e)
      {
         LOG.warn("Error building a developer panel", e, logPair("provider", provider.getId()), logPair("table", context.getTableName()));
         String message = e instanceof QUserFacingException ? e.getMessage() : "This panel could not be built - see the server log for details.";
         return (withProviderDefaults(new DeveloperPanel().withError(message), provider));
      }
   }



   /*******************************************************************************
    ** Fill the panel's id, title, order and icon from its provider where unset.
    *******************************************************************************/
   private DeveloperPanel withProviderDefaults(DeveloperPanel panel, DeveloperPanelProviderInterface provider)
   {
      if(panel.getId() == null)
      {
         panel.setId(provider.getId());
      }
      if(panel.getTitle() == null)
      {
         panel.setTitle(provider.getTitle());
      }
      if(panel.getOrder() == null)
      {
         panel.setOrder(Objects.requireNonNullElse(provider.getOrder(), DEFAULT_ORDER));
      }
      if(panel.getIcon() == null)
      {
         panel.setIcon(provider.getIcon());
      }
      return (panel);
   }

}
