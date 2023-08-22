package jp.ogapee.onscripter;

import java.io.File;
import java.io.FileFilter;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.HashMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.res.AssetManager;
import android.content.res.AssetFileDescriptor;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Build;
import android.content.Context;
import android.content.ContentResolver;
import android.graphics.Color;
import android.graphics.Point;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;
import android.view.Display;
import android.view.Menu;
import android.view.SubMenu;
import android.view.MenuItem;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.animation.AlphaAnimation;
import android.os.PowerManager;
import android.os.Environment;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.StrictMode;
import android.text.Html;
import android.widget.TextView;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.FrameLayout;
import android.widget.PopupMenu;
import android.media.AudioManager;
import android.util.Log;
import android.net.Uri;
import android.Manifest;

import android.provider.DocumentsContract;
import android.support.v4.provider.DocumentFile;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

public class ONScripter extends Activity implements AdapterView.OnItemClickListener
{
	// Launcher contributed by katane-san
	
	private File mCurrentDirectory = null;
	private File mOldCurrentDirectory = null;
	private File [] mDirectoryFiles = null;
	private ListView listView = null;
	private int num_file = 0;
	private byte[] buf = null;
	private int screen_w, screen_h;
	private int top_left_x, top_left_y;
	private Button[] btn = new Button[10];
	private FrameLayout frame_layout  = null;
	private LinearLayout layout1  = null;
	private LinearLayout layout1_1 = null;
	private LinearLayout layout1_2 = null;
	private LinearLayout layout2 = null;
	private LinearLayout layout2_1 = null;
	private LinearLayout layout2_2 = null;
	private int mButtonAlpha = 1;
	private static final int num_alpha = 4;
	private Uri ons_uri = null;
	private static final String system_default_font_name = "mplus-2m-medium.ttf";
	private static final String default_font_name = "default.ttf";
	private AssetManager as = null;
	private Map<String, Boolean> dir_map = null;

	static class FileSort implements Comparator<File>{
		public int compare(File src, File target){
			return src.getName().compareTo(target.getName());
		}
	}

	private void setupDirectorySelector() {
		mDirectoryFiles = mCurrentDirectory.listFiles(new FileFilter() {
			public boolean accept(File file) {
				return (!file.isHidden() && file.isDirectory());
			}
		});

		Arrays.sort(mDirectoryFiles, new FileSort());

		int length = mDirectoryFiles.length;
		if (mCurrentDirectory.getParent() != null) length++;
		String [] names = new String[length];

		int j=0;
		if (mCurrentDirectory.getParent() != null) names[j++] = "..";
		for (int i=0 ; i<mDirectoryFiles.length ; i++){
			names[j++] = mDirectoryFiles[i].getName();
		}

		ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, names);

		listView.setAdapter(arrayAdapter);
		listView.setOnItemClickListener(this);
	}

	private void runLauncher() {
		boolean intent_flag = false;

		if (Build.VERSION.SDK_INT >= 21){
			intent_flag = true;
			Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
			try{
				startActivityForResult(i, 1);
			} catch(Exception e){
				intent_flag = false;
			}
		}

		if (intent_flag == false){
			gCurrentDirectoryPath = Environment.getExternalStorageDirectory() + "/ons";
			mCurrentDirectory = new File(gCurrentDirectoryPath);
			if (mCurrentDirectory.exists() == false){
				gCurrentDirectoryPath = Environment.getExternalStorageDirectory().getPath();
				mCurrentDirectory = new File(gCurrentDirectoryPath);

				if (mCurrentDirectory.exists() == false)
					showErrorDialog("Could not find SD card.");
			}
		
			listView = new ListView(this);

			LinearLayout layoutH = new LinearLayout(this);

			checkRFO = new CheckBox(this);
			checkRFO.setText("Render Font Outline");
			checkRFO.setBackgroundColor(Color.rgb(244,244,255));
			checkRFO.setTextColor(Color.BLACK);
			checkRFO.setChecked(gRenderFontOutline);
			checkRFO.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked){
					Editor e = getSharedPreferences("pref", MODE_PRIVATE).edit();
					e.putBoolean("render_font_outline", isChecked);
					e.commit();
				}
			});

			layoutH.addView(checkRFO, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT, 1.0f));
			listView.addHeaderView(layoutH, null, false);
			setupDirectorySelector();
			setContentView(listView);
		}
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
		if (requestCode == 1 && resultCode == RESULT_OK){
			gCurrentDirectoryPath = null;
			ons_uri = resultData.getData();

			if (ons_uri.getAuthority().equals("com.android.externalstorage.documents")){
				String docId = DocumentsContract.getTreeDocumentId(ons_uri);
				ons_uri = DocumentsContract.buildChildDocumentsUriUsingTree(ons_uri, docId);
				String split[] = docId.split(":");
				if (split[0].equals("primary")){
					gCurrentDirectoryPath = Environment.getExternalStorageDirectory().getPath();
					if (split.length > 1) gCurrentDirectoryPath += "/" + split[1];
				}
				else{
					File[] dirs = getExternalFilesDirs(null);
					for (File dir : dirs){
						String path = dir.getPath().substring(0, dir.getPath().indexOf("/Android/data"));
						if (path.equals(Environment.getExternalStorageDirectory().getPath())) continue;
						if (split.length > 1) path += "/" + split[1];
						File file = new File(path);
						if (file.exists() == false) continue;
						gCurrentDirectoryPath = path;
						break;
					}
					if (gCurrentDirectoryPath == null){
						gCurrentDirectoryPath = Environment.getExternalStorageDirectory().getPath();
						if (split.length > 1) gCurrentDirectoryPath += "/" + split[1];
					}
				}
			}

			if (gCurrentDirectoryPath != null){
				mCurrentDirectory = new File(gCurrentDirectoryPath);
				if (checkGameData()){
					runSDLApp();
					return;
				}
			}
			runLauncher();
		}
	}

	public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
		position--; // for header

		TextView textView = (TextView)v;
		mOldCurrentDirectory = mCurrentDirectory;

		if (textView.getText().equals("..")){
			mCurrentDirectory = new File(mCurrentDirectory.getParent());
			gCurrentDirectoryPath = mCurrentDirectory.getPath();
		} else {
			if (mCurrentDirectory.getParent() != null) position--;
			gCurrentDirectoryPath = mDirectoryFiles[position].getPath();
			mCurrentDirectory = new File(gCurrentDirectoryPath);
		}

		if (checkGameData()){
			gRenderFontOutline = checkRFO.isChecked();
			runSDLApp();
		}
		else{
			mCurrentDirectory = mOldCurrentDirectory;
			setupDirectorySelector();
		}
	}

	public Uri findFile(Uri uri, String query_name, boolean is_dir)
	{
		ContentResolver resolver = this.getContentResolver();
		try {
			Cursor c = resolver.query(uri, new String[] {
						DocumentsContract.Document.COLUMN_DOCUMENT_ID,
						DocumentsContract.Document.COLUMN_DISPLAY_NAME }, null, null, null);
			while (c.moveToNext()){
				String document_id = c.getString(0);
				String display_name = c.getString(1);
				if (display_name.equalsIgnoreCase(query_name)){
					c.close();

					if (is_dir)
						return DocumentsContract.buildChildDocumentsUriUsingTree(uri, document_id);
					else
						return DocumentsContract.buildDocumentUriUsingTree(uri, document_id);
				}
			}
			c.close();
		} catch (Exception e) {
			Log.e("ONS", "findFile failed query: " + e);
		}

		return null;
	}

	public boolean checkGameData()
	{
		if (Build.VERSION.SDK_INT >= 21){
			String [] font_names = {"0.txt",
									"00.txt",
									"nscr_sec.dat",
									"nscript.___",
									"nscript.dat",
									"pscript.dat"};

			Uri uri = null;
			for (String font_name : font_names){
				uri = findFile(ons_uri, font_name, false);
				if (uri != null) break;
			}
			if (uri == null) return false;

			uri = findFile(ons_uri, default_font_name, false);
			if (uri != null) gFontPath = default_font_name;
		}
		else{
			mDirectoryFiles = mCurrentDirectory.listFiles(new FileFilter() {
				public boolean accept(File file) {
					return (file.isFile() && 
						(file.getName().equalsIgnoreCase("0.txt") ||
						file.getName().equalsIgnoreCase("00.txt") ||
						file.getName().equalsIgnoreCase("nscr_sec.dat") ||
						file.getName().equalsIgnoreCase("nscript.___") ||
						file.getName().equalsIgnoreCase("nscript.dat") ||
						file.getName().equalsIgnoreCase("pscript.dat")));
				}
			});

			if (mDirectoryFiles == null || mDirectoryFiles.length == 0) return false;

			mDirectoryFiles = mCurrentDirectory.listFiles(new FileFilter() {
				public boolean accept(File file) {
					return (file.isFile() && 
						(file.getName().equalsIgnoreCase(default_font_name)));
				}
			});

			if (mDirectoryFiles.length != 0)
				gFontPath = gCurrentDirectoryPath + "/" + default_font_name;
		}

		if (gFontPath.equals("")){
			alertDialogBuilder.setTitle(getString(R.string.app_name));
			alertDialogBuilder.setMessage(default_font_name + " is missing.");
			alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface dialog, int whichButton) {
					setResult(RESULT_OK);
				}
			});
			AlertDialog alertDialog = alertDialogBuilder.create();
			alertDialog.show();

			return false;
		}

		return true;
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		if (grantResults[0]== PackageManager.PERMISSION_GRANTED){
			//you have the permission now.
			runSub();
		}
		else{
			finish();
		}
	}
    
	private void copyDefaultFont()
	{
		try{
			gFontPath = getFilesDir() + "/" + default_font_name;
			
			File dst_file = new File(gFontPath);
			if (dst_file.exists()) return;

			InputStream is;
			try{
				is = as.open(default_font_name);
			} catch(Exception e){
				is = as.open(system_default_font_name);
			}

			BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(dst_file));
			int len = 0;
			do{
				len = is.read(buf);
				if (len > 0) os.write(buf, 0, len);
			} while (len >= 0);
			os.flush();
			os.close();
			is.close();
		}
		catch(Exception e){
			Log.e("ONS", "copyDefaultFont: " + e.toString());
			gFontPath = "";
		}
	}

	private void runSDLApp() {
		nativeInitJavaCallbacks();

		mAudioThread = new AudioThread(this);
		mGLView = new DemoGLSurfaceView(this);
		mGLView.setFocusableInTouchMode(true);
		mGLView.setFocusable(true);
		mGLView.requestFocus();

		int game_width  = nativeGetWidth();
		int game_height = nativeGetHeight();

		Display disp = ((WindowManager)this.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		int dw = disp.getWidth();
		int dh = disp.getHeight();

		if (Build.VERSION.SDK_INT >= 19){
			getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) ;
			Point p = new Point(0,0);
			disp.getRealSize(p);
			dw = p.x;
			dh = p.y;
		}

		frame_layout  = new FrameLayout(this);
		frame_layout.setBackgroundColor(Color.BLACK);

		// Main display
		layout1   = new LinearLayout(this);
		layout1_1 = new LinearLayout(this);
		layout1_2 = new LinearLayout(this);

		// Button
		layout2   = new LinearLayout(this);
		layout2_1 = new LinearLayout(this);
		layout2_2 = new LinearLayout(this);

		screen_w = dw;
		screen_h = dh;
		if (dw * game_height >= dh * game_width){
			screen_w = (dh*game_width/game_height) & (~0x01); // to be 2 bytes aligned
		}
		else{
			screen_h = dw*game_height/game_width;
			layout1.setOrientation(LinearLayout.VERTICAL);
		}

		for (int i=0 ; i<10 ; i++){
			btn[i] = new Button(this);
			if (i < 7){
				btn[i].setMinWidth(dw/6);
				btn[i].setMinHeight(dh/7);
				btn[i].setWidth(dw/6);
				btn[i].setHeight(dh/7);
				LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dw/6, dh/7);
				lp.setMargins(0, 0, 0, 0);
				btn[i].setLayoutParams(lp);
			}
			else{
				btn[i].setVisibility(View.INVISIBLE);
			}
		}
        
		btn[0].setText(getResources().getString(R.string.button_lclick));
		btn[0].setOnClickListener(new OnClickListener(){
			public void onClick(View v){
				nativeKey( KeyEvent.KEYCODE_ENTER, 1 );
				nativeKey( KeyEvent.KEYCODE_ENTER, 0 );
			}
		});

		btn[1].setText(getResources().getString(R.string.button_rclick));
		btn[1].setOnClickListener(new OnClickListener(){
			public void onClick(View v){
				nativeKey( KeyEvent.KEYCODE_BACK, 1 );
				nativeKey( KeyEvent.KEYCODE_BACK, 0 );
			}
		});

		btn[2].setText(getResources().getString(R.string.button_left));
		btn[2].setOnClickListener(new OnClickListener(){
			public void onClick(View v){
				nativeKey( KeyEvent.KEYCODE_DPAD_LEFT, 1 );
				nativeKey( KeyEvent.KEYCODE_DPAD_LEFT, 0 );
			}
		});

		btn[3].setText(getResources().getString(R.string.button_right));
		btn[3].setOnClickListener(new OnClickListener(){
			public void onClick(View v){
				nativeKey( KeyEvent.KEYCODE_DPAD_RIGHT, 1 );
				nativeKey( KeyEvent.KEYCODE_DPAD_RIGHT, 0 );
			}
		});

		btn[4].setText(getResources().getString(R.string.button_up));
		btn[4].setOnClickListener(new OnClickListener(){
			public void onClick(View v){
				nativeKey( KeyEvent.KEYCODE_DPAD_UP, 1 );
				nativeKey( KeyEvent.KEYCODE_DPAD_UP, 0 );
			}
		});

		btn[5].setText(getResources().getString(R.string.button_down));
		btn[5].setOnClickListener(new OnClickListener(){
			public void onClick(View v){
				nativeKey( KeyEvent.KEYCODE_DPAD_DOWN, 1 );
				nativeKey( KeyEvent.KEYCODE_DPAD_DOWN, 0 );
			}
		});

		btn[6].setText(getResources().getString(R.string.button_menu));
		btn[6].setOnClickListener(new OnClickListener(){
			public void onClick(View v){
				PopupMenu popup = new PopupMenu(getApplicationContext(), v);
				onPrepareOptionsMenu(popup.getMenu());
				popup.show();
				popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
					public boolean onMenuItemClick(MenuItem item) {
						return onOptionsItemSelected(item);
					}
				});
			}
		});

		layout1_1.addView(btn[7]);
		layout1_2.addView(btn[8]);
		top_left_x = (dw-screen_w)/2;
		top_left_y = (dh-screen_h)/2;
		layout1.addView(layout1_1, 0, new LinearLayout.LayoutParams(top_left_x, top_left_y));
		layout1.addView(mGLView, 1, new LinearLayout.LayoutParams(screen_w, screen_h));
		layout1.addView(layout1_2, 2);

		layout2_1.addView(btn[9]);
		for (int i=0 ; i<7 ; i++) layout2_2.addView(btn[i], i);
		layout2.addView(layout2_1, 0);
		layout2.addView(layout2_2, 1);
		layout2.setVisibility(View.INVISIBLE);

		resetLayout();

		frame_layout.addView(layout1);
		frame_layout.addView(layout2);

		setContentView(frame_layout);

		gd = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener(){
			public boolean onFling(MotionEvent e1, MotionEvent e2, float x, float y){
				float x2 = x*x, y2 = y*y;
				float thresh = 500.0f;
				if (x2+y2 < thresh*thresh) return false;

				AlphaAnimation fade;
				boolean change = true;
				if (button_state == 0){
					layout2.setVisibility(View.VISIBLE);
					if (Math.abs(x) > Math.abs(y)){
						if (x > 0.0) button_state = 1;
						else         button_state = 3;
					}
					else{
						if (y > 0.0) button_state = 2;
						else         button_state = 4;
					}
					fade = new AlphaAnimation(0, 1);
				}
				else{
					change = false;
					if (Math.abs(x) > Math.abs(y)){
						if (x > 0.0 && button_state == 3 ||
							x < 0.0 && button_state == 1)
								change = true;
					}
					else{
						if (y > 0.0 && button_state == 4 ||
							y < 0.0 && button_state == 2)
								change = true;
					}
					fade = new AlphaAnimation(1, 0);
					if (change) layout2.setVisibility(View.INVISIBLE);
				}

				if (change){
					fade.setDuration(200);
					layout2.startAnimation(fade);
				}
                
				resetLayout();
				return true;
			}
		});

		if (wakeLock == null){
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "ONScripter");
			wakeLock.acquire();
		}
	}

	public void resetLayout()
	{
		Display disp = ((WindowManager)this.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		int dw = disp.getWidth();
		int dh = disp.getHeight();

		if (Build.VERSION.SDK_INT >= 19){
			getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) ;
			Point p = new Point(0,0);
			disp.getRealSize(p);
			dw = p.x;
			dh = p.y;
		}

		int game_width  = nativeGetWidth();
		int game_height = nativeGetHeight();
		screen_w = dw;
		screen_h = dh;
		if (dw * game_height >= dh * game_width)
			screen_w = (dh*game_width/game_height) & (~0x01); // to be 2 bytes aligned
		else
			screen_h = dw*game_height/game_width;
        
		if (Build.VERSION.SDK_INT >= 11){
			for (int i=0 ; i<7 ; i++)
				btn[i].setAlpha((mButtonAlpha+1)*0.2f);
		}

		if (button_state == 1 || button_state == 3){
			layout2.setOrientation(LinearLayout.HORIZONTAL);
			layout2_2.setOrientation(LinearLayout.VERTICAL);
			btn[6].setVisibility(View.VISIBLE);
		}
		else{
			layout2.setOrientation(LinearLayout.VERTICAL);
			layout2_2.setOrientation(LinearLayout.HORIZONTAL);
			btn[6].setVisibility(View.INVISIBLE);
		}

		if (button_state == 1 || button_state == 2)
			layout2.updateViewLayout(layout2_1, new LinearLayout.LayoutParams(0,0));
		else if (button_state == 3)
			layout2.updateViewLayout(layout2_1, new LinearLayout.LayoutParams(dw-dw/6, dh));
		else if (button_state == 4)
			layout2.updateViewLayout(layout2_1, new LinearLayout.LayoutParams(dw, dh-dh/7));

		if (layout2.getVisibility() == View.INVISIBLE)
			button_state = 0;
	}

	public void playVideo(char[] filename){
		try{
			String filename2 = "file://" + gCurrentDirectoryPath + "/" + new String(filename);
			filename2 = filename2.replace('\\', '/');
			Log.v("ONS", "playVideo: " + filename2);
			Uri uri = Uri.parse(filename2);
			Intent i = new Intent(Intent.ACTION_VIEW);
			i.setDataAndType(uri, "video/*");
			StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
			StrictMode.VmPolicy policy = StrictMode.getVmPolicy();
			StrictMode.setVmPolicy(builder.build());
			startActivityForResult(i, -1);
			StrictMode.setVmPolicy(policy);
		}
		catch(Exception e){
			Log.e("ONS", "playVideo error:  " + e.getClass().getName());
		}
	}

	public long[] getFD(char[] filename, int mode){
		long[] ret = new long[3];
		ret[0] = ret[1] = ret[2] = -1;
		try{
			String path = new String(filename);
			String paths[] = path.split("\\/");
			if (Build.VERSION.SDK_INT >= 21 && ons_uri != null){ // SDK_INT >= 21 and use_launcher
				Uri uri = ons_uri;
				String sub_path = "";
				for (int i=0 ; i<paths.length-1 ; i++){
					sub_path += paths[i] + "\\";
					Boolean val = dir_map.get(sub_path);
					if (val != null && val.booleanValue() == false){
						return ret;
					}
					uri = findFile(uri, paths[i], true);
					if (uri == null){
						if (val == null)
							dir_map.put(sub_path, false);
						return ret;
					}
					else{
						if (val == null)
							dir_map.put(sub_path, true);
					}
				}

				Uri uri2 = findFile(uri, paths[paths.length-1], false);
				if (mode == 0){
					if (uri2 == null) return ret;
				}
				else{
					if (uri2 != null) DocumentsContract.deleteDocument(this.getContentResolver(), uri2);
					uri2 = DocumentsContract.createDocument(this.getContentResolver(), uri, "application/octet-stream", paths[paths.length-1]);
				}
				ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri2, mode==0?"r":"w");
				ret[0] = pfd.detachFd();
			}
			else{ // SDK_INT < 21 or not use_launcher
				String filename2 = gCurrentDirectoryPath + "/" + path;
				filename2 = filename2.replace('\\', '/');
				File file = new File(filename2);
				if (mode == 0){ // read
					if (file.exists()){
						ParcelFileDescriptor pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
						ret[0] = pfd.detachFd();
					}
					else{
						AssetFileDescriptor afd = as.openFd(path);
						ParcelFileDescriptor pfd = afd.getParcelFileDescriptor();
						ret[0] = pfd.detachFd();
						ret[1] = afd.getStartOffset();
						ret[2] = afd.getLength();
					}
				}
				else{ // write
					File parent_file = file.getParentFile();
					if (!parent_file.exists())
						parent_file.mkdirs();
					ParcelFileDescriptor pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_CREATE | ParcelFileDescriptor.MODE_WRITE_ONLY);
					ret[0] = pfd.detachFd();
				}
			}
		}
		catch(Exception e){
			//Log.e("ONS", "getFD error:  " + e);
		}

		return ret;
	}

	public int mkdir(char[] filename){
		try{
			if (Build.VERSION.SDK_INT >= 21 && ons_uri != null){
				String path = new String(filename);
				String paths[] = path.split("\\/");
				
				DocumentFile df = DocumentFile.fromTreeUri(this, ons_uri);
				String sub_path = "";
				for (int i=0 ; i<paths.length ; i++){
					sub_path += paths[i] + "\\";
					DocumentFile df2 = df.findFile(paths[i]);
					if (df2 == null){
						df2 = df.createDirectory(paths[i]);
						dir_map.put(sub_path, true);
					}
					df = df2;
				}
			}
			else{
				String filename2 = gCurrentDirectoryPath + "/" + new String(filename);
				filename2 = filename2.replace('\\', '/');
				File dir = new File(filename2);
				dir.mkdirs();
			}
		}
		catch(Exception e){
			Log.e("ONS", "mkdir: " + e.toString());
			return -1;
		}
		return 0;
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		// fullscreen mode
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
							WindowManager.LayoutParams.FLAG_FULLSCREEN); 

		alertDialogBuilder = new AlertDialog.Builder(this);

		SharedPreferences sp = getSharedPreferences("pref", MODE_PRIVATE);
		mButtonAlpha = sp.getInt("button_alpha", getResources().getInteger(R.integer.button_alpha));
		gRenderFontOutline = sp.getBoolean("render_font_outline", getResources().getBoolean(R.bool.render_font_outline));

		buf = new byte[8192*2];
		dir_map = new HashMap();

		if (Build.VERSION.SDK_INT >= 23 && Build.VERSION.SDK_INT < 33){
			if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
				ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
				ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
				return;
			}
		}
		
		runSub();
	}

	public void runSub()
	{
		as = getApplicationContext().getAssets();
		
		copyDefaultFont();
		
		if (getResources().getBoolean(R.bool.use_launcher)){
			runLauncher();
		}
		else{
			gCurrentDirectoryPath = getFilesDir().getPath();
			runSDLApp();
		}
	}

	@Override
	public boolean onTouchEvent(final MotionEvent event) 
	{
		// TODO: add multitouch support (added in Android 2.0 SDK)
		int action = -1;
		if( event.getAction() == MotionEvent.ACTION_DOWN )
			action = 0;
		if( event.getAction() == MotionEvent.ACTION_UP )
			action = 1;
		if( event.getAction() == MotionEvent.ACTION_MOVE )
			action = 2;

		if (gd != null && gd.onTouchEvent(event)) action = -1;
		
		if ( action >= 0 )
			nativeMouse( (int)event.getX()-top_left_x, (int)event.getY()-top_left_y, action );

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		if (mGLView != null){
			menu.clear();
			menu.add(Menu.NONE, Menu.FIRST,   0, getResources().getString(R.string.menu_automode));
			menu.add(Menu.NONE, Menu.FIRST+1, 0, getResources().getString(R.string.menu_skip));
			menu.add(Menu.NONE, Menu.FIRST+2, 0, getResources().getString(R.string.menu_speed));

			SubMenu sm = menu.addSubMenu(getResources().getString(R.string.menu_settings));
			SubMenu sm_alpha = sm.addSubMenu(getResources().getString(R.string.menu_button_alpha));
			for (int i=0 ; i<num_alpha ; i++){
				MenuItem mi = sm_alpha.add(1, Menu.FIRST+3+i, i, String.valueOf((i+1)*20)+"%");
				if (i == mButtonAlpha) mi.setChecked(true);
			}
			sm_alpha.setGroupCheckable(1, true, true);

			SubMenu sm_font = sm.addSubMenu(getResources().getString(R.string.menu_change_font_outline));
			MenuItem mif1 = sm_font.add(1, Menu.FIRST+num_alpha+3, 0, getResources().getString(R.string.menu_show_font_outline));
			MenuItem mif2 = sm_font.add(1, Menu.FIRST+num_alpha+4, 0, getResources().getString(R.string.menu_hide_font_outline));
			if (gRenderFontOutline) mif1.setChecked(true);
			else mif2.setChecked(true);
			sm_font.setGroupCheckable(1, true, true);

			menu.add(Menu.NONE, Menu.FIRST+num_alpha+5, 0, getResources().getString(R.string.menu_version));
			menu.add(Menu.NONE, Menu.FIRST+num_alpha+6, 0, getResources().getString(R.string.menu_privacy_policy));
			menu.add(Menu.NONE, Menu.FIRST+num_alpha+7, 0, getResources().getString(R.string.menu_quit));
		}

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if (item.getItemId() == Menu.FIRST){
			nativeKey( KeyEvent.KEYCODE_A, 1 );
			nativeKey( KeyEvent.KEYCODE_A, 0 );
		} else if (item.getItemId() == Menu.FIRST+1){
			nativeKey( KeyEvent.KEYCODE_S, 1 );
			nativeKey( KeyEvent.KEYCODE_S, 0 );
		} else if (item.getItemId() == Menu.FIRST+2){
			nativeKey( KeyEvent.KEYCODE_O, 1 );
			nativeKey( KeyEvent.KEYCODE_O, 0 );
		} else if (item.getItemId() >= Menu.FIRST+3 && item.getItemId() < Menu.FIRST+3+num_alpha){
			mButtonAlpha = item.getItemId() - (Menu.FIRST+3);
			item.setChecked(true);
			resetLayout();
		} else if (item.getItemId() == Menu.FIRST+num_alpha+3){
			gRenderFontOutline = true;
			item.setChecked(true);
		} else if (item.getItemId() == Menu.FIRST+num_alpha+4){
			gRenderFontOutline = false;
			item.setChecked(true);
		} else if (item.getItemId() == Menu.FIRST+num_alpha+5){
			alertDialogBuilder.setTitle(getResources().getString(R.string.menu_version));
			alertDialogBuilder.setMessage(getResources().getString(R.string.version));
			alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface dialog, int whichButton) {
					setResult(RESULT_OK);
				}
			});
			AlertDialog alertDialog = alertDialogBuilder.create();
			alertDialog.show();
		} else if (item.getItemId() == Menu.FIRST+num_alpha+6){
			alertDialogBuilder.setTitle(getResources().getString(R.string.menu_privacy_policy));
			alertDialogBuilder.setMessage(getResources().getString(R.string.privacy_policy_message) + " " + getResources().getString(R.string.privacy_policy_url));
			alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface dialog, int whichButton) {
					Intent i = new Intent(Intent.ACTION_VIEW);
					i.setData(Uri.parse(getResources().getString(R.string.privacy_policy_url)));
					startActivity(i);
					setResult(RESULT_OK);
				}
			});
			alertDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface dialog, int whichButton) {
					setResult(RESULT_OK);
				}
			});
			AlertDialog alertDialog = alertDialogBuilder.create();
			alertDialog.show();
		} else if (item.getItemId() == Menu.FIRST+num_alpha+7){
			nativeKey( KeyEvent.KEYCODE_MENU, 2 ); // send SDL_QUIT
		} else{
			return false;
		}

		Editor e = getSharedPreferences("pref", MODE_PRIVATE).edit();
		e.putInt("button_alpha", mButtonAlpha);
		e.putBoolean("render_font_outline", gRenderFontOutline);
		e.commit();

		return true;
	}

	@Override
	public boolean onKeyDown(int keyCode, final KeyEvent event)
	{
		if(keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN){
			AudioManager audio = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
			int volume = audio.getStreamVolume(AudioManager.STREAM_MUSIC) + (keyCode == KeyEvent.KEYCODE_VOLUME_UP ? 1 : (-1));
			if(volume >= 0 && volume <= audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)){
				audio.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_SHOW_UI);
			}
			return true;
		}

		if (keyCode == KeyEvent.KEYCODE_MENU){
			super.onKeyDown(keyCode, event);
			return false;
		}
		nativeKey( keyCode, 1 );

		return true;
	 }
	
	@Override
	public boolean onKeyUp(int keyCode, final KeyEvent event)
	{
		if(keyCode == KeyEvent.KEYCODE_MENU){
			super.onKeyUp(keyCode, event);
			return false;
		}
		nativeKey( keyCode, 0 );
		return true;
	}

	@Override
	protected void onPause()
	{
		nativeKey( 0, 3 ); // send SDL_ACTIVEEVENT
		// TODO: if application pauses it's screen is messed up
		if( wakeLock != null )
			wakeLock.release();
		super.onPause();
		if( mGLView != null )
			mGLView.onPause();
		if( mAudioThread != null )
			mAudioThread.onPause();
	}

	@Override
	protected void onResume()
	{
		if( wakeLock != null && !wakeLock.isHeld() )
			wakeLock.acquire();
		super.onResume();
		if( mGLView != null )
			mGLView.onResume();
		if( mAudioThread != null )
			mAudioThread.onResume();
		nativeKey( 0, 3 ); // send SDL_ACTIVEEVENT
	}

	@Override
	protected void onStop()
	{
		super.onStop();
		if( mGLView != null )
			mGLView.onStop();
	}

	@Override
	protected void onDestroy() 
	{
		if( mGLView != null )
			mGLView.exitApp();
		super.onDestroy();
	}

	private void showErrorDialog(String mes)
	{
		alertDialogBuilder.setTitle("Error");
		alertDialogBuilder.setMessage(mes);
		alertDialogBuilder.setPositiveButton("Quit", new DialogInterface.OnClickListener(){
			public void onClick(DialogInterface dialog, int whichButton) {
				finish();
			}
		});
		AlertDialog alertDialog = alertDialogBuilder.create();
		alertDialog.show();
	}

	private DemoGLSurfaceView mGLView = null;
	private AudioThread mAudioThread = null;
	private PowerManager.WakeLock wakeLock = null;
	public static String gCurrentDirectoryPath = null;
	public static boolean gRenderFontOutline;
	public static CheckBox checkRFO = null;
	public static String gFontPath = "";
	private native int nativeInitJavaCallbacks();
	private native int nativeGetWidth();
	private native int nativeGetHeight();
	private native void nativeMouse( int x, int y, int action );
	private native void nativeKey( int keyCode, int down );
	private AlertDialog.Builder alertDialogBuilder = null;
	private ProgressDialog progDialog = null;
	private GestureDetector gd;
	private int button_state = 0;
	
	static {
		System.loadLibrary("mad");
		System.loadLibrary("bz2");
		System.loadLibrary("ogg");
		System.loadLibrary("tremor");
		System.loadLibrary("lua");
		System.loadLibrary("sdl");
		System.loadLibrary("sdl_mixer");
		System.loadLibrary("sdl_image");
		System.loadLibrary("sdl_ttf");
		System.loadLibrary("smpeg");
		System.loadLibrary("application");
		System.loadLibrary("sdl_main");
	}
}
