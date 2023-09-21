package com.itheima.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.Setmeal;

import java.util.List;

public interface SetmealService extends IService<Setmeal> {
    /*
    * 新增套餐，同时需保存套餐和菜品之间的关系
    * @param setmealDao
    * */
    public void saveWithDish(SetmealDto setmealDto);

    /*
    * 删除套餐，同时需要删除套餐和菜品的关联信息
    * @param ids
    * */
    public void removeWithDish(List<Long> ids);

    /*
    * 根据id查询套餐信息，以及对应的菜品信息
    * */
    public SetmealDto getByIdWithDish(Long id);

    /*
    * 更新套餐的同时，更新菜品信息
    * */
    public void updateWithDish(SetmealDto setmealDto);
}
