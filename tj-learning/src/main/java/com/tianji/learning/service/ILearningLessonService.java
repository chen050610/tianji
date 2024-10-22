package com.tianji.learning.service;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.learning.domain.dto.LearningPlanDTO;
import com.tianji.learning.domain.po.LearningLesson;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.domain.vo.LearningPlanPageVO;

import java.util.List;

/**
 * <p>
 * 学生课程表 服务类
 * </p>
 *
 * @author chenw
 * @since 2024-10-20
 */
public interface ILearningLessonService extends IService<LearningLesson> {

    /**
     * @description:  分页查询我的课表
     * @param:
     * @param query
     * @return: com.tianji.common.domain.dto.PageDTO<com.tianji.learning.domain.vo.LearningLessonVO>
     * @author chenw
     * @date: 2024/10/20 下午2:42
     */
    PageDTO<LearningLessonVO> queryMyLessons(PageQuery query);

    /** 
     * @description: 添加课程到课表
     * @param: 
 * @param userId 
 * @param courseIds  
     * @return: void 
     * @author chenw
     * @date: 2024/10/20 下午3:23
     */ 
    void addUserLesson(Long userId, List<Long> courseIds);

    /** 
     * @description: 查询当前正在学习的课程
     * @param:
     * @return: com.tianji.learning.domain.vo.LearningLessonVO 
     * @author chenw
     * @date: 2024/10/20 下午3:58
     */ 
    LearningLessonVO getMyCurrentLesson();

    /**
     * @description: 检查课程是否有效
     * @param:
     * @param courseId  课程的id
     * @return: java.lang.Long  如果有效返回lessonId 如果没有肖返回空
     * @author chenw
     * @date: 2024/10/21 下午3:51
     */
    Long isLessonValid(Long courseId);

    /** 
     * @description: 删除该用户的课程
     * @param: 
 * @param userId 
 * @param courseIds  
     * @return: void 
     * @author chenw
     * @date: 2024/10/21 下午4:01
     */ 
    void removeLesson(Long userId, List<Long> courseIds);

    /** 
     * @description: 根据课程id，查询当前用户的课表中是否有该课程，如果有该课程则需要返回课程的学习进度、课程有效期等信息
     * @param: 
 * @param courseId
     * @return: com.tianji.learning.domain.vo.LearningLessonVO 
     * @author chenw
     * @date: 2024/10/21 下午4:10
     */ 
    LearningLessonVO getLessonStatus(Long courseId);

    /**
     * @description: 统计课程的学习人数
     * @param:
     * @param courseId
     * @return: java.lang.Integer
     * @author chenw
     * @date: 2024/10/21 下午4:21
     */
    Integer countLearningLessonByCourse(Long courseId);

    /** 
     * @description: 添加学习记录
     * @param: 
 * @param dto  
     * @return: void 
     * @author chenw
     * @date: 2024/10/22 下午8:42
     */ 
    void createLearningPlan(LearningPlanDTO dto);

    /** 
     * @description: 查询我的课程计划
     * @param: 
 * @param query  
     * @return: com.tianji.learning.domain.vo.LearningPlanPageVO 
     * @author chenw
     * @date: 2024/10/22 下午8:58
     */ 
    LearningPlanPageVO queryMyPlans(PageQuery query);
}
