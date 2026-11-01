package net.abing.service;

import net.abing.dto.ResumeAnalyseMessageDTO;

public interface ResumeAnalyseService {
    boolean processResumeAnalyse(ResumeAnalyseMessageDTO messageDTO);
}
