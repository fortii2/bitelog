package me.forty2.controller;


import me.forty2.dto.Result;
import me.forty2.service.ShopTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/shop-type")
public class ShopTypeController {

    @Autowired
    private ShopTypeService typeService;

    @GetMapping("/list")
    public Result queryTypeList() {
        return typeService.queryType();
    }
}
