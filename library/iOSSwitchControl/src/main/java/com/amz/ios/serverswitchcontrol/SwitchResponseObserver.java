package com.amz.ios.serverswitchcontrol;

import com.amz.ios.serverswitchcontrol.bean.response.SwitchResponseBean;

import java.util.HashMap;
import java.util.List;

public interface SwitchResponseObserver {
    void onSwitchCallback(HashMap<String,SwitchResponseBean.ServerResponseBean> response);
    List<String> onRequestKey();
}
