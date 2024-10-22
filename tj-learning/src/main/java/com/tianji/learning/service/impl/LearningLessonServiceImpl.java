package com.tianji.learning.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDto;
import com.tianji.api.client.course.CatalogueClient;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.dto.course.*;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.LearningPlanDTO;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.domain.vo.LearningPlanPageVO;
import com.tianji.learning.enums.LessonStatus;
import com.tianji.learning.enums.PlanStatus;
import com.tianji.learning.mapper.LearningLessonMapper;
import com.tianji.learning.service.ILearningLessonService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.events.Event;

import java.nio.channels.WritePendingException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 学生课程表 服务实现类
 * </p>
 *
 * @author chenw
 * @since 2024-10-20
 */
@Service
@RequiredArgsConstructor
public class LearningLessonServiceImpl extends ServiceImpl<LearningLessonMapper, LearningLesson> implements ILearningLessonService {
    final CourseClient courseClient;
    final CatalogueClient catalogueClient;
    @Override
    public PageDTO<LearningLessonVO> queryMyLessons(PageQuery query) {
        //1.获取当前登录的用于
        Long userId = UserContext.getUser();
        if (userId == null){
            throw new BadRequestException("用户未登录");
        }
        //2.分页查询 默认按照最近学习的课程降序排序
        Page<LearningLesson> page = this.lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .page(query.toMpPage("latest_learning_time", false));
        List<LearningLesson> results = page.getRecords();
        if (CollUtil.isEmpty(results)){
            return PageDTO.empty(page);
        }
        //3.远程带用课程微服务 获取 课程名字 封面和章节数
        List<CourseSimpleInfoDTO> infoList = courseClient
                .getSimpleInfoList(results.stream().map(LearningLesson::getCourseId).collect(Collectors.toList()));
        if (CollUtil.isEmpty(infoList)){
            throw new BizIllegalException("课程不存在");
        }
        /*将远程服务查询的结果 封装成 map的结构 key是课程的id 结果是CourseSimpleInfoDTO 方便后续的vo对象的填充*/
        Map<Long, CourseSimpleInfoDTO> resultMap
                = infoList.stream().collect(Collectors.toMap(CourseSimpleInfoDTO::getId, c -> c));
        //4.po转vo返回
        List<LearningLessonVO> returnVoResult = BeanUtil.copyToList(results, LearningLessonVO.class);
        for (LearningLessonVO lessonVO : returnVoResult) {
            Long courseId = lessonVO.getCourseId();
            CourseSimpleInfoDTO csid = resultMap.get(courseId);
            if (csid!=null){
                lessonVO.setCourseName(csid.getName());
                lessonVO.setCourseCoverUrl(csid.getCoverUrl());
                lessonVO.setSections(csid.getSectionNum());
            }
        }
        return PageDTO.of(page,returnVoResult);
    }

    @Override
    public void addUserLesson(Long userId, List<Long> courseIds) {
        //1.调用课程微服务 批量查询课程的信息
        List<CourseSimpleInfoDTO> infoList
                = courseClient.getSimpleInfoList(courseIds);
        //2.封装po实体 填充过期的时间
        ArrayList<LearningLesson> batchLesson = new ArrayList<>();
        for (CourseSimpleInfoDTO dto : infoList) {
            LearningLesson lesson = new LearningLesson();
            lesson.setUserId(userId);
            lesson.setCourseId(dto.getId());
            //课程的有效期+当前的时间
            Integer validDuration = dto.getValidDuration(); //单位是月
            if (validDuration != null){
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime expireTime = now.plusMonths(validDuration);
                lesson.setExpireTime(expireTime);
                lesson.setCreateTime(now);
            }
            batchLesson.add(lesson);
        }
        //3.批量的保存
        this.saveBatch(batchLesson);
    }

    @Override
    public LearningLessonVO getMyCurrentLesson() {
        //1.获取当前的登录的用户
        Long userId = UserContext.getUser();
        if (userId == null){
            throw new BadRequestException("用户未登录");
        }
        //2.查询当前用户的最近的学习课程 按照 latest_learning_time降序的第一条 而且状态还是学习中
        LearningLesson result = this.lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getStatus, LessonStatus.LEARNING)
                .orderByDesc(LearningLesson::getLatestLearnTime)
                .last("limit 1")
                .one();
        if (result == null){
            return null;
        }
        //返回的结果对象
        LearningLessonVO returnVo = BeanUtils.copyBean(result, LearningLessonVO.class);
        //3.远程调用课程微服务获取名称等信息
        CourseFullInfoDTO info = courseClient.getCourseInfoById(result.getCourseId(),false,false);
        String courseName = info.getName();
        String coverUrl = info.getCoverUrl();
        Integer sections = info.getSectionNum();
        //3.1查询当前登录人已经报名总的课程数
        Integer total = this.lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                /*.eq(LearningLesson::getStatus, LessonStatus.LEARNING)*/ //已经报名的 而不是正在学习的
                .count();
        //3.2远程调用章节服务 根据id获取最近学习小节的名称和标号
        Long latestSectionId = result.getLatestSectionId(); //最近学习的小节的id
        List<CataSimpleInfoDTO> section =
                catalogueClient.batchQueryCatalogue(CollUtils.singletonList(latestSectionId));

        if (!CollUtil.isEmpty(section)){
            returnVo.setLatestSectionName(section.get(0).getName());
            returnVo.setLatestSectionIndex(section.get(0).getCIndex());
        }
        //4.返回

        returnVo.setCourseName(courseName);
        returnVo.setCourseCoverUrl(coverUrl);
        returnVo.setSections(sections);
        return returnVo;
    }

    @Override
    public Long isLessonValid(Long courseId) {
        //1.获取当前的用户
        Long userId = UserContext.getUser();
        if (userId == null){
            throw new BadRequestException("用户未登录");
        }
        //2.查询课程是否存在 并且状态是正常的
        LearningLesson result = this.lambdaQuery()
                .eq(LearningLesson::getCourseId, courseId)
                .eq(LearningLesson::getUserId, userId)
                .one();
        if (result == null){
            return null;
        }
        LocalDateTime expireTime = result.getExpireTime();
        LocalDateTime nowTime = LocalDateTime.now();
        if (expireTime != null && nowTime.isAfter(expireTime)){
            //课程过期
            return null;
        }
        return result.getId();
    }

    @Override
    public void removeLesson(Long userId, List<Long> courseIds) {
        LambdaQueryChainWrapper<LearningLesson> wrapper = this.lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .in(LearningLesson::getCourseId, courseIds);
        this.remove(wrapper);
    }

    @Override
    public LearningLessonVO getLessonStatus(Long courseId) {
        //1.获取当前的用户
        Long userId = UserContext.getUser();
        if (userId == null){
            throw new BadRequestException("当前用户未登录");
        }
        //2.查询
        LearningLesson lesson = this.getOne(this.lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getCourseId, courseId));
        if (lesson == null){
            //说明没有购买 加入到我的课程中
            return null;
        }
        LearningLessonVO result = BeanUtil.toBean(lesson, LearningLessonVO.class);
        result.setLatestSectionIndex(null);
        result.setSections(null);
        result.setCourseName(null);
        result.setCourseCoverUrl(null);
        result.setCourseAmount(null);
        result.setWeekFreq(null);
        return result;
    }

    @Override
    public Integer countLearningLessonByCourse(Long courseId) {
        Integer nums = this.lambdaQuery()
                .eq(LearningLesson::getCourseId, courseId)
                .count();
        return nums;
    }

    @Override
    public void createLearningPlan(LearningPlanDTO dto) {
        //1.获取当前登录人
        Long userId = UserContext.getUser();
        if (userId == null){
            throw new BadRequestException("当前用户未登录");
        }
        //2.查询
        LearningLesson lesson = this.lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getCourseId, dto.getCourseId())
                .one();
        if (lesson == null){
            throw new BizIllegalException("该课程未加入课表");
        }
        // 3.修改
        /*lesson.setWeekFreq(dto.getFreq());
        this.updateById(lesson); */
        this.lambdaUpdate()
                .set(LearningLesson::getWeekFreq,dto.getFreq())
                .set(LearningLesson::getPlanStatus, PlanStatus.PLAN_RUNNING)
                .eq(LearningLesson::getId,lesson.getId())
                .update();
    }

    @Override
    public LearningPlanPageVO queryMyPlans(PageQuery query) {
        //1.获取当前登录用户
        Long userId = UserContext.getUser();
        if (userId == null){
            throw new BadRequestException("用户未登录");
        }
        //todo:2.查询积分
        //3.查询本周学习计划总数据 和 已经学习的计划总数据
        QueryWrapper<LearningLesson> wrapper = new QueryWrapper<>();
        wrapper.select("sum(week_freq) as plansTotal");
        wrapper.eq("user_id",userId);
        wrapper.in("status",LessonStatus.LEARNING,LessonStatus.NOT_BEGIN);
        wrapper.eq("plan_status",PlanStatus.PLAN_RUNNING);
        Map<String, Object> map = this.getMap(wrapper);
        //本周学习计划的总数 plansTotal
        Integer plansTotal = Integer.valueOf(map.get("plansTotal").toString());
        //4.查询课表数据

        //5.远程调用课程服务获取课程的信息

        //6.查询学习记录表 本周 当前用户 每一门的已经学习的小节的数量
    }


}
