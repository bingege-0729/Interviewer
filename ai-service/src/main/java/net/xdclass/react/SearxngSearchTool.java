//package net.xdclass.react;
//
//import lombok.AllArgsConstructor;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import net.xdclass.config.SearxngProperties;
//import org.springframework.ai.chat.model.ToolContext;
//import org.springframework.stereotype.Component;
//import org.springframework.util.CollectionUtils;
//import org.springframework.web.client.RestTemplate;
//
//import java.util.List;
//import java.util.function.BiFunction;
//import java.util.stream.Collectors;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class SearxngSearchTool implements BiFunction<String, ToolContext,String> {
//
//
//    private final RestTemplate restTemplate;
//
//    private final SearxngProperties searxngProperties;
//
//    @Override
//    public String apply(String s, ToolContext toolContext) {
//        return search(s);
//    }
//
//    private String search(String query) {
//        log.info("query: {}", query);
//        try {
//
//            String url = searxngProperties.getBaseUrl() + "/search?q=" + query + "&format=" + searxngProperties.getFormat() + "&language=" + searxngProperties.getLanguage() + "&engines=" + searxngProperties.getEngines();
//            SearxngResponse searxngResponse = restTemplate.getForObject(url, SearxngResponse.class);
//            if(searxngResponse == null || CollectionUtils.isEmpty(searxngResponse.getResults())){
//                log.warn("查询结果为空");
//                return "没有找到结果";
//            }
//
//            String collect = searxngResponse.getResults().stream()
//                    .map(r -> "标题" + r.getTitle() + "\n链接" + r.getUrl() + "\n摘要" + r.getContent())
//                    .collect(Collectors.joining("\n"));
//
//            log.info("查询结果:{}", collect);
//            return collect;
//        }catch (Exception e){
//            log.error("查询失败:{}", e.getMessage(), e);
//            return "搜索服务暂时不可用，请稍后尝试";
//        }
//    }
//
//
//    @Data
//    @AllArgsConstructor
//    @NoArgsConstructor
//    public static class SearxngResponse{
//        private List<SearchResult> results;
//    }
//
//
//
//    @Data
//    @AllArgsConstructor
//    @NoArgsConstructor
//    public static class SearchResult {
//        private String title;
//        private String url;
//        private String content;
//    }
//
//}
