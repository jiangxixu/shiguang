package io.github.jiangxixu.shiguang;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public class CaptureTileService extends TileService {
    @Override public void onStartListening(){super.onStartListening();Tile tile=getQsTile();if(tile!=null){tile.setLabel("拾光收藏");tile.setState(Tile.STATE_INACTIVE);tile.updateTile();}}
    @Override public void onClick(){super.onClick();if(isLocked())unlockAndRun(this::openCapture);else openCapture();}
    private void openCapture(){
        Intent intent=new Intent(this,ClipboardCaptureActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if(Build.VERSION.SDK_INT>=34)startActivityAndCollapse(PendingIntent.getActivity(this,0,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
        else startActivityAndCollapse(intent);
    }
}
