package com.tianji.learning.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.service.ILearningLessonService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 学生课程表 前端控制器
 * </p>
 *
 * @author chenw
 * @since 2024-10-20
 */
@RestController
@Api(tags = "我的课程的相关接口")
@RequestMapping("/lessons")
@RequiredArgsConstructor
public class LearningLessonController {


    private final ILearningLessonService lessonService;

    @GetMapping("page")
    @ApiOperation("分页查询我的课表")
    public PageDTO<LearningLessonVO> queryMyLessons(PageQuery query){
        return lessonService.queryMyLessons(query);
    }


    @GetMapping("now")
    @ApiOperation("查询正在学习的课程")
    public LearningLessonVO getMyCurrentLesson(){
        return lessonService.getMyCurrentLesson();
    }

}
