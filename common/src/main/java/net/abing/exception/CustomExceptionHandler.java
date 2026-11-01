package net.abing.exception;

import lombok.extern.slf4j.Slf4j;
import net.abing.util.JsonData;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Set;

/**
 *
 * @Description
 * @Version 1.0
 **/

@RestControllerAdvice
@Slf4j
public class CustomExceptionHandler {

    /**
     * 业务异常
     */
    @ExceptionHandler(value = BizException.class)
    public JsonData handleBizException(BizException e){
        log.error("[业务异常]{}",e.getMsg());
        return JsonData.buildCodeAndMsg(e.getCode(),e.getMsg());
    }

    /**
     * @RequestBody参数校验异常（JSON格式）
     */
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public JsonData handleMethodArgumentNotValidException(MethodArgumentNotValidException e){
        BindingResult bindingResult = e.getBindingResult();
        if (bindingResult.hasErrors()) {
            List<ObjectError> errors = bindingResult.getAllErrors();
            if (!errors.isEmpty()) {
                FieldError fieldError = (FieldError) errors.get(0);
                log.error("[参数校验异常]字段:{}, 错误:{}", fieldError.getField(), fieldError.getDefaultMessage());
                return JsonData.buildCodeAndMsg(1, fieldError.getDefaultMessage());
            }
        }
        return JsonData.buildCodeAndMsg(1,"参数校验失败");
    }


    /**
     * @RequestParam/@PathVariable单个参数校验异常
     */
    @ExceptionHandler(value = ConstraintViolationException.class)
    public JsonData handleConstraintViolationException(ConstraintViolationException e){
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        if (!violations.isEmpty()) {
            ConstraintViolation<?> violation = violations.iterator().next();
            String message = violation.getMessage();
            return JsonData.buildCodeAndMsg(1, message);
        }
        return JsonData.buildCodeAndMsg(1,"参数约束校验失败");
    }

    /**
     * 资源未找到异常处理
     */
    @ExceptionHandler(value = NoResourceFoundException.class)
    public JsonData handleNoResourceFoundException(NoResourceFoundException e) {
        log.warn("[资源未找到] path: {}", e.getResourcePath());
        return JsonData.buildCodeAndMsg(404, "资源未找到");
    }

    /**
     * 兜底异常处理
     */
    @ExceptionHandler(value = Exception.class)
    public JsonData handleException(Exception e){
        log.error("[系统异常]", e);
        return JsonData.buildError("系统异常，请稍后重试");
    }

}
