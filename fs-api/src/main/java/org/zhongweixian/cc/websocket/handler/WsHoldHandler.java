package org.zhongweixian.cc.websocket.handler;

import org.cti.cc.po.AgentInfo;
import org.cti.cc.po.AgentState;
import org.cti.cc.po.CallInfo;
import org.springframework.stereotype.Component;
import org.zhongweixian.cc.configration.HandlerType;
import org.zhongweixian.cc.websocket.event.WsHoldEvent;
import org.zhongweixian.cc.websocket.handler.base.WsBaseHandler;
import org.zhongweixian.cc.websocket.response.WsResponseEntity;

/**
 * Created by caoliang on 2021/7/19
 * <p>
 * 呼叫保持:坐席听不到用户声音，用户听到的是保持音
 */
@Component
@HandlerType("WS_HOLD")
public class WsHoldHandler extends WsBaseHandler<WsHoldEvent> {
    @Override
    public void handleEvent(WsHoldEvent event) {
        AgentInfo agentInfo = getAgent(event);
        if (agentInfo == null || agentInfo.getCallId() == null) {

            return;
        }
        String deviceId = agentInfo.getDeviceId();
        CallInfo callInfo = cacheService.getCallInfo(agentInfo.getCallId());
        if (deviceId == null || callInfo == null) {

            return;
        }
        this.hold(callInfo.getMedia(), callInfo.getCallId(), deviceId);
        agentInfo.setAgentState(AgentState.HOLD);
        sendMessgae(event, new WsResponseEntity<>(event.getCmd(), event.getAgentKey()));
        callInfo.getDeviceInfoMap().get(deviceId).setState(AgentState.HOLD.name());
    }

}
