package org.zhongweixian.cc.fs.handler;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.cti.cc.entity.CallLog;
import org.cti.cc.enums.CallType;
import org.cti.cc.enums.Direction;
import org.cti.cc.po.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.zhongweixian.cc.configration.HandlerType;
import org.zhongweixian.cc.fs.event.FsBridgeEvent;
import org.zhongweixian.cc.fs.handler.base.BaseEventHandler;
import org.zhongweixian.cc.websocket.response.WsCallEntity;
import org.zhongweixian.cc.websocket.response.WsResponseEntity;

import java.time.Instant;
import java.util.Date;

/**
 * Created by caoliang on 2020/8/23
 */
@Component
@HandlerType("CHANNEL_BRIDGE")
public class FsBridgeHandler extends BaseEventHandler<FsBridgeEvent> {
    private Logger logger = LoggerFactory.getLogger(FsBridgeHandler.class);

    @Override
    public void handleEvent(FsBridgeEvent event) {
        CallInfo callInfo = cacheService.getCallInfo(event.getDeviceId());
        if (callInfo == null) {
            return;
        }
        if (callInfo.getAnswerTime() == null || callInfo.getAnswerTime() == 0L) {
            callInfo.setAnswerTime(event.getTimestamp() / 1000);
            CallLog callLog = new CallLog();
            BeanUtils.copyProperties(callInfo, callLog);
            callLog.setDirection(callInfo.getDirection().name());
            callLog.setCts(callInfo.getCallTime());
            if (callInfo.getHiddenCustomer() == 1) {
                //隐藏客户侧号码
                if (Direction.INBOUND == callInfo.getDirection()) {
                    callLog.setCaller(hiddenNumber(callInfo.getCaller()));
                } else if (Direction.OUTBOUND == callInfo.getDirection()) {
                    callLog.setCalled(hiddenNumber(callInfo.getCalled()));
                }
            }
            callCdrService.saveOrUpdateCallLog(callLog);
            cacheService.addCallInfo(callInfo);
        }
        logger.info("桥接成功 callId:{}, device:{}, otherDevice:{}", callInfo.getCallId(), event.getDeviceId(), event.getOtherUniqueId());
        DeviceInfo deviceInfo1 = callInfo.getDeviceInfoMap().get(event.getDeviceId());
        DeviceInfo deviceInfo2 = callInfo.getDeviceInfoMap().get(event.getOtherUniqueId());
        if (deviceInfo1 != null && deviceInfo1.getBridgeTime() == null) {
            deviceInfo1.setBridgeTime(event.getTimestamp() / 1000);
        }
        if (deviceInfo2 != null && deviceInfo2.getBridgeTime() == null) {
            deviceInfo2.setBridgeTime(event.getTimestamp() / 1000);
        }
        if (!callInfo.getAnswerTime().equals(event.getTimestamp() / 1000)) {

        }

        GroupInfo groupInfo = cacheService.getGroupInfo(callInfo.getGroupId());
        if (groupInfo.getRecordType() > 0) {
            /**
             * 手动外呼：接通录音时在此录音
             * 呼入: 在此录音
             */
            if (callInfo.getCallType() == CallType.OUTBOUNT_CALL && groupInfo.getRecordType() == 2) {
                String record = recordPath +
                        DateFormatUtils.format(new Date(), "yyyyMMdd") + "/" + callInfo.getCallId() + "_" + callInfo.getCaller() + "_" + callInfo.getCalled() + "." + recordFile;
                super.record(event.getHostname(), callInfo.getCallId(), callInfo.getDeviceList().get(0), record);
                deviceInfo1.setRecord(record);
            } else if (callInfo.getCallType() == CallType.INBOUND_CALL) {
                String record = recordPath +
                        DateFormatUtils.format(new Date(), "yyyyMMdd") + "/" + callInfo.getCallId() + "_" + callInfo.getCaller() + "_" + callInfo.getCalled() + "." + recordFile;
                super.record(event.getHostname(), callInfo.getCallId(), event.getDeviceId(), record);
                deviceInfo1.setRecord(record);
            }
        }

        if (StringUtils.isBlank(callInfo.getAgentKey())) {
            return;
        }

        /**
         * 发送ws消息
         */
        WsCallEntity ringEntity = new WsCallEntity();
        ringEntity.setCallId(callInfo.getCallId());
        ringEntity.setCallType(callInfo.getCallType());
        ringEntity.setAgentState(AgentState.TALKING);
        ringEntity.setCaller(callInfo.getCaller());
        ringEntity.setCalled(callInfo.getCalled());
        AgentInfo agentInfo = cacheService.getAgentInfo(deviceInfo1.getAgentKey());
        if (agentInfo == null) {
            cacheService.getAgentInfo(deviceInfo2.getAgentKey());
        }
        if (agentInfo.getHiddenCustomer() == 1) {
            if (callInfo.getDirection() == Direction.OUTBOUND) {
                ringEntity.setCalled(hiddenNumber(callInfo.getCalled()));
            } else if (callInfo.getDirection() == Direction.INBOUND) {
                ringEntity.setCaller(hiddenNumber(callInfo.getCaller()));
            }
        }
        agentInfo.setBeforeState(agentInfo.getAgentState());
        agentInfo.setBeforeTime(agentInfo.getStateTime());
        agentInfo.setStateTime(Instant.now().toEpochMilli());
        agentInfo.setAgentState(AgentState.TALKING);
        sendAgentStateMessage(agentInfo, new WsResponseEntity<WsCallEntity>(AgentState.TALKING.name(), agentInfo.getAgentKey(), ringEntity));
    }
}
