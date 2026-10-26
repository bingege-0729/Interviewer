package net.xdclass.service;

import net.xdclass.controller.req.AccountLoginReq;
import net.xdclass.controller.req.SendCheckCodeReq;
import net.xdclass.dto.AccountDTO;
import net.xdclass.dto.AccountLoginResultDTO;
import net.xdclass.util.JsonData;
import org.springframework.web.multipart.MultipartFile;

public interface AccountService {
    JsonData sendCheckCode(SendCheckCodeReq req);

    AccountLoginResultDTO login(AccountLoginReq req);

    AccountDTO detail();

    JsonData uploadAvatar(MultipartFile file);
}
