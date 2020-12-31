package uk.nhs.adaptors.gp2gp.common.task;

import java.util.Optional;

import javax.jms.JMSException;
import javax.jms.Message;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.common.amqp.JmsReader;
import uk.nhs.adaptors.gp2gp.common.exception.TaskHandlerException;
import uk.nhs.adaptors.gp2gp.tasks.TaskDefinition;
import uk.nhs.adaptors.gp2gp.tasks.TaskDefinitionFactory;
import uk.nhs.adaptors.gp2gp.tasks.TaskExecutor;
import uk.nhs.adaptors.gp2gp.tasks.TaskExecutorFactory;

@Component
@AllArgsConstructor
@Slf4j
public class TaskHandler {
    private static final String MESSAGE_CLASS = "TaskName";

    private final TaskDefinitionFactory taskDefinitionFactory;
    private final TaskExecutorFactory taskExecutorFactory;

    public void handle(Message message) throws JMSException, JsonProcessingException, TaskHandlerException {
        var taskName = message.getStringProperty(MESSAGE_CLASS);
        var body = JmsReader.readMessage(message);

        Optional<TaskDefinition> taskDefinition = taskDefinitionFactory.getTaskDefinition(taskName, body);
        if (taskDefinition.isPresent()) {
            TaskExecutor taskExecutor = taskExecutorFactory.getTaskExecutor(taskDefinition.get()
                .getClass()
                .toString());
            taskExecutor.execute(taskDefinition.get());
        } else {
            LOGGER.error("Error while processing task definition from queue message {}", message.getJMSMessageID());
            throw new TaskHandlerException("No TaskDefinition Found for message: " + message.getJMSMessageID());
        }
    }
}