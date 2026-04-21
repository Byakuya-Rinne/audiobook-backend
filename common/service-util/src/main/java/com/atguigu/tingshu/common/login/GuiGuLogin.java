package com.atguigu.tingshu.common.login;

import java.lang.annotation.*;

/**
 * 自定义认证注解 将来用在需要登录才能访问controller方法上
 * 元注解：
 *  @Target({ElementType.*})：注解使用位置 TYPE:类上 METHOD:方法上  PARAMETER：参数上
 *  @Retention(RetentionPolicy.*)：注解保留到哪个阶段 例如：SOURCE保留到源码节点，一旦编译为字节码注解消失
 *  @Inherited：标注该注解可以被继承
 *  @Documented：当执行javadoc命令时会生成到接口文档中
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface GuiGuLogin {

    boolean required() default true;

}
