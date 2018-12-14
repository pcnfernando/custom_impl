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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.extension.siddhi.execution.bny.bean.Expression;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * createEvalStr(PROPERTY_MAPPING, STRUCTURED_PROPERTY_MAP, RULE_MSTR_ID, VARL_NM, VARL_TY_CD, VARL_OPER_CD,
 * VARL_VAL_TX ,OPER_CD , RULE_SEQ_NR)
 * Returns a logical expression string. 'true/false' flag is a flag to whether populate the
 * Variables in the expression.
 * Accept Type(s): OBJECT. Attribute Mappings.
 * Accept Type(s): OBJECT. Properties map created from the Raw message.
 * Accept Type(s): STRING. RULE_MSTR_ID.
 * Accept Type(s): STRING. VARL_NM.
 * Accept Type(s): STRING. VARL_TY_CD
 * Accept Type(s): STRING. VARL_OPER_CD.
 * Accept Type(s): STRING. VARL_VAL_TX
 * Accept Type(s): STRING. OPER_CD
 * Accept Type(s): INTEGER. RULE_SEQ_NR
 * Return Type(s): OBJECT
 */
@Extension(
        name = "createEvalStr",
        namespace = "bny",
        description = "Creates a map with populated and unpopulated logical expression strings from "
                + "each of the provided expressions per RuleID.",
        parameters = {
                @Parameter(name = "attribute.mapping",
                        description = "The attribute mappings needed when populating the logical expressions. "
                                + "Expects a map.",
                        type = {DataType.OBJECT}),
                @Parameter(name = "structured.properties",
                        description = "The Parameter Map which contains the key,value pairs extracted "
                                + "from the raw message.",
                        type = {DataType.OBJECT}),
                @Parameter(name = "rule.master.id",
                        description = "The Rule Master ID of each expression. When we group by RULE_MSTR_ID, "
                                + "the RULE_MSTR_ID will be the same within a single event chunk.",
                        type = {DataType.STRING}),
                @Parameter(name = "varl.nm",
                        description = "Attribute referencing the data within the message. The structured.properties "
                                + "should contain this parameter as the key, if not logical string will not be "
                                + "correctly populated.",
                        type = {DataType.STRING}),
                @Parameter(name = "varl.ty.cd",
                        description = "Attribute type of varl.nm",
                        type = {DataType.STRING}),
                @Parameter(name = "varl.oper.cd",
                        description = "Inequalities of the expression. eg: GE, LE, EQ, NOT",
                        type = {DataType.STRING}),
                @Parameter(name = "varl.val.tx",
                        description = "Static parameter customer configured while creating the rule. "
                                + "eg: Price > 200, RHS of the expression.",
                        type = {DataType.STRING}),
                @Parameter(name = "oper.cd",
                        description = "The logical operation used to combine the expression "
                                + "to create the logical statement.",
                        type = {DataType.STRING}),
                @Parameter(name = "rule.seq.nr",
                        description = "Expression order number for a given rule.master.id. This parameter is used "
                                + "to sort out the expression while create the logical statement "
                                + "from multiple expressions.",
                        type = {DataType.INT}),

        },
        returnAttributes = @ReturnAttribute(
                description = "Returns a map with populated and unpopulated logical statements. 'populated' key holds "
                        + "the logical statement with rules populated from the data from the message where as "
                        + "'unpopulated' key holds the unpopulated logical statement.",
                type = {DataType.OBJECT}),
        examples = {
                @Example(description = "This returns a map with populated and unpopulated logical statements created "
                        + "from the provided expression list as the output.",
                        syntax = "from TempStream1#rdbms:query('EAF_DEV_DB', RDBMS_QUERY, "
                                + "'RULE_MSTR_ID string, VARL_NM string,VARL_OPER_CD string,VARL_VAL_TX string,"
                                + "OPER_CD string, VARL_TY_CD string, RULE_SEQ_NR int')\n"
                                + "#window.batch()\n"
                                + "select bny:createEvalStr(PROPERTY_MAPPING, STRUCTURED_PROPERTY_MAP, RULE_MSTR_ID, "
                                + "VARL_NM, VARL_TY_CD, VARL_OPER_CD ,VARL_VAL_TX ,OPER_CD , RULE_SEQ_NR) "
                                + "as EVAL_STRING_MAP\n"
                                + "group by RULE_MSTR_ID\n"
                                + "insert into  TempStream2;")
        }
)
public class CreateEvalStringFunctionExtension extends AttributeAggregator {

    private static final Logger LOG = LoggerFactory.getLogger(CreateEvalStringFunctionExtension.class);
    private static final String SKIP_CHARACTER_FIELD_NAME = "SKIPOVER";
    private Attribute.Type returnType = Attribute.Type.STRING;
    private boolean canDestroy = false;
    private List<Expression> expressionList = new ArrayList<>();

    @Override
    protected void init(ExpressionExecutor[] attributeExpressionExecutors, ConfigReader configReader,
                        SiddhiAppContext siddhiAppContext) {

        int maxParameterCount = 9;
        if (attributeExpressionExecutors.length != maxParameterCount) {
            throw new SiddhiAppValidationException("Invalid number of parameters provided for createEvalStr extension. "
                    + "The extension requires exactly " + maxParameterCount + " parameters but found "
                    + attributeExpressionExecutors.length);
        }
        if (attributeExpressionExecutors[0] instanceof VariableExpressionExecutor) {
            if (attributeExpressionExecutors[0].getReturnType() != Attribute.Type.OBJECT) {
                throw new SiddhiAppValidationException("Invalid parameter type found for the attribute.mapping "
                        + "argument," + " required " + Attribute.Type.OBJECT + " but found "
                        + attributeExpressionExecutors[0].getReturnType().toString());
            }
        } else {
            throw new SiddhiAppValidationException("Parameter attribute.mapping must be a variable but found "
                    + attributeExpressionExecutors[0].getClass().getCanonicalName());
        }
        if (attributeExpressionExecutors[1] instanceof VariableExpressionExecutor) {
            if (attributeExpressionExecutors[1].getReturnType() != Attribute.Type.OBJECT) {
                throw new SiddhiAppValidationException("Invalid parameter type found for the structured.properties "
                        + "argument," + " required " + Attribute.Type.OBJECT + " but found "
                        + attributeExpressionExecutors[1].getReturnType().toString());
            }
        } else {
            throw new SiddhiAppValidationException("Parameter structured.properties must be a variable but found "
                    + attributeExpressionExecutors[1].getClass().getCanonicalName());
        }
        if (attributeExpressionExecutors[2] instanceof VariableExpressionExecutor) {
            if (attributeExpressionExecutors[2].getReturnType() != Attribute.Type.STRING) {
                throw new SiddhiAppValidationException("Invalid parameter type found for the RULE_MSTR_ID "
                        + "argument," + " required " + Attribute.Type.STRING + " but found "
                        + attributeExpressionExecutors[2].getReturnType().toString());
            }
        } else {
            throw new SiddhiAppValidationException("Parameter RULE_MSTR_ID must be a variable but found "
                    + attributeExpressionExecutors[2].getClass().getCanonicalName());
        }
        if (attributeExpressionExecutors[3] instanceof VariableExpressionExecutor) {
            if (attributeExpressionExecutors[3].getReturnType() != Attribute.Type.STRING) {
                throw new SiddhiAppValidationException("Invalid parameter type found for the VARL_NM "
                        + "argument," + " required " + Attribute.Type.STRING + " but found "
                        + attributeExpressionExecutors[3].getReturnType().toString());
            }
        } else {
            throw new SiddhiAppValidationException("Parameter VARL_NM must be a variable but found "
                    + attributeExpressionExecutors[3].getClass().getCanonicalName());
        }
        if (attributeExpressionExecutors[4] instanceof VariableExpressionExecutor) {
            if (attributeExpressionExecutors[4].getReturnType() != Attribute.Type.STRING) {
                throw new SiddhiAppValidationException("Invalid parameter type found for the VARL_TY_CD "
                        + "argument," + " required " + Attribute.Type.STRING + " but found "
                        + attributeExpressionExecutors[4].getReturnType().toString());
            }
        } else {
            throw new SiddhiAppValidationException("Parameter VARL_TY_CD must be a variable but found "
                    + attributeExpressionExecutors[4].getClass().getCanonicalName());
        }
        if (attributeExpressionExecutors[5] instanceof VariableExpressionExecutor) {
            if (attributeExpressionExecutors[5].getReturnType() != Attribute.Type.STRING) {
                throw new SiddhiAppValidationException("Invalid parameter type found for the VARL_OPER_CD "
                        + "argument," + " required " + Attribute.Type.STRING + " but found "
                        + attributeExpressionExecutors[5].getReturnType().toString());
            }
        } else {
            throw new SiddhiAppValidationException("Parameter VARL_OPER_CD must be a variable but found "
                    + attributeExpressionExecutors[5].getClass().getCanonicalName());
        }
        if (attributeExpressionExecutors[6] instanceof VariableExpressionExecutor) {
            if (attributeExpressionExecutors[6].getReturnType() != Attribute.Type.STRING) {
                throw new SiddhiAppValidationException("Invalid parameter type found for the VARL_VAL_TX "
                        + "argument," + " required " + Attribute.Type.STRING + " but found "
                        + attributeExpressionExecutors[6].getReturnType().toString());
            }
        } else {
            throw new SiddhiAppValidationException("Parameter VARL_VAL_TX must be a variable but found "
                    + attributeExpressionExecutors[6].getClass().getCanonicalName());
        }
        if (attributeExpressionExecutors[7] instanceof VariableExpressionExecutor) {
            if (attributeExpressionExecutors[7].getReturnType() != Attribute.Type.STRING) {
                throw new SiddhiAppValidationException("Invalid parameter type found for the OPER_CD "
                        + "argument," + " required " + Attribute.Type.STRING + " but found "
                        + attributeExpressionExecutors[7].getReturnType().toString());
            }
        } else {
            throw new SiddhiAppValidationException("Parameter OPER_CD must be a variable but found "
                    + attributeExpressionExecutors[7].getClass().getCanonicalName());
        }
        if (attributeExpressionExecutors[8] instanceof VariableExpressionExecutor) {
            if (attributeExpressionExecutors[8].getReturnType() != Attribute.Type.INT) {
                throw new SiddhiAppValidationException("Invalid parameter type found for the RULE_SEQ_NR "
                        + "argument," + " required " + Attribute.Type.INT + " but found "
                        + attributeExpressionExecutors[8].getReturnType().toString());
            }
        } else {
            throw new SiddhiAppValidationException("Parameter RULE_SEQ_NR must be a variable but found "
                    + attributeExpressionExecutors[8].getClass().getCanonicalName());
        }
    }

    @Override
    public Attribute.Type getReturnType() {

        return returnType;
    }

    @Override
    public Object processAdd(Object o) {

        return null;  //Since the extractValues function takes in exactly 2 parameters, this method does not get called.
        // Hence, not implemented.
    }

    @Override
    public Object processAdd(Object[] objects) {

        Map<String, String> structuredPropertiesMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (int i = 0; i < objects.length; i++) {
            if (objects[i] == null) {
                throw new SiddhiAppRuntimeException(String.format("Invalid input given to str:createEvalStr() " +
                        "function. %s argument cannot be null", (i + 1)));
            }
        }
        Map<String, String> ruleAttributeMapping = (HashMap<String, String>) objects[0];
        Map<String, String> property = (LinkedHashMap<String, String>) objects[1];
        property.forEach(structuredPropertiesMap::putIfAbsent);
        Expression expression = new Expression((String) objects[2], (String) objects[3],
                (String) objects[4], (String) objects[5], (String) objects[6],
                (String) objects[7], (int) objects[8]);
        expressionList.add(expression);
        return constructConcatString(structuredPropertiesMap, ruleAttributeMapping);
    }

    private String parseInEqualities(Map<String, String> structuredPropertiesMap,
                                     Map<String, String> ruleAttributeMapping,
                                     Expression expression) {

        Object valueFromMessage;
        valueFromMessage = structuredPropertiesMap.get(expression.getVarlNm());
        if (valueFromMessage == null) {
            String mapping = ruleAttributeMapping.get(expression.getVarlNm().toUpperCase());
            if (mapping != null) {
                valueFromMessage = structuredPropertiesMap.get(mapping);
            }
            if (valueFromMessage == null ||
                    (!(expression.getVarlNm().equalsIgnoreCase(SKIP_CHARACTER_FIELD_NAME))
                            && ((String) valueFromMessage).trim().isEmpty())) {
                LOG.error(String.format("Key %s returns a null while parsing expression, ",
                        expression.getVarlNm(), expression.toString()));
                valueFromMessage = "$".concat(expression.getVarlNm());
            }
        }
        String attributeType = expression.getVarlTyCd();
        String configuredRuleVal = expression.getVarlValTx();
        if ("EQ".equalsIgnoreCase(expression.getVarlOperCd())) {
            return generateExpress(valueFromMessage, attributeType, "==", configuredRuleVal);
        } else if ("GT".equalsIgnoreCase(expression.getVarlOperCd())) {
            return generateExpress(valueFromMessage, attributeType, ">", configuredRuleVal);
        } else if ("GE".equalsIgnoreCase(expression.getVarlOperCd())) {
            return generateExpress(valueFromMessage, attributeType, ">=", configuredRuleVal);
        } else if ("LT".equalsIgnoreCase(expression.getVarlOperCd())) {
            return generateExpress(valueFromMessage, attributeType, "<", configuredRuleVal);
        } else if ("LE".equalsIgnoreCase(expression.getVarlOperCd())) {
            return generateExpress(valueFromMessage, attributeType, "<=", configuredRuleVal);
        } else if ("NOT".equalsIgnoreCase(expression.getVarlOperCd())) {
            return generateExpress(valueFromMessage, attributeType, "!", configuredRuleVal);
        } else {
            LOG.error("Invalid inequality found while creating logical expression:" + expression.getVarlOperCd());
            return generateExpress(valueFromMessage, attributeType, "|INVALID_INEQUALITY:|"
                    + expression.getVarlOperCd(), configuredRuleVal);
        }
    }

    private String generateExpress(Object valueFromMessage, String attributeType, String ops,
                                   Object configuredRuleVal) {

        StringBuilder build = new StringBuilder("(");
        if ("NUMBER".equalsIgnoreCase(attributeType)) {
            build.append(valueFromMessage).append(" ").append(ops).append(" ").append(configuredRuleVal);
        } else if ("STRING".equalsIgnoreCase(attributeType)) {
            String upperCaseMapValue = ((String) valueFromMessage).toUpperCase();
            build.append("\"").append(upperCaseMapValue).append("\" ").append(ops).append(" ").append("\"")
                    .append(((String) configuredRuleVal).toUpperCase()).append("\"");
        } else {
            LOG.error(String.format("Invalid VARL_TY_CD:%s found while creating the logical expression %s %s %s"
                    , attributeType, valueFromMessage, ops, configuredRuleVal));
            return build.append("").toString();
        }
        return build.append(")").toString();
    }

    private Object constructConcatString(Map structuredPropertiesMap, Map ruleAttributeMapping) {

        StringBuilder populateStringBuilder = new StringBuilder();
        Collections.sort(expressionList);
        for (Expression expression : expressionList) {
            populateStringBuilder.append(parseInEqualities(structuredPropertiesMap,
                    ruleAttributeMapping, expression));
            populateStringBuilder.append(parseLogicalOperators(expression));
        }
        return populateStringBuilder.toString();
    }

    private String parseLogicalOperators(Expression expression) {

        if ("AND".equalsIgnoreCase(expression.getOperCd())) {
            return " && ";
        } else if ("OR".equalsIgnoreCase(expression.getOperCd())) {
            return " || ";
        } else if ("END".equalsIgnoreCase(expression.getOperCd())) {
            return "";
        } else {
            LOG.error(String.format("Invalid OPER_CD:%s found while joining multiple expressions: %s %s %s"
                    , expression.getOperCd(), expression.getVarlNm(), expression.getVarlOperCd(),
                    expression.getVarlValTx()));
            throw new SiddhiAppRuntimeException(String.format("Invalid OPER_CD:%s found while joining "
                            + "multiple expressions: %s %s %s", expression.getOperCd(), expression.getVarlNm(),
                    expression.getVarlOperCd(), expression.getVarlValTx()));
        }
    }

    @Override
    public Object processRemove(Object o) {

        return null;
    }

    @Override
    public Object processRemove(Object[] objects) {
        Expression expression = new Expression((String) objects[2], (String) objects[3],
                (String) objects[4], (String) objects[5], (String) objects[6],
                (String) objects[7], Integer.parseInt((String) objects[8]));
        expressionList.remove(expression);
        if (expressionList.size() == 0) {
            canDestroy = true;
        }
        return "";
    }

    @Override
    public boolean canDestroy() {

        return canDestroy;
    }

    @Override
    public Object reset() {

        expressionList.clear();
        canDestroy = true;
        return "";
    }

    @Override
    public Map<String, Object> currentState() {

        HashMap<String, Object> state = new HashMap<>();
        state.put("expressionList", expressionList);
        return state;
    }

    @Override
    public void restoreState(Map<String, Object> state) {
        expressionList = (List<Expression>) state.get("expressionList");
    }
}
