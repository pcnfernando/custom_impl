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

import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.siddhi.annotation.Example;
import org.wso2.siddhi.annotation.Extension;
import org.wso2.siddhi.annotation.Parameter;
import org.wso2.siddhi.annotation.ReturnAttribute;
import org.wso2.siddhi.annotation.util.DataType;
import org.wso2.siddhi.core.config.SiddhiAppContext;
import org.wso2.siddhi.core.exception.SiddhiAppRuntimeException;
import org.wso2.siddhi.core.executor.ExpressionExecutor;
import org.wso2.siddhi.core.executor.function.FunctionExecutor;
import org.wso2.siddhi.core.util.config.ConfigReader;
import org.wso2.siddhi.query.api.definition.Attribute;
import org.wso2.siddhi.query.api.exception.SiddhiAppValidationException;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * createMap(MQ_Raw_Message, GroupedKeys)
 * Returns a map with extracted keys and values.
 * Accept Type(s): (STRING,STRING)
 * Return Type(s): OBJECT
 */

@Extension(
        name = "createMap",
        namespace = "bny",
        description = "Returns a map with values extracted from the raw message.",
        parameters = {
                @Parameter(name = "raw.input.message",
                        description = "The input string to be processed.",
                        type = {DataType.STRING}),
                @Parameter(name = "grouped.keys",
                        description = "Process string used to extract values from the raw message.",
                        type = {DataType.STRING})
        },
        returnAttributes = @ReturnAttribute(
                description = "This returns a map with values extracted from the raw message.",
                type = {DataType.OBJECT}),
        examples = @Example(description = "This returns a boolean value by evaluation the given expression. "
                + "In this case, it will return a map extracted from the raw message using grouped.keys "
                + "to process the message.",
                syntax = "bny:createMap(message, groupedKeys)")
)
public class CreateMapFunctionExtension extends FunctionExecutor {

    private static final Logger logger = LoggerFactory.getLogger(CreateMapFunctionExtension.class);
    Attribute.Type returnType = Attribute.Type.OBJECT;
    private static final int COLUMN_COUNT = 7;
    private static final String ATTRIBUTE_DELIMITER = "#";
    private static final String OBJECT_DELIMITER = ",";
    private static final String ATTRIBUTE_TYPE_CONSTANT = "CONST";
    private static final String NO_ATTRIBUTE_REFERENCE = "NOCOPY";
    private static final String NUMBER_FORMAT_FIELD = "DBL";

    @Override
    protected void init(ExpressionExecutor[] attributeExpressionExecutors, ConfigReader configReader,
                        SiddhiAppContext siddhiAppContext) {
        if (attributeExpressionExecutors.length != 2) {
            throw new SiddhiAppValidationException("bny:createMap() function requires exactly 2 parameters, "
                    + "namely the Raw MQ Message and the Grouped Keys process string, but found "
                    + attributeExpressionExecutors.length);
        }
        if (attributeExpressionExecutors[0].getReturnType() != Attribute.Type.STRING) {
            throw new SiddhiAppValidationException("Invalid parameter type found for the raw.message argument of "
                    + "bny:createMap() function, " + "required " + Attribute.Type.STRING + ", but found "
                    + attributeExpressionExecutors[0].getReturnType().toString());
        }
        if (attributeExpressionExecutors[1].getReturnType() != Attribute.Type.STRING) {
            throw new SiddhiAppValidationException("Invalid parameter type found for the grouped keys argument of "
                    + "bny:createMap() function, " + "required " + Attribute.Type.STRING + ", but found "
                    + attributeExpressionExecutors[0].getReturnType().toString());
        }
    }

    @Override
    protected Object execute(Object[] data) {
        Map<String, String> map = new LinkedHashMap<>();
        String rawEvent;
        String processString;
        for (int i = 0; i < data.length; i++) {
            if (data[i] == null) {
                throw new SiddhiAppRuntimeException(String.format("Invalid input given to str:createEvalStr() "
                        + "function. %s argument cannot be null", (i + 1)));
            }
        }
        rawEvent = (String) data[0];
        processString = (String) data[1];
        String[] eventObjects = processString.split(OBJECT_DELIMITER);
        for (String event : eventObjects) {
            String[] tokens = event.split(ATTRIBUTE_DELIMITER);
            if (tokens.length == COLUMN_COUNT) {
                String fieldName = tokens[0];
                String fieldDataType = tokens[1];
                String fieldDataSize = tokens[2];
                String fieldExpandStr = tokens[3];
                String fieldCopyStr = tokens[4];
                String fieldType = tokens[5];
                int startIndex = Integer.parseInt(tokens[6]);
                int endIndex = startIndex + Integer.parseInt(fieldDataSize);
                String value = "";
                if (fieldDataType.equalsIgnoreCase(ATTRIBUTE_TYPE_CONSTANT)) {
                    if (fieldCopyStr.equalsIgnoreCase(NO_ATTRIBUTE_REFERENCE)) {
                        value = fieldExpandStr;
                    } else {
                        value = map.get(fieldCopyStr);
                    }
                } else {
                    try {
                        value = rawEvent.substring(startIndex, endIndex);
                    } catch (Throwable e) {
                        logger.error("Error occurred when retrieving " + fieldName
                                + " start:" + startIndex + " end:" + endIndex);
                    }
                }
                if (fieldType.contains(NUMBER_FORMAT_FIELD)) {
                    int numberOfDecimals = Integer.valueOf(fieldType.replaceAll(NUMBER_FORMAT_FIELD, ""));
                    NumberFormat formatter = null;
                    switch (numberOfDecimals) {
                        case 1: {
                            formatter = new DecimalFormat("#,###.0");
                            break;
                        }
                        case 2: {
                            formatter = new DecimalFormat("#,###.00");
                            break;
                        }
                        case 3: {
                            formatter = new DecimalFormat("#,###.000");
                            break;
                        }
                        case 4: {
                            formatter = new DecimalFormat("#,###.0000");
                            break;
                        }
                        case 5: {
                            formatter = new DecimalFormat("#,###.00000");
                            break;
                        }
                        default:
                            break;
                    }
                    if (formatter != null && NumberUtils.isCreatable(value)) {
                        double amount = Double.parseDouble(value);
                        value = formatter.format(amount);
                    }
                }
                map.put(fieldName, value.trim());
            } else {
                throw new SiddhiAppRuntimeException(String.format("Invalid input given for bny:createMap() function. "
                        + "Second argument contains %s attributes. Please check the output "
                        + "from the bny:groupConcat() function.", tokens.length));
            }
        }
        return map;
    }

    @Override
    protected Object execute(Object data) {
        return null;  //Since the createMap function takes in exactly 2 parameters, this method does not get called.
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


