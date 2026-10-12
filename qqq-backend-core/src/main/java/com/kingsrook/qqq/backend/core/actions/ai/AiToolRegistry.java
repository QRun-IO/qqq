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

package com.kingsrook.qqq.backend.core.actions.ai;


import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import com.kingsrook.qqq.backend.core.actions.ai.tools.AbstractAiTool;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QSupplementalInstanceMetaData;


/*******************************************************************************
 ** Tools contributed to the AI tool kernel by qbits and applications.
 **
 ** A qbit registers typed tools here while it produces its meta-data; every
 ** AiToolKernel built over the instance afterwards offers them next to the
 ** built-in tools, and runs them through the same path: the caller's session
 ** in QContext, strict input-key validation, the untrusted-data envelope,
 ** redaction, outcome mapping (DENIED / NOT_FOUND / INVALID_INPUT / ERROR) and
 ** the tool's own use of AiToolContext.prepareInput (core permission
 ** enforcement and record security locks).
 **
 ** Only READ and NAVIGATION tools may be registered.  The kernel has no
 ** approval flow, so a WRITE_PROPOSAL tool is refused: a qbit that lets a model
 ** propose changes runs those tools in its own, approval-aware runtime.
 **
 ** Contributed tool names must not collide with the built-in tools or each
 ** other; registration refuses a duplicate, and the instance validator reports
 ** a collision with a built-in name.
 **
 ** Not published to frontends: the v1 meta-data response only includes
 ** supplemental instance meta-data it knows, and this class exposes no
 ** frontend settings.
 *******************************************************************************/
public class AiToolRegistry implements QSupplementalInstanceMetaData
{
   public static final String NAME = "aiToolRegistry";

   private static final Pattern TOOL_NAME_PATTERN = Pattern.compile("^[a-z][a-z0-9_]{0,63}$");

   private final Map<String, AbstractAiTool> tools = new TreeMap<>();



   /*******************************************************************************
    ** The registry of an instance, or null if nothing was registered.
    *******************************************************************************/
   public static AiToolRegistry of(QInstance qInstance)
   {
      return (QSupplementalInstanceMetaData.of(qInstance, NAME));
   }



   /*******************************************************************************
    ** The registry of an instance, creating (and adding) it if needed.
    *******************************************************************************/
   public static AiToolRegistry ofOrWithNew(QInstance qInstance)
   {
      return (QSupplementalInstanceMetaData.ofOrWithNew(qInstance, NAME, AiToolRegistry::new));
   }



   /*******************************************************************************
    ** Register a tool.  Throws IllegalArgumentException for a null tool, a name
    ** that isn't lower_snake_case (at most 64 characters), a duplicate name, or
    ** a kind other than READ or NAVIGATION.
    *******************************************************************************/
   public AiToolRegistry withTool(AbstractAiTool tool)
   {
      if(tool == null)
      {
         throw (new IllegalArgumentException("An AI tool may not be null."));
      }

      String name = tool.getName();
      if(name == null || !TOOL_NAME_PATTERN.matcher(name).matches())
      {
         throw (new IllegalArgumentException("AI tool name [" + name + "] must be lower_snake_case, starting with a letter, at most 64 characters."));
      }

      AiToolKind kind = tool.getKind();
      if(kind != AiToolKind.READ && kind != AiToolKind.NAVIGATION)
      {
         throw (new IllegalArgumentException("AI tool [" + name + "] has kind [" + kind + "]; only READ and NAVIGATION tools may be registered with the kernel."));
      }

      if(tools.containsKey(name))
      {
         throw (new IllegalArgumentException("An AI tool named [" + name + "] is already registered."));
      }

      tools.put(name, tool);
      return (this);
   }



   /*******************************************************************************
    ** The registered tools, by name (sorted, unmodifiable).
    *******************************************************************************/
   public Map<String, AbstractAiTool> getTools()
   {
      return (Collections.unmodifiableMap(tools));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String getName()
   {
      return (NAME);
   }



   /*******************************************************************************
    ** A contributed tool may not take a built-in tool's name.
    *******************************************************************************/
   @Override
   public void validate(QInstance qInstance, QInstanceValidator validator)
   {
      Set<String> builtIns = AiToolKernel.getBuiltInToolNames();
      for(String name : tools.keySet())
      {
         validator.assertCondition(!builtIns.contains(name), "AI tool [" + name + "] in the " + NAME + " has the name of a built-in AI tool kernel tool.");
      }
   }

}
