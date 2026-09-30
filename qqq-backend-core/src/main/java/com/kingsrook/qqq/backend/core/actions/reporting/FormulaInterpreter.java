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

package com.kingsrook.qqq.backend.core.actions.reporting;


import java.io.Serializable;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import com.kingsrook.qqq.backend.core.exceptions.QFormulaException;
import com.kingsrook.qqq.backend.core.exceptions.QValueException;
import com.kingsrook.qqq.backend.core.instances.QMetaDataVariableInterpreter;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/*******************************************************************************
 ** Helper for Generating reports - to interpret formulas in report columns,
 ** that are in "excel-style", ala: =MINUS(47,42) or
 ** =IF(LT(ADD(${input.x},${input.y}),10,Yes,No)
 *******************************************************************************/
public class FormulaInterpreter
{

   /*******************************************************************************
    ** public method to interpret a formula.  Takes a variableInterpreter, optionally
    ** full of maps of variables, and the formula string, assumed to have its leading
    ** '=' char already trimmed away.
    *******************************************************************************/
   public static Serializable interpretFormula(QMetaDataVariableInterpreter variableInterpreter, String formula) throws QFormulaException
   {
      try
      {
         List<Serializable> results = interpretFormula(variableInterpreter, formula, new AtomicInteger(0));
         if(results.size() == 1)
         {
            return (results.get(0));
         }
         else if(results.isEmpty())
         {
            throw (new QFormulaException("No results from formula"));
         }
         else
         {
            throw (new QFormulaException("More than 1 result from formula"));
         }
      }
      catch(Exception e)
      {
         throw (new QFormulaException("Error interpreting formula [" + formula + "]", e));
      }
   }



   /*******************************************************************************
    ** Recursive method that does the work of interpreting a formula.
    ** Uses AtomicInteger `i` to track index through the string into and out of
    ** recursive calls.
    *******************************************************************************/
   static List<Serializable> interpretFormula(QMetaDataVariableInterpreter variableInterpreter, String formula, AtomicInteger i) throws QFormulaException
   {
      StringBuilder      token  = new StringBuilder();
      List<Serializable> result = new ArrayList<>();

      char previousChar = 0;
      while(i.get() < formula.length())
      {
         if(i.get() > 0)
         {
            previousChar = formula.charAt(i.get() - 1);
         }
         char c = formula.charAt(i.getAndIncrement());
         if(c == '(' && i.get() < formula.length() - 1)
         {
            //////////////////////////////////////////////////////////////////////////////////////////
            // open paren means:  go into a sub-parse.  Get back a list of arguments, and use those //
            // as arguments for the current token, which must be a function name then.              //
            //////////////////////////////////////////////////////////////////////////////////////////
            List<Serializable> args     = interpretFormula(variableInterpreter, formula, i);
            Serializable       evaluate = evaluate(token.toString(), args, variableInterpreter);
            result.add(evaluate);
         }
         else if(c == ')')
         {
            //////////////////////////////////////////////////////////////////////////
            // close paren means:  end this sub-parse.  evaluate the current token, //
            // add it to the result list, and return the result list.               //
            // unless we just closed a paren - then we can just return.            //
            //////////////////////////////////////////////////////////////////////////
            if(previousChar != ')')
            {
               Serializable evaluate = evaluate(token.toString(), Collections.emptyList(), variableInterpreter);
               result.add(evaluate);
            }
            return (result);
         }
         else if(c == ',')
         {
            /////////////////////////////////////////////////////////////////////////
            // comma means:  evaluate the current token; add it to the result list //
            // unless we just closed a paren - then we can just return.            //
            /////////////////////////////////////////////////////////////////////////
            if(previousChar != ')')
            {
               Serializable evaluate = evaluate(token.toString(), Collections.emptyList(), variableInterpreter);
               result.add(evaluate);
            }
            token = new StringBuilder();
         }
         else
         {
            /////////////////////////////////////////////////
            // else, we add this char to the current token //
            /////////////////////////////////////////////////
            token.append(c);
         }
      }

      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // if we haven't found a result yet, assume we have just a literal, not a function call, and evaluate as such //
      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      if(result.isEmpty())
      {
         if(!token.isEmpty())
         {
            Serializable evaluate = evaluate(token.toString(), Collections.emptyList(), variableInterpreter);
            result.add(evaluate);
         }
      }

      return (result);
   }



   /*******************************************************************************
    ** Evaluate a token - maybe a literal, or variable, or function name -
    ** with arguments if it's a function, and in the context of the variableInterpreter.
    *******************************************************************************/
   private static Serializable evaluate(String token, List<Serializable> args, QMetaDataVariableInterpreter variableInterpreter) throws QFormulaException
   {
      switch(token)
      {
         case "ADD":
         {
            List<BigDecimal> numbers = getNumberArgumentList(args, 2, variableInterpreter);
            return nullIfAnyNullArgsElseBigDecimal(numbers, () -> numbers.get(0).add(numbers.get(1)));
         }
         case "MINUS":
         {
            List<BigDecimal> numbers = getNumberArgumentList(args, 2, variableInterpreter);
            return nullIfAnyNullArgsElseBigDecimal(numbers, () -> numbers.get(0).subtract(numbers.get(1)));
         }
         case "MULTIPLY":
         {
            List<BigDecimal> numbers = getNumberArgumentList(args, 2, variableInterpreter);
            return nullIfAnyNullArgsElseBigDecimal(numbers, () -> numbers.get(0).multiply(numbers.get(1)));
         }
         case "DIVIDE":
         {
            List<BigDecimal> numbers = getNumberArgumentList(args, 2, variableInterpreter);
            if(numbers.get(1) == null || numbers.get(1).compareTo(BigDecimal.ZERO) == 0)
            {
               return null;
            }
            return nullIfAnyNullArgsElseBigDecimal(numbers, () -> numbers.get(0).divide(numbers.get(1), 4, RoundingMode.HALF_UP));
         }
         case "DIVIDE_SCALE":
         {
            List<BigDecimal> numbers = getNumberArgumentList(args, 3, variableInterpreter);
            if(numbers.get(1) == null || numbers.get(1).compareTo(BigDecimal.ZERO) == 0)
            {
               return null;
            }
            return nullIfAnyNullArgsElseBigDecimal(numbers, () -> numbers.get(0).divide(numbers.get(1), numbers.get(2).intValue(), RoundingMode.HALF_UP));
         }
         case "ROUND":
         {
            List<BigDecimal> numbers = getNumberArgumentList(args, 2, variableInterpreter);
            return nullIfAnyNullArgsElseBigDecimal(numbers, () -> numbers.get(0).round(new MathContext(numbers.get(1).intValue())));
         }
         case "SCALE":
         {
            List<BigDecimal> numbers = getNumberArgumentList(args, 2, variableInterpreter);
            return nullIfAnyNullArgsElseBigDecimal(numbers, () -> numbers.get(0).setScale(numbers.get(1).intValue(), RoundingMode.HALF_UP));
         }
         case "NVL":
         {
            List<BigDecimal> numbers = getNumberArgumentList(args, 2, variableInterpreter);
            return Objects.requireNonNullElse(numbers.get(0), numbers.get(1));
         }
         case "IF":
         {
            ///////////////////////////////////////////////////////////////////////////////////////
            // IF(CONDITION,TRUE,ELSE)                                                           //
            // behavior in a spreadsheet appears to be:                                          //
            // booleans are evaluated naturally.                                                 //
            // strings - if they look like 'true' or 'false, they are evaluated, else they error //
            // numbers - 0 is false, all else are true.                                          //
            ///////////////////////////////////////////////////////////////////////////////////////
            List<Serializable> actualArgs = getArgumentList(args, 3, variableInterpreter);
            Serializable       condition  = actualArgs.get(0);
            boolean            conditionBoolean;
            if(condition == null)
            {
               conditionBoolean = false;
            }
            else if(condition instanceof Boolean b)
            {
               conditionBoolean = b;
            }
            else if(condition instanceof BigDecimal bd)
            {
               conditionBoolean = (bd.compareTo(BigDecimal.ZERO) != 0);
            }
            else if(condition instanceof String s)
            {
               if("true".equalsIgnoreCase(s))
               {
                  conditionBoolean = true;
               }
               else if("false".equalsIgnoreCase(s))
               {
                  conditionBoolean = false;
               }
               else
               {
                  throw (new QFormulaException("Could not evaluate string '" + s + "' as a boolean."));
               }
            }
            else
            {
               conditionBoolean = false;
            }

            return conditionBoolean ? actualArgs.get(1) : actualArgs.get(2);
         }
         case "LT":
         {
            List<BigDecimal> numbers = getNumberArgumentList(args, 2, variableInterpreter);
            return nullIfAnyNullArgsElseBoolean(numbers, () -> numbers.get(0).compareTo(numbers.get(1)) < 0);
         }
         case "LTE":
         {
            List<BigDecimal> numbers = getNumberArgumentList(args, 2, variableInterpreter);
            return nullIfAnyNullArgsElseBoolean(numbers, () -> numbers.get(0).compareTo(numbers.get(1)) <= 0);
         }
         case "GT":
         {
            List<BigDecimal> numbers = getNumberArgumentList(args, 2, variableInterpreter);
            return nullIfAnyNullArgsElseBoolean(numbers, () -> numbers.get(0).compareTo(numbers.get(1)) > 0);
         }
         case "GTE":
         {
            List<BigDecimal> numbers = getNumberArgumentList(args, 2, variableInterpreter);
            return nullIfAnyNullArgsElseBoolean(numbers, () -> numbers.get(0).compareTo(numbers.get(1)) >= 0);
         }
         default:
         {
            ////////////////////////////////////////////////////////////////////////////////////////
            // if there aren't arguments, then we can try to evaluate the thing not as a function //
            ////////////////////////////////////////////////////////////////////////////////////////
            if(CollectionUtils.nullSafeIsEmpty(args))
            {
               try
               {
                  return (ValueUtils.getValueAsBigDecimal(token));
               }
               catch(Exception e)
               {
                  // continue
               }

               try
               {
                  return (variableInterpreter.interpret(token));
               }
               catch(Exception e)
               {
                  // continue
               }
            }
         }
      }

      throw (new QFormulaException("Unable to evaluate unrecognized expression: " + token + ""));
   }



   /*******************************************************************************
    ** if any number in the list is null, get back null - else, return the result of the supplier.
    *******************************************************************************/
   private static Serializable nullIfAnyNullArgsElseBigDecimal(List<BigDecimal> numbers, Supplier<BigDecimal> supplier)
   {
      if(numbers.stream().anyMatch(Objects::isNull))
      {
         return (null);
      }
      return supplier.get();
   }



   /*******************************************************************************
    ** if any number in the list is null, get back null - else, return the result of the supplier.
    *******************************************************************************/
   private static Serializable nullIfAnyNullArgsElseBoolean(List<BigDecimal> numbers, Supplier<Boolean> supplier)
   {
      if(numbers.stream().anyMatch(Objects::isNull))
      {
         return (null);
      }
      return supplier.get();
   }



   /*******************************************************************************
    ** given a list of arguments, get back a specific number of arguments, all of which we
    ** validate to be numbers (e.g., possibly interpreted variables) - else we throw.
    ** also throw if not the right number is present.
    *******************************************************************************/
   private static List<BigDecimal> getNumberArgumentList(List<Serializable> originalArgs, Integer howMany, QMetaDataVariableInterpreter variableInterpreter) throws QFormulaException
   {
      if(howMany != null)
      {
         if(!howMany.equals(originalArgs.size()))
         {
            throw (new QFormulaException("Wrong number of arguments (required: " + howMany + ", received: " + originalArgs.size() + ")"));
         }
      }

      List<BigDecimal> rs = new ArrayList<>();
      for(Serializable originalArg : originalArgs)
      {
         try
         {
            Serializable interpretedArg = variableInterpreter.interpretForObject(ValueUtils.getValueAsString(originalArg), null);
            rs.add(ValueUtils.getValueAsBigDecimal(interpretedArg));
         }
         catch(QValueException e)
         {
            throw (new QFormulaException("Could not process [" + originalArg + "] as a number"));
         }
      }
      return (rs);
   }



   /*******************************************************************************
    ** given a list of arguments, get back a specific number of arguments, all of which we
    ** get interpreted.  throw if not the right number of args is present.
    *******************************************************************************/
   private static List<Serializable> getArgumentList(List<Serializable> originalArgs, Integer howMany, QMetaDataVariableInterpreter variableInterpreter) throws QFormulaException
   {
      if(howMany != null)
      {
         if(!howMany.equals(originalArgs.size()))
         {
            throw (new QFormulaException("Wrong number of arguments (required: " + howMany + ", received: " + originalArgs.size() + ")"));
         }
      }

      List<Serializable> rs = new ArrayList<>();
      for(Serializable originalArg : originalArgs)
      {
         Serializable interpretedArg = variableInterpreter.interpretForObject(ValueUtils.getValueAsString(originalArg), null);
         rs.add(interpretedArg);
      }
      return (rs);
   }

}
