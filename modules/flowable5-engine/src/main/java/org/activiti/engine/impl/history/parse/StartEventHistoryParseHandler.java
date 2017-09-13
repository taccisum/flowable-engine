/* Licensed under the Apache License, Version 2.0 (the "License");
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
package org.activiti.engine.impl.history.parse;

import org.activiti.engine.impl.bpmn.parser.BpmnParse;
import org.activiti.engine.impl.bpmn.parser.handler.AbstractBpmnParseHandler;
import org.activiti.engine.impl.history.handler.StartEventEndHandler;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.StartEvent;

/**
 * @author Joram Barrez
 */
public class StartEventHistoryParseHandler extends AbstractBpmnParseHandler<StartEvent> {

    protected static final StartEventEndHandler START_EVENT_END_HANDLER = new StartEventEndHandler();

    @Override
    protected Class<? extends BaseElement> getHandledType() {
        return StartEvent.class;
    }

    @Override
    protected void executeParse(BpmnParse bpmnParse, StartEvent element) {
        bpmnParse.getCurrentActivity().addExecutionListener(org.activiti.engine.impl.pvm.PvmEvent.EVENTNAME_END, START_EVENT_END_HANDLER);
    }

}
