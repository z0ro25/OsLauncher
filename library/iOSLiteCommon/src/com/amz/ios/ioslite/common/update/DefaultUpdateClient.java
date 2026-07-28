package com.amz.ios.ioslite.common.update;

import android.content.Context;

public class DefaultUpdateClient extends BaseUpdateClient {

    public DefaultUpdateClient(Context context) {
        super(context);
    }

    @Override
    public void updateApp() {
    }

    @Override
    protected void doCheckTask() {
    }

    @Override
    protected long getCheckIntervalMillis() {
        return 0;
    }
}
