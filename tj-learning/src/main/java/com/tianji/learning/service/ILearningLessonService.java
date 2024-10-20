package com.tianji.learning.service;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.learning.domain.po.LearningLesson;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domain.vo.LearningLessonVO;

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
}
