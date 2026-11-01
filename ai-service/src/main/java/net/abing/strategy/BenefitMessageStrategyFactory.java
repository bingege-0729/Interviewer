package net.abing.strategy;

import lombok.extern.slf4j.Slf4j;
import net.abing.enums.BenefitEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@Slf4j
public class BenefitMessageStrategyFactory {


    private final Map<String, BenefitMessageStrategy> strategyMap = new ConcurrentHashMap<String, BenefitMessageStrategy>();

    /**
     * 通过构造函数注入全部策略实现类
     */
    @Autowired
    public BenefitMessageStrategyFactory(List<BenefitMessageStrategy> list) {
        for(BenefitMessageStrategy strategy : list){
            strategyMap.put(strategy.getStrategyType(), strategy);
            log.info("注册权益消息策略:{}->{}", strategy.getStrategyType(), strategy.getClass().getSimpleName());
        }
    }


    /**
     * 根据用户传递的权益编码获取策略
     */
    public BenefitMessageStrategy getStrategy(String strategyType){
        BenefitMessageStrategy strategy = strategyMap.get(strategyType);

        //先精确匹配
        if(strategy != null){
            return strategy;
        }

        if(isInterviewType(strategyType)){
            return strategyMap.get(BenefitEnum.INTERVIEW.name());
        }

        throw new IllegalArgumentException("不支持的权益编码");
    }


    private boolean isInterviewType(String benefitCode){
        BenefitEnum benefitEnum = BenefitEnum.valueOf(benefitCode);
        return benefitEnum == BenefitEnum.SPECIAL_INTERVIEW || benefitEnum == BenefitEnum.COMMON_INTERVIEW;
    }


}
