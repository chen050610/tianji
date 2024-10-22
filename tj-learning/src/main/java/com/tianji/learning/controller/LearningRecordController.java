package com.tianji.learning.controller;


import com.tianji.api.dto.leanring.LearningLessonDTO;
import com.tianji.api.dto.leanring.LearningRecordFormDTO;
import com.tianji.learning.domain.po.LearningRecord;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.service.ILearningRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 学习记录表 前端控制器
 * </p>
 *
 * @author chenw
 * @since 2024-10-21
 */
@RestController
@RequestMapping("/learning-records")
@RequiredArgsConstructor
@Api(tags = "学习记录相关接口")
public class LearningRecordController {

    final ILearningRecordService recordService;

    /**
     * @description: 该接口提供给course服务远程调用 当打开播放页面时 远程调用查询 当前课程的学习记录
     * @param:
     * @param courseId
     * @return: com.tianji.api.dto.leanring.LearningLessonDTO
     * @author chenw
     * @date: 2024/10/21 下午5:36
     */
    @GetMapping("course/{courseId}")
    @ApiOperation("查询当前用户指定课程的学习进度")
    public LearningLessonDTO queryLearningRecordByCourse(@PathVariable Long courseId){
        return recordService.queryLearningRecordByCourse(courseId);
    }

    /**
     * @description: 视频定期提交播放进度，需要记录下来，方便下次续播
     * 视频进度超过50%，或者是提交考试记录，则标记当前小节为已完成，用于统计已学习小节数家
     * 课表:
     * 学习视频需要更新最近学习的小节id和最近学习时间
     * 完成小节后要更新课表中的已学习小节数，如果数量等于课程总小节数量，则标记课程为已学完
     * @param:
 * @param dto
     * @return: void
     * @author chenw
     * @date: 2024/10/21 下午6:15
     */
    @PostMapping
    @ApiOperation("提交学习记录")
    public void addLearningRecord(@RequestBody @Validated LearningRecordFormDTO dto){
        recordService.addLearningRecord(dto);
    }

}
