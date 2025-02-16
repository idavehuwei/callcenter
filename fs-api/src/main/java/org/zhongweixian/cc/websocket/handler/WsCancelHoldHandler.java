package org.zhongweixian.cc.websocket.handler;

import org.cti.cc.po.AgentInfo;
import org.cti.cc.po.CallInfo;
import org.springframework.stereotype.Component;
import org.zhongweixian.cc.configration.HandlerType;
import org.zhongweixian.cc.websocket.event.WsCancelHoldEvent;
import org.zhongweixian.cc.websocket.handler.base.WsBaseHandler;

/**
 * Created by caoliang on 2021/7/16
 * <p>
 * 取消保持
 */
@Component
@HandlerType("WS_CANCEL_HOLD")
public class WsCancelHoldHandler extends WsBaseHandler<WsCancelHoldEvent> {

    @Override
    public void handleEvent(WsCancelHoldEvent event) {
        AgentInfo agentInfo = getAgent(event);
        if (agentInfo == null || agentInfo.getCallId() == null) {

            return;
        }
        String deviceId = agentInfo.getDeviceId();
        CallInfo callInfo = cacheService.getCallInfo(agentInfo.getCallId());
        if (deviceId == null || callInfo == null) {
            return;
        }
        callBridge(callInfo.getMedia(), callInfo.getDeviceList().get(0), callInfo.getDeviceList().get(1));
    }
}
