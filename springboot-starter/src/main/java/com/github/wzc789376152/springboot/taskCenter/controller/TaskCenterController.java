package com.github.wzc789376152.springboot.taskCenter.controller;

import com.github.wzc789376152.springboot.utils.TaskCenterUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/taskCenterBase")
public class TaskCenterController {
    @GetMapping("/redo")
    public Boolean redo(@RequestParam("id") Integer id, @RequestParam("timer") Integer timer) {
        if (timer == 5) {
            return false;
        }
        TaskCenterUtils.redo(id, timer);
        return true;
    }
}
