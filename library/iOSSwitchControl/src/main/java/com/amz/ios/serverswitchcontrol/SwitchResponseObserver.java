package com.ezt.ios.serverswitchcontrol;

import com.ezt.ios.serverswitchcontrol.bean.response.SwitchResponseBean;

import java.util.HashMap;
import java.util.List;

public interface SwitchResponseObserver {
    void onSwitchCallback(HashMap<String,SwitchResponseBean.ServerResponseBean> response);
    List<String> onRequestKey();
}
