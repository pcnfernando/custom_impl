/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.extension.siddhi.execution.bny.aggregate;

import org.wso2.siddhi.annotation.Example;
import org.wso2.siddhi.annotation.Extension;
import org.wso2.siddhi.annotation.Parameter;
import org.wso2.siddhi.annotation.ReturnAttribute;
import org.wso2.siddhi.annotation.util.DataType;
import org.wso2.siddhi.core.config.SiddhiAppContext;
import org.wso2.siddhi.core.exception.SiddhiAppRuntimeException;
import org.wso2.siddhi.core.executor.ExpressionExecutor;
import org.wso2.siddhi.core.executor.VariableExpressionExecutor;
import org.wso2.siddhi.core.query.selector.attribute.aggregator.AttributeAggregator;
import org.wso2.siddhi.core.util.config.ConfigReader;
import org.wso2.siddhi.query.api.definition.Attribute;
import org.wso2.siddhi.query.api.exception.SiddhiAppValidationException;

import java.util.HashMap;
import java.util.Map;

/**
 * createMapFromBatch(key, value)
 * Returns a hashmap object created from provided key-value pairs.
 * Accept Type(s): STRING. Key.
 * Accept Type(s): STRING. Value.
 * Return Type(s): OBJECT
 */
@Extension(
        name = "createMapFromBatch",
        namespace = "bny",
        description = "Aggregates the received key-value pairs and returns an hashmap object",
        parameters = {
                @Parameter(name = "key",
                        description = "Key of the key-value pair",
                        type = {DataType.STRING}),
                @Parameter(name = "value",
                        description = "Value of the key-value pair",
                        type = {DataType.STRING})
        },
        returnAttributes = @ReturnAttribute(
                description = "Returns a hashmap object aggregated from the provided key-value pairs",
                type = {DataType.OBJECT}),
        examples = @Example(description = "This returns a hashmap object aggregated from key-value pairs provided.",
                syntax = "createMapFromBatch(VARL_NM, DATA_TAG_NM) as PROPERTY_MAPPING")
)
public class CreateMapFromBatchFunctionExtension extends AttributeAggregator {

    private Attribute.Type returnType = Attribute.Type.OBJECT;
    private Map<String, String> dataSet;
    private boolean canDestroy = false;

    @Override
    protected void init(ExpressionExecutor[] attributeExpressionExecutors, ConfigReader configReader,
                        SiddhiAppContext siddhiAppContext) {

        if (attributeExpressionExecutors.length != 2) {
            throw new SiddhiAppValidationException("bny:createMapFromBatch() function requires exactly " +
                    "two attributes.");
        }
        if (attributeExpressionExecutors[0] instanceof VariableExpressionExecutor) {
            if (attributeExpressionExecutors[0].getReturnType() != Attribute.Type.STRING) {
                throw new SiddhiAppValidationException("Invalid parameter type found for the key "
                        + "argument," + " required " + Attribute.Type.STRING + " but found "
                        + attributeExpressionExecutors[0].getReturnType().toString());
            }
        } else {
            throw new SiddhiAppValidationException("Parameter key must be a variable but found "
                    + attributeExpressionExecutors[0].getClass().getCanonicalName());
        }
        if (attributeExpressionExecutors[1] instanceof VariableExpressionExecutor) {
            if (attributeExpressionExecutors[1].getReturnType() != Attribute.Type.STRING) {
                throw new SiddhiAppValidationException("Invalid parameter type found for the value "
                        + "argument," + " required " + Attribute.Type.STRING + " but found "
                        + attributeExpressionExecutors[1].getReturnType().toString());
            }
        } else {
            throw new SiddhiAppValidationException("Parameter value must be a variable but found "
                    + attributeExpressionExecutors[1].getClass().getCanonicalName());
        }
        dataSet = new HashMap<>();
    }

    @Override
    public Attribute.Type getReturnType() {
        return returnType;
    }

    @Override
    public Object processAdd(Object o) {
        return null;  //Since the createMapFromBatch function requires two parameters, this method does not get called.
        // Hence, not implemented.
    }

    @Override
    public Object processAdd(Object[] objects) {
        for (int i = 0; i < objects.length; i++) {
            if (objects[i] == null) {
                throw new SiddhiAppRuntimeException(String.format("bny:createMapFromBatch() function requires " +
                        "not null attribute for index: %s", i + 1));
            }
        }
        String key = (String) objects[0];
        String value = (String) objects[1];
        dataSet.put(key, value);
        return new HashMap<>(dataSet);
    }

    @Override
    public Object processRemove(Object o) {
        dataSet.remove(o);
        if (dataSet.size() == 0) {
            canDestroy = true;
        }
        return dataSet;
    }

    @Override
    public Object processRemove(Object[] objects) {
        for (Object object : objects) {
            dataSet.remove(object);
        }
        if (dataSet.size() == 0) {
            canDestroy = true;
        }
        return dataSet;
    }

    @Override
    public boolean canDestroy() {

        return canDestroy;
    }

    @Override
    public Object reset() {
        dataSet.clear();
        canDestroy = true;
        return "";
    }

    @Override
    public Map<String, Object> currentState() {
        HashMap<String, Object> state = new HashMap<>();
        state.put("dataSet", dataSet);
        return state;
    }

    @Override
    public void restoreState(Map<String, Object> state) {
        dataSet = (Map<String, String>) state.get("dataSet");
    }
}
