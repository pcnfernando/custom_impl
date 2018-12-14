package org.wso2.extension.siddhi.execution.bny.streamprocessor;

import org.testng.annotations.Test;
import org.wso2.siddhi.core.SiddhiAppRuntime;
import org.wso2.siddhi.core.SiddhiManager;
import org.wso2.siddhi.core.event.Event;
import org.wso2.siddhi.core.stream.input.InputHandler;
import org.wso2.siddhi.core.stream.output.StreamCallback;
import org.wso2.siddhi.core.util.EventPrinter;

public class TestCaseOfBNYStreamProcessor {


    @Test
    public void testCreateFromJSONFunctionExtension() throws InterruptedException {
        SiddhiManager siddhiManager = new SiddhiManager();
        String inStreamDefinition = "\ndefine stream inputStream (exp string);";
        String query = ("@info(name = 'query1') from inputStream#bny:eval(exp) select * "
                + "insert into outputStream;");
        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(
                inStreamDefinition + query);
        siddhiAppRuntime.addCallback("outputStream", new StreamCallback() {
            @Override
            public void receive(Event[] inEvents) {
                EventPrinter.print(inEvents);
                }
            });
        InputHandler inputHandler = siddhiAppRuntime.getInputHandler("inputStream");
        siddhiAppRuntime.start();
        inputHandler.send(new Object[]{"(100 >= 1) && (200 < 9999999.999999) && \"SELL\"==\"SELL\""});
        siddhiAppRuntime.shutdown();
    }
}


