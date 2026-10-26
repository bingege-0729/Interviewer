//package net.xdclass.react;
//
//import com.alibaba.cloud.ai.graph.RunnableConfig;
//import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
//import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
//import com.alibaba.cloud.ai.graph.agent.hook.messages.AgentCommand;
//import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesModelHook;
//import com.alibaba.cloud.ai.graph.agent.hook.messages.UpdatePolicy;
//import org.springframework.ai.chat.messages.Message;
//
//import java.util.List;
//
//@HookPositions({HookPosition.AFTER_MODEL})
//public class MessageDeletionHook extends MessagesModelHook {
//    @Override
//    public String getName() {
//        return "message_delete";
//    }
//
//
//    @Override
//    public AgentCommand afterModel(List<Message> previousMessages, RunnableConfig config) {
//
//        if(previousMessages.size()>2){
//            List<Message> newMessages = previousMessages.subList(2, previousMessages.size());
//            return new AgentCommand(newMessages, UpdatePolicy.REPLACE);
//        }
//        return new AgentCommand(previousMessages);
//
//    }
//}
