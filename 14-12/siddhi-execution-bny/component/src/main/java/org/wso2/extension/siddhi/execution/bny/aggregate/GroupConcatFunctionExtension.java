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
import org.wso2.siddhi.core.query.selector.attribute.aggregator.AttributeAggregator;
import org.wso2.siddhi.core.util.config.ConfigReader;
import org.wso2.siddhi.query.api.definition.Attribute;
import org.wso2.siddhi.query.api.exception.SiddhiAppValidationException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 * groupConcat(key1, key2, key3...)
 * Returns concatenated string for all the events separated by a comma(,).
 * Accept Type(s): STRING. The strings that need to be aggregated.
 * Return Type(s): STRING
 */
@Extension(
        name = "groupConcat",
        namespace = "bny",
        description = "Concatenates given columns in a tuples with # delimiter and aggregates multiple " +
                "such tuples with , separation. Returns an aggregated string of the provided batch.",
        parameters = {
                @Parameter(name = "column.list",
                        description = "The column list that need to be aggregated.",
                        type = {DataType.STRING})
        },
        returnAttributes = @ReturnAttribute(
                description = "Returns a string that is the result of the concatenated values of the provided "
                        + "column.list separated by a comma (,)",
                type = {DataType.STRING}),
        examples = @Example(description = "This returns a concatenated string for the event chunk with values "
                + "for the given keys. In this scenario, the output is "
                + "1#AppCode#Data#3#NOEXPAND#NOCOPY, 2#TransType#Data#3#NOEXPAND#NOCOPY...",
                syntax = "groupConcat(FLD_ORDER_NUM, FLD_NM, FLD_DATA_TYPE, FLD_DATA_SIZE, FLD_EXPAND_STR, " +
                        "FLD_COPY_STR)")
)
public class GroupConcatFunctionExtension extends AttributeAggregator {
    private Attribute.Type returnType = Attribute.Type.STRING;
    private Map<Integer, Object[]> dataSet;
    private boolean canDestroy = false;

    private static final String ATTRIBUTE_DELIMITER = "#";
    private static final String EVENT_DELIMITER = ",";
    private static final int FIELD_CHARACTER_SIZE_INDEX = 3;

    @Override
    protected void init(ExpressionExecutor[] attributeExpressionExecutors, ConfigReader configReader,
                        SiddhiAppContext siddhiAppContext) {
        if (attributeExpressionExecutors.length < 2) {
            throw new SiddhiAppValidationException("bny:groupConcat() function requires at least two or more keys");
        }
        dataSet = new HashMap<>();
    }

    @Override
    public Attribute.Type getReturnType() {
        return returnType;
    }

    @Override
    public Object processAdd(Object o) {
        return null;  //Since the groupConcat function requires two or more keys, this method does not get called.
        // Hence, not implemented.
    }

    @Override
    public synchronized Object processAdd(Object[] objects) {
        for (int i = 0; i < objects.length; i++) {
            if (objects[i] == null) {
                throw new SiddhiAppRuntimeException(String.format("bny:groupConcat() function requires " +
                        "not null attribute for index: %s", i + 1));
            }
        }
        int orderNum = (int) objects[0];
        dataSet.put(orderNum, objects);
        return constructConcatString(EVENT_DELIMITER);
    }

    @Override
    public Object processRemove(Object o) {
        return null;
    }

    @Override
    public Object processRemove(Object[] objects) {
        dataSet.remove(objects[0]);
        if (dataSet.size() == 0) {
            canDestroy = true;
        }
        return constructConcatString(EVENT_DELIMITER);
    }

    private Object constructConcatString(String separator) {
        StringBuilder stringBuilder = new StringBuilder();
        int startIndex = 0;
        int count = 0;
        Map<Integer, Object[]> treeMap = new TreeMap<Integer, Object[]>(dataSet);
        for (Iterator<Map.Entry<Integer, Object[]>> iterator = treeMap.entrySet().iterator();
             iterator.hasNext(); ) {
            Map.Entry<Integer, Object[]> entry = iterator.next();
            StringBuilder sb = new StringBuilder();
            Object[] objArr = entry.getValue();
            for (int j = 1; j < objArr.length; j++) {
                sb.append(String.valueOf(objArr[j]));
                sb.append(ATTRIBUTE_DELIMITER);
            }
            sb.append(startIndex);
            stringBuilder.append(sb.toString());
            count++;
            if (count != 0) {
                startIndex += Integer.parseInt((String) objArr[FIELD_CHARACTER_SIZE_INDEX]);
            }
            if (iterator.hasNext()) {
                stringBuilder.append(separator);
            }
        }
        return stringBuilder.toString();
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
        dataSet = (Map<Integer, Object[]>) state.get("dataSet");
    }
}
