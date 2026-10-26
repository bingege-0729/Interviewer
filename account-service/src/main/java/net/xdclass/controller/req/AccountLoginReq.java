package net.xdclass.controller.req;

import lombok.Data;

@Data
public class AccountLoginReq {

    private String identifier;

    private String checkCode;

    private String type;
}
