package net.xdclass.service;

public interface BenefitTaskService {

    boolean markFinish(String messageId,String businessId,String businessType);


    boolean rollback(String messageId,String businessId,String businessType,String failReason);
}
