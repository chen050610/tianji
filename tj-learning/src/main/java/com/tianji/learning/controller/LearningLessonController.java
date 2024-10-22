package com.tianji.learning.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.learning.domain.dto.LearningPlanDTO;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.domain.vo.LearningPlanPageVO;
import com.tianji.learning.service.ILearningLessonService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("{courseId}/valid")
    @ApiOperation("检查指定课程的状态是否有效")
    public Long isLessonValid(@PathVariable Long courseId){
        return lessonService.isLessonValid(courseId);
    }

    @GetMapping("{courseId}")
    @ApiOperation("查询用户课表中指定课程状态")
    public LearningLessonVO getLessonStatus(@PathVariable Long courseId){
        return lessonService.getLessonStatus(courseId);
    }

    @GetMapping("/{courseId}/count")
    @ApiOperation("统计课程的学习人数")
    public Integer countLearningLessonByCourse(@PathVariable("courseId") Long courseId){
        return lessonService.countLearningLessonByCourse(courseId);
    }

    @ApiOperation("添加课程的学习计划")
    @PostMapping("plans")
    public void createLearningPlan(@RequestBody @Validated LearningPlanDTO dto){
        lessonService.createLearningPlan(dto);
    }

    @ApiOperation("查询我的课程计划")
    @GetMapping("plans")
    public LearningPlanPageVO queryMyPlans(PageQuery query){
        return lessonService.queryMyPlans(query);
    }


}
