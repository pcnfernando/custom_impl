/*
 * Copyright (c)  2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.extension.siddhi.execution.bny.streamfunction;

import org.wso2.extension.siddhi.execution.bny.util.PopulateFromMap;
import org.wso2.siddhi.annotation.Example;
import org.wso2.siddhi.annotation.Extension;
import org.wso2.siddhi.annotation.Parameter;
import org.wso2.siddhi.annotation.ReturnAttribute;
import org.wso2.siddhi.annotation.util.DataType;
import org.wso2.siddhi.core.config.SiddhiAppContext;
import org.wso2.siddhi.core.exception.SiddhiAppRuntimeException;
import org.wso2.siddhi.core.executor.ExpressionExecutor;
import org.wso2.siddhi.core.executor.VariableExpressionExecutor;
import org.wso2.siddhi.core.executor.function.FunctionExecutor;
import org.wso2.siddhi.core.util.config.ConfigReader;
import org.wso2.siddhi.query.api.definition.Attribute;
import org.wso2.siddhi.query.api.exception.SiddhiAppValidationException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * createMapFromRef(propertiesMap,SUBTOPIC, SUBTOPIC1, SUBTOPIC2, SUBTOPIC3, SUBTOPIC4, RULE_FLAG, EMAIL_FLAG,
 * ALR_PRI_ID, ALR_DESC_TX, ALR_TY_DESC_TX)
 * Returns a map with populated references from MQMSGRECNAMESPACE table.
 * Accept Type(s): (OBJECT, STRING, STRING, STRING, STRING, STRING, STRING, STRING, STRING, STRING, STRING)
 * Return Type(s): OBJECT
 */
@Extension(
        name = "createMapFromRef",
        namespace = "bny",
        description = "Returns a map with populated values for references from the raw event. ",
        parameters = {
                @Parameter(name = "structured.message.map",
                        description = "The initial property map created from the raw message.",
                        type = {DataType.OBJECT}),
                @Parameter(name = "references.list",
                        description = "List of references from the MQMSGRECNAMESPACE for the alert message type.",
                        type = {DataType.STRING}),
        },
        returnAttributes = @ReturnAttribute(
                description = "This returns a map with populated values for the references "
                        + "from MQMSGRECNAMESPACE table.",
                type = {DataType.OBJECT}),
        examples = @Example(description = "This returns a hash map by adding values for the keys referenced by "
                + "the references.list",
                syntax = "bny:createMapFromRef(propertiesMap,SUBTOPIC, SUBTOPIC1, SUBTOPIC2, SUBTOPIC3, SUBTOPIC4, "
                        + "RULE_FLAG, EMAIL_FLAG,ALR_PRI_ID, ALR_DESC_TX, ALR_TY_DESC_TX)")
)
public class CreateMapFromReferenceFunctionExtension extends FunctionExecutor {

    Attribute.Type returnType = Attribute.Type.OBJECT;
    List<String> attrList = new ArrayList<>();

    @Override
    protected void init(ExpressionExecutor[] attributeExpressionExecutors, ConfigReader configReader,
                        SiddhiAppContext siddhiAppContext) {
        for (int i = 1; i < attributeExpressionExecutors.length; i++) {
            if (attributeExpressionExecutors[i] != null) {
                attrList.add(((VariableExpressionExecutor) attributeExpressionExecutors[i]).getAttribute().getName());
            } else {
                throw new SiddhiAppValidationException(String.format("CreateMapFromReferenceFunctionExtension "
                        + "%s parameter needs to be non empty", i));
            }
        }
    }

    @Override
    protected Object execute(Object[] data) {
        for (int i = 0; i < data.length; i++) {
            if (data[i] == null) {
                throw new SiddhiAppRuntimeException(String.format("Invalid input given to str:createMapFromRef() "
                        + "function. %s argument cannot be null", (i + 1)));
            }
        }
        Map<String, String> propertiesMap = (LinkedHashMap<String, String>) data[0];
        for (int i = 1; i < attributeExpressionExecutors.length; i++) {
            String attribute = attrList.get(i - 1);
            if (propertiesMap.get(data[i]) == null) {
                propertiesMap.put(attribute, (String) data[i]);
            } else {
                propertiesMap.put(attribute, propertiesMap.get(data[i]));
            }
        }
        String alertDesc = PopulateFromMap.populateTemplateFromMap(propertiesMap,
                propertiesMap.get("ALR_DESC_TX"), "\\+", "\\+");
        String alertTypeDesc = PopulateFromMap.populateTemplateFromMap(propertiesMap,
                propertiesMap.get("ALR_TY_DESC_TX"), "\\+", "\\+");
        propertiesMap.replace("ALR_DESC_TX", alertDesc);
        propertiesMap.replace("ALR_TY_DESC_TX", alertTypeDesc);
        return propertiesMap;
    }

    @Override
    protected Object execute(Object data) {
        return null;  //Since the CreateMapFromReference function takes in exactly 2 parameters,
        // this method does not get called.
        // Hence, not implemented.
    }

    @Override
    public Attribute.Type getReturnType() {
        return returnType;
    }

    @Override
    public Map<String, Object> currentState() {
        return null;    //No need to maintain a state.
    }

    @Override
    public void restoreState(Map<String, Object> map) {
    }
}
