package com.dodola.watchdogkiller;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class MainActivity extends Activity {
    private TextView mStatusView;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        resetWatchDogStatus();
        findViewById(R.id.kill_watchdog).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WatchDogKiller.stopWatchDog();
                // 触发生效  TimeoutException 异常
                Runtime.getRuntime().gc();
                System.runFinalization();
                resetWatchDogStatus();
            }
        });
        findViewById(R.id.fire_timeout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 因为 stopWatchDog需要下一次循环才会生效，这里先post一下
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                fireTimeout();
                                Runtime.getRuntime().gc();
                                System.runFinalization();
                            }
                        }).start();
                    }
                }, 100);

                Toast.makeText(MainActivity.this, "请等待。。。。", Toast.LENGTH_SHORT).show();
            }
        });
        //hook click
        Button btn_hook_click = findViewById(R.id.btn_hook_click);
        findViewById(R.id.btn_hook_click).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "onClick");
            }
        });
        hookOnClickListener(btn_hook_click);

        //hook toast  值出现在7.0上
        //UI线程发生阻塞，导致TN.show()没有及时执行，当NotificationManager的检测超时后便会删除WMS中的该token，即造成token失效。
        showToast(btn_hook_click);
    }

    private void fireTimeout() {
        GhostObject object = new GhostObject();
    }

    private void resetWatchDogStatus() {
        boolean alive = WatchDogKiller.checkWatchDogAlive();
        mStatusView = findViewById(R.id.watch_status);
        mStatusView.setText(alive ? "ON" : "OFF");
    }

    private void hookOnClickListener(View view) {
        try {
            // 得到 View 的 ListenerInfo 对象
            Method getListenerInfo = View.class.getDeclaredMethod("getListenerInfo");
            getListenerInfo.setAccessible(true);
            Object listenerInfo = getListenerInfo.invoke(view);
            // 得到 原始的 OnClickListener 对象
            Class<?> listenerInfoClz = Class.forName("android.view.View$ListenerInfo");
            Field mOnClickListener = listenerInfoClz.getDeclaredField("mOnClickListener");
            mOnClickListener.setAccessible(true);
            View.OnClickListener originOnClickListener = (View.OnClickListener) mOnClickListener.get(listenerInfo);
            // 用自定义的 OnClickListener 替换原始的 OnClickListener
            View.OnClickListener hookedOnClickListener = new HookedOnClickListener(originOnClickListener);
            mOnClickListener.set(listenerInfo, hookedOnClickListener);
        } catch (Exception e) {
            Log.i(TAG, "lickListener failed!", e);
        }
    }

    class HookedOnClickListener implements View.OnClickListener {
        private View.OnClickListener origin;

        HookedOnClickListener(View.OnClickListener origin) {
            this.origin = origin;
        }

        @Override
        public void onClick(View v) {
            Toast.makeText(MainActivity.this, "hook click", Toast.LENGTH_SHORT).show();
            Log.i(TAG, "Before click, do what you want to to.");
            if (origin != null) {
                origin.onClick(v);
            }
            Log.i(TAG, "After click, do what you want to to.");
        }
    }

    public void showToast(View view) {
        ToastUtils.showToast(this, "hello", Toast.LENGTH_LONG);
//        Toast.makeText(this,"hello", Toast.LENGTH_LONG);
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
        }
    }
}
