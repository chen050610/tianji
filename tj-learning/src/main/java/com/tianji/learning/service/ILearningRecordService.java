package com.tianji.learning.service;

import com.tianji.api.dto.leanring.LearningLessonDTO;
import com.tianji.api.dto.leanring.LearningRecordFormDTO;
import com.tianji.learning.domain.po.LearningRecord;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 学习记录表 服务类
 * </p>
 *
 * @author chenw
 * @since 2024-10-21
 */
public interface ILearningRecordService extends IService<LearningRecord> {

    /**
     * @description: 查询当前用户指定课程的学习进度
     * @param:
     * @param courseId
     * @return: com.tianji.api.dto.leanring.LearningLessonDTO
     * @author chenw
     * @date: 2024/10/21 下午5:35
     */
    LearningLessonDTO queryLearningRecordByCourse(Long courseId);

    /** 
     * @description: 提交学习记录
     * @param: 
 * @param dto  
     * @return: void 
     * @author chenw
     * @date: 2024/10/21 下午6:16
     */ 
    void addLearningRecord(LearningRecordFormDTO dto);
}
