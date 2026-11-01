package net.abing.service;

import net.abing.controller.req.AccountLoginReq;
import net.abing.controller.req.SendCheckCodeReq;
import net.abing.dto.AccountDTO;
import net.abing.dto.AccountLoginResultDTO;
import net.abing.util.JsonData;
import org.springframework.web.multipart.MultipartFile;

public interface AccountService {
    JsonData sendCheckCode(SendCheckCodeReq req);

    AccountLoginResultDTO login(AccountLoginReq req);

    AccountDTO detail();

    JsonData uploadAvatar(MultipartFile file);
}
