package org.wso2.extension.siddhi.execution.bny.streamprocessor;

import org.apache.log4j.Logger;
import org.wso2.extension.siddhi.execution.bny.util.RegexUtil;
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
import org.wso2.siddhi.core.executor.ExpressionExecutor;
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
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

/**
 * eval(POPULATED_EVAL_STR, NOTPOPULATED_EVAL_STR)
 * Returns the evaluation result and the matched condition.
 * Accept Type(s): (STRING, STRING)
 * Return Type(s): (STRING, STRING)
 */
@Extension(
        name = "eval",
        namespace = "bny",
        description = "Returns a string that is the result of concatenating two or more input string values.",
        parameters = {
                @Parameter(name = "exp",
                        description = "It can have two or more `string` type input parameters.",
                        type = {DataType.STRING})
        },
        returnAttributes = {
                @ReturnAttribute(name = "evalResult",
                        description = "The predicted value (`true/false`)",
                        type = {DataType.BOOL}),
                @ReturnAttribute(name = "matchedCondition",
                        description = "The probability of the prediction",
                        type = {DataType.STRING})
        },
        examples = @Example(description = "This returns a boolean value as evalResult by evaluating the passed "
                + "'POPULATED_EVAL_STR' and the matchedCondition. In this case, it will return true as the evalResult "
                + "and \"Amount > 5\" as the matchedCondition",
                syntax = "bny:eval(\"10 > 5\", \"Amount > 5\")")
)
public class EvaluateStreamProcessor extends StreamProcessor {

    private static final Logger log = Logger.getLogger(EvaluateStreamProcessor.class);
    private static final String ENGINE_NAME = "nashorn";
    private static final String OPERATOR_REGEX = "[<>=]+";
    private static final String LOGICAL_CONJUNCTION_REGEX = "\\|\\||\\&\\&";
    private static final String NON_ALPHA_NUMERIC_REGEX = "[^A-Za-z0-9.]";
    private static final String RULE_THRESHHOLD_CONSTANT = "RULETHRESHOLD";
    private static final String RULE_OPERATOR_CONSTANT = "RULEOPERATOR";
    private ScriptEngine engine;
    private final Object mutex = new Object();

    @Override
    protected List<Attribute> init(AbstractDefinition abstractDefinition, ExpressionExecutor[] expressionExecutors,
                                   ConfigReader configReader, SiddhiAppContext siddhiAppContext) {

        if (attributeExpressionExecutors.length == 1) {
            if (attributeExpressionExecutors[0] == null) {
                throw new SiddhiAppValidationException("bny:eval 1st parameter, logical expression needs " +
                        "to be non empty");
            }
        } else {
            throw new SiddhiAppValidationException("bny:eval should only have one parameter but found "
                    + attributeExpressionExecutors.length + " input attributes");
        }
        engine = new ScriptEngineManager().getEngineByName(ENGINE_NAME);
        List<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute("evalResult", Attribute.Type.BOOL));
        attributes.add(new Attribute("matchedConditionMap", Attribute.Type.OBJECT));
        return attributes;
    }

    @Override
    protected void process(ComplexEventChunk<StreamEvent> streamEventChunk, Processor processor,
                           StreamEventCloner streamEventCloner, ComplexEventPopulater complexEventPopulater) {
        while (streamEventChunk.hasNext()) {
            String expression = "";
            StreamEvent event = streamEventChunk.next();
            if (attributeExpressionExecutors[0].execute(event) != null) {
                expression = (String) attributeExpressionExecutors[0].execute(event);
            }
            boolean evalResult = execute(expression);
            Map<String, String> ruleMap = new HashMap<>();
            if (evalResult) {
                String[] tokens = expression.split(LOGICAL_CONJUNCTION_REGEX);
                for (int i = 0; i < tokens.length; i++) {
                    if (execute(tokens[i])) {
                        String operatorKey = RULE_OPERATOR_CONSTANT.concat(String.valueOf(i + 1));
                        String ruleKey = RULE_THRESHHOLD_CONSTANT.concat(String.valueOf(i + 1));
                        String operator = RegexUtil.runRegex(OPERATOR_REGEX, tokens[i], 0);
                        String rhs = "";
                        String[] operands = tokens[i].split(OPERATOR_REGEX);
                        if (operands.length == 2) {
                            rhs = operands[1].replaceAll(NON_ALPHA_NUMERIC_REGEX, "");
                        }
                        ruleMap.put(operatorKey, operator);
                        ruleMap.put(ruleKey, rhs);
                    }
                }
            }
            Object[] out = {evalResult, ruleMap};
            // If output has values, then add those values to output stream
            complexEventPopulater.populateComplexEvent(event, out);
        }
        nextProcessor.process(streamEventChunk);
    }

    private boolean execute(String expression) {

        if (log.isDebugEnabled()) {
            log.debug("Executing expression " + expression);
        }
        Object bool = false;
        try {
            if (engine != null) {
                if (expression != null || !(expression.isEmpty())) {
                    synchronized (mutex) {
                        bool = engine.eval(expression);
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("Result for the expression('" + expression + "') : " + bool);
                    }
                } else {
                    log.error("Expression is null or empty");
                }
            } else {
                log.error("Rule Engine is null.");
            }

            return (boolean) bool;
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.error("Error occurred evaluating expression, " + expression, e);
            }
            log.error("Error occurred evaluating expression, " + expression);
            return false;
        }
    }

    /**
     * Used to collect the serializable state of the processing element, that need to be
     * persisted for reconstructing the element to the same state on a different point of time
     *
     * @return stateful objects of the processing element as an map
     */
    @Override
    public Map<String, Object> currentState() {

        return new HashMap<>();
    }

    /**
     * Used to restore serialized state of the processing element, for reconstructing
     * the element to the same state as if was on a previous point of time.
     *
     * @param state the stateful objects of the processing element as a map.
     *              This is the same map that is created upon calling currentState() method.
     */
    @Override
    public void restoreState(Map<String, Object> state) {

    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    public static void main(String[] args) {

    }
}
