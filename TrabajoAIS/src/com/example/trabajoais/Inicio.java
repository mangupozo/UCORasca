package com.example.trabajoais;

import java.io.File;
import java.util.ArrayList;
import java.util.Vector;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Camera.Size;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.Toast;

public class Inicio extends Activity implements OnTouchListener, Runnable {

	// UI attributes	
	public static ImageView drawingArea;
	
	// Layer attributes
	private ArrayList<Bitmap> layers; // Pila de Bitmaps que seran las capas de la app siendo la ultima la resolucion
	private int numLayers;
	private int[][] logicLayer;
	private int limitX;
	private int limitY;
	private int logicX;
	private int logicY;
	
	// Scratch attributes
	private int[] bufferPixels;
	public static int pixelGray;
	private Timer timer;
	
	//public static Bitmap imageToErase;
	//public static Bitmap imageBackground;
	//private int stride= 30;
	private int stride= 16;
	

	/**
	 * Method which calculates the divisors of a number
	 * 
	 * @param n: Number to calculate its divisors
	 * 
	 * @return div: Vector containing all integer divisors of the number
	 * 
	 * @date 19/03/2013
	 * @version 1.0
	 * @author Alberto Jimenez Lopez
	 */
	public static Vector<Integer> dividers(int n) {
		Vector<Integer> div = new Vector<Integer>();

		for (int i = 1; i <= n; i++) {
			if (n % i == 0)
				div.addElement(i);
		}

		return div;
	}

	/**
	 * Method which calculates the minimum Common divisors from two arrays of dividers
	 * 
	 * @param a: Vector of integers containing the dividers of first number
	 * @param b: Vector of integers containing the dividers of second number
	 * 
	 * @return min: Integer containing the minimum common divider
	 * 
	 * @date 19/03/2013
	 * @version 1.0
	 * @author Alberto Jimenez Lopez
	 */
	public static int minimumCommonDivider(Vector<Integer> a, Vector<Integer> b) {
		int min = 0;

		if (a.size() < b.size()) {
			for (int i = 0; i < a.size(); i++) {
				for (int j = 0; j < b.size(); j++) {
					if (a.elementAt(i) == b.elementAt(j)) {
						min = a.elementAt(i);
						break;
					}
				}
			}
		} else {
			for (int i = 0; i < b.size(); i++) {
				for (int j = 0; j < a.size(); j++) {
					if (b.elementAt(i) == a.elementAt(j)) {
						min = b.elementAt(i);
						break;
					}
				}
			}
		}

		return min;
	}
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		
		requestWindowFeature(Window.FEATURE_NO_TITLE); // FullScreen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_inicio);
		
		// Get the reference to the UI elements
		drawingArea= (ImageView)findViewById(R.id.viewDraw);
		drawingArea.post(this);
		
		// Initialize all the attributes of the layers
		layers= new ArrayList<Bitmap>();
		numLayers= getIntent().getExtras().getInt(MainActivity.N_LAYERS_KEY);
		Log.d("NUM LAYER", String.valueOf(numLayers));
	}
	
	
	protected void onDestroy()
	{
		super.onDestroy();
		
		// Destroy all the used bitmaps
		for(int i=0; i<layers.size(); ++i)
			layers.get(i).recycle();
		
		// Clean the ArrayList object
		//layers.clear();
		
		// Check if the timer is still working to stop it
		//f( timer.hasMessages(1) )
		//	timer.removeMessages(1);
	}

	/*@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.inicio, menu);
		return true;
	}*/
	
	public void initialize(boolean releaseMemory)
	{
		// Check if it is necessary to release memory
		if( releaseMemory )
		{
			// Destroy all the layers
			for(int i=0; i<layers.size(); ++i)
				layers.get(i).recycle();
			layers.clear();
		}
		
		// Create the stack of layers requested
		for(int i=0; i<numLayers; ++i)
		{
			layers.add(Bitmap.createBitmap(drawingArea.getWidth(), drawingArea.getHeight(), Bitmap.Config.RGB_565));
			if( i%2 == 0)
				layers.get(i).eraseColor(Color.GRAY);
			else
				layers.get(i).eraseColor(Color.CYAN);
		}
		
		// Get the image which will be shown after the scratched
		layers.add( BitmapFactory.decodeResource(getResources(), R.drawable.ganar) );
		layers.set(layers.size()-1, Bitmap.createScaledBitmap(layers.get(layers.size()-1), drawingArea.getWidth(), drawingArea.getHeight(), false));
		
		// Set the first grey image to scratch
		drawingArea.setImageBitmap(layers.get(0));
		drawingArea.setOnTouchListener(this);
		
		// Initialize the logical matrix which it tells us the order of the layers
		limitX= drawingArea.getWidth()/stride;
		limitY= drawingArea.getHeight()/stride;
		//Log.d("LOGIC LIMIT", "[" + limitX + ", " + limitY + "]");
		
		logicLayer= new int[limitX][limitY];
		for(int i=0; i<limitX; ++i)
			for(int j=0; j<limitY; ++j)
				logicLayer[i][j]= 1;
		
		// Create the buffer which it will be used to do the scratch effect
		bufferPixels= new int[drawingArea.getWidth() * drawingArea.getHeight()];
	}
	
	public void scratch(int x, int y, int radius)
	{
		// Get the logic coordinates 
		logicX= x/stride;
		logicY= y/stride;
		
		
		if( logicLayer[logicX][logicY] != layers.size() )
		{
			layers.get(logicLayer[logicX][logicY]).getPixels(bufferPixels, 0, layers.get(0).getWidth(), logicX*stride, logicY*stride, stride, stride);
			layers.get(0).setPixels(bufferPixels, 0, layers.get(0).getWidth(), logicX*stride, logicY*stride, stride, stride);
			drawingArea.setImageBitmap(layers.get(0));
			
			logicLayer[logicX][logicY]+=1;
		}
		
		// Generate the indicated scratch effect
		/*for(int i=logicX-radius; i<logicX+radius; ++i)
			for(int j=logicY-radius; j<logicY+radius; ++j)
			{
				try
				{
					if( logicLayer[i][j] != layers.size() )
					{
						layers.get(logicLayer[i][j]).getPixels(bufferPixels, 0, layers.get(0).getWidth(), i*stride, j*stride, stride, stride);
						layers.get(0).setPixels(bufferPixels, 0, layers.get(0).getWidth(), i*stride, j*stride, stride, stride);
						drawingArea.setImageBitmap(layers.get(0));
						
						logicLayer[i][j]+=1;
					}
				}catch(Exception e){}
			}*/
		
		//Log.d("Logic XY", "[" + (int)event.getX() + ", " + (int)event.getY() + "]" + " -> " +  "[" + logicX + ", " + logicY + "]");
	}

	@Override
	public boolean onTouch(View v, MotionEvent event)
	{
		// Check if there is only one finger
		if( event.getPointerCount() == 1 )
		{
			// Check the correct action
			if( (event.getAction() == MotionEvent.ACTION_MOVE) /*|| (event.getAction() == MotionEvent.ACTION_DOWN)*/ )
			{	
				scratch((int)event.getX(), (int)event.getY(), 2);
				
				/*
				// Make the scratch effect 
				try
				{
					layers.get(logicLayer[(int)event.getX()][(int)event.getY()]).getPixels(bufferPixels, 0, layers.get(logicLayer[(int)event.getX()][(int)event.getY()]).getWidth(), (int)event.getX(), (int)event.getY(), stride, stride);
					layers.get(0).setPixels(bufferPixels, 0, layers.get(0).getWidth(), (int)event.getX(), (int)event.getY(), stride, stride);
					drawingArea.setImageBitmap(layers.get(0));
					
					//if( (logicLayer[(int)event.getX()][(int)event.getY()] + 1) < numLayers )
					for(int i=(int)event.getX(); i<(int)event.getX()+stride; ++i)
						for(int j=(int)event.getX(); j<(int)event.getX()+stride; ++j)
							if( logicLayer[i][j] < numLayers )
								logicLayer[i][j]= logicLayer[i][j] + 1;
					
					Log.d("LAYER", String.valueOf(logicLayer[(int)event.getX()][(int)event.getY()]));
					
					
					//imageBackground.getPixels(pixels, 0, imageBackground.getWidth(), (int)event.getX(), (int)event.getY(), stride, stride);				
					//imageToErase.setPixels(pixels, 0, imageToErase.getWidth(), (int)event.getX(), (int)event.getY(), stride, stride);				
					//drawingArea.setImageBitmap(imageToErase);
					
				}catch(Exception ex){}*/
			}
		}
			
		return true;
				
				
				
	}

	@Override
	public void run() {

		Log.d("IMAGEVIEW", "[" + drawingArea.getWidth() + ", " + drawingArea.getHeight() + "]");
		
		initialize(false);
		
		// Create the stack of layers
		/*for(int i=0; i<numLayers; ++i)
		{
			layers.add(Bitmap.createBitmap(drawingArea.getWidth(), drawingArea.getHeight(), Bitmap.Config.RGB_565));
			if( i%2 == 0)
				layers.get(i).eraseColor(Color.GRAY);
			else
				layers.get(i).eraseColor(Color.CYAN);
		}
		
		// Initialize the logical matrix which it tells us the order of the layers
		limitX= drawingArea.getWidth()/stride;
		limitY= drawingArea.getHeight()/stride;
		Log.d("LOGIC LIMIT", "[" + limitX + ", " + limitY + "]");
		
		logicLayer= new int[limitX][limitY];
		for(int i=0; i<limitX; ++i)
			for(int j=0; j<limitY; ++j)
				logicLayer[i][j]= 1;
		
		// Get the image which will be shown after the scratched image
		layers.add( BitmapFactory.decodeResource(getResources(), R.drawable.ganar) );
		layers.set(layers.size()-1, Bitmap.createScaledBitmap(layers.get(layers.size()-1), drawingArea.getWidth(), drawingArea.getHeight(), false));
		
		// Create the buffer which it will be used to do the scratch effect
		bufferPixels= new int[drawingArea.getWidth() * drawingArea.getHeight()];*/
		
		// Initialize the timer
		//timer= new Timer();
		//timer.sendEmptyMessageDelayed(1, 2000);
	}
	
	
	static class Timer extends Handler
	{
		private float totalPixel;
		private float count;
		private float percent;
		
		public Timer()
		{
			//totalPixel= Inicio.imageToErase.getWidth() * Inicio.imageToErase.getHeight();
		}
		
		public void handleMessage(Message msg)
		{
			super.handleMessage(msg);
			
			count= 0;
			//for(int i=0; i<Inicio.imageToErase.getWidth(); ++i)
			//	for(int j=0; j<Inicio.imageToErase.getHeight(); ++j)
				//	if( Inicio.imageToErase.getPixel(i, j) == Inicio.pixelGray )
					//	++count;
			
			percent= (count/totalPixel)*100;
			
			if( (percent > 0) && (percent <= 50) )
			{
				// Release the background image
				//Inicio.drawingArea.setImageBitmap(imageBackground);
				Inicio.drawingArea.setOnTouchListener(null);
			}
			else
				this.sendEmptyMessageDelayed(1, 2000);
		}
	};
}
