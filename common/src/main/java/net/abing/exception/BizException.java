package net.abing.exception;

import net.abing.enums.BizCodeEnum;

/**
 *
 * @Description
 * @Version 1.0
 **/

public class BizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int code;

    private final String msg;


    public BizException(BizCodeEnum bizCodeEnum){
        super(bizCodeEnum.getMessage());
        this.code = bizCodeEnum.getCode();
        this.msg = bizCodeEnum.getMessage();
    }

    public BizException(BizCodeEnum bizCodeEnum, Exception e){
        super(bizCodeEnum.getMessage(), e);
        this.code = bizCodeEnum.getCode();
        this.msg = bizCodeEnum.getMessage();
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }





}
