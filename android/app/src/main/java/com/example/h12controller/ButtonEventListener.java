package com.example.h12controller;

/**
 * 按钮事件监听器接口
 * 用于监听 RC 遥控器左右按钮的按下事件
 */
public interface ButtonEventListener {
    /**
     * 左按钮按下时调用
     */
    void onLeftButtonPressed();
    
    /**
     * 右按钮按下时调用
     */
    void onRightButtonPressed();
}

