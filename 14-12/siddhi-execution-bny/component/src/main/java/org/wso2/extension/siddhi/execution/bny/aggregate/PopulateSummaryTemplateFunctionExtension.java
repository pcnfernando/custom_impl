package org.wso2.extension.siddhi.execution.bny.aggregate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.extension.siddhi.execution.bny.util.CreateMapFromJSON;
import org.wso2.extension.siddhi.execution.bny.util.PopulateFromMap;
import org.wso2.extension.siddhi.execution.bny.util.RegexUtil;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * populateSummary(alertCategory, summaryType, concatenatedTemplate, batchTemplate, actionString)
 * Returns summary email String populated from the provided template.
 * Accept Type(s): (STRING, STRING, STRING, STRING, STRING, STRING)
 * Return Type(s): STRING
 */
@Extension(
        name = "populateSummary",
        namespace = "bny",
        description = "Returns summary email populated from the provided template ",
        parameters = {
                @Parameter(name = "alert.category",
                        description = "ALR_TY_CD of the alert sent",
                        type = {DataType.STRING}),
                @Parameter(name = "summary.type",
                        description = "Type of summary. It can be either 'A' or 'T'. Type 'A' stands for "
                                + "summary created from multiple alert categories where as 'T' stands for "
                                + "a single alert category",
                        type = {DataType.STRING}),
                @Parameter(name = "concatenated.template",
                        description = "Concatenated summary email templates",
                        type = {DataType.STRING}),
                @Parameter(name = "batch.template",
                        description = "Batch Summary Template for the specific alert.category",
                        type = {DataType.STRING}),
                @Parameter(name = "action.string",
                        description = "JSON formatted action string of the sent-out alert",
                        type = {DataType.STRING}),

        },
        returnAttributes = @ReturnAttribute(
                description = "This returns the summary email for the sent-out alerts provided.",
                type = {DataType.OBJECT}),
        examples = @Example(description = "This returns summary created for the provided sent-out alerts. ",
                syntax = "populateSummary(alertCategory, summaryType, concatenatedTemplate, " +
                        "batchTemplate, actionString)")
)
public class PopulateSummaryTemplateFunctionExtension extends AttributeAggregator {
    private static final Logger LOG = LoggerFactory.getLogger(PopulateSummaryTemplateFunctionExtension.class);
    private static final String SUMMARY_HEADER_REGEX = "(BatchEmail_GroupHeader\\?=\\?)(.+?)\\*=\\*";
    private static final String SUMMARY_FOOTER_REGEX = "(BatchEmail_GroupFooter\\?=\\?)(.+?)\\*=\\*";

    private String summaryHeader;
    private String summaryFooter;
    private String concatenatedTemplate;
    private Attribute.Type returnType = Attribute.Type.STRING;
    private Map<String, List<String>> map;
    private boolean canDestroy = true;

    @Override
    protected void init(ExpressionExecutor[] attributeExpressionExecutors, ConfigReader configReader,
                        SiddhiAppContext siddhiAppContext) {
        if (attributeExpressionExecutors.length != 5) {
            throw new SiddhiAppValidationException("bny:populateSummary() function requires exactly 5 parameters, "
                    + "namely AlertCategory, SummaryType, ConcatenatedTemplateString, BatchTemplate, ActionString");
        }
        for (int i = 0; i < attributeExpressionExecutors.length; i++) {
            if (attributeExpressionExecutors[i] instanceof VariableExpressionExecutor) {
                if (attributeExpressionExecutors[i].getReturnType() != Attribute.Type.STRING) {
                    throw new SiddhiAppValidationException("Invalid parameter type found as the " + i
                            + "argument," + " required " + Attribute.Type.STRING + " but found "
                            + attributeExpressionExecutors[i].getReturnType().toString());
                }
            } else {
                throw new SiddhiAppValidationException("Parameter attribute.mapping must be a variable but found "
                        + attributeExpressionExecutors[i].getClass().getCanonicalName());
            }
        }
        map = new HashMap<>();
    }

    @Override
    public Attribute.Type getReturnType() {
        return returnType;
    }

    @Override
    public Object processAdd(Object o) {
        return null; //Since the populateSummary function requires more than one parameter,
        // this method does not get called. Hence, not implemented.
    }

    @Override
    public synchronized Object processAdd(Object[] objects) {
        for (int i = 0; i < objects.length; i++) {
            if (objects[i] == null) {
                throw new SiddhiAppRuntimeException(String.format("Invalid input given to str:populateSummary() " +
                        "function. %s argument cannot be null", (i + 1)));
            }
        }
        String alertCategory = (String) objects[0];
        String summaryType = (String) objects[1];
        concatenatedTemplate = (String) objects[2];
        String batchTemplate = (String) objects[3];
        String actionString = (String) objects[4];

        if (map.get(alertCategory) == null) {
            List<String> list = new ArrayList<>();
            map.put(alertCategory, list);
        }
        if (summaryHeader == null) {
            summaryHeader = RegexUtil.runRegex(SUMMARY_HEADER_REGEX, concatenatedTemplate, 2);
        }
        if (summaryFooter == null) {
            summaryFooter = RegexUtil.runRegex(SUMMARY_FOOTER_REGEX, concatenatedTemplate, 2);
        }
        Map propertiesMap = (Map) CreateMapFromJSON.createMapFromJson(actionString);
        String populatedTemplate = PopulateFromMap.populateTemplateFromMap(propertiesMap, batchTemplate);
        map.get(alertCategory).add(populatedTemplate);
        return constructConcatString();
    }

    private Object constructConcatString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(summaryHeader);
        Iterator<Map.Entry<String, List<String>>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, List<String>> pair = (Map.Entry<String, List<String>>) iterator.next();
            String groupHeaderRegex = "(BatchEmail_GroupHeader_";
            groupHeaderRegex = groupHeaderRegex.concat(pair.getKey()).concat("\\?=\\?)(.+?)\\*=\\*");
            String typeHeader = RegexUtil.runRegex(groupHeaderRegex, concatenatedTemplate, 2);
            stringBuilder.append(typeHeader);
            List<String> value = (ArrayList<String>) pair.getValue();
            for (String message : value) {
                stringBuilder.append(message);
            }
            String groupFooterRegex = "(BatchEmail_GroupFooter_";
            groupFooterRegex = groupFooterRegex.concat(pair.getKey()).concat("\\?=\\?)(.+?)\\*=\\*");
            String typeFOOTER = RegexUtil.runRegex(groupFooterRegex, concatenatedTemplate, 2);
            stringBuilder.append(typeFOOTER);
        }
        stringBuilder.append(summaryFooter);
        return stringBuilder.toString();
    }

    @Override
    public Object processRemove(Object o) {
        return null;
    }

    @Override
    public Object processRemove(Object[] objects) {
        map.remove(objects[0]);
        if (map.size() == 0) {
            canDestroy = true;
        }
        return constructConcatString();
    }

    @Override
    public boolean canDestroy() {
        return canDestroy;
    }

    @Override
    public Object reset() {
        map.clear();
        canDestroy = true;
        return "";
    }

    @Override
    public Map<String, Object> currentState() {
        HashMap<String, Object> state = new HashMap<>();
        state.put("dataSet", map);
        return state;
    }

    @Override
    public void restoreState(Map<String, Object> state) {
        map = (Map<String, List<String>>) state.get("dataSet");
    }
}


