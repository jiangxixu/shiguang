package io.github.jiangxixu.shiguang;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;

/** User-enabled, movable capture control. Never reads clipboard in the background. */
public class FloatingCaptureService extends Service {
    private static final String CHANNEL="floating-capture";
    private WindowManager manager;
    private WindowManager.LayoutParams params;
    private TextView ball;
    private boolean docked,right,dragged,longPressed;
    private float downX,downY;
    private int startX,startY,touchSlop;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable longPress=()->{longPressed=true;Toast.makeText(this,"悬浮收藏已关闭，可在「我」中重新开启",Toast.LENGTH_LONG).show();stopSelf();};

    @Override public void onCreate(){
        super.onCreate();manager=(WindowManager)getSystemService(WINDOW_SERVICE);
        touchSlop=ViewConfiguration.get(this).getScaledTouchSlop();
    }
    @Override public int onStartCommand(Intent intent,int flags,int id){
        if(intent!=null&&"stop".equals(intent.getAction())){stopSelf();return START_NOT_STICKY;}
        if(!Settings.canDrawOverlays(this)){stopSelf();return START_NOT_STICKY;}
        try{
            NotificationManager notifications=getSystemService(NotificationManager.class);
            notifications.createNotificationChannel(new NotificationChannel(CHANNEL,"悬浮收藏",NotificationManager.IMPORTANCE_LOW));
            PendingIntent open=PendingIntent.getActivity(this,20,new Intent(this,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            PendingIntent stop=PendingIntent.getService(this,21,new Intent(this,FloatingCaptureService.class).setAction("stop"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            Notification notification=new Notification.Builder(this,CHANNEL).setSmallIcon(R.drawable.ic_capture_tile).setContentTitle("拾光悬浮收藏已开启")
                .setContentText("复制链接后点悬浮球；拖到边缘收起，长按关闭")
                .setContentIntent(open).setOngoing(true).addAction(new Notification.Action.Builder(null,"关闭悬浮球",stop).build()).build();
            if(Build.VERSION.SDK_INT>=34)startForeground(154,notification,ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            else startForeground(154,notification);
            if(ball==null)showBall();
        }catch(Exception e){Toast.makeText(this,"无法显示悬浮球，请检查「显示在其他应用上层」权限",Toast.LENGTH_LONG).show();stopSelf();}
        return START_NOT_STICKY;
    }
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private int screenWidth(){return getResources().getDisplayMetrics().widthPixels;}
    private int screenHeight(){return getResources().getDisplayMetrics().heightPixels;}
    private void showBall(){
        android.content.SharedPreferences prefs=getSharedPreferences("floating",MODE_PRIVATE);
        docked=prefs.getBoolean("docked",false);right=prefs.getBoolean("right",true);
        params=new WindowManager.LayoutParams(dp(docked?18:44),dp(44),WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,PixelFormat.TRANSLUCENT);
        params.gravity=Gravity.TOP|Gravity.LEFT;
        params.x=docked?(right?screenWidth()-params.width:0):OverlayGeometry.clamp(prefs.getInt("x",screenWidth()-dp(60)),screenWidth()-params.width);
        params.y=OverlayGeometry.clamp(prefs.getInt("y",screenHeight()/2),screenHeight()-dp(100));
        ball=new TextView(this);ball.setGravity(Gravity.CENTER);ball.setTextSize(19);ball.setTextColor(Color.WHITE);ball.setElevation(dp(3));
        style();ball.setOnTouchListener(this::touch);
        ball.setOnClickListener(v->{
            if(docked){expand();return;}
            try{startActivity(new Intent(this,ClipboardCaptureActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}
            catch(Exception e){Toast.makeText(this,"请打开拾光后重试收藏",Toast.LENGTH_SHORT).show();}
        });
        manager.addView(ball,params);
        prefs.edit().putBoolean("enabled",true).apply();
    }
    private void style(){
        GradientDrawable shape=new GradientDrawable();shape.setColor(Color.rgb(57,91,67));shape.setCornerRadius(dp(docked?9:22));
        ball.setBackground(shape);ball.setAlpha(docked?.62f:.94f);ball.setText(docked?(right?"‹":"›"):"拾");
        ball.setContentDescription(docked?"展开拾光收藏，可拖动，长按关闭":"收藏复制的链接，可拖到边缘收起，长按关闭");
    }
    private boolean touch(View view,MotionEvent event){
        switch(event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:
                downX=event.getRawX();downY=event.getRawY();startX=params.x;startY=params.y;dragged=false;longPressed=false;
                handler.postDelayed(longPress,900);return true;
            case MotionEvent.ACTION_MOVE:
                int dx=Math.round(event.getRawX()-downX),dy=Math.round(event.getRawY()-downY);
                if(Math.abs(dx)>touchSlop||Math.abs(dy)>touchSlop){dragged=true;handler.removeCallbacks(longPress);}
                if(dragged&&!longPressed){params.x=OverlayGeometry.clamp(startX+dx,screenWidth()-params.width);params.y=OverlayGeometry.clamp(startY+dy,screenHeight()-dp(100));update();}return true;
            case MotionEvent.ACTION_UP:
                handler.removeCallbacks(longPress);
                if(longPressed)return true;
                if(!dragged)view.performClick();else release();return true;
            case MotionEvent.ACTION_CANCEL:
                handler.removeCallbacks(longPress);if(dragged&&!longPressed)release();return true;
            default:return false;
        }
    }
    private void release(){
        int edge=OverlayGeometry.edge(params.x,screenWidth(),params.width,dp(24));
        if(edge!=0){right=edge>0;docked=true;params.width=dp(18);params.x=right?screenWidth()-params.width:0;}
        else{docked=false;params.width=dp(44);params.x=OverlayGeometry.clamp(params.x,screenWidth()-params.width);}
        style();update();remember();
    }
    private void expand(){docked=false;params.width=dp(44);params.x=OverlayGeometry.expandedX(right,screenWidth(),params.width,dp(32));style();update();remember();}
    private void remember(){getSharedPreferences("floating",MODE_PRIVATE).edit().putInt("x",params.x).putInt("y",params.y).putBoolean("docked",docked).putBoolean("right",right).apply();}
    private void update(){try{if(ball!=null&&ball.isAttachedToWindow())manager.updateViewLayout(ball,params);}catch(Exception e){stopSelf();}}
    @Override public void onConfigurationChanged(Configuration config){super.onConfigurationChanged(config);if(ball!=null){params.x=docked?(right?screenWidth()-params.width:0):OverlayGeometry.clamp(params.x,screenWidth()-params.width);params.y=OverlayGeometry.clamp(params.y,screenHeight()-dp(100));update();remember();}}
    @Override public void onDestroy(){handler.removeCallbacksAndMessages(null);if(ball!=null){try{manager.removeView(ball);}catch(Exception ignored){}ball=null;}getSharedPreferences("floating",MODE_PRIVATE).edit().putBoolean("enabled",false).apply();stopForeground(STOP_FOREGROUND_REMOVE);super.onDestroy();}
    @Override public IBinder onBind(Intent intent){return null;}
}
