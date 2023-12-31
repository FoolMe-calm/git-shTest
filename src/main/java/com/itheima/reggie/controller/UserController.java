package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.User;
import com.itheima.reggie.service.UserService;
import com.itheima.reggie.utils.SMSUtils;
import com.itheima.reggie.utils.ValidateCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.Map;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {
    @Autowired
    private UserService userService;
    /*
    * 发送手机短信登陆
    * @param
    * @return
    * */
    @PostMapping("/sendMsg")
    public R<String> sendMsg(@RequestBody User user , HttpSession session){
        //获取手机号
        String phone = user.getPhone();
        if(StringUtils.isNotEmpty(phone)){
            //生成随机的4位验证码
            String code = ValidateCodeUtils.generateValidateCode(4).toString();
            log.info("code={}",code);
            //调用阿里云提供的短信服务api完成发送短信
            //SMSUtils.sendMessage("瑞吉外卖",phone,code);
            //需要生成的验证码保存到session中
            session.setAttribute(phone,code);
            return R.success("手机验证码发送成功");
        }
        return R.error("手机验证码发送失败");
    }

    /*
    *移动端用户登录
    * @param map
    * @param session
    * @return
    * */
    @PostMapping("/login")
    public R<User> login(@RequestBody Map map , HttpSession session){
        log.info(map.toString());
        //获取手机号
        String phone = map.get("phone").toString();
        //获取验证码
        String code = map.get("code").toString();
        //从session取出保存的验证码
        Object codeInSession = session.getAttribute(phone);
        //进行校验比对（页面提交的验证码和Session提交的验证码比对）
        if(codeInSession != null && codeInSession.equals(code)){
            //如果对比成功，说明登录成功
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getPhone,phone);

            User user = userService.getOne(queryWrapper);
            //判断该手机号是否为新用户，如果是则自动注册
            if(user == null){
                user = new User();
                user.setPhone(phone);
                user.setStatus(1);
                userService.save(user);
            }
            session.setAttribute("user",user.getId());
            return R.success(user);
        }
        return R.error("登录失败");
    }

    @PostMapping("/loginout")
    public R<String> loginout(HttpSession session){
        //去除seesion即可
        session.removeAttribute("user");

        return R.success("退出成功");
    }


}
