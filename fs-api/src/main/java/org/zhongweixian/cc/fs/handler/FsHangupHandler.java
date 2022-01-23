package org.zhongweixian.cc.fs.handler;

import com.alibaba.fastjson.JSON;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.cti.cc.constant.Constant;
import org.cti.cc.entity.CallDevice;
import org.cti.cc.entity.CallLog;
import org.cti.cc.entity.PushFailLog;
import org.cti.cc.enums.CallType;
import org.cti.cc.enums.CauseEnums;
import org.cti.cc.enums.Direction;
import org.cti.cc.enums.NextType;
import org.cti.cc.po.*;
import org.cti.cc.util.DateTimeUtil;
import org.cti.cc.request.AgentPreset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;
import org.zhongweixian.cc.configration.HandlerType;
import org.zhongweixian.cc.fs.event.FsHangupEvent;
import org.zhongweixian.cc.fs.handler.base.BaseEventHandler;
import org.zhongweixian.cc.websocket.response.WsCallAfterEntity;
import org.zhongweixian.cc.websocket.response.WsResponseEntity;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by caoliang on 2020/8/23
 * <p>
 * 设备挂断处理类
 */
@Component
@HandlerType("CHANNEL_HANGUP")
public class FsHangupHandler extends BaseEventHandler<FsHangupEvent> {
    private Logger logger = LoggerFactory.getLogger(FsHangupHandler.class);

    private static final String YYYYMMDDHH = "yyyyMMddHH";


    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private MinioClient minioClient;

    @Override
    public void handleEvent(FsHangupEvent event) {
        CallInfo callInfo = cacheService.getCallInfo(event.getDeviceId());
        if (callInfo == null) {
            return;
        }
        Integer count = callInfo.getDeviceList().size();
        callInfo.getDeviceList().remove(event.getDeviceId());
        String cause = event.getHangupCause();

        /**
         * 挂机原因
         */
        DeviceInfo deviceInfo = callInfo.getDeviceInfoMap().get(event.getDeviceId());
        if (deviceInfo == null) {
            logger.warn("device:{} is null", event.getDeviceId());
            return;
        }
        logger.info("callId:{}, deviceId:{}, deviceType:{} called:{} hangup sipStatus:{} hangupCause:{}", callInfo.getCallId(), event.getDeviceId(), deviceInfo.getDeviceType(), event.getCalled(), event.getSipStatus(), event.getHangupCause());
        /**
         * 上传录音
         */
        if (StringUtils.isNotBlank(deviceInfo.getRecord())) {
            try {
                String day = DateFormatUtils.format(new Date(), YYYYMMDDHH);
                String fileName = day.substring(0, 8) + Constant.SK + day.substring(8, 10) + Constant.SK + callInfo.getCallId() + Constant.UNDER_LINE + deviceInfo.getDeviceId() + Constant.POINT + recordFile;
                ResponseEntity<byte[]> responseEntity = restTemplate.getForEntity(Constant.HTTP + event.getLocalMediaIp() + deviceInfo.getRecord(), byte[].class);
                logger.info("get record file:{}", deviceInfo.getRecord());
                ObjectWriteResponse writeResponse = minioClient.putObject(PutObjectArgs.builder().stream(new ByteArrayInputStream(responseEntity.getBody()), responseEntity.getBody().length, -1).object(fileName).bucket(bucket).build());
                logger.info("callId:{}, record fileName:{}, minioTag:{}", deviceInfo.getCallId(), fileName, writeResponse.etag());
                deviceInfo.setRecord(fileName);
                if (StringUtils.isBlank(callInfo.getRecord())) {
                    callInfo.setRecord(fileName);
                }
            } catch (Exception e) {
                logger.info(e.getMessage(), e);
                callInfo.setRecord(deviceInfo.getRecord());
            }
        }

        deviceInfo.setHangupCause(event.getHangupCause());
        deviceInfo.setSipProtocol(event.getSipProtocol());
        deviceInfo.setSipStatus(event.getSipStatus());
        deviceInfo.setChannelName(event.getChannelName());
        deviceInfo.setEndTime(event.getTimestamp() / 1000);
        if (deviceInfo.getAnswerTime() != null) {
            deviceInfo.setTalkTime(deviceInfo.getEndTime() - deviceInfo.getAnswerTime());
        }
        if (deviceInfo.getAgentKey() == null) {
            deviceInfo.setAgentKey(callInfo.getAgentKey());
        }
        CallDevice callDevice = new CallDevice();
        BeanUtils.copyProperties(deviceInfo, callDevice);
        callDevice.setCts(deviceInfo.getCallTime() / 1000);
        callDevice.setUts(callDevice.getEndTime() / 1000);
        callInfo.setEndTime(callDevice.getEndTime());

        if (callInfo.getHiddenCustomer() == 1) {
            //隐藏客户侧号码
            if (callInfo.getDirection() == Direction.INBOUND && deviceInfo.getDeviceType() == 2) {
                callDevice.setCaller(hiddenNumber(deviceInfo.getCaller()));
                callDevice.setChannelName(event.getChannelName().replace(deviceInfo.getCaller(), callDevice.getCaller()));
            } else if (callInfo.getDirection() == Direction.OUTBOUND && deviceInfo.getDeviceType() == 2) {
                callDevice.setCalled(hiddenNumber(deviceInfo.getCalled()));
                callDevice.setChannelName(event.getChannelName().replace(deviceInfo.getCalled(), callDevice.getCalled()));
            }
            if (callInfo.getDirection() == Direction.INBOUND && deviceInfo.getDeviceType() != 2) {
                callDevice.setDisplay(hiddenNumber(callDevice.getDisplay()));
            }
        }
        callCdrService.saveCallDevice(callDevice);
        if (deviceInfo.getDeviceType() == 1) {
            //同步坐席状态
            agentState(deviceInfo, callInfo, cause);
        }
        NextCommand nextCommand = callInfo.getNextCommands().size() == 0 ? null : callInfo.getNextCommands().get(0);
        if (!CauseEnums.NORMAL_CLEARING.name().equals(cause)) {
            //非正常挂机处理
            hangupDir(callInfo, deviceInfo, cause);
            if (nextCommand != null) {
                callInfo.getNextCommands().remove(nextCommand);
                //呼入电话转接坐席，坐席未接起需要重新进入技能组
                if (nextCommand.getNextType() == NextType.NEXT_CALL_BRIDGE && deviceInfo.getDeviceType() == 1 && callInfo.getDirection() == Direction.INBOUND) {
                    //重新进入技能组
                    String deviceId = nextCommand.getNextValue();
                    GroupInfo groupInfo = cacheService.getGroupInfo(callInfo.getGroupId());
                    groupHandler.hander(callInfo, groupInfo, deviceId);
                    return;
                }
                if (StringUtils.isNotBlank(nextCommand.getNextValue())) {
                    hangupCall(callInfo.getMedia(), callInfo.getCallId(), nextCommand.getNextValue());
                } else {
                    if (count == 1) {
                        hangup(callInfo);
                        return;
                    }
                }
            }
            return;
        }
        if (nextCommand != null) {
            nextCmd(callInfo, deviceInfo, nextCommand, cause);
            return;
        }
        //判断挂机方向
        hangupDir(callInfo, deviceInfo, cause);
        if (count == 1) {
            hangup(callInfo);
            return;
        }
        DeviceInfo nextDevice = callInfo.getDeviceInfoMap().get(event.getDeviceId());
        if (nextDevice.getAnswerTime() == null) {
            nextDevice.setRingEndTime(event.getTimestamp() / 1000);
        }
        if (!CollectionUtils.isEmpty(callInfo.getDeviceList())) {
            hangupCall(callInfo.getMedia(), callInfo.getCallId(), callInfo.getDeviceList().get(0));
        }
    }

    /**
     * 全部挂机
     *
     * @param callInfo
     */
    private void hangup(CallInfo callInfo) {
        //计算用户等待总时长
        if (callInfo.getCallType() == CallType.INBOUND_CALL || callInfo.getCallType() == CallType.AUTO_CALL) {
            if (callInfo.getQueueStartTime() != null) {
                if (callInfo.getQueueEndTime() == null) {
                    callInfo.setQueueEndTime(callInfo.getEndTime());
                }
                callInfo.setWaitTime(callInfo.getQueueEndTime() - callInfo.getQueueStartTime());
            }
        }

        CallLog callLog = new CallLog();
        BeanUtils.copyProperties(callInfo, callLog);
        callLog.setCts(callInfo.getCallTime() / 1000);
        callLog.setUts(callInfo.getEndTime() / 1000);
        callLog.setEndTime(callInfo.getEndTime());
        callLog.setMonthTime(DateTimeUtil.getNowMonth());

        callLog.setCallType(callInfo.getCallType().name());
        callLog.setDirection(callInfo.getDirection().name());
        if (callInfo.getFollowData() != null) {
            callLog.setFollowData(JSON.toJSONString(callInfo.getFollowData()));
        }
        if (callInfo.getAnswerTime() == null) {
            //未接通
            callInfo.setTalkTime(0L);
        } else {
            callLog.setTalkTime(callInfo.getEndTime() - callInfo.getAnswerTime());
            callInfo.setTalkTime(callInfo.getTalkTime());
            callInfo.setAnswerFlag(0);
        }
        if (callInfo.getHiddenCustomer() == 1) {
            //隐藏客户侧号码
            if (callInfo.getDirection() == Direction.INBOUND) {
                callLog.setCaller(hiddenNumber(callInfo.getCaller()));
            } else if (callInfo.getDirection() == Direction.OUTBOUND) {
                callLog.setCalled(hiddenNumber(callInfo.getCalled()));
            }
        }
        callCdrService.saveCallDetail(callInfo.getCallDetails());
        callCdrService.saveOrUpdateCallLog(callLog);

        //清空电话信息
        cacheService.removeCallInfo(callInfo.getCallId());
        if (StringUtils.isBlank(callInfo.getCdrNotifyUrl())) {
            return;
        }
        //话单推送
        CallLogPo callLogPo = new CallLogPo();
        BeanUtils.copyProperties(callLog, callLogPo);
        List<CallDevice> callDeviceList = new ArrayList<>();

        callInfo.getDeviceInfoMap().forEach((k, v) -> {
            CallDevice callDevice = new CallDevice();
            BeanUtils.copyProperties(v, callDevice);
            callDeviceList.add(callDevice);
        });
        if (StringUtils.isNotBlank(media)) {
            callLogPo.setMedia(media);
        }
        callLogPo.setCallDeviceList(callDeviceList);
        callLogPo.setCaller(callInfo.getCaller());
        callLogPo.setCalled(callInfo.getCalled());
        ResponseEntity<String> responseEntity = null;
        String payload = JSON.toJSONString(callLogPo);
        PushFailLog pushFailLog = new PushFailLog();
        pushFailLog.setCallId(callLogPo.getCallId());
        pushFailLog.setCts(Instant.now().getEpochSecond());
        pushFailLog.setUts(Instant.now().getEpochSecond());
        pushFailLog.setCompanyId(callLogPo.getCompanyId());
        pushFailLog.setPushTimes(1);
        pushFailLog.setCdrNotifyUrl(callInfo.getCdrNotifyUrl());
        pushFailLog.setContent(payload);
        try {
            logger.info("send push data:{}", payload);
            responseEntity = restTemplate.postForEntity(callInfo.getCdrNotifyUrl(), payload, String.class);
            logger.info("push call:{} to {} success:{}", callInfo.getCallId(), callInfo.getCdrNotifyUrl(), responseEntity.getBody());
            pushFailLog.setStatus(2);
            pushFailLog.setPushResponse(responseEntity.getBody());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            pushFailLog.setStatus(1);
            logger.warn("push call:{} to {} error", callInfo.getCallId(), callInfo.getCdrNotifyUrl());
        }
        callCdrService.savePushFailLog(pushFailLog);
    }


    /**
     * 判断挂机方向
     *
     * @param callInfo
     * @param deviceInfo
     */
    private void hangupDir(CallInfo callInfo, DeviceInfo deviceInfo, String cause) {
        if (callInfo.getHangupDir() != null) {
            return;
        }

        /**
         * 判断挂机方向
         * 1:主叫挂机，2:被叫挂机
         */
        if (callInfo.getDirection() == Direction.OUTBOUND) {
            if (deviceInfo.getDeviceType() == 1) {
                if (deviceInfo.getAnswerTime() == null) {
                    callInfo.setAnswerFlag(1);
                }
                //主叫挂机
                callInfo.setHangupDir(1);
            } else if (deviceInfo.getDeviceType() == 2) {
                if (deviceInfo.getAnswerTime() == null) {
                    callInfo.setAnswerFlag(2);
                }
                callInfo.setHangupDir(2);
            }

        } else if (callInfo.getDirection() == Direction.INBOUND) {
            if (deviceInfo.getDeviceType() == 2) {
                //主叫挂机
                callInfo.setHangupDir(1);
            } else {
                if (deviceInfo.getAnswerTime() == null) {
                    callInfo.setAnswerFlag(3);
                }
                callInfo.setHangupDir(2);
            }
        }
        cacheService.addCallInfo(callInfo);
        logger.info("callId:{} direction:{} hangupDir:{}, cause:{}", callInfo.getCallId(), callInfo.getDirection(), callInfo.getHangupDir(), cause);
    }


    /**
     * 同步坐席状态
     *
     * @param deviceInfo
     * @param callInfo
     */
    private void agentState(DeviceInfo deviceInfo, CallInfo callInfo, String cause) {
        AgentInfo agentInfo = cacheService.getAgentInfo(deviceInfo.getAgentKey());
        if (agentInfo == null) {
            return;
        }
        WsCallAfterEntity afterEntity = new WsCallAfterEntity();
        BeanUtils.copyProperties(callInfo, afterEntity);
        if (callInfo.getHiddenCustomer() == 1) {
            //隐藏客户侧号码
            if (callInfo.getDirection() == Direction.INBOUND) {
                afterEntity.setCaller(hiddenNumber(callInfo.getCaller()));
            } else if (callInfo.getDirection() == Direction.OUTBOUND) {
                afterEntity.setCalled(hiddenNumber(callInfo.getCalled()));
            }
        }
        agentInfo.setBeforeTime(agentInfo.getStateTime());
        agentInfo.setBeforeState(agentInfo.getAgentState());
        agentInfo.setStateTime(Instant.now().toEpochMilli());
        agentInfo.setAgentState(AgentState.AFTER);
        sendAgentStateMessage(agentInfo, new WsResponseEntity<WsCallAfterEntity>(AgentState.AFTER.name(), agentInfo.getAgentKey(), afterEntity));
        agentInfo.setCallId(null);
        agentInfo.setDeviceId(null);

        /**
         * 接口设置了预设状态
         */
        if (agentInfo.getAgentPreset() == null || agentInfo.getAgentPreset().getAgentState() == null) {
            return;
        }
        AgentPreset agentPreset = agentInfo.getAgentPreset();
        agentInfo.setBeforeTime(agentInfo.getStateTime());
        agentInfo.setBeforeState(agentInfo.getAgentState());
        agentInfo.setStateTime(Instant.now().toEpochMilli());
        if (agentPreset.getAgentState() == AgentState.READY) {
            agentInfo.setAgentState(AgentState.READY);
        } else if (agentPreset.getAgentState() == AgentState.NOT_READY) {
            agentInfo.setAgentState(AgentState.NOT_READY);
        }
        sendAgentStateMessage(agentInfo, new WsResponseEntity<String>(agentPreset.getAgentState().name(), agentInfo.getAgentKey(), null));
    }

    /**
     * 根据当前挂机事件执行下一步
     *
     * @param callInfo
     * @param deviceInfo
     * @param cause
     */
    private void nextCmd(CallInfo callInfo, DeviceInfo deviceInfo, NextCommand nextCommand, String cause) {
        /**
         * 呼入转到坐席，坐席拒接和坐席sip呼不通的时候，都需要再次转回来到技能组排队。
         */
        switch (nextCommand.getNextType()) {
            case NEXT_CALL_BRIDGE:
                //桥接
                CauseEnums causeEnums = CauseEnums.valueOf(cause);
                if (causeEnums != null && causeEnums != CauseEnums.NORMAL_CLEARING) {
                    //重新进入技能组
                    String deviceId = nextCommand.getNextValue();
                    GroupInfo groupInfo = cacheService.getGroupInfo(callInfo.getGroupId());
                    groupHandler.hander(callInfo, groupInfo, deviceId);
                }
                break;
            case NEXT_TRANSFER_SUCCESS:
                //完成转接
                logger.info("callId:{}, deviceId:{} NEXT_TRANSFER_SUCCESS", callInfo.getCallId(), deviceInfo.getDeviceId());
                String[] values = nextCommand.getNextValue().split(":");
                if (values == null || values.length != 2) {
                    return;
                }
                callBridge(callInfo.getMedia(), values[0], values[1]);
                break;
            case NEXT_TRANSFER_BRIDGE:
                //转接后桥接
                logger.info("callId:{}, deviceId:{} NEXT_TRANSFER_BRIDGE", callInfo.getCallId(), deviceInfo.getDeviceId());
                break;
            default:
                break;
        }
        callInfo.getNextCommands().remove(nextCommand);
        cacheService.addCallInfo(callInfo);
        return;
    }
}
