/*
 * Copyright (c)  2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.extension.siddhi.execution.bny.streamprocessor;

import org.wso2.extension.siddhi.execution.bny.util.PopulateFromMap;
import org.wso2.siddhi.annotation.Example;
import org.wso2.siddhi.annotation.Extension;
import org.wso2.siddhi.annotation.Parameter;
import org.wso2.siddhi.annotation.ReturnAttribute;
import org.wso2.siddhi.annotation.util.DataType;
import org.wso2.siddhi.core.config.SiddhiAppContext;
import org.wso2.siddhi.core.event.ComplexEventChunk;
import org.wso2.siddhi.core.event.stream.StreamEvent;
import org.wso2.siddhi.core.event.stream.StreamEventCloner;
import org.wso2.siddhi.core.event.stream.populater.ComplexEventPopulater;
import org.wso2.siddhi.core.exception.SiddhiAppRuntimeException;
import org.wso2.siddhi.core.executor.ExpressionExecutor;
import org.wso2.siddhi.core.executor.VariableExpressionExecutor;
import org.wso2.siddhi.core.query.processor.Processor;
import org.wso2.siddhi.core.query.processor.stream.StreamProcessor;
import org.wso2.siddhi.core.util.config.ConfigReader;
import org.wso2.siddhi.query.api.definition.AbstractDefinition;
import org.wso2.siddhi.query.api.definition.Attribute;
import org.wso2.siddhi.query.api.exception.SiddhiAppValidationException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * populateTemplate(TPL_TX, TPL_CD, STRUCTURED_PROPERTY_MAP)
 * Returns a new string that is populated with the values from the STRUCTURED_PROPERTY_MAP
 * Accept Type(s): (STRING,STRING,OBJECT)
 * Return Type(s): STRING,STRING
 */
@Extension(
        name = "populateTemplate",
        namespace = "bny",
        description = "Returns the populated template text with the values from the STRUCTURED_PROPERTY_MAP.",
        parameters = {
                @Parameter(name = "template.string",
                        description = "The template that needs to be populated.",
                        type = {DataType.STRING}),
                @Parameter(name = "tpl.cd",
                        description = "Template category.",
                        type = {DataType.STRING}),
                @Parameter(name = "structured.property.map",
                        description = "Key value pairs extracted from the raw incoming message.",
                        type = {DataType.OBJECT})
        },
        returnAttributes = {
                @ReturnAttribute(name = "populatedBody",
                        description = "The populated template.",
                        type = {DataType.STRING}),
                @ReturnAttribute(name = "publishType",
                        description = "The publish strategy for the template.",
                        type = {DataType.STRING})
        },
        examples = {
                @Example(description = "This populates the template in TPL_TX by looping through the values in "
                        + "STRUCTURED_PROPERTY_MAP. And returns the populated template and the publish strategy "
                        + "for the template.",
                        syntax = "populateTemplate(TPL_TX, TPL_CD, STRUCTURED_PROPERTY_MAP)")
        }
)
public class PopulateTemplateStreamProcessor extends StreamProcessor {
    private static final int MAX_PARAMETER_COUNT = 3;

    @Override
    protected List<Attribute> init(AbstractDefinition abstractDefinition, ExpressionExecutor[] expressionExecutors,
                                   ConfigReader configReader, SiddhiAppContext siddhiAppContext) {
        if (attributeExpressionExecutors.length != MAX_PARAMETER_COUNT) {
            throw new SiddhiAppValidationException("bny:populateTemplate() extension requires exactly " +
                    "three parameters, namely TemplateString, PropertiesMap and TemplateType");
        }
        for (int i = 0; i < MAX_PARAMETER_COUNT; i++) {
            if (!(attributeExpressionExecutors[i] instanceof VariableExpressionExecutor)) {
                throw new SiddhiAppValidationException(String.format("bny:populateTemplate() " +
                        "VariableExpressionExecutor as the %s parameter", i));
            }
        }
        List<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute("populatedBody", Attribute.Type.STRING));
        attributes.add(new Attribute("publishType", Attribute.Type.STRING));
        return attributes;
    }

    @Override
    protected void process(ComplexEventChunk<StreamEvent> streamEventChunk, Processor processor,
                           StreamEventCloner streamEventCloner, ComplexEventPopulater complexEventPopulater) {
            while (streamEventChunk.hasNext()) {
                StreamEvent event = streamEventChunk.next();
                for (int i = 0; i < attributeExpressionLength; i++) {
                    if (attributeExpressionExecutors[i].execute(event) == null) {
                        throw new SiddhiAppRuntimeException("populateTemplate extension does not accept Null "
                                + "at position:" + i);
                    }
                }
                String template = (String) attributeExpressionExecutors[0].execute(event);
                String templateType = (String) attributeExpressionExecutors[1].execute(event);
                HashMap<String, String> propertiesMap =
                        (HashMap<String, String>) attributeExpressionExecutors[2].execute(event);
                String populatedTemplate = PopulateFromMap.populateTemplateFromMap(propertiesMap, template);
                String type = "";
                if (templateType.contains("BATCH")) {
                    type = "BATCH";
                } else if (templateType.contains("CELL")) {
                    type = "CELL";
                } else if (templateType.equalsIgnoreCase("SUBJECT")) {
                    type = "SUBJECT";
                } else {
                    type = "EMAIL";
                }
                Object[] data = {populatedTemplate, type};
                // If output has values, then add those values to output stream
                complexEventPopulater.populateComplexEvent(event, data);
            }
        nextProcessor.process(streamEventChunk);
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public Map<String, Object> currentState() {
        return new HashMap<>();    //No need to maintain a state.
    }

    @Override
    public void restoreState(Map<String, Object> map) {
    }
}
