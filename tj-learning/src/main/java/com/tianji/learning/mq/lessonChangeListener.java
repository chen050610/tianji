package com.tianji.learning.mq;

import cn.hutool.core.collection.CollUtil;
import com.tianji.api.dto.trade.OrderBasicDTO;
import com.tianji.common.constants.Constant;
import com.tianji.common.constants.MqConstants;
import com.tianji.learning.service.ILearningLessonService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author chenw
 * @version 1.0
 * @description: TODO
 * @date 2024/10/20 下午3:00
 */

@Component
@Slf4j
public class lessonChangeListener {
    @Resource
    ILearningLessonService lessonService;

    /**
     * @description:  课程支付成功以后 trade服务远程mq通知 将课程加入课表
     * @param:  
     * @return: void 
     * @author chenw
     * @date: 2024/10/20 下午3:01
     */ 
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "learning.lesson.pay.queue",durable = "true"),
            exchange = @Exchange(value = MqConstants.Exchange.ORDER_EXCHANGE),
            key = MqConstants.Key.ORDER_PAY_KEY
    ))
    public void onMessage(OrderBasicDTO dto){
        log.info("lessonChangeListener监听到消息---->"+dto);
        //1.参数的校验
        if (CollUtil.isEmpty(dto.getCourseIds())
        ||dto.getUserId() == null
        ||dto.getOrderId() == null
        ){
            return;
        }
        lessonService.addUserLesson(dto.getUserId(),dto.getCourseIds());
    }
}
