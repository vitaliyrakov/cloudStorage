package handler;

import model.Message;

@FunctionalInterface
public interface CallBack {

    void call(Message arg);
}
