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

package com.kingsrook.qqq.backend.core.actions.templates;


import java.io.StringWriter;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.AbstractQActionFunction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.templates.RenderTemplateInput;
import com.kingsrook.qqq.backend.core.model.actions.templates.RenderTemplateOutput;
import com.kingsrook.qqq.backend.core.model.templates.TemplateType;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.event.EventCartridge;
import org.apache.velocity.app.event.MethodExceptionEventHandler;
import org.apache.velocity.context.Context;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Basic action to render a template!
 **
 ** hard-coded built to only assume Velocity right now.  could expand (and refactor) in future.
 *******************************************************************************/
public class RenderTemplateAction extends AbstractQActionFunction<RenderTemplateInput, RenderTemplateOutput>
{
   private static final QLogger LOG = QLogger.getLogger(RenderTemplateAction.class);



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public RenderTemplateOutput execute(RenderTemplateInput input) throws QException
   {
      RenderTemplateOutput output = new RenderTemplateOutput();

      if(TemplateType.VELOCITY.equals(input.getTemplateType()))
      {
         Velocity.init();
         Context context = new VelocityContext();

         if(input.getContext() != null)
         {
            for(Map.Entry<String, ?> entry : input.getContext().entrySet())
            {
               context.put(entry.getKey(), entry.getValue());
            }
         }

         setupEventHandlers(context);

         StringWriter stringWriter = new StringWriter();
         Velocity.evaluate(context, stringWriter, StringUtils.hasContent(input.getTemplateIdentifier()) ? input.getTemplateIdentifier() : "anonymous", input.getCode());
         output.setResult(stringWriter.getBuffer().toString());
      }
      else
      {
         throw (new QException("Unsupported Template Type: " + input.getTemplateType()));
      }

      return (output);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void setupEventHandlers(Context context)
   {
      EventCartridge eventCartridge = new EventCartridge();
      eventCartridge.addEventHandler((MethodExceptionEventHandler) (ctx, aClass, method, exception, info) ->
      {
         LOG.info("Exception in velocity template", exception, logPair("at", info.toString()));
         return (null);
      });
      eventCartridge.attachToContext(context);
   }



   /*******************************************************************************
    ** Most convenient static wrapper to render a Velocity template.
    *******************************************************************************/
   public static String renderVelocity(Map<String, Object> context, String code) throws QException
   {
      return (render(TemplateType.VELOCITY, context, code));
   }



   /*******************************************************************************
    ** Convenient static wrapper to render a template of an arbitrary type (language).
    *******************************************************************************/
   public static String render(TemplateType templateType, Map<String, Object> context, String code) throws QException
   {
      RenderTemplateInput renderTemplateInput = new RenderTemplateInput();
      renderTemplateInput.setCode(code);
      renderTemplateInput.setContext(context);
      renderTemplateInput.setTemplateType(templateType);
      RenderTemplateOutput output = new RenderTemplateAction().execute(renderTemplateInput);
      return (output.getResult());
   }

}
