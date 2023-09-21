package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.OrdersDto;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.*;
import com.itheima.reggie.service.OrderDetailService;
import com.itheima.reggie.service.OrdersService;
import com.itheima.reggie.service.ShoppingCartService;
import com.itheima.reggie.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/*
* 订单
* */
@Slf4j
@RestController
@RequestMapping("/order")
public class OrderController {
    @Autowired
    private OrdersService ordersService;
    @Autowired
    private UserService userService;
    @Autowired
    private OrderDetailService orderDetailService;
    @Autowired
    private ShoppingCartService shoppingCartService;
    /*
    * 用户下单
    * @param orders
    * @return
    * */
    @PostMapping("/submit")
    public R<String> submit(@RequestBody Orders orders){
        log.info("订单数据：{}",orders);
        ordersService.submit(orders);
        return R.success("下单成功");
    }

    /*
     *订单回显
     * @param page
     * @param pageSize
     * @param name
     * @return
     * */
    @GetMapping("/page")
    public R<Page> page(int page , int pageSize , Long number , Date beginTime , Date endTime){
        //构造分页构造器
        Page<Orders> pageInfo = new Page<>(page,pageSize);
        Page<OrdersDto> ordersDtoPage = new Page<>();
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        //添加查询条件，根据number进行like模糊查询
        queryWrapper.like(number != null,Orders::getId,number);

        //添加限制条件
        if(beginTime != null && endTime != null){
            queryWrapper.between(Orders::getOrderTime,beginTime,endTime);
        }
        ordersService.page(pageInfo,queryWrapper);

        //对象拷贝
        BeanUtils.copyProperties(pageInfo,ordersDtoPage,"records");
        List<Orders> records = pageInfo.getRecords();

        List<OrdersDto> list = records.stream().map((item)->{
            OrdersDto ordersDto = new OrdersDto();
            //对象拷贝
            BeanUtils.copyProperties(item,ordersDto);
            //用户id
            Long userId = item.getUserId();
            //通过用户id查找用户对象
            User user = userService.getById(userId);
            if(user != null){
                //用户名称
                String userName = user.getName();
                ordersDto.setUserName(userName);
            }
            return ordersDto;
        }).collect(Collectors.toList());
        ordersDtoPage.setRecords(list);
        return R.success(ordersDtoPage);
    }

    /*
    * 状态修改
    * @param orders
    * @return
    * */
    @PutMapping
    public R<String> beginSend(@RequestBody Orders orders){
        ordersService.updateById(orders);
        return R.success("修改成功");
    }

    /*
    * 查看手机历史记录
    * @page
    * @pageSize
    * @return
    * */
    @GetMapping("/userPage")
    public R<Page> pagePhone(int page,int pageSize){
        //新创返回类型Page
        Page<Orders> pageInfo = new Page<>(page,pageSize);
        Page<OrdersDto> ordersDtoPage = new Page<>();
        //用户ID
        Long currentId = BaseContext.getCurrentId();
        //原条件写入
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Orders::getUserId,currentId);
        queryWrapper.orderByDesc(Orders::getOrderTime);

        ordersService.page(pageInfo,queryWrapper);
        //普通赋值
        BeanUtils.copyProperties(pageInfo,ordersDtoPage,"records");
        //订单赋值
        List<Orders> records = pageInfo.getRecords();

        List<OrdersDto> ordersDtoList = records.stream().map((item) ->{
            //创建内部元素
            OrdersDto ordersDto = new OrdersDto();
            //普通元素赋值
            BeanUtils.copyProperties(item,ordersDto);
            //菜单详情赋值
            //select *from orderDetial where id=''
            Long itemId = item.getId();

            LambdaQueryWrapper<OrderDetail> orderDetailLambdaQueryWrapper = new LambdaQueryWrapper<>();
            orderDetailLambdaQueryWrapper.eq(OrderDetail::getOrderId,itemId);

            int count = orderDetailService.count(orderDetailLambdaQueryWrapper);
            List<OrderDetail> orderDetailList = orderDetailService.list(orderDetailLambdaQueryWrapper);
            ordersDto.setSumNum(count);
            ordersDto.setOrderDetails(orderDetailList);
            return ordersDto;
        }).collect(Collectors.toList());
        ordersDtoPage.setRecords(ordersDtoList);
        return R.success(ordersDtoPage);
    }

    /*
    * 用户点击再来一单
    * 我们需要将订单内的菜品重新加入购物车，所以在此之前我们需要清空购物车（业务层方法）
    * */
    @PostMapping("/again")
    public R<String> againSubmit(@RequestBody Map<String,String> map){
        //获取ID
        String ids = map.get("id");
        Long id = Long.parseLong(ids);
        //制作判断条件
        LambdaQueryWrapper<OrderDetail> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderDetail::getOrderId,id);
        //获取用户对应的所有对应订单明细表
        List<OrderDetail> orderDetailList = orderDetailService.list(queryWrapper);
        //通过用户id把原来的购物车清空
        shoppingCartService.clean();

        //获取用户id
        Long userId = BaseContext.getCurrentId();
        //给整体赋值
        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map((item)->{
            //以下均为赋值操作
            ShoppingCart shoppingCart = new ShoppingCart();
            shoppingCart.setUserId(userId);
            shoppingCart.setImage(item.getImage());

            Long dishId = item.getDishId();
            Long setmealId = item.getSetmealId();

            if(dishId != null){
                //如果是菜品就添加菜品的查询条件
                shoppingCart.setDishId(dishId);
            }else {
                //如果是套餐就添加套餐的查询条件
                shoppingCart.setSetmealId(setmealId);
            }
            shoppingCart.setName(item.getName());
            shoppingCart.setDishFlavor(item.getDishFlavor());
            shoppingCart.setNumber(item.getNumber());
            shoppingCart.setAmount(item.getAmount());
            shoppingCart.setCreateTime(LocalDateTime.now());
            return shoppingCart;
        }).collect(Collectors.toList());
        //将携带数据的购物车批量插入购物表中
        shoppingCartService.saveBatch(shoppingCartList);
        return R.success("操作成功");
    }
}
