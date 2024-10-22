package com.tianji.learning.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.db.DbRuntimeException;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.api.dto.leanring.LearningLessonDTO;
import com.tianji.api.dto.leanring.LearningRecordDTO;
import com.tianji.api.dto.leanring.LearningRecordFormDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.exceptions.DbException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.ObjectUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.po.LearningRecord;
import com.tianji.learning.enums.LessonStatus;
import com.tianji.learning.enums.SectionType;
import com.tianji.learning.mapper.LearningLessonMapper;
import com.tianji.learning.mapper.LearningRecordMapper;
import com.tianji.learning.service.ILearningRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.input.TaggedReader;
import org.springframework.stereotype.Service;

import javax.xml.stream.events.EndElement;
import java.util.List;

/**
 * <p>
 * 学习记录表 服务实现类
 * </p>
 *
 * @author chenw
 * @since 2024-10-21
 */
@Service
@RequiredArgsConstructor
public class LearningRecordServiceImpl extends ServiceImpl<LearningRecordMapper, LearningRecord> implements ILearningRecordService {
    final LearningLessonMapper lessonMapper;
    final CourseClient courseClient;
    @Override
    public LearningLessonDTO queryLearningRecordByCourse(Long courseId) {
        if (courseId == null){
            throw new BadRequestException("课程id不能为空");
        }
        //1.获取当前的用户
        Long userId = UserContext.getUser();
        if (userId == null){
            throw new BadRequestException("当前用户未登录");
        }
        //2.根据userid和courseId 去Learning_lesson表中查询id
        LearningLesson lesson = lessonMapper.selectOne(Wrappers.<LearningLesson>lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getCourseId, courseId)
                );
        if (lesson == null){
            throw new BizIllegalException("该课程未加入课表");
        }
        Long lessonId = lesson.getId();
        //3.根据查询到的lessonId在record表中查询
        List<LearningRecord> recordList = this.lambdaQuery()
                .eq(LearningRecord::getUserId, userId)
                .eq(LearningRecord::getLessonId, lessonId)
                .list();
        LearningLessonDTO returnDto = new LearningLessonDTO();
        returnDto.setLatestSectionId(lesson.getLatestSectionId()); //最近学习的小节id
        returnDto.setId(lessonId);
        returnDto.setRecords(BeanUtil.copyToList(recordList, LearningRecordDTO.class));
        return returnDto;
    }

    @Override
    public void addLearningRecord(LearningRecordFormDTO dto) {
        //1.获取当前的登录用户
        Long userId = UserContext.getUser();
        if (userId==null){
            throw new BadRequestException("当前用户未登录");
        }
        boolean isCompleted = false; //判断本小节是否已经学完
        //2.处理学习记录
        if (ObjectUtils.equal(dto.getSectionType(), SectionType.EXAM))
        {
            //2.1处理考试

           isCompleted = handlerExamRecord(dto,userId);
        } else if (ObjectUtils.equal(dto.getSectionType(),SectionType.VIDEO))
        {
            //2.2处理视频播放记录
           isCompleted = handlerVideoRecord(dto,userId);
        } else {
            throw new BadRequestException("参数异常");
        }
        //3.处理课表数据
        handlerLessonData(dto,isCompleted);
    }

    private void handlerLessonData(LearningRecordFormDTO dto, boolean isCompleted) {
        //TODO:无论是否时第一次学完 都需要更新Learning_lesson表中的最近学习的时间
        //1.查询课表
        LearningLesson lesson = lessonMapper.selectById(dto.getLessonId());
        if (lesson == null){
            throw new BizIllegalException("课表不存在");
        }
        //2.判断是否是第一次学玩
        boolean allFinish = false; //所有的小节是否全部已经学完
        if (isCompleted){
            //3.远程调用课程服务 得到课程信息 课程的小节总数
            CourseFullInfoDTO courseInfo = courseClient
                    .getCourseInfoById(lesson.getCourseId(), false, false);
            if (courseInfo == null){
                throw new BizIllegalException("课程不存在");
            }
            Integer totalSectionNum = courseInfo.getSectionNum();
            //4.如果isCompleted为true 本小节是第一次学完 判断该用户对该课程下的全部小节是否全部的学完
            Integer learnSectioned = lesson.getLearnedSections();
            allFinish = learnSectioned + 1 >= totalSectionNum;
        }
        //5.更新
        LambdaUpdateWrapper<LearningLesson> wrapper = Wrappers.<LearningLesson>lambdaUpdate()
                .set(lesson.getStatus() == LessonStatus.NOT_BEGIN, LearningLesson::getStatus, LessonStatus.LEARNING)
                .set(allFinish, LearningLesson::getStatus, LessonStatus.FINISHED)
                .set(LearningLesson::getLatestSectionId, dto.getSectionId())
                .set(LearningLesson::getLatestLearnTime, dto.getCommitTime())
                .setSql(isCompleted, "learning_sections = learning_sections + 1")
                .eq(LearningLesson::getId, dto.getLessonId());
        lessonMapper.update(null,wrapper);
    }

    private boolean handlerVideoRecord(LearningRecordFormDTO dto, Long userId) {
        //1.查询旧的学习记录 条件 lessonId userId sectionId
        LearningRecord record = this.lambdaQuery()
                .eq(LearningRecord::getLessonId, dto.getLessonId())
                .eq(LearningRecord::getSectionId, dto.getSectionId())
                .eq(LearningRecord::getUserId, userId)
                .one();
        //2.判断是否存在
        if (record == null){
            //2.1 如果不存在则新增学习记录
            //2.1.1.dto --> po
            LearningRecord result =
                    BeanUtils.copyBean(dto, LearningRecord.class);
            result.setUserId(userId);
            //保存学习记录
            boolean flag = this.save(result);
            if (!flag){
                throw new DbException("新增考试记录失败");
            }
            return false;
        }
        //2.2如果存在则更新学习记录 更新moment
        //2.2.1 判断本小节是否是第一次完成
        //如果之前没有完成 并且这次的观看时长超过50% 证明是第一次完成
        boolean flag = !record.getFinished() && dto.getDuration() <= dto.getMoment() * 2;
        boolean result = this.lambdaUpdate()
                .set(LearningRecord::getMoment, dto.getMoment())
                .set(flag, LearningRecord::getFinished, true)
                .set(flag, LearningRecord::getFinishTime, dto.getCommitTime())
                .eq(LearningRecord::getId, record.getId())
                .update();
        if (!result){
            throw new DbException("更新视频学习进度失败");
        }
        return flag;

    }

    /** 
     * @description: 处理考试记录
     * @param: 
 * @param dto 
 * @param userId  
     * @return: boolean 
     * @author chenw
     * @date: 2024/10/21 下午6:28
     */ 
    private boolean handlerExamRecord(LearningRecordFormDTO dto, Long userId) {
        //1.dto --> po
        LearningRecord result =
                BeanUtils.copyBean(dto, LearningRecord.class);
        result.setUserId(userId);
        result.setFinished(true); //代表本小节已经学完
        result.setFinishTime(dto.getCommitTime());
        //保存学习记录
        boolean flag = this.save(result);
        if (!flag){
            throw new DbException("新增考试记录失败");
        }

        return true;
    }
}
