package jp.ogapee.onscripter;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.media.AudioTrack;
import android.media.AudioManager;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.os.Build;
import java.io.*;
import java.nio.ByteBuffer;
import android.util.Log;
import java.lang.Thread;


class AudioThread {

	private Activity mParent;
	private AudioTrack mAudio;
	private byte[] mAudioBuffer;

	public AudioThread(Activity parent)
	{
		mParent = parent;
		mAudio = null;
		mAudioBuffer = null;
		nativeAudioInitJavaCallbacks();
	}
	
	public int fillBuffer()
	{
		if (mAudio == null) return 1;
		while (mAudio.getPlayState() == mAudio.PLAYSTATE_PAUSED)
			try{
				Thread.currentThread().sleep(500);
			} catch(Exception e){};
		mAudio.write( mAudioBuffer, 0, mAudioBuffer.length );
		return 1;
	}
	
	public int initAudio(int rate, int channels, int encoding, int bufSize)
	{
		if( mAudio == null )
		{
			channels = ( channels == 1 ) ? AudioFormat.CHANNEL_OUT_MONO : 
											AudioFormat.CHANNEL_OUT_STEREO;
			encoding = ( encoding == 1 ) ? AudioFormat.ENCODING_PCM_16BIT :
											AudioFormat.ENCODING_PCM_8BIT;

			if ( AudioTrack.getMinBufferSize( rate, channels, encoding) > bufSize )
				bufSize = AudioTrack.getMinBufferSize( rate, channels, encoding );

			mAudioBuffer = new byte[bufSize];

			if (Build.VERSION.SDK_INT >= 23){
				mAudio = new AudioTrack.Builder()
					.setAudioAttributes(new AudioAttributes.Builder()
						.setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
					.setAudioFormat(new AudioFormat.Builder()
						.setEncoding(encoding).setSampleRate(rate).setChannelMask(channels).build())
					.setBufferSizeInBytes(bufSize).build();
			}
			else{
				mAudio = new AudioTrack(AudioManager.STREAM_MUSIC, 
										rate,
										channels,
										encoding,
										bufSize,
										AudioTrack.MODE_STREAM );
			}
			
			if (mAudio.getState() == AudioTrack.STATE_INITIALIZED)
				mAudio.play();
			else
				mAudio = null;
		}
		return mAudioBuffer.length;
	}
	
	public byte[] getBuffer()
	{
		return mAudioBuffer;
	}
	
	public int deinitAudio()
	{
		if( mAudio != null )
		{
			mAudio.stop();
			mAudio.release();
			mAudio = null;
		}
		mAudioBuffer = null;
		return 1;
	}
	
	public int initAudioThread()
	{
		// Make audio thread priority higher so audio thread won't get underrun
		Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
		return 1;
	}
	
	public void onPause() {
		if( mAudio != null )
			mAudio.pause();
	}

	public void onResume() {
		if( mAudio != null )
			mAudio.play();
	}

	private native int nativeAudioInitJavaCallbacks();
}

