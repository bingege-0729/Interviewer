//package net.abing.react;
//
//import com.alibaba.cloud.ai.graph.RunnableConfig;
//import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
//import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
//import com.alibaba.cloud.ai.graph.agent.hook.messages.AgentCommand;
//import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesModelHook;
//import com.alibaba.cloud.ai.graph.agent.hook.messages.UpdatePolicy;
//import org.springframework.ai.chat.messages.Message;
//
//import java.util.ArrayList;
//import java.util.List;
//
//@HookPositions({HookPosition.BEFORE_MODEL})
//public class MessageTrimmingHook extends MessagesModelHook {
//
//
//    private static final int MAX_MESSAGE = 3;
//
//
//    @Override
//    public String getName() {
//        return "message_trimming";
//    }
//
//    @Override
//    public AgentCommand beforeModel(List<Message> previousMessages, RunnableConfig config) {
//
//        //判断消息数量
//        if(previousMessages.size()<= MAX_MESSAGE){
//            return new AgentCommand(previousMessages);
//        }
//
//        //截取消息
//        Message firstMsg = previousMessages.get(0);
//
//        // 保留最新的几条消息（保证消息对数偶数对齐：user/assistant交替）
//        int keepCount = previousMessages.size() % 2 == 0 ? 3 : 4;
//        List<Message> recentMessages = previousMessages.subList(
//                previousMessages.size() - keepCount,
//                previousMessages.size()
//        );
//
//        List<Message> trimList = new ArrayList<>();
//        trimList.add(firstMsg);
//        trimList.addAll(recentMessages);
//
//
//        return new AgentCommand(trimList, UpdatePolicy.REPLACE);
//    }
//
//
//}
