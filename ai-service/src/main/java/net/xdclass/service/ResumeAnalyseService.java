package net.xdclass.service;

import net.xdclass.dto.ResumeAnalyseMessageDTO;

public interface ResumeAnalyseService {
    boolean processResumeAnalyse(ResumeAnalyseMessageDTO messageDTO);
}
