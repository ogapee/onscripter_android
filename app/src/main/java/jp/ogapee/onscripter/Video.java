package jp.ogapee.onscripter;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGL11;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import android.widget.TextView;


class DemoRenderer extends GLSurfaceView_SDL.Renderer {

	public DemoRenderer(Activity _context)
	{
		context = _context;
		int n = 3;
		if (ONScripter.gRenderFontOutline) n++;
		String[] arg = new String[n];
		n = 0;
		arg[n++] = "--open-only";
		if (ONScripter.gRenderFontOutline) arg[n++] = "--render-font-outline";
		arg[n++] = "--font";
		arg[n++] = ONScripter.gFontPath;
		nativeInit(ONScripter.gCurrentDirectoryPath, arg);
	}
	
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		// nativeInit();
	}

	public void onSurfaceChanged(GL10 gl, int w, int h) {
		//gl.glViewport(0, 0, w, h);
		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glLoadIdentity();
		gl.glViewport(0, 0, w, h);
		gl.glOrthof(0.0f, (float) w, (float) h, 0.0f, 0.0f, 1.0f);
		nativeResize(w, h);
	}

	public void onDrawFrame(GL10 gl) {

		nativeInitJavaCallbacks();

		// Calls main() and never returns, hehe - we'll call eglSwapBuffers() from native code
		int n = 2;
		if (ONScripter.gRenderFontOutline) n++;
		String[] arg = new String[n];
		n = 0;
		if (ONScripter.gRenderFontOutline) arg[n++] = "--render-font-outline";
		arg[n++] = "--font";
		arg[n++] = ONScripter.gFontPath;
		nativeInit(ONScripter.gCurrentDirectoryPath, arg);

	}

	public int swapBuffers() // Called from native code, returns 1 on success, 0 when GL context lost (user put app to background)
	{
		return super.SwapBuffers() ? 1 : 0;
	}

	public void exitApp() {
		 nativeDone();
	};

	private native void nativeInitJavaCallbacks();
	private native void nativeInit(String currentDirectoryPath, String[] arg);
	private native void nativeResize(int w, int h);
	private native void nativeDone();

	private Activity context = null;
	
	private EGL10 mEgl = null;
	private EGLDisplay mEglDisplay = null;
	private EGLSurface mEglSurface = null;
	private EGLContext mEglContext = null;
	private int skipFrames = 0;
}

class DemoGLSurfaceView extends GLSurfaceView_SDL {
	public DemoGLSurfaceView(Activity context) {
		super(context);
		mRenderer = new DemoRenderer(context);
		setRenderer(mRenderer);
	}

	public void exitApp() {
		mRenderer.exitApp();
	};

	@Override
	public void onPause() {
		super.onPause();
		surfaceDestroyed(this.getHolder());
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public void onStop()
	{
		super.onStop();
	}

	DemoRenderer mRenderer;
}
