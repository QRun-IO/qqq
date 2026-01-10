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

package com.kingsrook.qqq.backend.core.actions.processes;


import java.io.Serializable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import com.kingsrook.qqq.backend.core.actions.ActionHelper;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.NoCodeWidgetRenderer;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessState;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.NoCodeWidgetFrontendComponentMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendComponentMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QStateMachineStep;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QStepMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.processes.implementations.basepull.BasepullConfiguration;
import com.kingsrook.qqq.backend.core.processes.tracing.ProcessTracerInterface;
import com.kingsrook.qqq.backend.core.state.InMemoryStateProvider;
import com.kingsrook.qqq.backend.core.state.StateProviderInterface;
import com.kingsrook.qqq.backend.core.state.StateType;
import com.kingsrook.qqq.backend.core.state.UUIDAndTypeStateKey;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import org.apache.commons.lang3.BooleanUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Action handler for running q-processes (which are a sequence of q-steps).
 *
 *******************************************************************************/
public class RunProcessAction
{
   private static final QLogger LOG = QLogger.getLogger(RunProcessAction.class);

   public static final String BASEPULL_KEY_VALUE        = "basepullKeyValue";
   public static final String BASEPULL_THIS_RUNTIME_KEY = "basepullThisRuntimeKey";
   public static final String BASEPULL_LAST_RUNTIME_KEY = "basepullLastRuntimeKey";
   public static final String BASEPULL_TIMESTAMP_FIELD  = "basepullTimestampField";
   public static final String BASEPULL_CONFIGURATION    = "basepullConfiguration";

   public static final String PROCESS_TRACER_CODE_REFERENCE_FIELD = "processTracerCodeReference";

   ////////////////////////////////////////////////////////////////////////////////////////////////
   // indicator that the timestamp field should be updated - e.g., the execute step is finished. //
   ////////////////////////////////////////////////////////////////////////////////////////////////
   public static final String BASEPULL_READY_TO_UPDATE_TIMESTAMP_FIELD = "basepullReadyToUpdateTimestamp";
   public static final String BASEPULL_DID_QUERY_USING_TIMESTAMP_FIELD = "basepullDidQueryUsingTimestamp";

   private ProcessTracerInterface processTracer;



   /*******************************************************************************
    **
    *******************************************************************************/
   public RunProcessOutput execute(RunProcessInput runProcessInput) throws QException
   {
      ActionHelper.validateSession(runProcessInput);

      QProcessMetaData process = QContext.getQInstance().getProcess(runProcessInput.getProcessName());
      if(process == null)
      {
         throw new QException("Process [" + runProcessInput.getProcessName() + "] is not defined in this instance.");
      }

      RunProcessOutput runProcessOutput = new RunProcessOutput();

      //////////////////////////////////////////////////////////
      // generate a UUID for the process, if one wasn't given //
      //////////////////////////////////////////////////////////
      if(runProcessInput.getProcessUUID() == null)
      {
         runProcessInput.setProcessUUID(UUID.randomUUID().toString());
      }
      runProcessOutput.setProcessUUID(runProcessInput.getProcessUUID());

      traceStartOrResume(runProcessInput, process);

      UUIDAndTypeStateKey stateKey     = new UUIDAndTypeStateKey(UUID.fromString(runProcessInput.getProcessUUID()), StateType.PROCESS_STATUS);
      ProcessState        processState = primeProcessState(runProcessInput, stateKey, process);

      /////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // these should always be clear when we're starting a run - so make sure they haven't leaked from previous //
      /////////////////////////////////////////////////////////////////////////////////////////////////////////////
      processState.clearNextStepName();
      processState.clearBackStepName();

      /////////////////////////////////////////////////////////
      // if process is 'basepull' style, keep track of 'now' //
      /////////////////////////////////////////////////////////
      BasepullConfiguration basepullConfiguration = process.getBasepullConfiguration();
      if(basepullConfiguration != null)
      {
         ///////////////////////////////////////
         // get the stored basepull timestamp //
         ///////////////////////////////////////
         persistLastRunTime(runProcessInput, process, basepullConfiguration);
      }

      try
      {
         switch(Objects.requireNonNull(process.getStepFlow(), "Process [" + process.getName() + "] has a null stepFlow."))
         {
            case LINEAR -> runLinearStepLoop(process, processState, stateKey, runProcessInput, runProcessOutput);
            case STATE_MACHINE -> runStateMachineStep(runProcessInput.getStartAfterStep(), process, processState, stateKey, runProcessInput, runProcessOutput, 0);
            default -> throw (new QException("Unhandled process step flow: " + process.getStepFlow()));
         }

         ///////////////////////////////////////////////////////////////////////////
         // if 'basepull' style process, update the stored basepull timestamp     //
         // but only when we've been signaled to do so - i.e., only if we did our //
         // query using the timestamp field, and only after an Execute step runs. //
         ///////////////////////////////////////////////////////////////////////////
         if(basepullConfiguration != null
            && BooleanUtils.isTrue(ValueUtils.getValueAsBoolean(runProcessInput.getValue(BASEPULL_DID_QUERY_USING_TIMESTAMP_FIELD)))
            && BooleanUtils.isTrue(ValueUtils.getValueAsBoolean(runProcessInput.getValue(BASEPULL_READY_TO_UPDATE_TIMESTAMP_FIELD))))
         {
            storeLastRunTime(runProcessInput, process, basepullConfiguration);
         }
      }
      catch(QException qe)
      {
         ////////////////////////////////////////////////////////////
         // upon exception (e.g., one thrown by a step), throw it. //
         ////////////////////////////////////////////////////////////
         traceBreakOrFinish(runProcessInput, runProcessOutput, qe);
         throw (qe);
      }
      catch(Exception e)
      {
         ////////////////////////////////////////////////////////////
         // upon exception (e.g., one thrown by a step), throw it. //
         ////////////////////////////////////////////////////////////
         traceBreakOrFinish(runProcessInput, runProcessOutput, e);
         throw (new QException("Error running process", e));
      }
      finally
      {
         //////////////////////////////////////////////////////
         // always put the final state in the process result //
         //////////////////////////////////////////////////////
         runProcessOutput.setProcessState(processState);
      }

      traceBreakOrFinish(runProcessInput, runProcessOutput, null);

      return (runProcessOutput);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private void runLinearStepLoop(QProcessMetaData process, ProcessState processState, UUIDAndTypeStateKey stateKey, RunProcessInput runProcessInput, RunProcessOutput runProcessOutput) throws Exception
   {
      String lastStepName = runProcessInput.getStartAfterStep();
      String startAtStep  = runProcessInput.getStartAtStep();

      while(true)
      {
         ///////////////////////////////////////////////////////////////////////////////////////////////////////
         // always refresh the step list - as any step that runs can modify it (in the process state).        //
         // this is why we don't do a loop over the step list - as we'd get ConcurrentModificationExceptions. //
         // deal with if we were told, from the input, to start After a step, or start At a step.             //
         ///////////////////////////////////////////////////////////////////////////////////////////////////////
         List<QStepMetaData> stepList;
         if(startAtStep == null)
         {
            stepList = getAvailableStepList(processState, process, lastStepName, false);
         }
         else
         {
            stepList = getAvailableStepList(processState, process, startAtStep, true);

            ///////////////////////////////////////////////////////////////////////////////////
            // clear this field - so after we run a step, we'll then loop in last-step mode. //
            ///////////////////////////////////////////////////////////////////////////////////
            startAtStep = null;

            ///////////////////////////////////////////////////////////////////////////////////
            // if we're going to run a backend step now, let it see that this is a step-back //
            ///////////////////////////////////////////////////////////////////////////////////
            processState.setIsStepBack(true);
         }

         if(stepList.isEmpty())
         {
            break;
         }

         QStepMetaData step = stepList.get(0);
         lastStepName = step.getName();

         if(step instanceof QFrontendStepMetaData frontendStep)
         {
            LoopTodo loopTodo = prepareForFrontendStep(runProcessInput, process, frontendStep, processState);
            if(loopTodo == LoopTodo.BREAK)
            {
               break;
            }
         }
         else if(step instanceof QBackendStepMetaData backendStepMetaData)
         {
            RunBackendStepOutput runBackendStepOutput = runBackendStep(process, processState, stateKey, runProcessInput, runProcessOutput, backendStepMetaData, step);

            /////////////////////////////////////////////////////////////////////////////////////////
            // if the step returned an override lastStepName, use that to determine how we proceed //
            /////////////////////////////////////////////////////////////////////////////////////////
            if(runBackendStepOutput.getOverrideLastStepName() != null)
            {
               LOG.debug("Process step [" + lastStepName + "] returned an overrideLastStepName [" + runBackendStepOutput.getOverrideLastStepName() + "]!");
               lastStepName = runBackendStepOutput.getOverrideLastStepName();
            }
         }
         else
         {
            //////////////////////////////////////////////////
            // in case we have a different step type, throw //
            //////////////////////////////////////////////////
            throw (new QException("Unsure how to run a step of type: " + step.getClass().getName()));
         }

         ////////////////////////////////////////////////////////////////////////////////////////
         // only let this value be set for the original back step - don't let it stick around. //
         // if a process wants to keep track of this itself, it can, but in a different slot.  //
         ////////////////////////////////////////////////////////////////////////////////////////
         processState.setIsStepBack(false);
      }

      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // in case we broke from the loop above (e.g., by going directly into a frontend step), once again make sure to lower this flag. //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      processState.setIsStepBack(false);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private enum LoopTodo
   {
      BREAK,
      CONTINUE
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private LoopTodo prepareForFrontendStep(RunProcessInput runProcessInput, QProcessMetaData process, QFrontendStepMetaData step, ProcessState processState) throws QException
   {
      ////////////////////////////////////////////////////////////////
      // Handle what to do with frontend steps, per request setting //
      ////////////////////////////////////////////////////////////////
      switch(runProcessInput.getFrontendStepBehavior())
      {
         case BREAK ->
         {
            LOG.trace("Breaking process [" + process.getName() + "] at frontend step (as requested by caller): " + step.getName());
            processFrontendStepFieldDefaultValues(processState, step);
            processFrontendComponents(processState, step);
            processState.setNextStepName(step.getName());

            if(StringUtils.hasContent(step.getBackStepName()) && processState.getBackStepName().isEmpty())
            {
               processState.setBackStepName(step.getBackStepName());
            }

            return LoopTodo.BREAK;
         }
         case SKIP ->
         {
            LOG.trace("Skipping frontend step [" + step.getName() + "] in process [" + process.getName() + "] (as requested by caller)");
            return LoopTodo.CONTINUE;
         }
         case FAIL ->
         {
            LOG.trace("Throwing error for frontend step [" + step.getName() + "] in process [" + process.getName() + "] (as requested by caller)");
            throw (new QException("Failing process at step " + step.getName() + " (as requested, to fail on frontend steps)"));
         }
         default -> throw new IllegalStateException("Unexpected value: " + runProcessInput.getFrontendStepBehavior());
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private void runStateMachineStep(String lastStepName, QProcessMetaData process, ProcessState processState, UUIDAndTypeStateKey stateKey, RunProcessInput runProcessInput, RunProcessOutput runProcessOutput, int stackDepth) throws Exception
   {
      //////////////////////////////
      // check for stack-overflow //
      //////////////////////////////
      Integer maxStateMachineProcessStepFlowStackDepth = Objects.requireNonNullElse(runProcessInput.getValueInteger("maxStateMachineProcessStepFlowStackDepth"), 20);
      if(stackDepth > maxStateMachineProcessStepFlowStackDepth)
      {
         throw (new QException("StateMachine process recurred too many times (exceeded maxStateMachineProcessStepFlowStackDepth of " + maxStateMachineProcessStepFlowStackDepth + ")"));
      }

      //////////////////////////////////
      // figure out what step to run: //
      //////////////////////////////////
      QStepMetaData step = null;
      if(!StringUtils.hasContent(lastStepName))
      {
         ////////////////////////////////////////////////////////////////////
         // if no lastStepName is given, start at the process's first step //
         ////////////////////////////////////////////////////////////////////
         if(CollectionUtils.nullSafeIsEmpty(process.getStepList()))
         {
            throw (new QException("Process [" + process.getName() + "] does not have a step list defined."));
         }
         step = process.getStepList().get(0);
      }
      else
      {
         /////////////////////////////////////
         // else run the given lastStepName //
         /////////////////////////////////////
         processState.clearNextStepName();
         processState.clearBackStepName();
         step = process.getStep(lastStepName);
         if(step == null)
         {
            throw (new QException("Could not find step by name [" + lastStepName + "]"));
         }
      }

      /////////////////////////////////////////////////////////////////////////
      // for the flow of:                                                    //
      // we were on a frontend step (as a sub-step of a state machine step), //
      // and now we're here to run that state-step's backend step -          //
      // find the state-machine step containing this frontend step.          //
      /////////////////////////////////////////////////////////////////////////
      String skipSubStepsUntil = null;
      if(step instanceof QFrontendStepMetaData frontendStepMetaData)
      {
         QStateMachineStep stateMachineStep = getStateMachineStepContainingSubStep(process, frontendStepMetaData.getName());
         if(stateMachineStep == null)
         {
            throw (new QException("Could not find stateMachineStep that contains last-frontend step: " + frontendStepMetaData.getName()));
         }
         step = stateMachineStep;

         //////////////////////////////////////////////////////////////////////////////////
         // set this flag, to know to skip this frontend step in the sub-step loop below //
         //////////////////////////////////////////////////////////////////////////////////
         skipSubStepsUntil = frontendStepMetaData.getName();
      }

      if(!(step instanceof QStateMachineStep stateMachineStep))
      {
         throw (new QException("Have a non-stateMachineStep in a process using stateMachine flow... " + step.getClass().getName()));
      }

      ///////////////////////
      // run the sub-steps //
      ///////////////////////
      boolean ranAnySubSteps = false;
      for(QStepMetaData subStep : stateMachineStep.getSubSteps())
      {
         ///////////////////////////////////////////////////////////////////////////////////////////////
         // ok, well, skip them if this flag is set (and clear the flag once we've hit this sub-step) //
         ///////////////////////////////////////////////////////////////////////////////////////////////
         if(skipSubStepsUntil != null)
         {
            if(skipSubStepsUntil.equals(subStep.getName()))
            {
               skipSubStepsUntil = null;
            }
            continue;
         }

         ranAnySubSteps = true;
         if(subStep instanceof QFrontendStepMetaData frontendStep)
         {
            LoopTodo loopTodo = prepareForFrontendStep(runProcessInput, process, frontendStep, processState);
            if(loopTodo == LoopTodo.BREAK)
            {
               return;
            }
         }
         else if(subStep instanceof QBackendStepMetaData backendStepMetaData)
         {
            RunBackendStepOutput runBackendStepOutput = runBackendStep(process, processState, stateKey, runProcessInput, runProcessOutput, backendStepMetaData, step);
            Optional<String>     nextStepName         = runBackendStepOutput.getProcessState().getNextStepName();

            if(nextStepName.isEmpty() && StringUtils.hasContent(stateMachineStep.getDefaultNextStepName()))
            {
               nextStepName = Optional.of(stateMachineStep.getDefaultNextStepName());
            }

            if(nextStepName.isPresent())
            {
               //////////////////////////////////////////////////////////////////////////////////////////////////////
               // if we've been given a next-step-name, go to that step now.                                       //
               // it might be a backend-only stateMachineStep, in which case, we should run that backend step now. //
               // or it might be a frontend-then-backend step, in which case, we want to go to that frontend step. //
               // if we weren't given a next-step-name, then we should stay in the same state - either to finish   //
               // its sub-steps, or, to fall out of the loop and end the process.                                  //
               //////////////////////////////////////////////////////////////////////////////////////////////////////
               processState.clearNextStepName();
               processState.clearBackStepName();
               runStateMachineStep(nextStepName.get(), process, processState, stateKey, runProcessInput, runProcessOutput, stackDepth + 1);
               return;
            }
         }
         else
         {
            //////////////////////////////////////////////////
            // in case we have a different step type, throw //
            //////////////////////////////////////////////////
            throw (new QException("Unsure how to run a step of type: " + step.getClass().getName()));
         }
      }

      if(!ranAnySubSteps)
      {
         if(StringUtils.hasContent(stateMachineStep.getDefaultNextStepName()))
         {
            runStateMachineStep(stateMachineStep.getDefaultNextStepName(), process, processState, stateKey, runProcessInput, runProcessOutput, stackDepth + 1);
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QStateMachineStep getStateMachineStepContainingSubStep(QProcessMetaData process, String stepName)
   {
      for(QStepMetaData step : process.getAllSteps().values())
      {
         if(step instanceof QStateMachineStep stateMachineStep)
         {
            for(QStepMetaData subStep : stateMachineStep.getSubSteps())
            {
               if(subStep.getName().equals(stepName))
               {
                  return (stateMachineStep);
               }
            }
         }
      }

      return (null);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void processFrontendComponents(ProcessState processState, QFrontendStepMetaData frontendStep) throws QException
   {
      for(QFrontendComponentMetaData component : CollectionUtils.nonNullList(frontendStep.getComponents()))
      {
         if(component instanceof NoCodeWidgetFrontendComponentMetaData noCodeWidgetComponent)
         {
            NoCodeWidgetRenderer noCodeWidgetRenderer = new NoCodeWidgetRenderer();
            Map<String, Object>  context              = noCodeWidgetRenderer.initContext(null);
            context.putAll(processState.getValues());
            String html = noCodeWidgetRenderer.renderOutputs(context, noCodeWidgetComponent.getOutputs());
            processState.getValues().put(frontendStep.getName() + ".html", html);
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void processFrontendStepFieldDefaultValues(ProcessState processState, QFrontendStepMetaData step)
   {
      for(QFieldMetaData formField : CollectionUtils.mergeLists(step.getFormFields(), step.getInputFields(), step.getViewFields(), step.getOutputFields()))
      {
         if(formField.getDefaultValue() != null && processState.getValues().get(formField.getName()) == null)
         {
            processState.getValues().put(formField.getName(), formField.getDefaultValue());
         }
      }
   }



   /*******************************************************************************
    ** When we start running a process (or resuming it), get data in the RunProcessRequest
    ** either from the state provider (if they're found, for a resume).
    *******************************************************************************/
   ProcessState primeProcessState(RunProcessInput runProcessInput, UUIDAndTypeStateKey stateKey, QProcessMetaData process) throws QException
   {
      Optional<ProcessState> optionalProcessState = loadState(stateKey);
      if(optionalProcessState.isEmpty())
      {
         if(runProcessInput.getStartAfterStep() == null)
         {
            ///////////////////////////////////////////////////////////////////////////////////
            // This condition (no state in state-provider, and no start-after-step) means    //
            // that we're starting a new process!  Init the process state here, then         //
            // Go ahead and store the state that we have (e.g., w/ initial records & values) //
            ///////////////////////////////////////////////////////////////////////////////////
            ProcessState processState = runProcessInput.getProcessState();
            processState.setStepList(process.getStepList().stream().map(QStepMetaData::getName).toList());
            storeState(stateKey, processState);
            optionalProcessState = Optional.of(processState);
         }
         else
         {
            ////////////////////////////////////////////////////////////////////////////////////////
            // if this isn't the first step, but there's no state, then that's a problem, so fail //
            ////////////////////////////////////////////////////////////////////////////////////////
            throw (new QException("Could not find state for process [" + runProcessInput.getProcessName() + "] [" + stateKey.getUuid() + "] in state provider."));
         }
      }
      else
      {
         //////////////////////////////////////////////////////////////////////////////////////////////////////
         // capture any values that the caller may have supplied in the request, before restoring from state //
         //////////////////////////////////////////////////////////////////////////////////////////////////////
         Map<String, Serializable> valuesFromCaller = runProcessInput.getValues();

         ///////////////////////////////////////////////////
         // if there is a previously stored state, use it //
         ///////////////////////////////////////////////////
         runProcessInput.seedFromProcessState(optionalProcessState.get());

         /////////////////////////////////////////////////////////////////////////////////////////////////////
         // if we're restoring an old state, we can discard a previously stored processMetaDataAdjustment - //
         // it is only needed on the transitional edge from a backend-step to a frontend step, but not      //
         // in the other directly                                                                           //
         /////////////////////////////////////////////////////////////////////////////////////////////////////
         optionalProcessState.get().setProcessMetaDataAdjustment(null);

         ///////////////////////////////////////////////////////////////////////////
         // if there were values from the caller, put those (back) in the request //
         ///////////////////////////////////////////////////////////////////////////
         if(valuesFromCaller != null)
         {
            for(Map.Entry<String, Serializable> entry : valuesFromCaller.entrySet())
            {
               runProcessInput.addValue(entry.getKey(), entry.getValue());
            }
         }
      }

      ProcessState processState = optionalProcessState.get();
      return processState;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private RunBackendStepOutput runBackendStep(QProcessMetaData process, ProcessState processState, UUIDAndTypeStateKey stateKey, RunProcessInput runProcessInput, RunProcessOutput runProcessOutput, QBackendStepMetaData backendStepMetaData, QStepMetaData step) throws Exception
   {
      ///////////////////////
      // Run backend steps //
      ///////////////////////
      LOG.debug("Running backend step [" + step.getName() + "] in process [" + process.getName() + "]");
      RunBackendStepOutput runBackendStepOutput = runBackendStep(runProcessInput, process, runProcessOutput, stateKey, backendStepMetaData, process, processState);

      //////////////////////////////////////////////////////////////////////////////////////////////
      // similarly, if the step produced a processMetaDataAdjustment, propagate that data outward //
      //////////////////////////////////////////////////////////////////////////////////////////////
      if(runBackendStepOutput.getProcessMetaDataAdjustment() != null)
      {
         LOG.debug("Process step [" + step.getName() + "] generated a ProcessMetaDataAdjustment [" + runBackendStepOutput.getProcessMetaDataAdjustment() + "]!");
         runProcessOutput.setProcessMetaDataAdjustment(runBackendStepOutput.getProcessMetaDataAdjustment());
      }

      return runBackendStepOutput;
   }



   /*******************************************************************************
    ** Run a single backend step.
    *******************************************************************************/
   RunBackendStepOutput runBackendStep(RunProcessInput runProcessInput, QProcessMetaData process, RunProcessOutput runProcessOutput, UUIDAndTypeStateKey stateKey, QBackendStepMetaData backendStep, QProcessMetaData qProcessMetaData, ProcessState processState) throws Exception
   {
      RunBackendStepInput runBackendStepInput = new RunBackendStepInput(processState);
      runBackendStepInput.setProcessName(process.getName());
      runBackendStepInput.setStepName(backendStep.getName());
      runBackendStepInput.setCallback(runProcessInput.getCallback());
      runBackendStepInput.setFrontendStepBehavior(runProcessInput.getFrontendStepBehavior());
      runBackendStepInput.setAsyncJobCallback(runProcessInput.getAsyncJobCallback());
      runBackendStepInput.setProcessTracer(processTracer);

      runBackendStepInput.setTableName(process.getTableName());
      if(!StringUtils.hasContent(runBackendStepInput.getTableName()))
      {
         ////////////////////////////////////////////////////////////////
         // help support generic (e.g., not tied-to-a-table) processes //
         ////////////////////////////////////////////////////////////////
         if(runProcessInput.getValue("tableName") != null)
         {
            runBackendStepInput.setTableName(ValueUtils.getValueAsString(runProcessInput.getValue("tableName")));
         }
      }

      ///////////////////////////////////////////////////////////////
      // if 'basepull' values are in the inputs, add to step input //
      ///////////////////////////////////////////////////////////////
      if(runProcessInput.getValues().containsKey(BASEPULL_LAST_RUNTIME_KEY))
      {
         runBackendStepInput.setBasepullLastRunTime((Instant) runProcessInput.getValues().get(BASEPULL_LAST_RUNTIME_KEY));
      }

      traceStepStart(runBackendStepInput);

      RunBackendStepOutput runBackendStepOutput = new RunBackendStepAction().execute(runBackendStepInput);
      storeState(stateKey, runBackendStepOutput.getProcessState());

      traceStepFinish(runBackendStepInput, runBackendStepOutput);

      if(runBackendStepOutput.getException() != null)
      {
         runProcessOutput.setException(runBackendStepOutput.getException());
         throw (runBackendStepOutput.getException());
      }

      return (runBackendStepOutput);
   }



   /*******************************************************************************
    ** Get the list of steps which are eligible to run.
    **
    ** lastStep will be included in the list, or not, based on includeLastStep.
    *******************************************************************************/
   static List<QStepMetaData> getAvailableStepList(ProcessState processState, QProcessMetaData process, String lastStep, boolean includeLastStep) throws QException
   {
      if(lastStep == null)
      {
         ///////////////////////////////////////////////////////////////////////
         // if the caller did not supply a 'lastStep', then use the full list //
         ///////////////////////////////////////////////////////////////////////
         return (stepNamesToSteps(process, processState.getStepList()));
      }
      else
      {
         ////////////////////////////////////////////////////////////////////////////
         // else, loop until the 'lastStep' is found, and return the ones after it //
         ////////////////////////////////////////////////////////////////////////////
         boolean      foundLastStep  = false;
         List<String> validStepNames = new ArrayList<>();

         for(String stepName : processState.getStepList())
         {
            if(foundLastStep)
            {
               validStepNames.add(stepName);
            }

            if(stepName.equals(lastStep))
            {
               foundLastStep = true;
               if(includeLastStep)
               {
                  validStepNames.add(stepName);
               }
            }
         }
         return (stepNamesToSteps(process, validStepNames));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static List<QStepMetaData> stepNamesToSteps(QProcessMetaData process, List<String> stepNames) throws QException
   {
      List<QStepMetaData> result = new ArrayList<>();

      for(String stepName : stepNames)
      {
         QStepMetaData step = process.getStep(stepName);
         if(step == null)
         {
            throw (new QException("Could not find a step named [" + stepName + "] in this process."));
         }
         result.add(step);
      }

      return (result);
   }



   /*******************************************************************************
    ** Load an instance of the appropriate state provider
    **
    *******************************************************************************/
   public static StateProviderInterface getStateProvider()
   {
      // TODO - read this from somewhere in meta data eh?
      return InMemoryStateProvider.getInstance();

      // todo - by using JSON serialization internally, this makes stupidly large payloads and crashes things.
      // return TempFileStateProvider.getInstance();
   }



   /*******************************************************************************
    ** public method to get a process state just by UUID.
    *******************************************************************************/
   public static Optional<ProcessState> getState(String processUUID)
   {
      return (getStateProvider().get(ProcessState.class, new UUIDAndTypeStateKey(UUID.fromString(processUUID), StateType.PROCESS_STATUS)));
   }



   /*******************************************************************************
    ** Store the process state from a function result to the state provider
    **
    *******************************************************************************/
   private void storeState(UUIDAndTypeStateKey stateKey, ProcessState processState)
   {
      getStateProvider().put(stateKey, processState);
   }



   /*******************************************************************************
    ** Load the process state.
    **
    *******************************************************************************/
   private Optional<ProcessState> loadState(UUIDAndTypeStateKey stateKey)
   {
      return (getStateProvider().get(ProcessState.class, stateKey));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   protected String determineBasepullKeyValue(QProcessMetaData process, RunProcessInput runProcessInput, BasepullConfiguration basepullConfiguration) throws QException
   {
      String basepullKeyValue = (basepullConfiguration.getKeyValue() != null) ? basepullConfiguration.getKeyValue() : process.getName();
      if(runProcessInput.getValueString(BASEPULL_KEY_VALUE) != null)
      {
         basepullKeyValue = runProcessInput.getValueString(BASEPULL_KEY_VALUE);
      }

      //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // if process specifies that it uses variants, look for that data in the session and append to our basepull key //
      //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      if(process.getVariantBackend() != null)
      {
         QSession         session         = QContext.getQSession();
         QBackendMetaData backendMetaData = QContext.getQInstance().getBackend(process.getVariantBackend());
         String           variantTypeKey  = backendMetaData.getBackendVariantsConfig().getVariantTypeKey();
         if(session.getBackendVariants() == null || !session.getBackendVariants().containsKey(variantTypeKey))
         {
            LOG.warn("Could not find Backend Variant information for Backend '" + backendMetaData.getName() + "'");
         }
         else
         {
            basepullKeyValue += "-" + session.getBackendVariants().get(variantTypeKey);
         }
      }

      return (basepullKeyValue);
   }



   /*******************************************************************************
    ** Insert or update the last runtime value for this basepull into the backend.
    *******************************************************************************/
   protected void storeLastRunTime(RunProcessInput runProcessInput, QProcessMetaData process, BasepullConfiguration basepullConfiguration) throws QException
   {
      String basepullTableName            = basepullConfiguration.getTableName();
      String basepullKeyFieldName         = basepullConfiguration.getKeyField();
      String basepullLastRunTimeFieldName = basepullConfiguration.getLastRunTimeFieldName();
      String basepullKeyValue             = determineBasepullKeyValue(process, runProcessInput, basepullConfiguration);

      ///////////////////////////////////////
      // get the stored basepull timestamp //
      ///////////////////////////////////////
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(basepullTableName);
      queryInput.setFilter(new QQueryFilter().withCriteria(
         new QFilterCriteria()
            .withFieldName(basepullKeyFieldName)
            .withOperator(QCriteriaOperator.EQUALS)
            .withValues(List.of(basepullKeyValue))));
      QueryOutput queryOutput = new QueryAction().execute(queryInput);

      //////////////////////////////////////////
      // get the runtime for this process run //
      //////////////////////////////////////////
      Instant newRunTime = (Instant) runProcessInput.getValues().get(BASEPULL_THIS_RUNTIME_KEY);

      /////////////////////////////////////////////////
      // update if found, otherwise insert new value //
      /////////////////////////////////////////////////
      if(CollectionUtils.nullSafeHasContents(queryOutput.getRecords()))
      {
         ///////////////////////////////////////////////////////////////////////////////
         // update the basepull table with 'now' (which is before original query ran) //
         ///////////////////////////////////////////////////////////////////////////////
         QRecord basepullRecord = queryOutput.getRecords().get(0);
         basepullRecord.setValue(basepullLastRunTimeFieldName, newRunTime);

         ////////////
         // update //
         ////////////
         UpdateInput updateInput = new UpdateInput();
         updateInput.setTableName(basepullTableName);
         updateInput.setRecords(List.of(basepullRecord));
         new UpdateAction().execute(updateInput);
      }
      else
      {
         QRecord basepullRecord = new QRecord()
            .withValue(basepullKeyFieldName, basepullKeyValue)
            .withValue(basepullLastRunTimeFieldName, newRunTime);

         ////////////////////////////////
         // insert new basepull record //
         ////////////////////////////////
         InsertInput insertInput = new InsertInput();
         insertInput.setTableName(basepullTableName);
         insertInput.setRecords(List.of(basepullRecord));
         new InsertAction().execute(insertInput);
      }
   }



   /*******************************************************************************
    ** Lookup the last runtime for this basepull, and set it (plus now) in the process's
    ** values.
    *******************************************************************************/
   protected void persistLastRunTime(RunProcessInput runProcessInput, QProcessMetaData process, BasepullConfiguration basepullConfiguration) throws QException
   {
      ////////////////////////////////////////////////////////
      // if these values were already computed, don't re-do //
      ////////////////////////////////////////////////////////
      if(runProcessInput.getValue(BASEPULL_THIS_RUNTIME_KEY) != null)
      {
         return;
      }

      /////////////////////////////////////////////////////////////////////////////////////////////////
      // store 'now', which will be used to update basepull record if process completes successfully //
      /////////////////////////////////////////////////////////////////////////////////////////////////
      Instant now = Instant.now();
      runProcessInput.getValues().put(BASEPULL_THIS_RUNTIME_KEY, now);

      String  basepullTableName                    = basepullConfiguration.getTableName();
      String  basepullKeyFieldName                 = basepullConfiguration.getKeyField();
      String  basepullLastRunTimeFieldName         = basepullConfiguration.getLastRunTimeFieldName();
      Integer basepullHoursBackForInitialTimestamp = basepullConfiguration.getHoursBackForInitialTimestamp();
      String  basepullKeyValue                     = determineBasepullKeyValue(process, runProcessInput, basepullConfiguration);

      ///////////////////////////////////////
      // get the stored basepull timestamp //
      ///////////////////////////////////////
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(basepullTableName);
      queryInput.setFilter(new QQueryFilter().withCriteria(
         new QFilterCriteria()
            .withFieldName(basepullKeyFieldName)
            .withOperator(QCriteriaOperator.EQUALS)
            .withValues(List.of(basepullKeyValue))));
      QueryOutput queryOutput = new QueryAction().execute(queryInput);

      ///////////////////////////////////////////////////////////////////////////////////////////////////
      // get the stored time, if not, default to 'now' unless a number of hours to offset was provided //
      ///////////////////////////////////////////////////////////////////////////////////////////////////
      Instant lastRunTime = now;
      if(CollectionUtils.nullSafeHasContents(queryOutput.getRecords()))
      {
         QRecord basepullRecord = queryOutput.getRecords().get(0);
         lastRunTime = ValueUtils.getValueAsInstant(basepullRecord.getValue(basepullLastRunTimeFieldName));
      }
      else if(basepullHoursBackForInitialTimestamp != null)
      {
         lastRunTime = lastRunTime.minus(basepullHoursBackForInitialTimestamp, ChronoUnit.HOURS);
      }

      runProcessInput.getValues().put(BASEPULL_LAST_RUNTIME_KEY, lastRunTime);
      runProcessInput.getValues().put(BASEPULL_TIMESTAMP_FIELD, basepullConfiguration.getTimestampField());
      runProcessInput.getValues().put(BASEPULL_CONFIGURATION, basepullConfiguration);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private void setupProcessTracer(RunProcessInput runProcessInput, QProcessMetaData process)
   {
      try
      {
         if(process.getProcessTracerCodeReference() != null)
         {
            processTracer = QCodeLoader.getAdHoc(ProcessTracerInterface.class, process.getProcessTracerCodeReference());
         }

         Serializable processTracerCodeReference = runProcessInput.getValue(PROCESS_TRACER_CODE_REFERENCE_FIELD);
         if(processTracerCodeReference != null)
         {
            if(processTracerCodeReference instanceof QCodeReference codeReference)
            {
               processTracer = QCodeLoader.getAdHoc(ProcessTracerInterface.class, codeReference);
            }
         }
      }
      catch(Exception e)
      {
         LOG.warn("Error setting up processTracer", e, logPair("processName", runProcessInput.getProcessName()));
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private void traceStartOrResume(RunProcessInput runProcessInput, QProcessMetaData process)
   {
      setupProcessTracer(runProcessInput, process);

      try
      {
         if(processTracer != null)
         {
            if(StringUtils.hasContent(runProcessInput.getStartAfterStep()) || StringUtils.hasContent(runProcessInput.getStartAtStep()))
            {
               processTracer.handleProcessResume(runProcessInput);
            }
            else
            {
               processTracer.handleProcessStart(runProcessInput);
            }
         }
      }
      catch(Exception e)
      {
         LOG.info("Error in traceStart", e, logPair("processName", runProcessInput.getProcessName()));
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private void traceBreakOrFinish(RunProcessInput runProcessInput, RunProcessOutput runProcessOutput, Exception processException)
   {
      try
      {
         if(processTracer != null)
         {
            ProcessState processState = runProcessOutput.getProcessState();
            boolean      isBreak      = true;

            /////////////////////////////////////////////////////////////
            // if there's no next step, that means the process is done //
            /////////////////////////////////////////////////////////////
            if(processState.getNextStepName().isEmpty())
            {
               isBreak = false;
            }
            else
            {
               /////////////////////////////////////////////////////////////////
               // or if the next step is the last index, then we're also done //
               /////////////////////////////////////////////////////////////////
               String nextStepName  = processState.getNextStepName().get();
               int    nextStepIndex = processState.getStepList().indexOf(nextStepName);
               if(nextStepIndex == processState.getStepList().size() - 1)
               {
                  isBreak = false;
               }
            }

            if(isBreak)
            {
               processTracer.handleProcessBreak(runProcessInput, runProcessOutput, processException);
            }
            else
            {
               processTracer.handleProcessFinish(runProcessInput, runProcessOutput, processException);
            }
         }
      }
      catch(Exception e)
      {
         LOG.info("Error in traceProcessFinish", e, logPair("processName", runProcessInput.getProcessName()));
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private void traceStepStart(RunBackendStepInput runBackendStepInput)
   {
      try
      {
         if(processTracer != null)
         {
            processTracer.handleStepStart(runBackendStepInput);
         }
      }
      catch(Exception e)
      {
         LOG.info("Error in traceStepFinish", e, logPair("processName", runBackendStepInput.getProcessName()), logPair("stepName", runBackendStepInput.getStepName()));
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private void traceStepFinish(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput)
   {
      try
      {
         if(processTracer != null)
         {
            processTracer.handleStepFinish(runBackendStepInput, runBackendStepOutput);
         }
      }
      catch(Exception e)
      {
         LOG.info("Error in traceStepFinish", e, logPair("processName", runBackendStepInput.getProcessName()), logPair("stepName", runBackendStepInput.getStepName()));
      }
   }

}
