/*
 * Copyright 2012 JBoss by Red Hat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.droolsjbpm.services.impl.event.listeners;

import java.util.Date;
import java.util.List;
import javax.enterprise.event.Observes;
import javax.enterprise.event.Reception;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import org.droolsjbpm.services.impl.model.BAMProcessSummary;
import org.droolsjbpm.services.impl.model.BAMTaskSummary;
import org.jboss.seam.transaction.Transactional;
import org.jbpm.task.Task;
import org.jbpm.task.events.AfterTaskActivatedEvent;
import org.jbpm.task.events.AfterTaskAddedEvent;
import org.jbpm.task.events.AfterTaskClaimedEvent;
import org.jbpm.task.events.AfterTaskCompletedEvent;
import org.jbpm.task.events.AfterTaskExitedEvent;
import org.jbpm.task.events.AfterTaskFailedEvent;
import org.jbpm.task.events.AfterTaskStartedEvent;
import org.jbpm.task.events.AfterTaskStoppedEvent;
import org.jbpm.task.lifecycle.listeners.TaskLifeCycleEventListener;

/**
 *
 * @author salaboy
 */
@Singleton
@Transactional
@BAM
public class CDIBAMTaskEventListener implements TaskLifeCycleEventListener {

    @Inject
    private EntityManager em;

    public CDIBAMTaskEventListener() {
    }

    public void afterTaskStartedEvent(@Observes(notifyObserver = Reception.ALWAYS) @AfterTaskStartedEvent Task ti) {
        List<BAMTaskSummary> taskSummaries = (List<BAMTaskSummary>) em.createQuery("select bts from BAMTaskSummary bts where bts.taskId=:taskId")
                .setParameter("taskId", ti.getId()).getResultList();
        if (taskSummaries.isEmpty()) {
            String actualOwner = "";
            if (ti.getTaskData().getActualOwner() != null) {
                actualOwner = ti.getTaskData().getActualOwner().getId();
            }
            BAMTaskSummary bamTaskSummary = new BAMTaskSummary(ti.getId(), ti.getNames().get(0).getText(), "Started", new Date(), actualOwner, ti.getTaskData().getProcessInstanceId());
            bamTaskSummary.setStartDate(new Date());
            em.persist(bamTaskSummary);
            
        } else if (taskSummaries.size() == 1) {
            
            BAMTaskSummary taskSummaryById = taskSummaries.get(0);
            taskSummaryById.setStatus("Started");
            taskSummaryById.setStartDate(new Date());
            if (ti.getTaskData().getActualOwner() != null) {
                taskSummaryById.setUserId(ti.getTaskData().getActualOwner().getId());
            }
            em.merge(taskSummaryById);

        } else {
            throw new IllegalStateException("We cannot have more than one BAM Task Summary for the task id = " + ti.getId());
        }

    }

    public void afterTaskActivatedEvent(@Observes(notifyObserver = Reception.ALWAYS) @AfterTaskActivatedEvent Task ti) {
    }

    public void afterTaskClaimedEvent(@Observes(notifyObserver = Reception.ALWAYS) @AfterTaskClaimedEvent Task ti) {
        
        List<BAMTaskSummary> taskSummaries = (List<BAMTaskSummary>) em.createQuery("select bts from BAMTaskSummary bts where bts.taskId=:taskId")
                .setParameter("taskId", ti.getId()).getResultList();
        if (taskSummaries.isEmpty()) {
            
            String actualOwner = "";
            if (ti.getTaskData().getActualOwner() != null) {
                actualOwner = ti.getTaskData().getActualOwner().getId();
            }

            em.persist(new BAMTaskSummary(ti.getId(), ti.getNames().get(0).getText(), "Claimed", new Date(), actualOwner, ti.getTaskData().getProcessInstanceId()));
        } else if (taskSummaries.size() == 1) {
            
            BAMTaskSummary taskSummaryById = taskSummaries.get(0);
            taskSummaryById.setStatus("Claimed");
            if (ti.getTaskData().getActualOwner() != null) {
                taskSummaryById.setUserId(ti.getTaskData().getActualOwner().getId());
            }
            em.merge(taskSummaryById);

        } else {
            throw new IllegalStateException("We cannot have more than one BAM Task Summary for the task id = " + ti.getId());
        }

    }

    public void afterTaskSkippedEvent(Task ti) {
    }

    public void afterTaskStoppedEvent(@Observes(notifyObserver = Reception.ALWAYS) @AfterTaskStoppedEvent Task ti) {
    }

    public void afterTaskCompletedEvent(@Observes(notifyObserver = Reception.ALWAYS) @AfterTaskCompletedEvent Task ti) {

        List<BAMTaskSummary> summaries = (List<BAMTaskSummary>) em.createQuery("select bts from BAMTaskSummary bts where bts.taskId=:taskId")
                .setParameter("taskId", ti.getId()).getResultList();
        
        if(summaries.size() == 1){
        
          BAMTaskSummary taskSummaryById = (BAMTaskSummary)summaries.get(0);

          taskSummaryById.setStatus("Completed");
          Date completedDate = new Date();
          taskSummaryById.setEndDate(completedDate);
          taskSummaryById.setDuration(completedDate.getTime() - taskSummaryById.getStartDate().getTime());
          em.merge(taskSummaryById);
        }else{
          // Log
          System.out.print("EEEE: Something went wrong with the Task BAM Listener");
        }
    }

    public void afterTaskFailedEvent(@Observes(notifyObserver = Reception.ALWAYS) @AfterTaskFailedEvent Task ti) {
    }

    public void afterTaskAddedEvent(@Observes(notifyObserver = Reception.ALWAYS) @AfterTaskAddedEvent Task ti) {
    }

    public void afterTaskExitedEvent(@Observes(notifyObserver = Reception.ALWAYS) @AfterTaskExitedEvent Task ti) {
    }
}
